package io.github.iamanthonycruz.aqui

import android.content.Context
import android.location.Geocoder
import java.util.Locale

object Message {

    /**
     * [template] filled in for [spot].
     * Blocks on the place / address lookup (a few seconds at most), so call it off the main thread.
     */
    fun build(c: Context, template: String, minutes: Int, spot: Spot?): String = MessageTemplate.render(
        template = template,
        place = spot?.let { describe(c, it) },
        minutes = minutes,
        mapLink = spot?.let { MessageTemplate.mapLink(it.lat, it.lng) },
    )

    /** What {place} says. Never throws, so the lookup can't stop a text from going out. Blocks. */
    fun describe(c: Context, spot: Spot): String = address(c, spot)

    /** Turns coordinates into an address, for example "123 Main St, Boston". */
    private fun address(c: Context, spot: Spot): String = try {
        if (!Geocoder.isPresent()) {
            "this spot"
        } else {
            @Suppress("DEPRECATION") // Still works. It blocks, so we call it off the main thread.
            val a = Geocoder(c, Locale.getDefault())
                .getFromLocation(spot.lat, spot.lng, 1)
                ?.firstOrNull()
            when {
                a == null -> "this spot"
                a.thoroughfare != null && a.locality != null ->
                    listOfNotNull(a.subThoroughfare, a.thoroughfare).joinToString(" ") +
                        ", " + a.locality
                a.locality != null -> a.locality
                else -> a.getAddressLine(0) ?: "this spot"
            }
        }
    } catch (e: Exception) {
        "this spot"
    }
}
