package com.example.cielocase.payment

import com.example.cielocase.data.payment.cielo.CieloCallbackParser
import com.example.cielocase.domain.model.PaymentFailureKind
import com.example.cielocase.domain.payment.PaymentOutcome
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder
import java.util.Base64

/**
 * Covers the `order://response` payloads documented by Cielo
 * (https://docs.cielo.com.br/cielo-smart/docs/recuperando-dados and .../codigos-de-erro).
 */
class CieloCallbackParserTest {

    private val parser = CieloCallbackParser(Gson())

    @Test
    fun `parses an authorized order as approved`() {
        val outcome = parser.parse(callback(APPROVED_ORDER_JSON))

        val approved = outcome as PaymentOutcome.Approved
        assertEquals("Order-123", approved.reference)
        assertEquals("140126", approved.record.authCode)
        assertEquals("799871", approved.record.cieloCode)
        assertEquals("Visa", approved.record.brand)
        assertEquals("424242-4242", approved.record.maskedCard)
        assertEquals("CREDITO A VISTA - I", approved.record.productName)
        assertEquals(1450, approved.record.amountInCents)
        assertEquals("69000007", approved.record.terminal)
        assertEquals(1_528_476_655_000, approved.record.paidAtEpochMillis)
    }

    @Test
    fun `parses the documented cancellation payload as user cancelled`() {
        // Base64 exactly as published in the Cielo error-codes documentation.
        val outcome = parser.parse(
            "order://response?response=eyJjb2RlIjoxLCJyZWFzb24iOiJDQU5DRUxBRE8gUEVMTyBVU1XDgVJJTyJ9" +
                "&responsecode=0",
        )

        val rejected = outcome as PaymentOutcome.Rejected
        assertEquals(PaymentFailureKind.USER_CANCELLED, rejected.failure.kind)
        assertEquals(1, rejected.failure.code)
        assertEquals("CANCELADO PELO USUÁRIO", rejected.failure.reason)
        // Error payloads carry no order reference.
        assertNull(rejected.reference)
    }

    @Test
    fun `maps cielo error codes to failure kinds`() {
        assertEquals(
            PaymentFailureKind.GENERIC,
            rejectedKind("""{"code":2,"reason":"ERRO GENERICO"}"""),
        )
        assertEquals(
            PaymentFailureKind.PAYMENT_DENIED,
            rejectedKind("""{"code":3,"reason":"ERRO NO PAGAMENTO"}"""),
        )
        assertEquals(
            PaymentFailureKind.AUTHENTICATION,
            rejectedKind("""{"code":4,"reason":"ERRO DE AUTENTICACAO"}"""),
        )
        assertEquals(
            PaymentFailureKind.GENERIC,
            rejectedKind("""{"code":99,"reason":"DESCONHECIDO"}"""),
        )
    }

    @Test
    fun `treats a reversal status code as denied instead of approved`() {
        val reversed = APPROVED_ORDER_JSON.replace("\"statusCode\": \"1\"", "\"statusCode\": \"2\"")

        val rejected = parser.parse(callback(reversed)) as PaymentOutcome.Rejected

        assertEquals(PaymentFailureKind.PAYMENT_DENIED, rejected.failure.kind)
        assertEquals("Order-123", rejected.reference)
    }

    @Test
    fun `reports unreadable payloads as invalid response`() {
        assertEquals(
            PaymentFailureKind.INVALID_RESPONSE,
            (parser.parse("order://response?response=!!!not-base64!!!") as PaymentOutcome.Rejected)
                .failure.kind,
        )
        assertEquals(
            PaymentFailureKind.INVALID_RESPONSE,
            (parser.parse("order://response?responsecode=0") as PaymentOutcome.Rejected)
                .failure.kind,
        )
        assertEquals(
            PaymentFailureKind.INVALID_RESPONSE,
            (parser.parse(callback("""{"id":"x","payments":[]}""")) as PaymentOutcome.Rejected)
                .failure.kind,
        )
    }

    /**
     * The terminal encodes the payload with `Base64.DEFAULT`, which wraps lines every 76
     * characters. Reproduced against the Cielo Smart emulator.
     */
    @Test
    fun `parses payloads whose base64 contains line breaks`() {
        val base64 = Base64.getMimeEncoder().encodeToString(
            APPROVED_ORDER_JSON.toByteArray(Charsets.UTF_8),
        )
        val uri = "order://response?response=$base64&responsecode=0"

        assertTrue(base64.contains('\n'))
        assertTrue(parser.parse(uri) is PaymentOutcome.Approved)
    }

    @Test
    fun `ignores deep links that are not payment callbacks`() {
        assertNull(parser.parse(null))
        assertNull(parser.parse("https://example.com/response?response=abc"))
        assertNull(parser.parse("lio://payment?request=abc"))
    }

    @Test
    fun `accepts a pix payment status code`() {
        val pix = APPROVED_ORDER_JSON.replace("\"statusCode\": \"1\"", "\"statusCode\": \"0\"")

        assertTrue(parser.parse(callback(pix)) is PaymentOutcome.Approved)
    }

    private fun rejectedKind(json: String): PaymentFailureKind =
        (parser.parse(callback(json)) as PaymentOutcome.Rejected).failure.kind

    private fun callback(json: String): String {
        val base64 = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
        return "order://response?response=${URLEncoder.encode(base64, "UTF-8")}&responsecode=0"
    }

    private companion object {
        /** Trimmed version of the order returned by Cielo after an authorized payment. */
        val APPROVED_ORDER_JSON = """
            {
              "createdAt": "Jun 8, 2018 1:51:58 PM",
              "id": "ba583f85-9252-48b5-8fed-12719ff058b9",
              "paidAmount": 1450,
              "price": 1450,
              "reference": "Order-123",
              "status": "ENTERED",
              "type": "PAYMENT",
              "payments": [
                {
                  "id": "bb9c6305-95e5-4024-8152-503d064c0224",
                  "amount": 1450,
                  "authCode": "140126",
                  "brand": "Visa",
                  "cieloCode": "799871",
                  "installments": 0,
                  "mask": "424242-4242",
                  "terminal": "69000007",
                  "requestDate": "1528476655000",
                  "paymentFields": {
                    "statusCode": "1",
                    "productName": "CREDITO A VISTA - I",
                    "merchantName": "POSTO ABC"
                  }
                }
              ]
            }
        """.trimIndent()
    }
}
