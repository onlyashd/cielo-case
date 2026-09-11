package com.example.cielocase.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    @ColumnInfo("title")
    val title: String,
    @ColumnInfo("description")
    val description: String,
    @ColumnInfo("datetime")
    val datetime: String,
    @ColumnInfo("location")
    val location: String,
    @ColumnInfo("availableTickets")
    val availableTickets: Int = 0,
    @ColumnInfo("salesOpen")
    val salesOpen: Boolean = false,
)
