package io.github.iamanthonycruz.aqui

/**
 * Where you stopped, and since when. Every new fix is compared to [center] (not to the previous
 * fix), so slow drift can't carry the stay along with you.
 * Who has been texted at this stay is in the ping log, not here: see [People.due].
 */
data class Stay(
    val center: Spot,
    val sinceMillis: Long,
    /** The last fix was outside the radius. One more in a row and you've left. */
    val awayOnce: Boolean = false,
) {
    fun minutesAt(now: Long): Int = ((now - sinceMillis) / 60_000L).toInt()
}

/** Pure logic, unit-tested. [DwellService] feeds it one fix about every minute. */
object StayTracker {
    /**
     * The stay after a fix at [here], taken at [now]:
     *  - No stay yet: a new stay starts here.
     *  - Outside the radius twice in a row: you left, so a new stay starts here.
     *  - Outside once: the stay keeps going. A single reading can land hundreds of meters off
     *    and be back on the next one; the log showed 9 of 37 resets were that.
     *  - Inside the radius: the stay keeps going.
     */
    fun next(stay: Stay?, here: Spot, now: Long): Stay {
        if (stay == null) return Stay(center = here, sinceMillis = now)
        val outside = PingPolicy.metersBetween(stay.center, here) > Config.RADIUS_METERS
        return when {
            !outside -> stay.copy(awayOnce = false)
            stay.awayOnce -> Stay(center = here, sinceMillis = now)
            else -> stay.copy(awayOnce = true)
        }
    }
}
