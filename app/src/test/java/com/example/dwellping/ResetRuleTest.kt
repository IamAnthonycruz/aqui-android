package com.example.dwellping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Saving a person's number or timer gives them a fresh start: no cooldown, not handled. */
class ResetRuleTest {

    private val t0 = 1_700_000_000_000L
    private fun min(m: Int) = t0 + m * 60_000L

    private val baber = Contact(id = 2, name = "Baber", phone = "+14045550132", minutes = 15)

    @Test
    fun `changing the number or timer resets the rule`() {
        assertTrue(People.resetsRule(baber, baber.copy(minutes = 30)))
        assertTrue(People.resetsRule(baber, baber.copy(phone = "+16785550199")))
    }

    @Test
    fun `changing only the name, message, or a pick does not`() {
        assertFalse(People.resetsRule(baber, baber.copy(name = "Babe")))
        assertFalse(People.resetsRule(baber, baber.copy(template = "Here!")))
        assertFalse(People.resetsRule(baber, baber.copy(lowBattery = true)))
        assertFalse(People.resetsRule(baber, baber.copy(phone = " +14045550132 ")))
    }

    @Test
    fun `a text from before the reset no longer blocks the next one`() {
        val reset = baber.copy(resetAtMillis = min(30))
        assertNull(People.cooldownFrom(SentPing(min(20), null), reset))
        val after = SentPing(min(40), null)
        assertEquals(after, People.cooldownFrom(after, reset))
    }

    @Test
    fun `someone handled before the reset is due again at this stay`() {
        val reset = baber.copy(resetAtMillis = min(30))
        assertEquals(emptySet<Long>(), People.handled(listOf(reset), lastEventAt = mapOf(2L to min(20))))
        assertEquals(setOf(2L), People.handled(listOf(reset), lastEventAt = mapOf(2L to min(35))))
        assertEquals(setOf(2L), People.handled(listOf(baber), lastEventAt = mapOf(2L to min(20))))
    }

    @Test
    fun `their row forgets results from before the reset`() {
        val reset = baber.copy(resetAtMillis = min(30))
        val skipped = PingEvent(
            timestamp = min(25), latitude = null, longitude = null, accuracyMeters = null, speedMps = null,
            result = PingResult.SKIPPED_SAME_SPOT, message = null, contactId = 2,
        )
        assertEquals(PersonStatus.Waiting, People.status(reset, listOf(skipped)))
        assertEquals(PersonStatus.NotSent(min(25), "texted here recently"), People.status(baber, listOf(skipped)))
    }
}
