package com.example.cielocase.domain.usecase

import com.example.cielocase.domain.EventRepository
import com.example.cielocase.domain.PurchaseRepository
import com.example.cielocase.domain.model.Event
import com.example.cielocase.domain.model.Purchase
import javax.inject.Inject

/** Validates the selected quantity and creates (or reuses) the pending purchase. */
class StartCheckoutUseCase @Inject constructor(
    private val events: EventRepository,
    private val purchases: PurchaseRepository,
) {
    suspend operator fun invoke(eventId: String, quantity: Int): Result {
        val event = events.getEvent(eventId) ?: return Result.EventNotFound
        if (!event.salesOpen) return Result.SalesClosed
        if (quantity < 1 || quantity > Event.MAX_TICKETS_PER_PURCHASE) {
            return Result.InvalidQuantity(Event.MAX_TICKETS_PER_PURCHASE)
        }
        if (quantity > event.availableTickets) {
            return Result.NotEnoughTickets(event.availableTickets)
        }
        return Result.Success(purchases.createOrReusePending(event, quantity))
    }

    sealed interface Result {
        data class Success(val purchase: Purchase) : Result
        data object EventNotFound : Result
        data object SalesClosed : Result
        data class InvalidQuantity(val max: Int) : Result
        data class NotEnoughTickets(val available: Int) : Result
    }
}
