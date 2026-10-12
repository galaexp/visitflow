package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "stores",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["name"]),
        Index(value = ["city"]),
        Index(value = ["isActive", "isArchived"])
    ]
)
data class StoreEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val name: String,
    val branchName: String = "",
    val address: String = "",
    val city: String = "",
    val region: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactPerson: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val category: String = "Supermarket", // Supermarket, Convenience, Pharmacy, Wholesale, Retail
    val assignedWorker: String = "Field Rep",
    val visitFrequency: String = "Weekly", // Daily, Weekly, Bi-weekly, Monthly
    val preferredDays: String = "Mon, Thu",
    val openingHours: String = "08:00 - 20:00",
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val localRevision: Int = 1
)
