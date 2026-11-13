package com.example.dwellping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionTest {

    private val now = 1_700_000_000_000L
    private val hour = 60 * 60 * 1000L
    private val day = 24 * hour

    @Test
    fun `ping events older than 90 days are pruned`() {
        val stayStarted = now - 20 * 60_000L
        assertEquals(now - 90 * day, Retention.eventsCutoff(now, stayStarted))
    }

    @Test
    fun `a stay longer than the window keeps every row since it began`() {
        // At the same spot for 100 days: the text sent on day 1 still marks them as handled.
        val stayStarted = now - 100 * day
        assertEquals(stayStarted, Retention.eventsCutoff(now, stayStarted))
    }

    @Test
    fun `a text inside the cooldown is never pruned`() {
        val sentAt = now - (Config.COOLDOWN_HOURS * hour - 1)
        assertTrue(Retention.eventsCutoff(now, staySinceMillis = now) < sentAt)
    }

    @Test
    fun `fixes older than 7 days are pruned`() {
        assertEquals(now - 7 * day, Retention.fixesCutoff(now))
    }

    @Test
    fun `pruning runs at most once an hour`() {
        assertTrue(Retention.isPruneDue(lastPrunedAt = 0L, now = now)) // service just started
        assertFalse(Retention.isPruneDue(lastPrunedAt = now - 59 * 60_000L, now = now))
        assertTrue(Retention.isPruneDue(lastPrunedAt = now - hour, now = now))
    }
}
