package io.github.iamanthonycruz.aqui

import org.junit.Assert.assertEquals
import org.junit.Test

class TokenEditTest {

    @Test
    fun `backspace on the end of a token removes the whole token`() {
        // "Hi {place}!" with the cursor after "}" -> backspace deletes "}"
        assertEquals(TokenEdit("Hi !", 3), TokenEdit.fix(old = "Hi {place}!", new = "Hi {place!", cursor = 9))
    }

    @Test
    fun `forward delete on the start of a token removes the whole token`() {
        assertEquals(TokenEdit("At  now", 3), TokenEdit.fix(old = "At {map} now", new = "At map} now", cursor = 3))
    }

    @Test
    fun `typing and deleting plain text is left alone`() {
        assertEquals(TokenEdit("Hi {place}!!", 12), TokenEdit.fix(old = "Hi {place}!", new = "Hi {place}!!", cursor = 12))
        assertEquals(TokenEdit("H {place}", 1), TokenEdit.fix(old = "Hi {place}", new = "H {place}", cursor = 1))
    }

    @Test
    fun `replacing a selection that cuts into a token replaces the whole token`() {
        // Selected "Hi {pl" and typed "Yo"
        assertEquals(TokenEdit("Yo", 2), TokenEdit.fix(old = "Hi {place}", new = "Yoace}", cursor = 2))
    }

    @Test
    fun `typing inside a token lands after it instead`() {
        assertEquals(TokenEdit("{map}x", 6), TokenEdit.fix(old = "{map}", new = "{mxap}", cursor = 3))
    }

    @Test
    fun `tokens are found where they are`() {
        assertEquals(listOf(3..9, 14..18), TokenEdit.tokens("Hi {place} at {map}"))
    }
}
