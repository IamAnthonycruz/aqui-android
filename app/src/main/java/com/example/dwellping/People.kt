package com.example.dwellping

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Someone who gets a text when you stay put for their [minutes]. A row in the `contacts` table. */
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Optional. Blank = shown by number. */
    val name: String = "",
    val phone: String,
    val minutes: Int = Config.DEFAULT_MINUTES,
    /** Their message. Empty = [MessageTemplate.DEFAULT]. */
    val template: String = "",
    /**
     * When their number or timer was last saved ([People.resetsRule]). Ping-log rows from
     * before this don't count for them: no cooldown, not handled at this stay. 0 = never.
     */
    @ColumnInfo(defaultValue = "0") val resetAtMillis: Long = 0,
    /** Gets one text with your last spot when the battery gets low ([LowBattery]). Off unless ticked. */
    @ColumnInfo(defaultValue = "0") val lowBattery: Boolean = false,
) {
    val label get() = name.trim().ifBlank { phone }
}

/** What a person's row on the Start screen says about the current stay. */
sealed interface PersonStatus {
    data object Waiting : PersonStatus
    data class Texted(val atMillis: Long) : PersonStatus
    data class Failed(val atMillis: Long) : PersonStatus
    data class NotSent(val atMillis: Long, val reason: String) : PersonStatus
}

/** Per-person timers. No Android code, so it is unit-tested. */
object People {

    /**
     * Who should be handled at this fix: their timer has run out at this [stay] and they have
     * no ping-log row since it began ([handled]). A timer lowered mid-stay counts from the stay's
     * start, so that person is due at the next fix.
     */
    fun due(stay: Stay, now: Long, contacts: List<Contact>, handled: Set<Long>): List<Contact> =
        contacts.filter { it.id !in handled && now - stay.sinceMillis >= it.minutes * 60_000L }

    /** [events] are the ping-log rows since the stay began. The latest row for [contactId] wins. */
    fun status(contactId: Long, events: List<PingEvent>): PersonStatus {
        val e = events.filter { it.contactId == contactId }.maxByOrNull { it.timestamp }
            ?: return PersonStatus.Waiting
        return when (e.result) {
            PingResult.SENT -> PersonStatus.Texted(e.timestamp)
            PingResult.SEND_FAILED -> PersonStatus.Failed(e.timestamp)
            PingResult.SKIPPED_MOVING -> PersonStatus.NotSent(e.timestamp, "moving")
            PingResult.SKIPPED_SAME_SPOT -> PersonStatus.NotSent(e.timestamp, "texted here recently")
        }
    }

    /** [events] since the stay began, minus the ones from before [c] was last reset. */
    fun status(c: Contact, events: List<PingEvent>): PersonStatus =
        status(c.id, events.filter { it.timestamp >= c.resetAtMillis })

    /**
     * Who has been handled at this stay. [lastEventAt] is each person's latest ping-log row since
     * the stay began; a row from before their reset doesn't count.
     */
    fun handled(contacts: List<Contact>, lastEventAt: Map<Long, Long>): Set<Long> =
        contacts.filter { c -> lastEventAt[c.id]?.let { it >= c.resetAtMillis } == true }.map { it.id }.toSet()

    /** The last text to [c] that still counts for the cooldown, or null. */
    fun cooldownFrom(lastSent: SentPing?, c: Contact): SentPing? =
        lastSent?.takeIf { it.timeMillis >= c.resetAtMillis }

    /**
     * Saving [new] over [old] gives them a fresh start: only a new number or timer counts.
     * Rewording the message doesn't, so fixing a typo can't re-text them at a spot they were just texted from.
     */
    fun resetsRule(old: Contact, new: Contact): Boolean =
        old.phone.trim() != new.phone.trim() || old.minutes != new.minutes

    /** Who gets the next text at this stay: the shortest timer among those not handled yet. */
    fun next(contacts: List<Contact>, handled: Set<Long>): Contact? =
        contacts.filter { it.id !in handled }.minByOrNull { it.minutes }

    /** How full the Start ring is: share of [next]'s timer used so far, 0..1. */
    fun ringProgress(elapsedMillis: Long, next: Contact?): Float =
        if (next == null) 1f else (elapsedMillis / (next.minutes * 60_000f)).coerceIn(0f, 1f)

    /**
     * A running duration split into number and unit: under an hour "45" + "min", after that hours
     * to one decimal ("1" + "hr", "1.1" + "hrs", "2.2" + "hrs"), so nobody has to divide 137 by 60.
     * Rounds down, so it never claims more time than has passed.
     */
    fun durationParts(minutes: Int): Pair<String, String> {
        if (minutes < 60) return "$minutes" to "min"
        val tenths = minutes / 6 // 6 minutes = 0.1 hr
        val text = if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
        return text to if (tenths == 10) "hr" else "hrs"
    }

    /** [durationParts] as one string, e.g. "45 min" or "2.2 hrs". */
    fun duration(minutes: Int): String = durationParts(minutes).let { (n, unit) -> "$n $unit" }

    /** The running notification's text, e.g. "Here 4 min · Alex at 10 min". */
    fun notification(minutes: Int, contacts: List<Contact>, handled: Set<Long>): String {
        val next = next(contacts, handled)
        val tail = when {
            contacts.isEmpty() -> "No one to text"
            next == null -> "Everyone's been texted from this spot"
            else -> "${next.label} at ${next.minutes} min"
        }
        return "Here ${duration(minutes)} · $tail"
    }

    /** The [-] / [+] buttons on a person's timer. */
    fun stepMinutes(current: Int, up: Boolean): Int =
        (if (up) current + 5 else current - 5).coerceIn(Config.MIN_MINUTES, Config.MAX_MINUTES)
}
