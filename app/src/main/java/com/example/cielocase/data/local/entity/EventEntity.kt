package com.example.cielocase.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cielocase.domain.model.Event

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val startsAtEpochMillis: Long,
    val location: String,
    val unitPriceInCents: Long,
    val availableTickets: Int,
    val salesOpen: Boolean,
)

fun EventEntity.toDomain() = Event(
    id = id,
    title = title,
    description = description,
    startsAtEpochMillis = startsAtEpochMillis,
    location = location,
    unitPriceInCents = unitPriceInCents,
    availableTickets = availableTickets,
    salesOpen = salesOpen,
)

fun Event.toEntity() = EventEntity(
    id = id,
    title = title,
    description = description,
    startsAtEpochMillis = startsAtEpochMillis,
    location = location,
    unitPriceInCents = unitPriceInCents,
    availableTickets = availableTickets,
    salesOpen = salesOpen,
)
