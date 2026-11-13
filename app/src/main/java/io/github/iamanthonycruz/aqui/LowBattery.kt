package io.github.iamanthonycruz.aqui

/**
 * The low-battery text: at [PERCENT] the people who opted in get one text with where you last were,
 * because a dead phone is when a safety app goes silent. No Android code, so it is unit-tested.
 */
object LowBattery {

    /** Texts go out at this battery level or below. */
    const val PERCENT = 10

    /**
     * Back up to here without the tracker seeing a charger means it was charged while tracking was
     * off: a new cycle. Well above [PERCENT], so a percent of wobble at the line can't text twice.
     */
    const val RESET_PERCENT = PERCENT + 10

    /** [send] = text people now. [fired] = what to save for the next check. */
    data class Step(val send: Boolean, val fired: Boolean)

    /** [fired] = a low-battery text already went out this charge cycle. */
    fun step(fired: Boolean, percent: Int, charging: Boolean): Step = when {
        charging || percent >= RESET_PERCENT -> Step(send = false, fired = false)
        !fired && percent <= PERCENT -> Step(send = true, fired = true)
        else -> Step(send = false, fired = fired)
    }
}
