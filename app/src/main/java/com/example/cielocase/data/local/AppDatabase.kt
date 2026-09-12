package com.example.cielocase.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.cielocase.data.local.dao.EventDao
import com.example.cielocase.data.local.dao.PurchaseDao
import com.example.cielocase.data.local.dao.TicketDao
import com.example.cielocase.data.local.entity.EventEntity
import com.example.cielocase.data.local.entity.PurchaseEntity
import com.example.cielocase.data.local.entity.TicketEntity

@Database(
    entities = [
        EventEntity::class,
        PurchaseEntity::class,
        TicketEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun ticketDao(): TicketDao

    companion object {
        const val NAME = "cielo-case.db"
    }
}
