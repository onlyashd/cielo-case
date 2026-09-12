package com.example.cielocase.domain

import com.example.cielocase.domain.model.PaymentFailureKind
import com.example.cielocase.domain.model.Purchase
import com.example.cielocase.domain.model.PurchaseStatus
import com.example.cielocase.domain.payment.PurchaseSettlement
import com.example.cielocase.fake.approved
import com.example.cielocase.fake.rejected
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseSettlementTest {

    private val pending = Purchase(
        id = "purchase-1",
        eventId = "evt-1",
        eventTitle = "Rock na Praça",
        quantity = 2,
        unitPriceInCents = 10_000,
        status = PurchaseStatus.PENDING,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
        paymentStartedAtEpochMillis = 2,
    )

    @Test
    fun `approval stores the payment data and asks for tickets`() {
        val decision = PurchaseSettlement.decide(pending, approved(pending.id, 20_000), nowEpochMillis = 10)

        val settle = decision as PurchaseSettlement.Decision.Settle
        assertTrue(settle.issueTickets)
        assertEquals(PurchaseStatus.APPROVED, settle.purchase.status)
        assertEquals("123456", settle.purchase.payment?.authCode)
        assertEquals(10, settle.purchase.updatedAtEpochMillis)
        assertEquals(null, settle.purchase.paymentStartedAtEpochMillis)
    }

    @Test
    fun `failure kinds map to purchase statuses`() {
        assertEquals(
            PurchaseStatus.CANCELLED,
            settledStatus(PaymentFailureKind.USER_CANCELLED),
        )
        assertEquals(PurchaseStatus.DENIED, settledStatus(PaymentFailureKind.PAYMENT_DENIED))
        assertEquals(PurchaseStatus.FAILED, settledStatus(PaymentFailureKind.AUTHENTICATION))
        assertEquals(PurchaseStatus.FAILED, settledStatus(PaymentFailureKind.INVALID_RESPONSE))
        assertEquals(PurchaseStatus.FAILED, settledStatus(PaymentFailureKind.GENERIC))
    }

    @Test
    fun `rejection never issues tickets`() {
        val decision = PurchaseSettlement.decide(
            pending,
            rejected(PaymentFailureKind.PAYMENT_DENIED, pending.id),
            nowEpochMillis = 10,
        )

        assertFalse((decision as PurchaseSettlement.Decision.Settle).issueTickets)
    }

    @Test
    fun `a settled purchase is immutable`() {
        val settled = pending.copy(status = PurchaseStatus.APPROVED)

        val decision = PurchaseSettlement.decide(settled, approved(settled.id, 20_000), nowEpochMillis = 99)

        assertEquals(
            PurchaseSettlement.Decision.AlreadySettled(settled),
            decision,
        )
    }

    private fun settledStatus(kind: PaymentFailureKind): PurchaseStatus {
        val decision = PurchaseSettlement.decide(pending, rejected(kind, pending.id), nowEpochMillis = 10)
        return (decision as PurchaseSettlement.Decision.Settle).purchase.status
    }
}
