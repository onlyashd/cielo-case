package com.example.cielocase.domain

import com.example.cielocase.domain.model.TicketQrPayload
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketQrPayloadTest {

    private val payload = TicketQrPayload.create(
        purchaseId = "purchase-1",
        ticketId = "ticket-1",
        eventId = "evt-1",
        sequence = 2,
        authCode = "123456",
        secret = SECRET,
    )

    @Test
    fun `payload identifies the purchase and the ticket`() {
        val parts = payload.split("|")

        assertTrue(parts[0] == TicketQrPayload.VERSION)
        assertTrue(parts.containsAll(listOf("purchase-1", "ticket-1", "evt-1", "2")))
    }

    @Test
    fun `payload is valid for the purchase authorization code`() {
        assertTrue(TicketQrPayload.verify(payload, authCode = "123456", secret = SECRET))
    }

    @Test
    fun `payload is rejected when the authorization code does not match`() {
        assertFalse(TicketQrPayload.verify(payload, authCode = "000000", secret = SECRET))
    }

    @Test
    fun `payload is rejected when the secret does not match`() {
        assertFalse(TicketQrPayload.verify(payload, authCode = "123456", secret = "other"))
    }

    @Test
    fun `tampered payloads are rejected`() {
        val tampered = payload.replace("purchase-1", "purchase-2")

        assertFalse(TicketQrPayload.verify(tampered, authCode = "123456", secret = SECRET))
        assertFalse(TicketQrPayload.verify("garbage", authCode = "123456", secret = SECRET))
    }

    private companion object {
        const val SECRET = "test-secret"
    }
}
