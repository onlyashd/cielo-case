package com.example.cielocase.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.example.cielocase.database.dao.EventsDao
import com.example.cielocase.database.dao.SalesDao
import com.example.cielocase.database.entity.EventsEntity
import com.example.cielocase.database.entity.SalesEntity
import javax.inject.Inject

@Database(
    entities = [
        EventsEntity::class,
        SalesEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventsDao(): EventsDao
    abstract fun salesDao(): SalesDao
}

class Database @Inject constructor() {
    @Volatile
    private var database: AppDatabase? = null
    fun getDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app-database"
            ).addMigrations(*getDatabaseMigrations())
                .fallbackToDestructiveMigration(true)
                .build()
            database = instance
            instance
        }
    }
}

fun getDatabaseMigrations(): Array<Migration> = arrayOf()
