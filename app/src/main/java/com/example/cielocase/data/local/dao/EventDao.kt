package com.example.cielocase.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cielocase.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startsAtEpochMillis ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :eventId")
    fun observeById(eventId: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getById(eventId: String): EventEntity?

    @Query("SELECT COUNT(*) FROM events")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<EventEntity>)

    /**
     * Reserves [quantity] tickets. Returns 0 when there is not enough inventory, which makes
     * the reservation safe against concurrent checkouts.
     */
    @Query(
        """
        UPDATE events SET availableTickets = availableTickets - :quantity
        WHERE id = :eventId AND availableTickets >= :quantity
        """,
    )
    suspend fun reserveTickets(eventId: String, quantity: Int): Int

    @Query("UPDATE events SET availableTickets = availableTickets + :quantity WHERE id = :eventId")
    suspend fun releaseTickets(eventId: String, quantity: Int)
}
