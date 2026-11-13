package io.github.iamanthonycruz.aqui

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/** Sends a normal text from YOUR phone number. Your phone plan pays. No server needed. */
object SmsSender {

    /** How a test text went. [failCode] is SmsManager's RESULT_ERROR_* code, null when every part went out. */
    data class TestResult(val parts: Int, val failCode: Int?, val answered: Boolean)

    /** How long the test button waits for the phone's report before saying it never heard back. */
    private const val TEST_WAIT_MILLIS = 60_000L

    /**
     * Sends a test text and calls [onDone] (on the main thread) once the phone has reported every
     * part, so the test button says what really happened instead of "sent" no matter what.
     */
    fun sendTest(c: Context, phone: String, text: String, onDone: (TestResult) -> Unit) {
        require(phone.isNotBlank()) { "No phone number set" }
        val app = c.applicationContext
        val sms = manager(app)
        val parts = sms.divideMessage(text)
        val action = "${app.packageName}.TEST_SENT.${System.nanoTime()}"
        val main = Handler(Looper.getMainLooper())
        var reported = 0
        var failCode: Int? = null
        var done = false

        lateinit var receiver: BroadcastReceiver
        fun finish(answered: Boolean) {
            if (done) return
            done = true
            main.removeCallbacksAndMessages(action)
            app.unregisterReceiver(receiver)
            onDone(TestResult(parts.size, failCode, answered))
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (resultCode != Activity.RESULT_OK && failCode == null) failCode = resultCode
                if (++reported >= parts.size) finish(answered = true)
            }
        }
        // The phone app reports through our own PendingIntents, so no other app needs to reach this.
        ContextCompat.registerReceiver(app, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
        main.postAtTime({ finish(answered = false) }, action, android.os.SystemClock.uptimeMillis() + TEST_WAIT_MILLIS)

        val sentIntents = ArrayList(parts.indices.map { i ->
            PendingIntent.getBroadcast(
                app, i, Intent(action).setPackage(app.packageName), PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        })
        try {
            sendParts(sms, phone, parts, sentIntents)
        } catch (e: Exception) {
            main.post { if (!done) { done = true; main.removeCallbacksAndMessages(action); app.unregisterReceiver(receiver) } }
            throw e
        }
    }

    /**
     * Sends the text for log row [rowId] and returns right away.
     * The phone reports each part later to [SmsResultReceiver], which marks the row failed on a
     * radio error (no signal, airplane mode).
     *
     * Why not wait here for the answer: this is called from inside the geofence broadcast, and
     * Android delivers an app's broadcasts one at a time. The "sent" report can't arrive until
     * that broadcast finishes, so waiting for it always timed out and logged real texts as failed.
     */
    fun sendForRow(c: Context, phone: String, text: String, rowId: Long) {
        require(phone.isNotBlank()) { "No phone number set" }
        val app = c.applicationContext
        val sms = manager(app)
        val parts = sms.divideMessage(text)
        val sentIntents = ArrayList(parts.indices.map { i ->
            val report = Intent(app, SmsResultReceiver::class.java)
                // A unique data URI per row and part, so Android keeps each PendingIntent separate.
                .setData(Uri.parse("aqui://sms/$rowId/$i"))
                .putExtra(SmsResultReceiver.EXTRA_ROW_ID, rowId)
            PendingIntent.getBroadcast(
                app, 0, report, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        })
        sendParts(sms, phone, parts, sentIntents)
    }

    /**
     * Sends a text with no log row to report to, so nobody hears back whether it went out (the
     * low-battery text: the phone may be dead before the report comes anyway). Returns right away.
     */
    fun send(c: Context, phone: String, text: String) {
        require(phone.isNotBlank()) { "No phone number set" }
        val sms = manager(c.applicationContext)
        sendParts(sms, phone, sms.divideMessage(text), sentIntents = null)
    }

    /**
     * One joined text when the phone allows it. Some carriers (MetroPCS) make Android check the SIM
     * before a multi-part text, which needs READ_PHONE_STATE; without it that throws a
     * SecurityException ("getGroupIdLevel1"). Then each part goes as its own short text, which
     * skips the check: the person gets 2 texts instead of 1, but they get them.
     */
    private fun sendParts(sms: SmsManager, phone: String, parts: ArrayList<String>, sentIntents: ArrayList<PendingIntent>?) {
        try {
            sms.sendMultipartTextMessage(phone, null, parts, sentIntents, null)
        } catch (e: SecurityException) {
            if (parts.size < 2) throw e
            parts.forEachIndexed { i, part -> sms.sendTextMessage(phone, null, part, sentIntents?.get(i), null) }
        }
    }

    private fun manager(c: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            c.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
}
