package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "checklist_items",
    indices = [
        Index(value = ["visitId"])
    ]
)
data class ChecklistItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val visitId: String,
    val category: String = "Display & Merchandising",
    val prompt: String,
    val status: String = "NOT_CHECKED", // NOT_CHECKED, PASS, FAIL, NA, NEEDS_FOLLOW_UP
    val notes: String = ""
)
