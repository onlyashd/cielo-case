package com.example.cielocase.data.repository

import com.example.cielocase.data.catalog.EventCatalog
import com.example.cielocase.data.local.dao.EventDao
import com.example.cielocase.data.local.entity.toDomain
import com.example.cielocase.data.local.entity.toEntity
import com.example.cielocase.domain.EventRepository
import com.example.cielocase.domain.model.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEventRepository @Inject constructor(
    private val eventDao: EventDao,
) : EventRepository {

    private val seedMutex = Mutex()

    override fun observeEvents(): Flow<List<Event>> = eventDao.observeAll()
        .onStart { seedIfEmpty() }
        .map { events -> events.map { it.toDomain() } }

    override fun observeEvent(eventId: String): Flow<Event?> = eventDao.observeById(eventId)
        .onStart { seedIfEmpty() }
        .map { it?.toDomain() }

    override suspend fun getEvent(eventId: String): Event? {
        seedIfEmpty()
        return eventDao.getById(eventId)?.toDomain()
    }

    private suspend fun seedIfEmpty() = seedMutex.withLock {
        if (eventDao.count() == 0) {
            eventDao.insertAll(EventCatalog.events.map { it.toEntity() })
        }
    }
}
