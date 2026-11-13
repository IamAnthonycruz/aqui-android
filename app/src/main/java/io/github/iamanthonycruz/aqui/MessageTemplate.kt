package io.github.iamanthonycruz.aqui

import java.util.Locale

/** Turns the user's own text into the message that gets sent. No Android code, so it is unit-tested. */
object MessageTemplate {

    const val PLACE = "{place}"
    const val MINUTES = "{minutes}"
    const val MAP = "{map}"

    /** What an empty template sends. The same text the app sent before templates existed. */
    const val DEFAULT = "Hey, I've been at {place} for {minutes}+ min.\n{map}"

    /** The default when there is no location fix: no address and no map link. */
    private const val DEFAULT_NO_LOCATION = "Hey, I've been at the same spot for {minutes}+ min."

    /**
     * [place] and [mapLink] are null when the phone had no location fix.
     * Unknown placeholders are left as typed. A text that comes out empty
     * (e.g. just "{map}" with no fix) sends the default instead.
     */
    fun render(template: String, place: String?, minutes: Int, mapLink: String?): String {
        val default = if (place == null) DEFAULT_NO_LOCATION else DEFAULT
        return fill(template, place, minutes, mapLink).ifBlank { fill(default, place, minutes, mapLink) }
    }

    /** The [LowBattery] text. Fixed, not per person: it's a warning, not a check-in. */
    private const val LOW_BATTERY = "My phone is at {battery}% and about to die. Last spot: {place}\n{map}"
    private const val LOW_BATTERY_NO_LOCATION = "My phone is at {battery}% and about to die."

    /** [place] and [mapLink] are null when there was no location fix to share. */
    fun lowBattery(percent: Int, place: String?, mapLink: String?): String =
        fill(if (place == null) LOW_BATTERY_NO_LOCATION else LOW_BATTERY, place, minutes = 0, mapLink)
            .replace("{battery}", percent.toString())


    private fun fill(template: String, place: String?, minutes: Int, mapLink: String?) =
        template
            .replace(PLACE, place ?: "this spot")
            .replace(MINUTES, minutes.toString())
            .replace(MAP, mapLink.orEmpty())
            .trimEnd() // A dropped {map} on the last line would leave a dangling newline.

    /** Locale.US so the decimal separator is always a dot, even on a German or French phone. */
    fun mapLink(lat: Double, lng: Double): String =
        "https://maps.google.com/?q=" + String.format(Locale.US, "%.5f,%.5f", lat, lng)
}
