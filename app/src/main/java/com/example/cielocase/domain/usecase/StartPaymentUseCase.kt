package com.example.cielocase.domain.usecase

import com.example.cielocase.domain.Clock
import com.example.cielocase.domain.PurchaseRepository
import com.example.cielocase.domain.model.PaymentFailure
import com.example.cielocase.domain.model.Purchase
import com.example.cielocase.domain.payment.InFlightPaymentStore
import com.example.cielocase.domain.payment.PaymentGateway
import com.example.cielocase.domain.payment.PaymentItem
import com.example.cielocase.domain.payment.PaymentLaunchResult
import com.example.cielocase.domain.payment.PaymentRequest
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * Sends a pending purchase to the payment application.
 *
 * Guards against duplicate charges: a purchase that is already settled or that has a payment
 * in flight is never sent again. After [IN_FLIGHT_TIMEOUT] (payment app killed, terminal
 * rebooted, callback lost) a retry is allowed reusing the same `reference`.
 */
class StartPaymentUseCase @Inject constructor(
    private val purchases: PurchaseRepository,
    private val gateway: PaymentGateway,
    private val inFlightPayments: InFlightPaymentStore,
    private val clock: Clock,
) {
    suspend operator fun invoke(purchaseId: String, paymentCode: String): Result {
        val purchase = purchases.getPurchase(purchaseId) ?: return Result.PurchaseNotFound
        if (purchase.status.isTerminal) return Result.AlreadySettled(purchase)
        if (isPaymentInFlight(purchase)) return Result.AlreadyInProgress(purchase)

        val now = clock.nowEpochMillis()
        purchases.markPaymentStarted(purchase.id, now)
        inFlightPayments.set(purchase.id)

        return when (val launch = gateway.start(purchase.toPaymentRequest(paymentCode))) {
            PaymentLaunchResult.Launched -> Result.Launched(purchase)
            is PaymentLaunchResult.NotLaunched -> {
                // Nothing was charged: release the in-flight guard so the user can retry.
                inFlightPayments.clear()
                purchases.clearPaymentStarted(purchase.id)
                Result.NotLaunched(launch.failure)
            }
        }
    }

    private fun isPaymentInFlight(purchase: Purchase): Boolean {
        val startedAt = purchase.paymentStartedAtEpochMillis ?: return false
        return clock.nowEpochMillis() - startedAt < IN_FLIGHT_TIMEOUT_MILLIS
    }

    private fun Purchase.toPaymentRequest(paymentCode: String) = PaymentRequest(
        reference = id,
        totalInCents = totalInCents,
        paymentCode = paymentCode,
        items = listOf(
            PaymentItem(
                sku = eventId,
                name = eventTitle.take(MAX_ITEM_NAME_LENGTH),
                quantity = quantity,
                unitPriceInCents = unitPriceInCents,
            ),
        ),
    )

    sealed interface Result {
        data class Launched(val purchase: Purchase) : Result
        data class AlreadyInProgress(val purchase: Purchase) : Result
        data class AlreadySettled(val purchase: Purchase) : Result
        data class NotLaunched(val failure: PaymentFailure) : Result
        data object PurchaseNotFound : Result
    }

    companion object {
        val IN_FLIGHT_TIMEOUT = 3.minutes
        private val IN_FLIGHT_TIMEOUT_MILLIS = IN_FLIGHT_TIMEOUT.inWholeMilliseconds
        private const val MAX_ITEM_NAME_LENGTH = 40
    }
}
