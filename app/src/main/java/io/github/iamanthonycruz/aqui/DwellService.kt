package io.github.iamanthonycruz.aqui

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Gets a location fix about once a minute and feeds it to [StayTracker].
 * Runs as a foreground service: Android only allows location this often to an app that shows a
 * notification while it runs.
 */
class DwellService : Service() {

    private lateinit var worker: HandlerThread
    private lateinit var workerHandler: Handler

    /** When the last fix arrived (or when we last asked for fixes). Only touched on [worker]. */
    private var lastFixAt = 0L
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            // Several fixes can arrive in one batch after the phone slept. Handle them in order.
            result.locations.forEach { onFix(it) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("Starting…"), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        worker = HandlerThread("dwell").apply { start() }
        workerHandler = Handler(worker.looper)
        // onStartCommand always follows and asks for fixes.
        workerHandler.postDelayed(watchdog, WATCHDOG_EVERY_MILLIS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Prefs.isEnabled(this) || !Perms.hasAll(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Ask for fixes again on every start (opening the app, an update, a reboot). When Android
        // restarts the service in the background its first request can quietly get nothing, and
        // asking again with the same callback just replaces the old request.
        requestUpdates()
        if (Prefs.stay(this) == null) requestFirstFix()
        // If Android kills the service to save memory, start it again.
        return START_STICKY
    }

    override fun onDestroy() {
        workerHandler.removeCallbacksAndMessages(null)
        client.removeLocationUpdates(callback)
        worker.quitSafely()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission") // Checked in onStartCommand, and Start checks first.
    private fun requestUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, FIX_EVERY_MILLIS)
            .setMinUpdateIntervalMillis(FIX_EVERY_MILLIS / 2)
            .build()
        try {
            client.requestLocationUpdates(request, callback, worker.looper)
                .addOnFailureListener { e ->
                    // Without this the service runs, shows its notification, and never gets a fix.
                    Log.e(TAG, "Asking for location failed, trying again in a minute", e)
                    workerHandler.postDelayed({ requestUpdates() }, FIX_EVERY_MILLIS)
                }
            workerHandler.post { lastFixAt = System.currentTimeMillis() }
        } catch (e: SecurityException) {
            Log.e(TAG, "No location permission", e)
            stopSelf()
        }
    }

    /**
     * The regular updates can take a minute or two to deliver their first fix, which looks stuck.
     * Ask once for a fix now (a recent cached one is fine) and feed it through the same path.
     * Same priority as the updates; [onFix] still drops it if it's too fuzzy.
     */
    @SuppressLint("MissingPermission") // Checked in onStartCommand.
    private fun requestFirstFix() {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxUpdateAgeMillis(FirstFix.MAX_AGE_MILLIS)
            .setDurationMillis(FirstFix.TIMEOUT_MILLIS)
            .build()
        try {
            client.getCurrentLocation(request, null).addOnSuccessListener { loc ->
                if (loc == null) return@addOnSuccessListener
                workerHandler.post {
                    // A regular fix may have landed first; then this one is old news.
                    if (Prefs.stay(this) == null) onFix(loc)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "No location permission for the first fix", e)
        }
    }

    /** Runs on [worker], so the database and the dwell's address lookup are off the main thread. */
    private fun onFix(loc: Location) {
        lastFixAt = System.currentTimeMillis()
        val now = loc.time
        val accuracy = loc.takeIf { it.hasAccuracy() }?.accuracy
        val speed = loc.takeIf { it.hasSpeed() }?.speed

        // A fix fuzzier than the circle itself could look like you moved. Don't let it reset the stay.
        val tooFuzzy = accuracy != null && accuracy > Config.RADIUS_METERS
        var minutes: Int? = null
        if (!tooFuzzy) {
            val stay = StayTracker.next(Prefs.stay(this), Spot(loc.latitude, loc.longitude), now)
            Prefs.setStay(this, stay)
            minutes = stay.minutesAt(now)
            // Re-read every fix, so people added or edited while running count right away.
            val people = PingLog.contacts(this).allNow()
            val lastEventAt = PingLog.dao(this).lastEventSince(stay.sinceMillis).associate { it.contactId to it.at }
            val handled = People.handled(people, lastEventAt).toMutableSet()
            for (person in People.due(stay, now, people, handled)) {
                try {
                    DwellHandler.onDwell(this, loc, now, person)
                } catch (e: Exception) {
                    Log.e(TAG, "Dwell handling failed for #${person.id}", e)
                }
                handled += person.id
            }
            updateNotification(People.notification(minutes, people, handled))
        }

        PingLog.fixes(this).insert(
            LocationFix(
                timestamp = now,
                latitude = loc.latitude,
                longitude = loc.longitude,
                accuracyMeters = accuracy,
                speedMps = speed,
                minutesHere = minutes,
            )
        )
        Log.i(TAG, "Fix ±${accuracy}m -> " + (minutes?.let { "$it min here" } ?: "ignored (too fuzzy)"))
        // A fuzzy fix can be far off: then the stay's center is the better last spot, if there is one.
        val lastSpot = Prefs.stay(this)?.center?.takeIf { tooFuzzy } ?: Spot(loc.latitude, loc.longitude)
        try {
            LowBatteryText.check(this, lastSpot)
        } catch (e: Exception) {
            Log.e(TAG, "Low-battery check failed", e)
        }
        pruneIfDue()
    }

    /** No fix for [NO_FIX_MILLIS]: the location request was lost somewhere, so ask again. */
    private val watchdog = object : Runnable {
        override fun run() {
            if (System.currentTimeMillis() - lastFixAt >= NO_FIX_MILLIS) {
                Log.w(TAG, "No fix for ${NO_FIX_MILLIS / 60_000} min, asking for location again")
                requestUpdates()
            }
            workerHandler.postDelayed(this, WATCHDOG_EVERY_MILLIS)
        }
    }

    /** Only touched on [worker]. 0 = not yet since the service started, so the first fix prunes. */
    private var lastPrunedAt = 0L

    /** Keeps the log small even when the service runs for weeks. See [Retention]. */
    private fun pruneIfDue() {
        val now = System.currentTimeMillis()
        if (!Retention.isPruneDue(lastPrunedAt, now)) return
        lastPrunedAt = now
        try {
            PingLog.prune(this, now, staySinceMillis = Prefs.stay(this)?.sinceMillis ?: now)
        } catch (e: Exception) {
            Log.e(TAG, "Pruning the log failed", e)
        }
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Running", NotificationManager.IMPORTANCE_LOW)
            .apply { description = "Shown while Aqui! is watching for stops" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Aqui! is on")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        private const val TAG = "Aqui"
        private const val CHANNEL_ID = "running"
        private const val NOTIFICATION_ID = 1
        private const val FIX_EVERY_MILLIS = 60_000L
        private const val NO_FIX_MILLIS = 5 * 60_000L
        private const val WATCHDOG_EVERY_MILLIS = 60_000L

        fun start(c: Context) {
            c.startForegroundService(Intent(c, DwellService::class.java))
        }

        fun stop(c: Context) {
            c.stopService(Intent(c, DwellService::class.java))
        }
    }
}
