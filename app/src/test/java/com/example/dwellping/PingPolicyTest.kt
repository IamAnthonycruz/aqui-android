package com.example.dwellping

import org.junit.Assert.assertEquals
import org.junit.Test

class PingPolicyTest {

    private val now = 1_700_000_000_000L
    private val home = Spot(lat = 33.7490, lng = -84.3880)

    private fun hoursAgo(h: Int) = now - h * 60 * 60 * 1000L

    @Test
    fun `first-ever ping sends`() {
        val d = PingPolicy.decide(now = now, here = home, speedMps = 0f, lastSent = null)
        assertEquals(Decision.SEND, d)
    }

    @Test
    fun `same spot inside cooldown skips`() {
        // ~50 m north of home: GPS drift, well inside Config.SAME_SPOT_METERS
        val drifted = Spot(lat = 33.74945, lng = -84.3880)
        val d = PingPolicy.decide(now, drifted, 0f, SentPing(hoursAgo(1), home))
        assertEquals(Decision.SKIP_SAME_SPOT, d)
    }

    @Test
    fun `same spot after cooldown sends`() {
        val d = PingPolicy.decide(now, home, 0f, SentPing(hoursAgo(5), home)) // cooldown is 4 h
        assertEquals(Decision.SEND, d)
    }

    @Test
    fun `different spot inside cooldown sends`() {
        // ~1.1 km north of home, far outside Config.SAME_SPOT_METERS
        val elsewhere = Spot(lat = 33.7590, lng = -84.3880)
        val d = PingPolicy.decide(now, elsewhere, 0f, SentPing(hoursAgo(1), home))
        assertEquals(Decision.SEND, d)
    }

    @Test
    fun `moving skips even on first-ever ping`() {
        val d = PingPolicy.decide(now, home, speedMps = 12f, lastSent = null) // ~27 mph
        assertEquals(Decision.SKIP_MOVING, d)
    }

    @Test
    fun `unknown location inside cooldown skips as same spot`() {
        val d = PingPolicy.decide(now, here = null, speedMps = null, lastSent = SentPing(hoursAgo(1), home))
        assertEquals(Decision.SKIP_SAME_SPOT, d)
    }
}
