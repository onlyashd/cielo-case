package com.example.cielocase.data.payment.cielo

import com.example.cielocase.domain.payment.PaymentRequest
import com.google.gson.Gson
import java.net.URLEncoder
import java.util.Base64
import javax.inject.Inject

/**
 * Builds the `lio://payment` deep link. Pure Kotlin/JVM (no Android types) so the wire
 * format can be unit tested.
 */
class CieloPaymentRequestFactory @Inject constructor(
    private val credentials: CieloCredentials,
    private val gson: Gson,
) {
    fun payload(request: PaymentRequest): CieloPaymentPayload = CieloPaymentPayload(
        accessToken = credentials.accessToken,
        clientId = credentials.clientId,
        reference = request.reference,
        merchantCode = credentials.merchantCode?.takeIf { it.isNotBlank() },
        email = request.customerEmail,
        installments = request.installments,
        items = request.items.map {
            CieloPaymentItemPayload(
                name = it.name,
                quantity = it.quantity,
                sku = it.sku,
                unitOfMeasure = it.unitOfMeasure,
                unitPrice = it.unitPriceInCents,
            )
        },
        paymentCode = request.paymentCode,
        value = request.totalInCents.toString(),
    )

    fun payloadJson(request: PaymentRequest): String = gson.toJson(payload(request))

    fun deeplink(
        request: PaymentRequest,
        callbackUrl: String = CieloDeeplink.CALLBACK_URL,
    ): String {
        val base64 = Base64.getEncoder()
            .encodeToString(payloadJson(request).toByteArray(Charsets.UTF_8))
        return buildString {
            append(CieloDeeplink.SCHEME).append("://").append(CieloDeeplink.HOST_PAYMENT)
            append('?').append(CieloDeeplink.PARAM_REQUEST).append('=').append(encode(base64))
            append('&').append(CieloDeeplink.PARAM_URL_CALLBACK).append('=')
                .append(encode(callbackUrl))
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
