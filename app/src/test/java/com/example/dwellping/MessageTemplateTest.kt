package com.example.dwellping

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTemplateTest {

    private val map = "https://maps.google.com/?q=42.36010,-71.05890"

    @Test
    fun `a custom template gets all three placeholders filled`() {
        val text = MessageTemplate.render(
            template = "At {place} for {minutes} min. {map}",
            place = "12 Elm St, Boston",
            minutes = 30,
            mapLink = map,
        )
        assertEquals("At 12 Elm St, Boston for 30 min. $map", text)
    }

    @Test
    fun `an empty template sends the default message`() {
        val text = MessageTemplate.render(template = "", place = "12 Elm St, Boston", minutes = 30, mapLink = map)
        assertEquals("Hey, I've been at 12 Elm St, Boston for 30+ min.\n$map", text)
    }

    @Test
    fun `an empty template with no location sends the default no-location message`() {
        val text = MessageTemplate.render(template = "", place = null, minutes = 30, mapLink = null)
        assertEquals("Hey, I've been at the same spot for 30+ min.", text)
    }

    @Test
    fun `with no location a custom template says this spot and drops the map link`() {
        val text = MessageTemplate.render(template = "I'm at {place}\n{map}", place = null, minutes = 30, mapLink = null)
        assertEquals("I'm at this spot", text)
    }

    @Test
    fun `plain text with no placeholders is sent exactly as typed`() {
        val text = MessageTemplate.render(template = "Made it to the gym 💪", place = "12 Elm St, Boston", minutes = 30, mapLink = map)
        assertEquals("Made it to the gym 💪", text)
    }

    @Test
    fun `unknown placeholders are left as typed`() {
        val text = MessageTemplate.render(template = "{name} is at {place} {Place} {", place = "Home", minutes = 30, mapLink = map)
        assertEquals("{name} is at Home {Place} {", text)
    }

    @Test
    fun `leading spaces and blank lines the user typed are kept`() {
        val text = MessageTemplate.render(template = "  hi\n\nbye", place = "Home", minutes = 30, mapLink = map)
        assertEquals("  hi\n\nbye", text)
    }

    @Test
    fun `the map link uses five decimals and a dot whatever the phone's language`() {
        assertEquals("https://maps.google.com/?q=42.36010,-71.05890", MessageTemplate.mapLink(42.3601, -71.0589))
    }

    @Test
    fun `a template that comes out empty sends the default instead of a blank text`() {
        val text = MessageTemplate.render(template = "{map}", place = null, minutes = 30, mapLink = null)
        assertEquals("Hey, I've been at the same spot for 30+ min.", text)
    }


    @Test
    fun `an address keeps the at`() {
        val text = MessageTemplate.render(template = "Still at {place}", place = "12 Elm St, Boston", minutes = 30, mapLink = map)
        assertEquals("Still at 12 Elm St, Boston", text)
    }

    @Test
    fun `the low-battery text gives the last spot and a map link`() {
        val text = MessageTemplate.lowBattery(percent = 10, place = "12 Elm St, Boston", mapLink = map)
        assertEquals("My phone is at 10% and about to die. Last spot: 12 Elm St, Boston\n$map", text)
    }

    @Test
    fun `the low-battery text reads right with a named place`() {
        val text = MessageTemplate.lowBattery(percent = 9, place = "123 Main St, Boston", mapLink = map)
        assertEquals("My phone is at 9% and about to die. Last spot: 123 Main St, Boston\n$map", text)
    }

    @Test
    fun `the low-battery text without a location still warns`() {
        val text = MessageTemplate.lowBattery(percent = 10, place = null, mapLink = null)
        assertEquals("My phone is at 10% and about to die.", text)
    }
}
