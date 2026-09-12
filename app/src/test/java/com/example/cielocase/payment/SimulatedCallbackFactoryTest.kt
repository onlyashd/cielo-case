package com.example.cielocase.payment

import com.example.cielocase.data.payment.cielo.CieloCallbackParser
import com.example.cielocase.data.payment.simulator.SimulatedCallbackFactory
import com.example.cielocase.domain.model.PaymentFailureKind
import com.example.cielocase.domain.payment.PaymentOutcome
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

/** The simulator must produce payloads the production parser understands. */
class SimulatedCallbackFactoryTest {

    private val gson = Gson()
    private val factory = SimulatedCallbackFactory(gson)
    private val parser = CieloCallbackParser(gson)

    @Test
    fun `approved simulation round-trips through the real parser`() {
        val uri = factory.callbackUri(
            reference = "purchase-1",
            amountInCents = 37_980,
            outcome = SimulatedCallbackFactory.Outcome.APPROVED,
            nowEpochMillis = 1_700_000_000_000,
        )

        val approved = parser.parse(uri) as PaymentOutcome.Approved
        assertEquals("purchase-1", approved.reference)
        assertEquals(37_980, approved.record.amountInCents)
        assertEquals(1_700_000_000_000, approved.record.paidAtEpochMillis)
    }

    @Test
    fun `rejected simulations map to the documented cielo error codes`() {
        assertEquals(
            PaymentFailureKind.PAYMENT_DENIED,
            failureKind(SimulatedCallbackFactory.Outcome.DENIED),
        )
        assertEquals(
            PaymentFailureKind.USER_CANCELLED,
            failureKind(SimulatedCallbackFactory.Outcome.CANCELLED),
        )
        assertEquals(
            PaymentFailureKind.INVALID_RESPONSE,
            failureKind(SimulatedCallbackFactory.Outcome.CORRUPTED_RESPONSE),
        )
    }

    private fun failureKind(outcome: SimulatedCallbackFactory.Outcome): PaymentFailureKind {
        val uri = factory.callbackUri("purchase-1", 1_000, outcome)
        return (parser.parse(uri) as PaymentOutcome.Rejected).failure.kind
    }
}
