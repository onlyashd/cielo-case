package com.example.cielocase.fake

import com.example.cielocase.domain.Clock
import com.example.cielocase.domain.EventRepository
import com.example.cielocase.domain.IdGenerator
import com.example.cielocase.domain.PurchaseRepository
import com.example.cielocase.domain.SettleResult
import com.example.cielocase.domain.model.Event
import com.example.cielocase.domain.model.Purchase
import com.example.cielocase.domain.model.PurchaseStatus
import com.example.cielocase.domain.model.Ticket
import com.example.cielocase.domain.model.TicketQrPayload
import com.example.cielocase.domain.payment.InFlightPaymentStore
import com.example.cielocase.domain.payment.PaymentGateway
import com.example.cielocase.domain.payment.PaymentLaunchResult
import com.example.cielocase.domain.payment.PaymentOutcome
import com.example.cielocase.domain.payment.PaymentRequest
import com.example.cielocase.domain.payment.PurchaseSettlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MutableClock(var now: Long = 1_700_000_000_000) : Clock {
    override fun nowEpochMillis(): Long = now
}

class SequentialIdGenerator(private val prefix: String = "id") : IdGenerator {
    private var counter = 0
    override fun newId(): String = "$prefix-${++counter}"
}

class FakeInFlightPaymentStore(private var reference: String? = null) : InFlightPaymentStore {
    override fun get(): String? = reference
    override fun set(reference: String) {
        this.reference = reference
    }

    override fun clear() {
        reference = null
    }
}

class FakePaymentGateway(
    var result: PaymentLaunchResult = PaymentLaunchResult.Launched,
) : PaymentGateway {
    val requests = mutableListOf<PaymentRequest>()

    override fun start(request: PaymentRequest): PaymentLaunchResult {
        requests += request
        return result
    }
}

class FakeEventRepository(events: List<Event>) : EventRepository {
    private val state = MutableStateFlow(events.associateBy { it.id })

    override fun observeEvents(): Flow<List<Event>> = state.map { it.values.toList() }

    override fun observeEvent(eventId: String): Flow<Event?> = state.map { it[eventId] }

    override suspend fun getEvent(eventId: String): Event? = state.value[eventId]

    fun reserve(eventId: String, quantity: Int) = update(eventId, -quantity)

    fun release(eventId: String, quantity: Int) = update(eventId, quantity)

    private fun update(eventId: String, delta: Int) {
        val event = state.value[eventId] ?: return
        state.value = state.value +
            (eventId to event.copy(availableTickets = event.availableTickets + delta))
    }
}

/**
 * In-memory [PurchaseRepository] mirroring the Room implementation (same [PurchaseSettlement]
 * decisions and the same "issue tickets only once" rule).
 */
class FakePurchaseRepository(
    private val events: FakeEventRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator = SequentialIdGenerator("purchase"),
) : PurchaseRepository {

    private val purchases = MutableStateFlow(emptyMap<String, Purchase>())
    private val tickets = MutableStateFlow(emptyList<Ticket>())

    override suspend fun createOrReusePending(event: Event, quantity: Int): Purchase {
        purchases.value.values
            .firstOrNull {
                it.eventId == event.id &&
                    it.quantity == quantity &&
                    it.status == PurchaseStatus.PENDING
            }
            ?.let { return it }

        val now = clock.nowEpochMillis()
        val purchase = Purchase(
            id = idGenerator.newId(),
            eventId = event.id,
            eventTitle = event.title,
            quantity = quantity,
            unitPriceInCents = event.unitPriceInCents,
            status = PurchaseStatus.PENDING,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        events.reserve(event.id, quantity)
        save(purchase)
        return purchase
    }

    override fun observePurchase(purchaseId: String): Flow<Purchase?> =
        purchases.map { it[purchaseId] }

    override suspend fun getPurchase(purchaseId: String): Purchase? = purchases.value[purchaseId]

    override suspend fun markPaymentStarted(purchaseId: String, nowEpochMillis: Long) {
        val purchase = purchases.value[purchaseId] ?: return
        if (purchase.status != PurchaseStatus.PENDING) return
        save(purchase.copy(paymentStartedAtEpochMillis = nowEpochMillis))
    }

    override suspend fun clearPaymentStarted(purchaseId: String) {
        val purchase = purchases.value[purchaseId] ?: return
        save(purchase.copy(paymentStartedAtEpochMillis = null))
    }

    override suspend fun settle(reference: String, outcome: PaymentOutcome): SettleResult {
        val current = purchases.value[reference] ?: return SettleResult.UnknownReference

        return when (
            val decision = PurchaseSettlement.decide(current, outcome, clock.nowEpochMillis())
        ) {
            is PurchaseSettlement.Decision.AlreadySettled ->
                SettleResult.Duplicate(decision.purchase)

            is PurchaseSettlement.Decision.Settle -> {
                save(decision.purchase)
                val issued = if (decision.issueTickets) {
                    issueTickets(decision.purchase)
                } else {
                    events.release(current.eventId, current.quantity)
                    emptyList()
                }
                SettleResult.Settled(decision.purchase, issued)
            }
        }
    }

    override fun observeTickets(): Flow<List<Ticket>> = tickets

    override fun observeTicketsOfPurchase(purchaseId: String): Flow<List<Ticket>> =
        tickets.map { list -> list.filter { it.purchaseId == purchaseId } }

    fun ticketsOf(purchaseId: String): List<Ticket> =
        tickets.value.filter { it.purchaseId == purchaseId }

    private fun issueTickets(purchase: Purchase): List<Ticket> {
        val existing = ticketsOf(purchase.id)
        if (existing.isNotEmpty()) return existing

        val issued = (1..purchase.quantity).map { sequence ->
            val ticketId = idGenerator.newId()
            Ticket(
                id = ticketId,
                purchaseId = purchase.id,
                eventId = purchase.eventId,
                eventTitle = purchase.eventTitle,
                sequence = sequence,
                qrPayload = TicketQrPayload.create(
                    purchaseId = purchase.id,
                    ticketId = ticketId,
                    eventId = purchase.eventId,
                    sequence = sequence,
                    authCode = purchase.payment?.authCode,
                    secret = SECRET,
                ),
                issuedAtEpochMillis = clock.nowEpochMillis(),
            )
        }
        tickets.value = tickets.value + issued
        return issued
    }

    private fun save(purchase: Purchase) {
        purchases.value = purchases.value + (purchase.id to purchase)
    }

    companion object {
        const val SECRET = "test-secret"
    }
}
