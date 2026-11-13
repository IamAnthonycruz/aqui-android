package com.example.dwellping

/** How long the ping log keeps its rows. Pure math, so it runs in plain JVM tests. */
object Retention {
    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    /**
     * Delete [PingEvent] rows older than this. Never cuts into the current stay (which started at
     * [staySinceMillis]): its rows say who was already texted here, see [People.handled].
     */
    fun eventsCutoff(now: Long, staySinceMillis: Long): Long =
        minOf(now - Config.KEEP_PING_EVENTS_DAYS * DAY_MILLIS, staySinceMillis)

    /** Delete [LocationFix] rows older than this. */
    fun fixesCutoff(now: Long): Long = now - Config.KEEP_FIXES_DAYS * DAY_MILLIS

    /** Prune once an hour while running, so a service that runs for weeks stays small. 0 = never yet. */
    fun isPruneDue(lastPrunedAt: Long, now: Long): Boolean = now - lastPrunedAt >= PRUNE_EVERY_MILLIS

    private const val PRUNE_EVERY_MILLIS = 60 * 60 * 1000L
}
