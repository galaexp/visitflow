package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "scheduled_visits",
    foreignKeys = [
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["storeId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["storeId"]),
        Index(value = ["scheduledDate"]),
        Index(value = ["isCompleted"])
    ]
)
data class ScheduledVisitEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val storeId: String,
    val scheduledDate: Long,
    val purpose: String = "Routine Inspection",
    val preparationNotes: String = "",
    val frequency: String = "ONCE", // ONCE, DAILY, WEEKLY, BI_WEEKLY, MONTHLY
    val isCompleted: Boolean = false,
    val linkedVisitId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
