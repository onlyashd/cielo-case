package com.example.cielocase.domain.model

/**
 * An event available for ticket sales. Prices are always handled in cents to avoid
 * floating point rounding issues (the Cielo contract also expects cents).
 */
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val startsAtEpochMillis: Long,
    val location: String,
    val unitPriceInCents: Long,
    val availableTickets: Int,
    val salesOpen: Boolean,
) {
    val isSoldOut: Boolean get() = availableTickets <= 0

    val isPurchasable: Boolean get() = salesOpen && !isSoldOut

    fun maxTicketsPerPurchase(limit: Int = MAX_TICKETS_PER_PURCHASE): Int =
        minOf(limit, availableTickets)

    companion object {
        const val MAX_TICKETS_PER_PURCHASE = 6
    }
}
