package com.example.cielocase.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.cielocase.data.local.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(purchase: PurchaseEntity)

    @Update
    suspend fun update(purchase: PurchaseEntity)

    @Query("SELECT * FROM purchases WHERE id = :purchaseId")
    suspend fun getById(purchaseId: String): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE id = :purchaseId")
    fun observeById(purchaseId: String): Flow<PurchaseEntity?>

    @Query(
        """
        SELECT * FROM purchases
        WHERE eventId = :eventId AND quantity = :quantity AND status = 'PENDING'
        ORDER BY createdAtEpochMillis DESC LIMIT 1
        """,
    )
    suspend fun findPending(eventId: String, quantity: Int): PurchaseEntity?

    @Query(
        """
        UPDATE purchases
        SET paymentStartedAtEpochMillis = :nowEpochMillis, updatedAtEpochMillis = :nowEpochMillis
        WHERE id = :purchaseId AND status = 'PENDING'
        """,
    )
    suspend fun markPaymentStarted(purchaseId: String, nowEpochMillis: Long)

    @Query(
        "UPDATE purchases SET paymentStartedAtEpochMillis = NULL WHERE id = :purchaseId",
    )
    suspend fun clearPaymentStarted(purchaseId: String)
}
