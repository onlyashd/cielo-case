package com.example.cielocase.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cielocase.database.entity.TicketsEntity

@Dao
interface TicketsDao {
    @Query("SELECT * FROM tickets WHERE holderId = :holerId")
    suspend fun getTickets(holerId: String): List<TicketsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTickets(tickets: List<TicketsEntity>)
}
