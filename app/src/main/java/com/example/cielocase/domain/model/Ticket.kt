package com.example.cielocase.domain.model

/** A ticket. Only issued for an approved [Purchase]. */
data class Ticket(
    val id: String,
    val purchaseId: String,
    val eventId: String,
    val eventTitle: String,
    val sequence: Int,
    val qrPayload: String,
    val issuedAtEpochMillis: Long,
)
