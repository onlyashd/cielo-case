package com.example.cielocase.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cielocase.domain.model.Ticket

@Entity(
    tableName = "tickets",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        // Guarantees a purchase can never produce the same ticket twice.
        Index(value = ["purchaseId", "sequence"], unique = true),
    ],
)
data class TicketEntity(
    @PrimaryKey
    val id: String,
    val purchaseId: String,
    val eventId: String,
    val eventTitle: String,
    val sequence: Int,
    val qrPayload: String,
    val issuedAtEpochMillis: Long,
)

fun TicketEntity.toDomain() = Ticket(
    id = id,
    purchaseId = purchaseId,
    eventId = eventId,
    eventTitle = eventTitle,
    sequence = sequence,
    qrPayload = qrPayload,
    issuedAtEpochMillis = issuedAtEpochMillis,
)

fun Ticket.toEntity() = TicketEntity(
    id = id,
    purchaseId = purchaseId,
    eventId = eventId,
    eventTitle = eventTitle,
    sequence = sequence,
    qrPayload = qrPayload,
    issuedAtEpochMillis = issuedAtEpochMillis,
)
