package com.example.dwellping

import org.junit.Assert.assertEquals
import org.junit.Test

class PeopleTest {

    private val t0 = 1_700_000_000_000L
    private val here = Spot(33.749, -84.388)
    private val stay = Stay(here, sinceMillis = t0)
    private fun min(m: Int) = t0 + m * 60_000L

    private val mom = Contact(id = 1, name = "Mom", phone = "+14045550132", minutes = 30)
    private val alex = Contact(id = 2, name = "Alex", phone = "+16785550199", minutes = 10)

    // --- Who is due a text ---

    @Test
    fun `each person is due at their own minutes`() {
        assertEquals(emptyList<Contact>(), People.due(stay, min(9), listOf(mom, alex), handled = emptySet()))
        assertEquals(listOf(alex), People.due(stay, min(10), listOf(mom, alex), handled = emptySet()))
        assertEquals(listOf(mom, alex), People.due(stay, min(30), listOf(mom, alex), handled = emptySet()))
    }

    @Test
    fun `someone already handled at this stay is not due again`() {
        assertEquals(listOf(mom), People.due(stay, min(45), listOf(mom, alex), handled = setOf(2L)))
    }

    @Test
    fun `lowering a timer mid-stay makes them due at the next fix`() {
        val momSooner = mom.copy(minutes = 5)
        assertEquals(listOf(momSooner), People.due(stay, min(12), listOf(momSooner), handled = emptySet()))
    }

    // --- What a row on the Start screen shows ---

    private fun event(contactId: Long?, at: Long, result: PingResult) = PingEvent(
        timestamp = at, latitude = null, longitude = null, accuracyMeters = null, speedMps = null,
        result = result, message = null, contactId = contactId,
    )

    @Test
    fun `no event for a person means still waiting`() {
        assertEquals(PersonStatus.Waiting, People.status(1, listOf(event(2, min(10), PingResult.SENT))))
    }

    @Test
    fun `each result maps to what the row says`() {
        assertEquals(PersonStatus.Texted(min(30)), People.status(1, listOf(event(1, min(30), PingResult.SENT))))
        assertEquals(PersonStatus.Failed(min(30)), People.status(1, listOf(event(1, min(30), PingResult.SEND_FAILED))))
        assertEquals(
            PersonStatus.NotSent(min(30), "moving"),
            People.status(1, listOf(event(1, min(30), PingResult.SKIPPED_MOVING))),
        )
        assertEquals(
            PersonStatus.NotSent(min(30), "texted here recently"),
            People.status(1, listOf(event(1, min(30), PingResult.SKIPPED_SAME_SPOT))),
        )
    }

    @Test
    fun `the latest event wins`() {
        val events = listOf(event(1, min(30), PingResult.SKIPPED_MOVING), event(1, min(31), PingResult.SENT))
        assertEquals(PersonStatus.Texted(min(31)), People.status(1, events))
    }

    // --- The running notification ---

    @Test
    fun `notification names who is texted next`() {
        assertEquals("Here 4 min · Alex at 10 min", People.notification(4, listOf(mom, alex), handled = emptySet()))
        assertEquals("Here 12 min · Mom at 30 min", People.notification(12, listOf(mom, alex), handled = setOf(2L)))
    }

    @Test
    fun `notification when everyone is handled or nobody is set up`() {
        assertEquals(
            "Here 40 min · Everyone's been texted from this spot",
            People.notification(40, listOf(mom, alex), handled = setOf(1L, 2L)),
        )
        assertEquals("Here 3 min · No one to text", People.notification(3, emptyList(), handled = emptySet()))
    }

    @Test
    fun `durations switch to decimal hours after 60 min`() {
        assertEquals("59 min", People.duration(59))
        assertEquals("1 hr", People.duration(60))
        assertEquals("1 hr", People.duration(65))
        assertEquals("1.1 hrs", People.duration(66))
        assertEquals("1.9 hrs", People.duration(119))
        assertEquals("2 hrs", People.duration(120))
        assertEquals("2.2 hrs", People.duration(137))
        assertEquals("Here 2.2 hrs · No one to text", People.notification(137, emptyList(), handled = emptySet()))
    }

    @Test
    fun `a person without a name is shown by number`() {
        assertEquals("+16785550199", alex.copy(name = " ").label)
    }

    // --- The Start ring ---

    @Test
    fun `the ring fills toward the next person's timer`() {
        assertEquals(alex, People.next(listOf(mom, alex), handled = emptySet()))
        assertEquals(0.5f, People.ringProgress(5 * 60_000L, alex), 0.001f)
        assertEquals(1f, People.ringProgress(12 * 60_000L, alex), 0.001f)
    }

    @Test
    fun `once Alex is texted the ring counts toward Mom, and is full when everyone is done`() {
        assertEquals(mom, People.next(listOf(mom, alex), handled = setOf(2L)))
        assertEquals(0.4f, People.ringProgress(12 * 60_000L, mom), 0.001f)
        assertEquals(null, People.next(listOf(mom, alex), handled = setOf(1L, 2L)))
        assertEquals(1f, People.ringProgress(40 * 60_000L, null), 0.001f)
    }

    // --- The minutes stepper ---

    @Test
    fun `the stepper moves in fives and stays inside the limits`() {
        assertEquals(20, People.stepMinutes(15, up = true))
        assertEquals(10, People.stepMinutes(15, up = false))
        assertEquals(Config.MIN_MINUTES, People.stepMinutes(Config.MIN_MINUTES, up = false))
        assertEquals(Config.MAX_MINUTES, People.stepMinutes(Config.MAX_MINUTES, up = true))
    }
}
