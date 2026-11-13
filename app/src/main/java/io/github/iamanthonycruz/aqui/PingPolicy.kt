package io.github.iamanthonycruz.aqui

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Spot(val lat: Double, val lng: Double)

/** The last text that actually went out. */
data class SentPing(val timeMillis: Long, val spot: Spot?)

enum class Decision { SEND, SKIP_SAME_SPOT, SKIP_MOVING }

object PingPolicy {
    private const val COOLDOWN_MILLIS = Config.COOLDOWN_HOURS * 60 * 60 * 1000L

    /**
     * Should we text for this dwell?
     * [speedMps] is null when the location has no speed reading; that counts as not moving.
     */
    fun decide(now: Long, here: Spot?, speedMps: Float?, lastSent: SentPing?): Decision {
        if (speedMps != null && speedMps > Config.MOVING_SPEED_MPS) return Decision.SKIP_MOVING
        if (lastSent != null && now - lastSent.timeMillis < COOLDOWN_MILLIS) {
            val lastSpot = lastSent.spot
            // No position on either side: we can't prove it's a new spot, so don't risk a repeat.
            if (here == null || lastSpot == null) return Decision.SKIP_SAME_SPOT
            if (metersBetween(here, lastSpot) <= Config.SAME_SPOT_METERS) return Decision.SKIP_SAME_SPOT
        }
        return Decision.SEND
    }

    /** Great-circle distance (haversine). Pure math, so it runs in plain JVM tests. */
    fun metersBetween(a: Spot, b: Spot): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * atan2(sqrt(h), sqrt(1 - h))
    }
}
