package com.example.cielocase.domain

import com.example.cielocase.domain.model.PaymentFailureKind
import com.example.cielocase.domain.model.PurchaseStatus
import com.example.cielocase.domain.model.TicketQrPayload
import com.example.cielocase.domain.usecase.RegisterPaymentResultUseCase
import com.example.cielocase.domain.usecase.StartCheckoutUseCase
import com.example.cielocase.fake.FakeEventRepository
import com.example.cielocase.fake.FakeInFlightPaymentStore
import com.example.cielocase.fake.FakePurchaseRepository
import com.example.cielocase.fake.MutableClock
import com.example.cielocase.fake.TestEvents
import com.example.cielocase.fake.approved
import com.example.cielocase.fake.rejected
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Recording of the purchase result: approved, denied, cancelled and duplicated callbacks. */
class RegisterPaymentResultUseCaseTest {

    private val clock = MutableClock()
    private val events = FakeEventRepository(TestEvents.all)
    private val purchases = FakePurchaseRepository(events, clock)
    private val inFlightPayments = FakeInFlightPaymentStore()

    private val startCheckout = StartCheckoutUseCase(events, purchases)
    private val registerResult = RegisterPaymentResultUseCase(purchases, inFlightPayments)

    @Test
    fun `approved payment issues one ticket per unit and keeps the reservation`() = runTest {
        val purchase = pendingPurchase(quantity = 3)

        val result = registerResult(approved(purchase.id, purchase.totalInCents))

        val settled = result as SettleResult.Settled
        assertEquals(PurchaseStatus.APPROVED, settled.purchase.status)
        assertEquals(3, settled.tickets.size)
        assertEquals(listOf(1, 2, 3), settled.tickets.map { it.sequence })
        assertEquals(7, events.getEvent(TestEvents.openEvent.id)?.availableTickets)
        assertNull(inFlightPayments.get())
    }

    @Test
    fun `tickets are bound to the concluded purchase`() = runTest {
        val purchase = pendingPurchase(quantity = 1)

        val settled = registerResult(
            approved(purchase.id, purchase.totalInCents, authCode = "654321"),
        ) as SettleResult.Settled
        val ticket = settled.tickets.single()

        assertEquals(purchase.id, ticket.purchaseId)
        assertTrue(ticket.qrPayload.contains(purchase.id))
        assertTrue(
            TicketQrPayload.verify(
                payload = ticket.qrPayload,
                authCode = "654321",
                secret = FakePurchaseRepository.SECRET,
            ),
        )
    }

    @Test
    fun `a duplicated callback does not issue tickets twice`() = runTest {
        val purchase = pendingPurchase(quantity = 2)
        val outcome = approved(purchase.id, purchase.totalInCents)

        val first = registerResult(outcome)
        val second = registerResult(outcome)

        assertEquals(2, (first as SettleResult.Settled).tickets.size)
        assertTrue(second is SettleResult.Duplicate)
        assertEquals(2, purchases.ticketsOf(purchase.id).size)
    }

    @Test
    fun `a denied payment issues no tickets and puts the inventory back on sale`() = runTest {
        val purchase = pendingPurchase(quantity = 2)

        val settled = registerResult(
            rejected(PaymentFailureKind.PAYMENT_DENIED, purchase.id, code = 3),
        ) as SettleResult.Settled

        assertEquals(PurchaseStatus.DENIED, settled.purchase.status)
        assertTrue(settled.tickets.isEmpty())
        assertEquals(10, events.getEvent(TestEvents.openEvent.id)?.availableTickets)
    }

    @Test
    fun `a cancellation without reference is settled through the in-flight reference`() = runTest {
        val purchase = pendingPurchase(quantity = 1)
        inFlightPayments.set(purchase.id)

        val settled = registerResult(
            rejected(PaymentFailureKind.USER_CANCELLED, reference = null, code = 1),
        ) as SettleResult.Settled

        assertEquals(purchase.id, settled.purchase.id)
        assertEquals(PurchaseStatus.CANCELLED, settled.purchase.status)
        assertTrue(purchases.ticketsOf(purchase.id).isEmpty())
    }

    @Test
    fun `integration failures are recorded as failed`() = runTest {
        val purchase = pendingPurchase(quantity = 1)

        val settled = registerResult(
            rejected(PaymentFailureKind.INVALID_RESPONSE, purchase.id),
        ) as SettleResult.Settled

        assertEquals(PurchaseStatus.FAILED, settled.purchase.status)
    }

    @Test
    fun `a callback without any known reference is ignored`() = runTest {
        assertEquals(
            SettleResult.UnknownReference,
            registerResult(rejected(PaymentFailureKind.GENERIC, reference = null)),
        )
    }

    @Test
    fun `a callback for an unknown purchase is ignored`() = runTest {
        assertEquals(
            SettleResult.UnknownReference,
            registerResult(approved("purchase-that-does-not-exist", 1_000)),
        )
    }

    private suspend fun pendingPurchase(quantity: Int) =
        (startCheckout(TestEvents.openEvent.id, quantity) as StartCheckoutUseCase.Result.Success)
            .purchase
}
