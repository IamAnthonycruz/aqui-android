package com.example.dwellping

import android.content.Context
import android.location.Location
import android.util.Log

/** What happens when one person's timer runs out ([People.due]): decide, maybe send, log one row. */
object DwellHandler {
    private const val TAG = "DwellPing"
    private val LOCK = Any()

    /**
     * [now] is the fix's time, so the row lands inside the stay that fix belongs to even when
     * fixes arrive in a late batch. Blocks (database, address lookup). Call off the main thread.
     */
    fun onDwell(c: Context, loc: Location?, now: Long, person: Contact) {
        // One dwell at a time, so two arriving together can't both pass the cooldown check.
        synchronized(LOCK) {
            val log = PingLog.dao(c)
            val here = loc?.let { Spot(it.latitude, it.longitude) }
            val speed = loc?.takeIf { it.hasSpeed() }?.speed
            // A text from before they were last edited doesn't count (People.resetsRule).
            val lastSent = People.cooldownFrom(log.lastSentTo(person.id)?.toSentPing(), person)
            val spot = here

            val decision = PingPolicy.decide(now, here, speed, lastSent)
            val text = if (decision == Decision.SEND) {
                Message.build(c, person.template, person.minutes, spot)
            } else null
            val result = when (decision) {
                Decision.SKIP_MOVING -> PingResult.SKIPPED_MOVING
                Decision.SKIP_SAME_SPOT -> PingResult.SKIPPED_SAME_SPOT
                // Logged as sent before sending, so the cooldown starts now.
                // SmsResultReceiver flips the row to send_failed if the phone reports an error.
                Decision.SEND -> PingResult.SENT
            }

            val rowId = log.insert(
                PingEvent(
                    timestamp = now,
                    latitude = loc?.latitude,
                    longitude = loc?.longitude,
                    accuracyMeters = loc?.takeIf { it.hasAccuracy() }?.accuracy,
                    speedMps = speed,
                    result = result,
                    message = text,
                    contactId = person.id,
                )
            )

            if (text != null) {
                try {
                    SmsSender.sendForRow(c, person.phone, text, rowId)
                } catch (e: Exception) {
                    Log.e(TAG, "Send failed", e)
                    log.markSendFailed(rowId, null) // Threw before the phone tried: no code.
                }
            }
            Log.i(TAG, "Dwell for #${person.id} -> ${result.code} (row $rowId)")
        }
    }
}
