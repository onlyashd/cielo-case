package com.example.cielocase.domain

import com.example.cielocase.domain.model.Event
import com.example.cielocase.domain.model.Purchase
import com.example.cielocase.domain.model.Ticket
import com.example.cielocase.domain.payment.PaymentOutcome
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {

    /**
     * Returns the pending purchase for [event]/[quantity] when one already exists, otherwise
     * creates it (reserving the tickets). Re-submitting the checkout action therefore never
     * produces a second purchase - and never a second charge.
     */
    suspend fun createOrReusePending(event: Event, quantity: Int): Purchase

    fun observePurchase(purchaseId: String): Flow<Purchase?>

    suspend fun getPurchase(purchaseId: String): Purchase?

    /** Flags the purchase as waiting for a payment callback. */
    suspend fun markPaymentStarted(purchaseId: String, nowEpochMillis: Long)

    /** Clears the in-flight flag when the payment app could not be launched. */
    suspend fun clearPaymentStarted(purchaseId: String)

    /**
     * Applies [outcome] to the purchase identified by [reference] atomically.
     * Duplicated callbacks are no-ops.
     */
    suspend fun settle(reference: String, outcome: PaymentOutcome): SettleResult

    fun observeTickets(): Flow<List<Ticket>>

    fun observeTicketsOfPurchase(purchaseId: String): Flow<List<Ticket>>
}

sealed interface SettleResult {
    data class Settled(val purchase: Purchase, val tickets: List<Ticket>) : SettleResult

    /** The purchase was already in a terminal state: nothing changed. */
    data class Duplicate(val purchase: Purchase) : SettleResult

    /** A callback arrived for a reference this app does not know about. */
    data object UnknownReference : SettleResult
}
