package com.example.cielocase.domain.usecase

import com.example.cielocase.domain.PurchaseRepository
import com.example.cielocase.domain.SettleResult
import com.example.cielocase.domain.payment.InFlightPaymentStore
import com.example.cielocase.domain.payment.PaymentOutcome
import javax.inject.Inject

/**
 * Records the result of a payment (approved, denied or cancelled).
 *
 * The reference comes from the Cielo order payload when available; error payloads have no
 * reference, so the locally stored in-flight reference is used as fallback.
 */
class RegisterPaymentResultUseCase @Inject constructor(
    private val purchases: PurchaseRepository,
    private val inFlightPayments: InFlightPaymentStore,
) {
    suspend operator fun invoke(outcome: PaymentOutcome): SettleResult {
        val reference = outcome.reference()?.takeIf { it.isNotBlank() }
            ?: inFlightPayments.get()
            ?: return SettleResult.UnknownReference

        return purchases.settle(reference, outcome).also { inFlightPayments.clear() }
    }

    private fun PaymentOutcome.reference(): String? = when (this) {
        is PaymentOutcome.Approved -> reference
        is PaymentOutcome.Rejected -> reference
    }
}
