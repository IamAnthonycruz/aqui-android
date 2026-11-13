package com.example.dwellping

import org.junit.Assert.assertEquals
import org.junit.Test

class FirstFixTest {

    private val start = 1_000_000L

    @Test
    fun `right after Start it says it is finding your spot`() {
        assertEquals("Finding your spot…", FirstFix.caption(startedAt = start, now = start + 5_000))
    }

    @Test
    fun `after 15 seconds without a fix it reassures instead`() {
        val slow = "Still finding your spot. This can take a minute indoors."
        assertEquals(slow, FirstFix.caption(startedAt = start, now = start + FirstFix.SLOW_AFTER_MILLIS))
        assertEquals(slow, FirstFix.caption(startedAt = start, now = start + 90_000))
    }

    @Test
    fun `with an unknown start time it assumes the wait has been long`() {
        assertEquals("Still finding your spot. This can take a minute indoors.", FirstFix.caption(startedAt = null, now = start))
    }
}
