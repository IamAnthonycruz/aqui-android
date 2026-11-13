package io.github.iamanthonycruz.aqui

/**
 * The first fix after Start. [DwellService] asks for one right away instead of waiting for the
 * once-a-minute updates, and Start says something reassuring if it is still slow.
 * No Android code, so it is unit-tested.
 */
object FirstFix {

    /** A cached fix older than this isn't used to start the stay: you may have just arrived. */
    const val MAX_AGE_MILLIS = 30_000L

    /** How long the one-off request may search before giving up (the regular updates keep going). */
    const val TIMEOUT_MILLIS = 30_000L

    /** After this long without a fix, Start stops saying just "Finding your spot…". */
    const val SLOW_AFTER_MILLIS = 15_000L

    /** How often Start re-reads the stay while waiting for the first fix. */
    const val POLL_MILLIS = 2_000L

    /** What Start says before the first fix. [startedAt] null = unknown (app reopened), so treat it as slow. */
    fun caption(startedAt: Long?, now: Long): String =
        if (startedAt != null && now - startedAt < SLOW_AFTER_MILLIS) "Finding your spot…"
        else "Still finding your spot. This can take a minute indoors."
}
