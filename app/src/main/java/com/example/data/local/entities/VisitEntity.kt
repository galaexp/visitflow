package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "visits",
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
        Index(value = ["startTimestampUtc"]),
        Index(value = ["status"])
    ]
)
data class VisitEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val storeId: String,
    val workerName: String = "Field Rep",
    val startTimestampUtc: Long = System.currentTimeMillis(),
    val endTimestampUtc: Long? = null,
    val timeZoneId: String = java.util.TimeZone.getDefault().id,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val notes: String = "",
    val keyObservations: String = "",
    val issuesIdentified: String = "",
    val positiveObservations: String = "",
    val recommendations: String = "",
    val priority: String = "NORMAL",
    val followUpRequired: Boolean = false,
    val nextFollowUpDate: Long? = null,
    val status: String = "IN_PROGRESS", // IN_PROGRESS, COMPLETED, DRAFT, CANCELLED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val localRevision: Int = 1,
    val isSynced: Boolean = false
) {
    val durationMinutes: Long
        get() {
            val end = endTimestampUtc ?: System.currentTimeMillis()
            return ((end - startTimestampUtc).coerceAtLeast(0)) / (1000 * 60)
        }
}
