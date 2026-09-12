package com.example.cielocase.domain

import com.example.cielocase.domain.model.Event
import com.example.cielocase.domain.usecase.StartCheckoutUseCase
import com.example.cielocase.fake.FakeEventRepository
import com.example.cielocase.fake.FakePurchaseRepository
import com.example.cielocase.fake.MutableClock
import com.example.cielocase.fake.TestEvents
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StartCheckoutUseCaseTest {

    private val clock = MutableClock()
    private val events = FakeEventRepository(TestEvents.all)
    private val purchases = FakePurchaseRepository(events, clock)
    private val startCheckout = StartCheckoutUseCase(events, purchases)

    @Test
    fun `creates a pending purchase with the selected quantity`() = runTest {
        val result = startCheckout(TestEvents.openEvent.id, quantity = 3)

        val purchase = (result as StartCheckoutUseCase.Result.Success).purchase
        assertEquals(3, purchase.quantity)
        assertEquals(30_000, purchase.totalInCents)
        // Tickets are reserved while the payment is pending.
        assertEquals(7, events.getEvent(TestEvents.openEvent.id)?.availableTickets)
    }

    @Test
    fun `re-submitting the same selection reuses the pending purchase`() = runTest {
        val first = startCheckout(TestEvents.openEvent.id, quantity = 2)
        val second = startCheckout(TestEvents.openEvent.id, quantity = 2)

        assertEquals(
            (first as StartCheckoutUseCase.Result.Success).purchase.id,
            (second as StartCheckoutUseCase.Result.Success).purchase.id,
        )
        // And it reserves the inventory only once.
        assertEquals(8, events.getEvent(TestEvents.openEvent.id)?.availableTickets)
    }

    @Test
    fun `rejects quantities above the purchase limit`() = runTest {
        val result = startCheckout(
            TestEvents.openEvent.id,
            quantity = Event.MAX_TICKETS_PER_PURCHASE + 1,
        )

        assertEquals(
            StartCheckoutUseCase.Result.InvalidQuantity(Event.MAX_TICKETS_PER_PURCHASE),
            result,
        )
    }

    @Test
    fun `rejects quantities above the available inventory`() = runTest {
        val result = startCheckout(TestEvents.almostSoldOut.id, quantity = 3)

        assertEquals(StartCheckoutUseCase.Result.NotEnoughTickets(available = 2), result)
    }

    @Test
    fun `rejects events with closed sales`() = runTest {
        assertEquals(
            StartCheckoutUseCase.Result.SalesClosed,
            startCheckout(TestEvents.salesClosed.id, quantity = 1),
        )
    }

    @Test
    fun `rejects unknown events`() = runTest {
        assertEquals(
            StartCheckoutUseCase.Result.EventNotFound,
            startCheckout("does-not-exist", quantity = 1),
        )
    }
}
