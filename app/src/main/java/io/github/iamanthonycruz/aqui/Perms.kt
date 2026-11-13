package io.github.iamanthonycruz.aqui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager

object Perms {
    private fun granted(c: Context, p: String) =
        c.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    /** Step 1 of the request flow: asked together in one system dialog. */
    val FOREGROUND_REQUEST = listOfNotNull(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        // Optional: some carriers (MetroPCS) make Android check the SIM before a text longer than
        // one SMS, and that check needs this. Without it long texts still go, as separate parts.
        Manifest.permission.READ_PHONE_STATE,
        // Optional: Android 13+ hides the "Aqui! is on" notification without it. Asked here, with
        // the others, instead of as a bare prompt the moment the app first opens.
        Manifest.permission.POST_NOTIFICATIONS.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU },
    ).toTypedArray()

    /** Step 2: Android only offers "Allow all the time" after step 1 is granted. */
    const val BACKGROUND_REQUEST = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    fun state(c: Context): PermState {
        val fine = granted(c, Manifest.permission.ACCESS_FINE_LOCATION)
        return PermState(
            location = fine,
            backgroundLocation = granted(c, BACKGROUND_REQUEST),
            sms = granted(c, Manifest.permission.SEND_SMS),
            approximateOnly = !fine && granted(c, Manifest.permission.ACCESS_COARSE_LOCATION),
            battery = c.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(c.packageName),
        )
    }

    /** The permissions a [step] asks for that are still missing. */
    fun missingFor(c: Context, step: PermStep): List<String> = when (step) {
        PermStep.FOREGROUND -> listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS)
        PermStep.BACKGROUND -> listOf(BACKGROUND_REQUEST)
        PermStep.BATTERY, PermStep.OPEN_SETTINGS, PermStep.NONE -> emptyList()
    }.filterNot { granted(c, it) }

    fun hasAll(c: Context) = state(c).hasAll
}
