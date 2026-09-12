package com.example.cielocase.domain.payment

import com.example.cielocase.domain.model.PaymentFailure
import com.example.cielocase.domain.model.PaymentRecord

/**
 * Starts a payment on an external payment application.
 *
 * The flow is asynchronous and out-of-process: [start] only launches the payment app and
 * the result arrives later through a callback deep link (see `CieloCallbackHandler`).
 */
interface PaymentGateway {
    fun start(request: PaymentRequest): PaymentLaunchResult
}

/**
 * @param reference idempotency key sent to Cielo (the purchase id).
 * @param totalInCents total amount in cents.
 * @param installments `0` means a single, up-front payment.
 */
data class PaymentRequest(
    val reference: String,
    val totalInCents: Long,
    val items: List<PaymentItem>,
    val paymentCode: String = PAYMENT_CODE_CREDIT_UP_FRONT,
    val installments: Int = 0,
    val customerEmail: String? = null,
) {
    companion object {
        const val PAYMENT_CODE_CREDIT_UP_FRONT = "CREDITO_AVISTA"
        const val PAYMENT_CODE_DEBIT_UP_FRONT = "DEBITO_AVISTA"
        const val PAYMENT_CODE_PIX = "PIX"
    }
}

data class PaymentItem(
    val sku: String,
    val name: String,
    val quantity: Int,
    val unitPriceInCents: Long,
    val unitOfMeasure: String = "unidade",
)

sealed interface PaymentLaunchResult {
    /** The payment app was launched; the result will arrive through the callback. */
    data object Launched : PaymentLaunchResult

    /** The payment could not even be started, so nothing was charged. */
    data class NotLaunched(val failure: PaymentFailure) : PaymentLaunchResult
}

/** Result of a payment, already translated from the Cielo payload into domain terms. */
sealed interface PaymentOutcome {
    data class Approved(val record: PaymentRecord, val reference: String?) : PaymentOutcome

    data class Rejected(val failure: PaymentFailure, val reference: String?) : PaymentOutcome
}
