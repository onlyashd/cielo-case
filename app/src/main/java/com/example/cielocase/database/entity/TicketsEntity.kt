package com.example.cielocase.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tickets",
    foreignKeys = [
        ForeignKey(
            entity = EventsEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["eventId"]),
        Index(value = ["holderId"]),
    ]
)
data class TicketsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    @ColumnInfo("eventId")
    val eventId: Int,
    @ColumnInfo("qrcode")
    val qrcode: String,
    @ColumnInfo("holderId")
    val holderId: String,
)
