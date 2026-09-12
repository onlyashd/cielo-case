package com.example.cielocase.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cielocase.data.local.entity.TicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tickets: List<TicketEntity>)

    @Query("SELECT * FROM tickets ORDER BY issuedAtEpochMillis DESC, sequence ASC")
    fun observeAll(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE purchaseId = :purchaseId ORDER BY sequence ASC")
    fun observeByPurchase(purchaseId: String): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE purchaseId = :purchaseId ORDER BY sequence ASC")
    suspend fun getByPurchase(purchaseId: String): List<TicketEntity>
}
