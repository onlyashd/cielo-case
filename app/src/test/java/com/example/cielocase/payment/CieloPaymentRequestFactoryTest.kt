package com.example.cielocase.payment

import com.example.cielocase.data.payment.cielo.CieloCredentials
import com.example.cielocase.data.payment.cielo.CieloDeeplink
import com.example.cielocase.data.payment.cielo.CieloPaymentRequestFactory
import com.example.cielocase.domain.payment.PaymentItem
import com.example.cielocase.domain.payment.PaymentRequest
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.util.Base64

/** Guards the wire format of `lio://payment` (Cielo Smart deep link contract). */
class CieloPaymentRequestFactoryTest {

    private val gson = Gson()

    private val factory = CieloPaymentRequestFactory(
        credentials = CieloCredentials(
            clientId = "client-id",
            accessToken = "access-token",
            merchantCode = "0012397373089400",
        ),
        gson = gson,
    )

    private val request = PaymentRequest(
        reference = "purchase-1",
        totalInCents = 37_980,
        items = listOf(
            PaymentItem(sku = "evt-1", name = "Rock na Praça", quantity = 2, unitPriceInCents = 18_990),
        ),
        paymentCode = PaymentRequest.PAYMENT_CODE_CREDIT_UP_FRONT,
    )

    @Test
    fun `builds the payment deep link with request and callback parameters`() {
        val deeplink = factory.deeplink(request)

        assertTrue(deeplink.startsWith("lio://payment?"))
        assertEquals(
            CieloDeeplink.CALLBACK_URL,
            queryParameter(deeplink, CieloDeeplink.PARAM_URL_CALLBACK),
        )
    }

    @Test
    fun `encodes the payload as base64 json following the Cielo contract`() {
        val deeplink = factory.deeplink(request)
        val encoded = queryParameter(deeplink, CieloDeeplink.PARAM_REQUEST)

        val json = gson.fromJson(
            String(Base64.getDecoder().decode(encoded), Charsets.UTF_8),
            JsonObject::class.java,
        )

        assertEquals("client-id", json["clientID"].asString)
        assertEquals("access-token", json["accessToken"].asString)
        assertEquals("purchase-1", json["reference"].asString)
        assertEquals("0012397373089400", json["merchantCode"].asString)
        assertEquals("CREDITO_AVISTA", json["paymentCode"].asString)
        assertEquals(0, json["installments"].asInt)
        // The total is sent in cents, as a string.
        assertEquals("37980", json["value"].asString)

        val item = json["items"].asJsonArray.single().asJsonObject
        assertEquals("evt-1", item["sku"].asString)
        assertEquals(2, item["quantity"].asInt)
        assertEquals(18_990, item["unitPrice"].asLong)
        assertEquals("unidade", item["unitOfMeasure"].asString)
    }

    @Test
    fun `omits optional fields that were not configured`() {
        val withoutMerchant = CieloPaymentRequestFactory(
            credentials = CieloCredentials(clientId = "c", accessToken = "t", merchantCode = ""),
            gson = gson,
        )

        val json = gson.fromJson(withoutMerchant.payloadJson(request), JsonObject::class.java)

        assertNull(json["merchantCode"])
        assertNull(json["email"])
    }

    private fun queryParameter(uri: String, name: String): String =
        uri.substringAfter('?')
            .split('&')
            .first { it.startsWith("$name=") }
            .substringAfter('=')
            .let { URLDecoder.decode(it, "UTF-8") }
}
