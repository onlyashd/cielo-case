package com.example.cielocase.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cielocase.database.entity.TicketsEntity

@Dao
interface EventsDao {
    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEvent(eventId: Int): List<TicketsEntity>
}
