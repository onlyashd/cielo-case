package com.example.cielocase.payment

import com.example.cielocase.util.observability.SensitiveData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveDataTest {

    @Test
    fun `redacts the base64 request of the payment deep link`() {
        val message = "No Activity found to handle Intent { act=android.intent.action.VIEW " +
            "dat=lio://payment?request=eyJhY2Nlc3NUb2tlbiI6InNlY3JldCJ9&urlCallback=order://response }"

        val redacted = SensitiveData.redact(message)!!

        assertFalse(redacted.contains("eyJhY2Nlc3NUb2tlbiI6InNlY3JldCJ9"))
        assertTrue(redacted.contains("request=<redacted>"))
        // Non-sensitive context is preserved for debugging.
        assertTrue(redacted.contains("urlCallback=order://response"))
    }

    @Test
    fun `redacts credentials in json payloads`() {
        val json = """{"accessToken":"WXMG95YK5Lp7","clientID":"1xa0tyQbJliVt","value":"1000"}"""

        val redacted = SensitiveData.redact(json)!!

        assertEquals(
            """{"accessToken":"<redacted>","clientID":"<redacted>","value":"1000"}""",
            redacted,
        )
    }

    @Test
    fun `redacts credentials passed as query parameters`() {
        val redacted = SensitiveData.redact("clientID=abc123&accessToken=def456&value=1000")!!

        assertEquals("clientID=<redacted>&accessToken=<redacted>&value=1000", redacted)
    }

    @Test
    fun `keeps harmless text untouched`() {
        assertEquals("no Activity found", SensitiveData.redact("no Activity found"))
        assertNull(SensitiveData.redact(null))
    }
}
