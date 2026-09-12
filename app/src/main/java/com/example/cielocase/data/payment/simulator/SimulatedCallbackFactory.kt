package com.example.cielocase.data.payment.simulator

import com.example.cielocase.data.payment.cielo.CieloDeeplink
import com.google.gson.Gson
import java.net.URLEncoder
import java.util.Base64
import javax.inject.Inject

/**
 * Builds callback deep links identical to the ones sent by the Cielo application, so the
 * production parsing/settlement path is exercised by the simulator and by unit tests.
 */
class SimulatedCallbackFactory @Inject constructor(private val gson: Gson) {

    enum class Outcome { APPROVED, DENIED, CANCELLED, CORRUPTED_RESPONSE }

    fun callbackUri(
        reference: String,
        amountInCents: Long,
        outcome: Outcome,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): String {
        val response = when (outcome) {
            Outcome.CORRUPTED_RESPONSE -> "not-a-base64-payload"
            else -> base64(json(reference, amountInCents, outcome, nowEpochMillis))
        }
        return CieloDeeplink.CALLBACK_URL + "?" + CieloDeeplink.PARAM_RESPONSE + "=" +
            URLEncoder.encode(response, "UTF-8") + "&responsecode=0"
    }

    private fun json(
        reference: String,
        amountInCents: Long,
        outcome: Outcome,
        nowEpochMillis: Long,
    ): String = when (outcome) {
        Outcome.APPROVED -> gson.toJson(approvedOrder(reference, amountInCents, nowEpochMillis))
        Outcome.DENIED -> gson.toJson(
            mapOf("code" to CieloDeeplink.ErrorCode.PAYMENT, "reason" to "TRANSACAO NEGADA"),
        )
        Outcome.CANCELLED -> gson.toJson(
            mapOf(
                "code" to CieloDeeplink.ErrorCode.USER_CANCELLED,
                "reason" to "CANCELADO PELO USUÁRIO",
            ),
        )
        Outcome.CORRUPTED_RESPONSE -> ""
    }

    private fun approvedOrder(
        reference: String,
        amountInCents: Long,
        nowEpochMillis: Long,
    ) = mapOf(
        "id" to "simulated-order-${reference.take(SHORT_ID_LENGTH)}",
        "reference" to reference,
        "status" to "ENTERED",
        "type" to "PAYMENT",
        "price" to amountInCents,
        "paidAmount" to amountInCents,
        "payments" to listOf(
            mapOf(
                "id" to "simulated-payment-${reference.take(SHORT_ID_LENGTH)}",
                "amount" to amountInCents,
                "authCode" to "SIM%06d".format(amountInCents % SIMULATED_CODE_MODULO),
                "cieloCode" to "999999",
                "brand" to "Visa",
                "mask" to "************0000",
                "installments" to 0,
                "terminal" to "EMULADOR",
                "requestDate" to nowEpochMillis.toString(),
                "paymentFields" to mapOf(
                    "statusCode" to CieloDeeplink.StatusCode.AUTHORIZED,
                    "productName" to "CREDITO A VISTA - I",
                ),
            ),
        ),
    )

    private fun base64(json: String): String =
        Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))

    private companion object {
        const val SHORT_ID_LENGTH = 8
        const val SIMULATED_CODE_MODULO = 1_000_000
    }
}
