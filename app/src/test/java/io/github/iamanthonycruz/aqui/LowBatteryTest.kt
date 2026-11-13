package io.github.iamanthonycruz.aqui

import org.junit.Assert.assertEquals
import org.junit.Test

class LowBatteryTest {

    @Test
    fun `dropping to 10 percent on battery sends once`() {
        assertEquals(LowBattery.Step(send = true, fired = true), LowBattery.step(fired = false, percent = 10, charging = false))
    }

    @Test
    fun `it doesn't text again as the battery keeps dropping`() {
        assertEquals(LowBattery.Step(send = false, fired = true), LowBattery.step(fired = true, percent = 4, charging = false))
    }

    @Test
    fun `plugging in starts a new charge cycle without texting`() {
        assertEquals(LowBattery.Step(send = false, fired = false), LowBattery.step(fired = true, percent = 8, charging = true))
    }

    @Test
    fun `back well above the line means it was charged while tracking was off, so a new cycle`() {
        assertEquals(LowBattery.Step(send = false, fired = false), LowBattery.step(fired = true, percent = 60, charging = false))
    }

    @Test
    fun `a percent of wobble just above the line doesn't start a new cycle`() {
        assertEquals(LowBattery.Step(send = false, fired = true), LowBattery.step(fired = true, percent = 12, charging = false))
    }

    @Test
    fun `a low phone already on the charger isn't about to die, so no text`() {
        assertEquals(LowBattery.Step(send = false, fired = false), LowBattery.step(fired = false, percent = 5, charging = true))
    }
}
