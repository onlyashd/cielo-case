package com.example.cielocase.data.repository

import androidx.room.withTransaction
import com.example.cielocase.data.local.AppDatabase
import com.example.cielocase.data.local.entity.toDomain
import com.example.cielocase.data.local.entity.toEntity
import com.example.cielocase.domain.Clock
import com.example.cielocase.domain.IdGenerator
import com.example.cielocase.domain.PurchaseRepository
import com.example.cielocase.domain.SettleResult
import com.example.cielocase.domain.model.Event
import com.example.cielocase.domain.model.Purchase
import com.example.cielocase.domain.model.PurchaseStatus
import com.example.cielocase.domain.model.Ticket
import com.example.cielocase.domain.model.TicketQrPayload
import com.example.cielocase.domain.payment.PaymentOutcome
import com.example.cielocase.domain.payment.PurchaseSettlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPurchaseRepository @Inject constructor(
    private val database: AppDatabase,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : PurchaseRepository {

    private val purchaseDao get() = database.purchaseDao()
    private val ticketDao get() = database.ticketDao()
    private val eventDao get() = database.eventDao()

    override suspend fun createOrReusePending(event: Event, quantity: Int): Purchase =
        database.withTransaction {
            purchaseDao.findPending(event.id, quantity)?.let { return@withTransaction it.toDomain() }

            require(eventDao.reserveTickets(event.id, quantity) > 0) {
                "not enough tickets available for ${event.id}"
            }

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
            purchaseDao.insert(purchase.toEntity())
            purchase
        }

    override fun observePurchase(purchaseId: String): Flow<Purchase?> =
        purchaseDao.observeById(purchaseId).map { it?.toDomain() }

    override suspend fun getPurchase(purchaseId: String): Purchase? =
        purchaseDao.getById(purchaseId)?.toDomain()

    override suspend fun markPaymentStarted(purchaseId: String, nowEpochMillis: Long) =
        purchaseDao.markPaymentStarted(purchaseId, nowEpochMillis)

    override suspend fun clearPaymentStarted(purchaseId: String) =
        purchaseDao.clearPaymentStarted(purchaseId)

    override suspend fun settle(reference: String, outcome: PaymentOutcome): SettleResult =
        database.withTransaction {
            val current = purchaseDao.getById(reference)?.toDomain()
                ?: return@withTransaction SettleResult.UnknownReference

            when (
                val decision =
                    PurchaseSettlement.decide(current, outcome, clock.nowEpochMillis())
            ) {
                is PurchaseSettlement.Decision.AlreadySettled ->
                    SettleResult.Duplicate(decision.purchase)

                is PurchaseSettlement.Decision.Settle -> {
                    purchaseDao.update(decision.purchase.toEntity())
                    val tickets = if (decision.issueTickets) {
                        issueTickets(decision.purchase)
                    } else {
                        // Nothing was charged: put the reserved tickets back on sale.
                        eventDao.releaseTickets(current.eventId, current.quantity)
                        emptyList()
                    }
                    SettleResult.Settled(decision.purchase, tickets)
                }
            }
        }

    override fun observeTickets(): Flow<List<Ticket>> =
        ticketDao.observeAll().map { tickets -> tickets.map { it.toDomain() } }

    override fun observeTicketsOfPurchase(purchaseId: String): Flow<List<Ticket>> =
        ticketDao.observeByPurchase(purchaseId).map { tickets -> tickets.map { it.toDomain() } }

    private suspend fun issueTickets(purchase: Purchase): List<Ticket> {
        val existing = ticketDao.getByPurchase(purchase.id)
        if (existing.isNotEmpty()) return existing.map { it.toDomain() }

        val issuedAt = clock.nowEpochMillis()
        val tickets = (1..purchase.quantity).map { sequence ->
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
                    secret = TICKET_SIGNING_SECRET,
                ),
                issuedAtEpochMillis = issuedAt,
            )
        }
        ticketDao.insertAll(tickets.map { it.toEntity() })
        return tickets
    }

    companion object {
        /**
         * Demo-only secret. In production the signature would be produced by the ticketing
         * backend and validated by the gate reader, never shipped inside the APK.
         */
        const val TICKET_SIGNING_SECRET = "cielo-case-demo-secret"
    }
}
