package io.github.iamanthonycruz.aqui

/** Change these values to tune the app. */
object Config {
    /** A new person's timer: how long you must stay before they get a text. */
    const val DEFAULT_MINUTES = 15

    /**
     * The shortest timer a person can have. Shorter would text at red lights and gas stations.
     * Set to 1 when you test.
     */
    const val MIN_MINUTES = 5

    /** The longest timer the stepper goes to. */
    const val MAX_MINUTES = 240

    /** Size of the "place" circle, in meters. 100–200 m is reliable and easy on the battery. */
    const val RADIUS_METERS = 150f


    /** No second text to the same person from the same spot within this many hours. */
    const val COOLDOWN_HOURS = 4

    /** A dwell this close to the last sent text counts as the "same spot". */
    const val SAME_SPOT_METERS = RADIUS_METERS * 1.5f

    /** Faster than this (m/s) = you are moving (driving, traffic). No text. ~2 m/s is a brisk walk. */
    const val MOVING_SPEED_MPS = 2f

    /** The ping log keeps each text / skip this many days. Far longer than the cooldown needs. */
    const val KEEP_PING_EVENTS_DAYS = 90

    /** The location trace keeps this many days of fixes. Plenty to explain a missed ping. */
    const val KEEP_FIXES_DAYS = 7
}
