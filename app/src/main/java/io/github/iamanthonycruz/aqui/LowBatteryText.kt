package io.github.iamanthonycruz.aqui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

/** Reads the battery at each fix and sends the [LowBattery] text when it's due. */
object LowBatteryText {
    private const val TAG = "Aqui"

    /** [here] = the fix that just came in: where you last were. Blocks (database, address lookup). */
    fun check(c: Context, here: Spot) {
        // The last battery broadcast, the same numbers the status bar shows. No receiver is registered.
        val battery = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return // Unknown on this phone.
        val percent = level * 100 / scale
        // Plugged in counts even when it's full and not charging any more.
        val charging = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val step = LowBattery.step(Prefs.isLowBatteryFired(c), percent, charging)
        Prefs.setLowBatteryFired(c, step.fired)
        if (!step.send) return

        val people = PingLog.contacts(c).allNow().filter { it.lowBattery }
        Log.i(TAG, "Battery at $percent%, texting ${people.size} people")
        // Build each different text once, not once per person.
        val texts = HashMap<Spot?, String>()
        for (person in people) {
            try {
                val spot: Spot? = here
                val text = texts.getOrPut(spot) {
                    MessageTemplate.lowBattery(
                        percent = percent,
                        place = spot?.let { Message.describe(c, it) },
                        mapLink = spot?.let { MessageTemplate.mapLink(it.lat, it.lng) },
                    )
                }
                SmsSender.send(c, person.phone, text)
            } catch (e: Exception) {
                Log.e(TAG, "Low-battery text failed for #${person.id}", e)
            }
        }
    }
}
