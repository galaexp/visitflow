package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncOutboxEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val entityType: String, // STORE, VISIT, ACTION, SCHEDULE, PHOTO
    val entityId: String,
    val operation: String, // CREATE, UPDATE, DELETE
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, IN_PROGRESS, FAILED, SYNCED
    val retryCount: Int = 0,
    val lastError: String? = null
)

@Entity(
    tableName = "audit_events",
    indices = [
        Index(value = ["entityType", "entityId"]),
        Index(value = ["timestamp"])
    ]
)
data class AuditEventEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val entityType: String,
    val entityId: String,
    val action: String, // CREATED, MODIFIED, STATUS_CHANGED, COMPLETED
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
