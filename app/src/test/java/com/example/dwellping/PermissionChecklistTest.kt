package com.example.dwellping

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionChecklistTest {

    @Test
    fun `nothing granted asks for location and SMS first`() {
        val s = PermState(location = false, backgroundLocation = false, sms = false)
        assertEquals(PermStep.FOREGROUND, PermissionChecklist.nextStep(s))
    }

    @Test
    fun `location and SMS granted asks for all-the-time location next`() {
        val s = PermState(location = true, backgroundLocation = false, sms = true)
        assertEquals(PermStep.BACKGROUND, PermissionChecklist.nextStep(s))
    }

    @Test
    fun `everything granted needs no step`() {
        val s = PermState(location = true, backgroundLocation = true, sms = true)
        assertEquals(PermStep.NONE, PermissionChecklist.nextStep(s))
    }

    @Test
    fun `SMS revoked asks for the foreground set again even with background granted`() {
        val s = PermState(location = true, backgroundLocation = true, sms = false)
        assertEquals(PermStep.FOREGROUND, PermissionChecklist.nextStep(s))
    }

    @Test
    fun `foreground the system won't ask for again sends the user to settings`() {
        val s = PermState(location = false, backgroundLocation = false, sms = true)
        assertEquals(PermStep.OPEN_SETTINGS, PermissionChecklist.nextStep(s, blockedAt = PermStep.FOREGROUND))
    }

    @Test
    fun `a block on the foreground step does not carry over to the background step`() {
        val s = PermState(location = true, backgroundLocation = false, sms = true)
        assertEquals(PermStep.BACKGROUND, PermissionChecklist.nextStep(s, blockedAt = PermStep.FOREGROUND))
    }

    @Test
    fun `still missing after a request with no way to ask again is blocked`() {
        val after = PermState(location = false, backgroundLocation = false, sms = true)
        assertEquals(
            PermStep.FOREGROUND,
            PermissionChecklist.blockedAfter(PermStep.FOREGROUND, after, canAskAgain = false),
        )
    }

    @Test
    fun `still missing but Android can ask again is not blocked`() {
        val after = PermState(location = false, backgroundLocation = false, sms = true)
        assertEquals(null, PermissionChecklist.blockedAfter(PermStep.FOREGROUND, after, canAskAgain = true))
    }

    @Test
    fun `granted after the request is not blocked`() {
        val after = PermState(location = true, backgroundLocation = false, sms = true)
        assertEquals(null, PermissionChecklist.blockedAfter(PermStep.FOREGROUND, after, canAskAgain = false))
    }
}
