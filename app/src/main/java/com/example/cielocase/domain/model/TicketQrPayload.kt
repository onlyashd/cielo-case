package com.example.cielocase.domain.model

import java.security.MessageDigest

/**
 * QR content of a ticket.
 *
 * Format: `CC1|<purchaseId>|<ticketId>|<eventId>|<sequence>|<signature>`.
 *
 * The signature binds the ticket to the *concluded* purchase (it includes the Cielo
 * authorization code), so a ticket cannot be forged from data visible in the app and a
 * gate reader can validate it offline with the shared secret.
 */
object TicketQrPayload {
    const val VERSION = "CC1"
    private const val SEPARATOR = "|"
    private const val SIGNATURE_LENGTH = 16
    private const val EXPECTED_PARTS = 6

    fun create(
        purchaseId: String,
        ticketId: String,
        eventId: String,
        sequence: Int,
        authCode: String?,
        secret: String,
    ): String {
        val body = body(purchaseId, ticketId, eventId, sequence)
        return body + SEPARATOR + sign(body, authCode, secret)
    }

    fun verify(payload: String, authCode: String?, secret: String): Boolean {
        val parts = payload.split(SEPARATOR)
        if (parts.size != EXPECTED_PARTS || parts[0] != VERSION) return false
        val body = parts.dropLast(1).joinToString(SEPARATOR)
        return parts.last() == sign(body, authCode, secret)
    }

    private fun body(purchaseId: String, ticketId: String, eventId: String, sequence: Int) =
        listOf(VERSION, purchaseId, ticketId, eventId, sequence.toString()).joinToString(SEPARATOR)

    private fun sign(body: String, authCode: String?, secret: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$body$SEPARATOR${authCode.orEmpty()}$SEPARATOR$secret".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(SIGNATURE_LENGTH)
    }
}
