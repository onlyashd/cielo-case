package com.example.cielocase.data.payment.simulator

import com.example.cielocase.data.payment.cielo.CieloCallbackHandler
import com.example.cielocase.domain.payment.InFlightPaymentStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feeds a simulated Cielo callback into the real pipeline, so the purchase flow can be
 * demoed and reviewed on a device without the Cielo Smart emulator. Only surfaced by the UI
 * on debug builds (`PAYMENT_SIMULATOR_ENABLED`).
 */
@Singleton
class PaymentSimulator @Inject constructor(
    private val callbackFactory: SimulatedCallbackFactory,
    private val callbackHandler: CieloCallbackHandler,
    private val inFlightPayments: InFlightPaymentStore,
) {
    fun simulate(
        reference: String,
        amountInCents: Long,
        outcome: SimulatedCallbackFactory.Outcome,
    ) {
        // Error payloads carry no reference, exactly like the real integration.
        inFlightPayments.set(reference)
        callbackHandler.handle(callbackFactory.callbackUri(reference, amountInCents, outcome))
    }
}
