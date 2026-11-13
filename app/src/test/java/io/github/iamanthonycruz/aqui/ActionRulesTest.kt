package io.github.iamanthonycruz.aqui

import org.junit.Assert.assertEquals
import org.junit.Test

class ActionRulesTest {

    private val allGranted = PermState(location = true, backgroundLocation = true, sms = true)

    @Test
    fun `start with nobody to text is blocked on people`() {
        assertEquals(Blocker.NO_PEOPLE, ActionRules.startBlocker(people = 0, perms = allGranted))
    }

    @Test
    fun `start without all-the-time location is blocked on permissions`() {
        val noBackground = allGranted.copy(backgroundLocation = false)
        assertEquals(Blocker.NO_PERMISSIONS, ActionRules.startBlocker(people = 1, perms = noBackground))
    }

    @Test
    fun `start with a person and every permission is allowed`() {
        assertEquals(null, ActionRules.startBlocker(people = 2, perms = allGranted))
    }

    @Test
    fun `test text works without all-the-time location`() {
        val noBackground = allGranted.copy(backgroundLocation = false)
        assertEquals(null, ActionRules.testTextBlocker("+15551234567", noBackground))
    }

    @Test
    fun `test text without SMS permission is blocked on permissions`() {
        assertEquals(Blocker.NO_PERMISSIONS, ActionRules.testTextBlocker("+15551234567", allGranted.copy(sms = false)))
    }

    @Test
    fun `test text with a blank number is blocked on the number`() {
        assertEquals(Blocker.NO_NUMBER, ActionRules.testTextBlocker("", allGranted))
    }

    @Test
    fun `a person can't be saved without a number`() {
        assertEquals(Blocker.NO_NUMBER, ActionRules.saveBlocker(phone = "  ", minutes = 15))
    }

    @Test
    fun `a person can't be saved with a timer under the minimum`() {
        assertEquals(Blocker.TOO_SHORT, ActionRules.saveBlocker(phone = "+15551234567", minutes = Config.MIN_MINUTES - 1))
    }

    @Test
    fun `a person with a number and a valid timer can be saved`() {
        assertEquals(null, ActionRules.saveBlocker(phone = "+15551234567", minutes = Config.MIN_MINUTES))
    }

}
