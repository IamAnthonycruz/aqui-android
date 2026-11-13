package io.github.iamanthonycruz.aqui

import android.content.Context

/** Small saved settings. People are in [PingLog.contacts]; the ping log (with locations) is in [PingLog]. */
object Prefs {
    private const val FILE = "aqui"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_ONBOARDED = "onboarded"
    private const val KEY_NOT_EMERGENCY_OK = "not_emergency_ok"
    private const val KEY_LOW_BATTERY_FIRED = "low_battery_fired"
    private const val KEY_STAY_LAT = "stay_lat"
    private const val KEY_STAY_LNG = "stay_lng"
    private const val KEY_STAY_SINCE = "stay_since"
    private const val KEY_STAY_AWAY_ONCE = "stay_away_once"

    /** Older builds tracked "texted at this stay" here. The ping log does that now. */
    private val OLD_STAY_KEYS = listOf("stay_pinged", "stay_pinged_at", "pinged_here")

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)


    /** The first-launch permission walkthrough was finished or skipped through. */
    fun isOnboarded(c: Context) = sp(c).getBoolean(KEY_ONBOARDED, false)
    fun setOnboarded(c: Context) = sp(c).edit().putBoolean(KEY_ONBOARDED, true).apply()

    /** Ticked "I understand Aqui! is not an emergency service" on the last setup page. */
    fun isNotEmergencyOk(c: Context) = sp(c).getBoolean(KEY_NOT_EMERGENCY_OK, false)
    fun setNotEmergencyOk(c: Context) = sp(c).edit().putBoolean(KEY_NOT_EMERGENCY_OK, true).apply()

    fun isEnabled(c: Context) = sp(c).getBoolean(KEY_ENABLED, false)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_ENABLED, v).apply()



    /** The low-battery text already went out this charge cycle. See [LowBattery.step]. */
    fun isLowBatteryFired(c: Context) = sp(c).getBoolean(KEY_LOW_BATTERY_FIRED, false)
    fun setLowBatteryFired(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_LOW_BATTERY_FIRED, v).apply()

    /** The current stay, saved so a restart of the app's process doesn't reset the count. */
    fun stay(c: Context): Stay? {
        val p = sp(c)
        if (!p.contains(KEY_STAY_SINCE)) return null
        return Stay(
            center = Spot(
                lat = Double.fromBits(p.getLong(KEY_STAY_LAT, 0)),
                lng = Double.fromBits(p.getLong(KEY_STAY_LNG, 0)),
            ),
            sinceMillis = p.getLong(KEY_STAY_SINCE, 0),
            awayOnce = p.getBoolean(KEY_STAY_AWAY_ONCE, false),
        )
    }

    fun setStay(c: Context, s: Stay?) {
        val e = sp(c).edit()
        OLD_STAY_KEYS.forEach { e.remove(it) }
        if (s == null) {
            e.remove(KEY_STAY_LAT).remove(KEY_STAY_LNG).remove(KEY_STAY_SINCE).remove(KEY_STAY_AWAY_ONCE)
        } else {
            e.putLong(KEY_STAY_LAT, s.center.lat.toRawBits())
                .putLong(KEY_STAY_LNG, s.center.lng.toRawBits())
                .putLong(KEY_STAY_SINCE, s.sinceMillis)
                .putBoolean(KEY_STAY_AWAY_ONCE, s.awayOnce)
        }
        e.apply()
    }
}
