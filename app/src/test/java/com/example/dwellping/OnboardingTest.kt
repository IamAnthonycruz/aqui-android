package com.example.dwellping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingTest {
    private val none = PermState(location = false, backgroundLocation = false, sms = false, battery = false)
    private val all = PermState(location = true, backgroundLocation = true, sms = true, battery = true)

    @Test
    fun `fresh install starts at location`() = assertEquals(SetupPage.LOCATION, Onboarding.next(none))

    @Test
    fun `everything allowed shows no walkthrough`() = assertNull(Onboarding.next(all))

    @Test
    fun `allowed pages are skipped`() =
        assertEquals(SetupPage.BATTERY, Onboarding.next(all.copy(battery = false)))

    @Test
    fun `skipping location also skips all-the-time location`() =
        assertEquals(SetupPage.TEXTS, Onboarding.next(none, from = SetupPage.BACKGROUND.ordinal))

    @Test
    fun `skipping a page moves on even though it's still missing`() =
        assertEquals(SetupPage.BATTERY, Onboarding.next(none.copy(location = true, backgroundLocation = true), from = SetupPage.BATTERY.ordinal))

    @Test
    fun `past the last page is the end`() = assertNull(Onboarding.next(none, from = SetupPage.entries.size))
}
