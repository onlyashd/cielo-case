package com.example.cielocase.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cielocase.data.local.AppDatabase
import com.example.cielocase.data.local.entity.toEntity
import com.example.cielocase.data.repository.RoomPurchaseRepository
import com.example.cielocase.domain.Clock
import com.example.cielocase.domain.IdGenerator
import com.example.cielocase.domain.SettleResult
import com.example.cielocase.domain.model.Event
import com.example.cielocase.domain.model.PaymentFailure
import com.example.cielocase.domain.model.PaymentFailureKind
import com.example.cielocase.domain.model.PaymentRecord
import com.example.cielocase.domain.model.PurchaseStatus
import com.example.cielocase.domain.payment.PaymentOutcome
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Exercises the real Room transactions: reservation, single settlement and ticket issuing.
 */
@RunWith(AndroidJUnit4::class)
class RoomPurchaseRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomPurchaseRepository

    private val event = Event(
        id = "evt-1",
        title = "Rock na Praça",
        description = "Festival",
        startsAtEpochMillis = 1_760_000_000_000,
        location = "São Paulo - SP",
        unitPriceInCents = 10_000,
        availableTickets = 5,
        salesOpen = true,
    )

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomPurchaseRepository(
            database = database,
            clock = Clock { 1_700_000_000_000 },
            idGenerator = IdGenerator { UUID.randomUUID().toString() },
        )
        database.eventDao().insertAll(listOf(event.toEntity()))
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun createOrReusePending_reservesInventoryOnlyOnce() = runTest {
        val first = repository.createOrReusePending(event, quantity = 2)
        val second = repository.createOrReusePending(event, quantity = 2)

        assertEquals(first.id, second.id)
        assertEquals(3, database.eventDao().getById(event.id)?.availableTickets)
    }

    @Test
    fun settle_isAppliedOnlyOnce_forDuplicatedCallbacks() = runTest {
        val purchase = repository.createOrReusePending(event, quantity = 3)

        val first = repository.settle(purchase.id, approved(purchase.id))
        val second = repository.settle(purchase.id, approved(purchase.id))

        assertEquals(3, (first as SettleResult.Settled).tickets.size)
        assertTrue(second is SettleResult.Duplicate)
        assertEquals(3, database.ticketDao().getByPurchase(purchase.id).size)
        assertEquals(PurchaseStatus.APPROVED, repository.getPurchase(purchase.id)?.status)
    }

    @Test
    fun settle_releasesInventory_whenPaymentIsRejected() = runTest {
        val purchase = repository.createOrReusePending(event, quantity = 2)

        val result = repository.settle(
            purchase.id,
            PaymentOutcome.Rejected(
                failure = PaymentFailure(PaymentFailureKind.USER_CANCELLED, code = 1),
                reference = purchase.id,
            ),
        )

        assertEquals(
            PurchaseStatus.CANCELLED,
            (result as SettleResult.Settled).purchase.status,
        )
        assertTrue(database.ticketDao().getByPurchase(purchase.id).isEmpty())
        assertEquals(5, database.eventDao().getById(event.id)?.availableTickets)
    }

    @Test
    fun settle_ignoresUnknownReferences() = runTest {
        assertEquals(
            SettleResult.UnknownReference,
            repository.settle("unknown", approved("unknown")),
        )
    }

    private fun approved(reference: String) = PaymentOutcome.Approved(
        record = PaymentRecord(
            orderId = "order-1",
            paymentId = "payment-1",
            authCode = "123456",
            cieloCode = "999999",
            brand = "Visa",
            maskedCard = "************0000",
            productName = "CREDITO A VISTA - I",
            installments = 0,
            amountInCents = 30_000,
            terminal = "69000007",
            paidAtEpochMillis = 1_700_000_000_000,
        ),
        reference = reference,
    )
}
