package io.github.iamanthonycruz.aqui

import org.junit.Assert.assertEquals
import org.junit.Test

class StayTrackerTest {

    private val t0 = 1_700_000_000_000L
    private val home = Spot(lat = 33.7490, lng = -84.3880)
    private val nearHome = Spot(lat = 33.7499, lng = -84.3880) // ~100 m north: inside the 150 m radius
    private val gym = Spot(lat = 33.7600, lng = -84.3880) // ~1.2 km north

    private fun min(m: Int) = t0 + m * 60_000L

    /** Feeds one fix per minute at [spot] from minute [from] to [to], returning every step. */
    private fun feed(start: Stay?, spot: Spot, from: Int, to: Int): List<Stay> {
        var stay = start
        return (from..to).map { m ->
            StayTracker.next(stay, spot, min(m)).also { stay = it }
        }
    }

    @Test
    fun `first fix starts a stay at zero minutes`() {
        val stay = StayTracker.next(null, home, t0)
        assertEquals(Stay(home, t0), stay)
        assertEquals(0, stay.minutesAt(t0))
    }

    @Test
    fun `staying counts the minutes from the first fix`() {
        assertEquals(45, feed(null, home, 0, 45).last().minutesAt(min(45)))
    }

    @Test
    fun `drift inside the radius keeps counting from the first spot`() {
        val start = StayTracker.next(null, home, t0)
        val stay = StayTracker.next(start, nearHome, min(10))
        assertEquals(t0, stay.sinceMillis)
        assertEquals(home, stay.center)
    }

    @Test
    fun `two fixes outside the radius start a new stay at the new spot`() {
        val atHome = feed(null, home, 0, 20).last()
        val once = StayTracker.next(atHome, gym, min(21))
        assertEquals(Stay(gym, min(22)), StayTracker.next(once, gym, min(22)))
    }

    @Test
    fun `one stray fix outside the radius does not reset the stay`() {
        // Nov 12, 21:07: one reading 224 m off, back home a minute later.
        val atHome = feed(null, home, 0, 29).last()
        val stray = StayTracker.next(atHome, gym, min(30))
        assertEquals(t0, stray.sinceMillis)
        val back = StayTracker.next(stray, home, min(31))
        assertEquals(Stay(home, t0), back)
        assertEquals(31, back.minutesAt(min(31)))
    }

    @Test
    fun `stray fixes separated by a good one never reset`() {
        var stay = feed(null, home, 0, 10).last()
        for (m in 11..20) stay = StayTracker.next(stay, if (m % 2 == 0) gym else home, min(m))
        assertEquals(t0, stay.sinceMillis)
    }

    @Test
    fun `missed fixes still count the real time`() {
        val start = StayTracker.next(null, home, t0)
        // Phone was asleep: the next fix arrives much later, same spot.
        assertEquals(31, StayTracker.next(start, home, min(31)).minutesAt(min(31)))
    }
}
