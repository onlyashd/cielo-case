package com.example.cielocase.domain.payment

import com.example.cielocase.domain.model.PaymentFailureKind
import com.example.cielocase.domain.model.Purchase
import com.example.cielocase.domain.model.PurchaseStatus

/**
 * Pure decision function that turns a [PaymentOutcome] into the next state of a [Purchase].
 *
 * Kept free of Android/Room dependencies so the critical rules (single settlement, no
 * double charge, no tickets for rejected payments) can be unit tested in isolation.
 */
object PurchaseSettlement {

    sealed interface Decision {
        /** The purchase must be updated and, when [issueTickets] is true, tickets issued. */
        data class Settle(val purchase: Purchase, val issueTickets: Boolean) : Decision

        /**
         * The purchase was already settled: the callback is a duplicate (re-delivered intent,
         * user re-sending the action, process restart) and must not change anything.
         */
        data class AlreadySettled(val purchase: Purchase) : Decision
    }

    fun decide(purchase: Purchase, outcome: PaymentOutcome, nowEpochMillis: Long): Decision {
        if (purchase.status.isTerminal) return Decision.AlreadySettled(purchase)

        return when (outcome) {
            is PaymentOutcome.Approved -> Decision.Settle(
                purchase = purchase.copy(
                    status = PurchaseStatus.APPROVED,
                    payment = outcome.record,
                    failure = null,
                    paymentStartedAtEpochMillis = null,
                    updatedAtEpochMillis = nowEpochMillis,
                ),
                issueTickets = true,
            )

            is PaymentOutcome.Rejected -> Decision.Settle(
                purchase = purchase.copy(
                    status = statusFor(outcome),
                    failure = outcome.failure,
                    paymentStartedAtEpochMillis = null,
                    updatedAtEpochMillis = nowEpochMillis,
                ),
                issueTickets = false,
            )
        }
    }

    private fun statusFor(outcome: PaymentOutcome.Rejected): PurchaseStatus =
        when (outcome.failure.kind) {
            PaymentFailureKind.USER_CANCELLED -> PurchaseStatus.CANCELLED
            PaymentFailureKind.PAYMENT_DENIED -> PurchaseStatus.DENIED
            else -> PurchaseStatus.FAILED
        }
}
