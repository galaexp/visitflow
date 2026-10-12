package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.AuditEventEntity
import com.example.data.local.entities.ChecklistItemEntity
import com.example.data.local.entities.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_items WHERE visitId = :visitId")
    fun getChecklistForVisitFlow(visitId: String): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE visitId = :visitId")
    suspend fun getChecklistForVisit(visitId: String): List<ChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ChecklistItemEntity>)

    @Update
    suspend fun updateItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE visitId = :visitId")
    suspend fun deleteForVisit(visitId: String)
}

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox WHERE status != 'SYNCED' ORDER BY createdAt ASC")
    fun getPendingEventsFlow(): Flow<List<SyncOutboxEntity>>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status != 'SYNCED'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingEvents(limit: Int = 20): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SyncOutboxEntity): Long

    @Update
    suspend fun updateEvent(event: SyncOutboxEntity)

    @Query("UPDATE sync_outbox SET status = :status, lastError = :error, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markEventStatus(id: String, status: String, error: String?)

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteEventById(id: String)

    @Query("DELETE FROM sync_outbox WHERE status = 'SYNCED'")
    suspend fun purgeSyncedEvents()

    @Query("SELECT * FROM sync_outbox ORDER BY createdAt DESC LIMIT 100")
    suspend fun getAllEvents(): List<SyncOutboxEntity>
}

@Dao
interface AuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logEvent(event: AuditEventEntity)

    @Query("SELECT * FROM audit_events ORDER BY timestamp DESC LIMIT 200")
    fun getRecentAuditEventsFlow(): Flow<List<AuditEventEntity>>

    @Query("SELECT * FROM audit_events WHERE entityId = :entityId ORDER BY timestamp DESC")
    suspend fun getEventsForEntity(entityId: String): List<AuditEventEntity>

    @Query("SELECT * FROM audit_events ORDER BY timestamp DESC")
    suspend fun getAllAuditEvents(): List<AuditEventEntity>
}
