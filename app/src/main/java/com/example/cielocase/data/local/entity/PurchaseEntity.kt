package com.example.cielocase.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cielocase.domain.model.PaymentFailure
import com.example.cielocase.domain.model.PaymentFailureKind
import com.example.cielocase.domain.model.PaymentRecord
import com.example.cielocase.domain.model.Purchase
import com.example.cielocase.domain.model.PurchaseStatus

@Entity(
    tableName = "purchases",
    indices = [Index(value = ["eventId", "status"])],
)
data class PurchaseEntity(
    @PrimaryKey
    val id: String,
    val eventId: String,
    val eventTitle: String,
    val quantity: Int,
    val unitPriceInCents: Long,
    val status: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val paymentStartedAtEpochMillis: Long? = null,
    @Embedded(prefix = "payment_")
    val payment: PaymentRecordEmbedded? = null,
    @Embedded(prefix = "failure_")
    val failure: PaymentFailureEmbedded? = null,
)

data class PaymentRecordEmbedded(
    val orderId: String? = null,
    val paymentId: String? = null,
    val authCode: String? = null,
    val cieloCode: String? = null,
    val brand: String? = null,
    val maskedCard: String? = null,
    val productName: String? = null,
    val installments: Int? = null,
    val amountInCents: Long? = null,
    val terminal: String? = null,
    val paidAtEpochMillis: Long? = null,
)

data class PaymentFailureEmbedded(
    val kind: String? = null,
    val code: Int? = null,
    val reason: String? = null,
)

fun PurchaseEntity.toDomain() = Purchase(
    id = id,
    eventId = eventId,
    eventTitle = eventTitle,
    quantity = quantity,
    unitPriceInCents = unitPriceInCents,
    status = runCatching { PurchaseStatus.valueOf(status) }.getOrDefault(PurchaseStatus.FAILED),
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    paymentStartedAtEpochMillis = paymentStartedAtEpochMillis,
    payment = payment?.toDomain(),
    failure = failure?.toDomain(),
)

fun Purchase.toEntity() = PurchaseEntity(
    id = id,
    eventId = eventId,
    eventTitle = eventTitle,
    quantity = quantity,
    unitPriceInCents = unitPriceInCents,
    status = status.name,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    paymentStartedAtEpochMillis = paymentStartedAtEpochMillis,
    payment = payment?.toEmbedded(),
    failure = failure?.toEmbedded(),
)

private fun PaymentRecordEmbedded.toDomain() = PaymentRecord(
    orderId = orderId,
    paymentId = paymentId,
    authCode = authCode,
    cieloCode = cieloCode,
    brand = brand,
    maskedCard = maskedCard,
    productName = productName,
    installments = installments ?: 0,
    amountInCents = amountInCents ?: 0,
    terminal = terminal,
    paidAtEpochMillis = paidAtEpochMillis,
)

private fun PaymentRecord.toEmbedded() = PaymentRecordEmbedded(
    orderId = orderId,
    paymentId = paymentId,
    authCode = authCode,
    cieloCode = cieloCode,
    brand = brand,
    maskedCard = maskedCard,
    productName = productName,
    installments = installments,
    amountInCents = amountInCents,
    terminal = terminal,
    paidAtEpochMillis = paidAtEpochMillis,
)

private fun PaymentFailureEmbedded.toDomain() = PaymentFailure(
    kind = runCatching { PaymentFailureKind.valueOf(kind.orEmpty()) }
        .getOrDefault(PaymentFailureKind.GENERIC),
    code = code,
    reason = reason,
)

private fun PaymentFailure.toEmbedded() = PaymentFailureEmbedded(
    kind = kind.name,
    code = code,
    reason = reason,
)
