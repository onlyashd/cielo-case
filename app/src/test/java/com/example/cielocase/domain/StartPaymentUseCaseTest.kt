package com.example.cielocase.domain

import com.example.cielocase.domain.model.PaymentFailure
import com.example.cielocase.domain.model.PaymentFailureKind
import com.example.cielocase.domain.payment.PaymentLaunchResult
import com.example.cielocase.domain.payment.PaymentRequest
import com.example.cielocase.domain.usecase.StartCheckoutUseCase
import com.example.cielocase.domain.usecase.StartPaymentUseCase
import com.example.cielocase.fake.FakeEventRepository
import com.example.cielocase.fake.FakeInFlightPaymentStore
import com.example.cielocase.fake.FakePaymentGateway
import com.example.cielocase.fake.FakePurchaseRepository
import com.example.cielocase.fake.MutableClock
import com.example.cielocase.fake.TestEvents
import com.example.cielocase.fake.approved
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The "no double charge on a re-sent action" rules. */
class StartPaymentUseCaseTest {

    private val clock = MutableClock()
    private val events = FakeEventRepository(TestEvents.all)
    private val purchases = FakePurchaseRepository(events, clock)
    private val gateway = FakePaymentGateway()
    private val inFlightPayments = FakeInFlightPaymentStore()

    private val startCheckout = StartCheckoutUseCase(events, purchases)
    private val startPayment = StartPaymentUseCase(purchases, gateway, inFlightPayments, clock)

    @Test
    fun `sends the purchase id as the cielo reference`() = runTest {
        val purchase = pendingPurchase()

        val result = startPayment(purchase.id, PaymentRequest.PAYMENT_CODE_CREDIT_UP_FRONT)

        assertTrue(result is StartPaymentUseCase.Result.Launched)
        assertEquals(1, gateway.requests.size)
        with(gateway.requests.single()) {
            assertEquals(purchase.id, reference)
            assertEquals(purchase.totalInCents, totalInCents)
            assertEquals(purchase.quantity, items.single().quantity)
        }
        assertEquals(purchase.id, inFlightPayments.get())
    }

    @Test
    fun `does not launch a second payment while one is in flight`() = runTest {
        val purchase = pendingPurchase()

        startPayment(purchase.id, PaymentRequest.PAYMENT_CODE_CREDIT_UP_FRONT)
        val second = startPayment(purchase.id, PaymentRequest.PAYMENT_CODE_CREDIT_UP_FRONT)

        assertTrue(second is StartPaymentUseCase.Result.AlreadyInProgress)
        assertEquals(1, gateway.requests.size)
    }

    @Test
    fun `allows retrying with the same reference after the in-flight timeout`() = runTest {
        val purchase = pendingPurchase()
        startPayment(purchase.id, PaymentRequest.PAYMENT_CODE_CREDIT_UP_FRONT)

        clock.now += StartPaymentUseCase.IN_FLIGHT_TIMEOUT.inWholeMilliseconds + 1
        val retry = startPayment(purchase.id, PaymentRequest.PAYMENT_CODE_CREDIT_UP_FRONT)

        assertTrue(retry is StartPaymentUseCase.Result.Launched)
        assertEquals(2, gateway.requests.size)
        assertEquals(
            gateway.requests[0].reference,
            gateway.requests[1].reference,
        )
    }

    @Test
    fun `never pays a purchase that is already settled`() = runTest {
        val purchase = pendingPurchase()
        purchases.settle(purchase.id, approved(purchase.id, purchase.totalInCents))

        val result = startPayment(purchase.id, PaymentRequest.PAYMENT_CODE_CREDIT_UP_FRONT)

        assertTrue(result is StartPaymentUseCase.Result.AlreadySettled)
        assertTrue(gateway.requests.isEmpty())
    }

    @Test
    fun `releases the guard when the payment app cannot be launched`() = runTest {
        val purchase = pendingPurchase()
        gateway.result = PaymentLaunchResult.NotLaunched(
            PaymentFailure(kind = PaymentFailureKind.CIELO_APP_UNAVAILABLE),
        )

        val result = startPayment(purchase.id, PaymentRequest.PAYMENT_CODE_CREDIT_UP_FRONT)

        assertEquals(
            PaymentFailureKind.CIELO_APP_UNAVAILABLE,
            (result as StartPaymentUseCase.Result.NotLaunched).failure.kind,
        )
        assertNull(inFlightPayments.get())
        assertNull(purchases.getPurchase(purchase.id)?.paymentStartedAtEpochMillis)
    }

    @Test
    fun `reports unknown purchases`() = runTest {
        assertEquals(
            StartPaymentUseCase.Result.PurchaseNotFound,
            startPayment("nope", PaymentRequest.PAYMENT_CODE_CREDIT_UP_FRONT),
        )
    }

    private suspend fun pendingPurchase(quantity: Int = 2) =
        (startCheckout(TestEvents.openEvent.id, quantity) as StartCheckoutUseCase.Result.Success)
            .purchase
}
