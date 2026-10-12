package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "photos",
    indices = [
        Index(value = ["visitId"]),
        Index(value = ["actionId"]),
        Index(value = ["capturedAt"])
    ]
)
data class PhotoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val visitId: String? = null,
    val actionId: String? = null,
    val filePath: String,
    val caption: String = "",
    val capturedAt: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val fileSize: Long = 0L,
    val isEvidence: Boolean = false,
    val isSynced: Boolean = false
)
