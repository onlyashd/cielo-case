package com.example.cielocase.domain

import com.example.cielocase.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvents(): Flow<List<Event>>
    fun observeEvent(eventId: String): Flow<Event?>
    suspend fun getEvent(eventId: String): Event?
}
