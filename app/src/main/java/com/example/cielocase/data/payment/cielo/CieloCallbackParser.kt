package com.example.cielocase.data.payment.cielo

import com.example.cielocase.domain.model.PaymentFailure
import com.example.cielocase.domain.model.PaymentFailureKind
import com.example.cielocase.domain.model.PaymentRecord
import com.example.cielocase.domain.payment.PaymentOutcome
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URLDecoder
import java.util.Base64
import javax.inject.Inject

/**
 * Translates the Cielo callback deep link into a [PaymentOutcome].
 *
 * Pure Kotlin/JVM on purpose: every branch (approved, cancelled, denied, authentication
 * error, corrupted payload) is covered by unit tests without an emulator.
 */
class CieloCallbackParser @Inject constructor(private val gson: Gson) {

    /** @return `null` when [uri] is not a Cielo payment callback. */
    fun parse(uri: String?): PaymentOutcome? {
        // Parsed by hand instead of java.net.URI/android.net.Uri: the terminal encodes the
        // payload with Base64.DEFAULT, which wraps lines every 76 chars, and those line
        // breaks make strict URI parsers reject the callback.
        val raw = uri?.trim() ?: return null
        if (!raw.startsWith("${CieloDeeplink.CALLBACK_SCHEME}://", ignoreCase = true)) return null

        val response = queryParameter(raw.substringAfter('?', ""), CieloDeeplink.PARAM_RESPONSE)
            ?: return unreadable("callback without the '${CieloDeeplink.PARAM_RESPONSE}' parameter")

        val json = decode(response) ?: return unreadable("response is not valid Base64/JSON")
        return if (json.has("code")) parseError(json) else parseOrder(json)
    }

    private fun decode(response: String): JsonObject? = runCatching {
        val bytes = Base64.getMimeDecoder().decode(response.filterNot { it.isWhitespace() })
        gson.fromJson(String(bytes, Charsets.UTF_8), JsonObject::class.java)
    }.getOrNull()

    private fun parseError(json: JsonObject): PaymentOutcome {
        val payload = runCatching { gson.fromJson(json, CieloErrorPayload::class.java) }
            .getOrNull() ?: return unreadable("unexpected error payload")
        return PaymentOutcome.Rejected(
            failure = PaymentFailure(
                kind = failureKind(payload.code),
                code = payload.code,
                reason = payload.description,
            ),
            reference = null,
        )
    }

    private fun parseOrder(json: JsonObject): PaymentOutcome {
        val order = runCatching { gson.fromJson(json, CieloOrderPayload::class.java) }
            .getOrNull() ?: return unreadable("unexpected order payload")
        val payment = order.payments?.lastOrNull()
            ?: return unreadable("order without payments")

        if (!payment.isAuthorized()) {
            return PaymentOutcome.Rejected(
                failure = PaymentFailure(
                    kind = PaymentFailureKind.PAYMENT_DENIED,
                    reason = payment.productName ?: order.status,
                ),
                reference = order.reference,
            )
        }

        return PaymentOutcome.Approved(
            record = PaymentRecord(
                orderId = order.id,
                paymentId = payment.id,
                authCode = payment.authCode,
                cieloCode = payment.cieloCode,
                brand = payment.brand?.takeIf { it.isNotBlank() },
                maskedCard = payment.mask,
                productName = payment.productName,
                installments = payment.installments ?: 0,
                amountInCents = payment.amount ?: order.paidAmount ?: order.price ?: 0,
                terminal = payment.terminal,
                paidAtEpochMillis = payment.requestDate?.toLongOrNull(),
            ),
            reference = order.reference,
        )
    }

    private fun CieloPaymentResponse.isAuthorized(): Boolean = statusCode == null ||
        statusCode == CieloDeeplink.StatusCode.AUTHORIZED ||
        statusCode == CieloDeeplink.StatusCode.PIX_PAYMENT

    private fun failureKind(code: Int?): PaymentFailureKind = when (code) {
        CieloDeeplink.ErrorCode.USER_CANCELLED -> PaymentFailureKind.USER_CANCELLED
        CieloDeeplink.ErrorCode.PAYMENT -> PaymentFailureKind.PAYMENT_DENIED
        CieloDeeplink.ErrorCode.AUTHENTICATION -> PaymentFailureKind.AUTHENTICATION
        else -> PaymentFailureKind.GENERIC
    }

    private fun unreadable(reason: String) = PaymentOutcome.Rejected(
        failure = PaymentFailure(kind = PaymentFailureKind.INVALID_RESPONSE, reason = reason),
        reference = null,
    )

    private fun queryParameter(rawQuery: String, name: String): String? = rawQuery
        .takeIf { it.isNotEmpty() }
        ?.split('&')
        ?.firstNotNullOfOrNull { parameter ->
            val (key, value) = parameter.split('=', limit = 2)
                .let { it.first() to it.getOrNull(1) }
            value?.takeIf { key == name }
        }
        ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
}
