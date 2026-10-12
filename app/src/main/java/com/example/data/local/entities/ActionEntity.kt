package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "actions",
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
        Index(value = ["originatingVisitId"]),
        Index(value = ["status"]),
        Index(value = ["dueAt"]),
        Index(value = ["priority"])
    ]
)
data class ActionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val storeId: String,
    val originatingVisitId: String? = null,
    val title: String,
    val description: String = "",
    val category: String = "Merchandising", // Merchandising, Pricing, Stock, Damaged Goods, Promotional, Staff Training, Compliance
    val priority: String = "NORMAL", // LOW, NORMAL, HIGH, CRITICAL
    val status: String = "OPEN", // OPEN, IN_PROGRESS, COMPLETED, UNABLE_TO_COMPLETE, DEFERRED, CANCELLED
    val assignedTo: String = "Store Manager",
    val createdAt: Long = System.currentTimeMillis(),
    val dueAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L), // Default 7 days
    val completedAt: Long? = null,
    val completedBy: String? = null,
    val completionNotes: String? = null,
    val deferredReason: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val localRevision: Int = 1,
    val isSynced: Boolean = false
) {
    val isOverdue: Boolean
        get() = status != "COMPLETED" && status != "CANCELLED" && System.currentTimeMillis() > dueAt
}
