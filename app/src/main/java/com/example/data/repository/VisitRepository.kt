package com.example.data.repository

import com.example.data.local.dao.AuditDao
import com.example.data.local.dao.ChecklistDao
import com.example.data.local.dao.SyncOutboxDao
import com.example.data.local.dao.VisitDao
import com.example.data.local.entities.AuditEventEntity
import com.example.data.local.entities.ChecklistItemEntity
import com.example.data.local.entities.SyncOutboxEntity
import com.example.data.local.entities.VisitEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

class VisitRepository(
    private val visitDao: VisitDao,
    private val checklistDao: ChecklistDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val auditDao: AuditDao
) {
    val allVisits: Flow<List<VisitEntity>> = visitDao.getAllVisitsFlow()
    val activeVisitFlow: Flow<VisitEntity?> = visitDao.getActiveVisitFlow()

    fun getVisitsForStore(storeId: String): Flow<List<VisitEntity>> = visitDao.getVisitsForStoreFlow(storeId)

    fun getVisitFlow(id: String): Flow<VisitEntity?> = visitDao.getVisitByIdFlow(id)

    suspend fun getVisit(id: String): VisitEntity? = visitDao.getVisitById(id)

    suspend fun getActiveVisit(): VisitEntity? = visitDao.getActiveVisit()

    suspend fun getActiveVisitForStore(storeId: String): VisitEntity? = visitDao.getActiveVisitForStore(storeId)

    suspend fun getLastCompletedVisitForStore(storeId: String): VisitEntity? =
        visitDao.getLastCompletedVisitForStore(storeId)

    suspend fun startVisit(
        storeId: String,
        workerName: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): VisitEntity {
        val existingActive = visitDao.getActiveVisitForStore(storeId)
        if (existingActive != null) {
            return existingActive
        }

        val now = System.currentTimeMillis()
        val visit = VisitEntity(
            id = UUID.randomUUID().toString(),
            storeId = storeId,
            workerName = workerName,
            startTimestampUtc = now,
            startLatitude = latitude,
            startLongitude = longitude,
            status = "IN_PROGRESS",
            createdAt = now,
            updatedAt = now,
            localRevision = 1
        )
        visitDao.insertVisit(visit)

        // Seed standard inspection checklist
        val checklistItems = listOf(
            ChecklistItemEntity(visitId = visit.id, category = "Display & Merchandising", prompt = "Product displays match brand planogram guidelines"),
            ChecklistItemEntity(visitId = visit.id, category = "Pricing & POS", prompt = "Shelf tags and promotional pricing signs are clearly visible and accurate"),
            ChecklistItemEntity(visitId = visit.id, category = "Stock Availability", prompt = "Key SKU items are in-stock and front-faced"),
            ChecklistItemEntity(visitId = visit.id, category = "Condition & Quality", prompt = "No damaged goods or expired merchandise on shelves"),
            ChecklistItemEntity(visitId = visit.id, category = "Staff & Engagement", prompt = "Store manager met, reviewed performance and previous action items")
        )
        checklistDao.insertItems(checklistItems)

        syncOutboxDao.insertEvent(
            SyncOutboxEntity(
                entityType = "VISIT",
                entityId = visit.id,
                operation = "CREATE",
                payloadJson = """{"id":"${visit.id}","storeId":"$storeId","status":"IN_PROGRESS"}"""
            )
        )
        auditDao.logEvent(
            AuditEventEntity(
                entityType = "VISIT",
                entityId = visit.id,
                action = "STARTED",
                details = "Visit started at store $storeId by $workerName."
            )
        )
        return visit
    }

    suspend fun updateVisit(visit: VisitEntity) {
        val updated = visit.copy(
            updatedAt = System.currentTimeMillis(),
            localRevision = visit.localRevision + 1
        )
        visitDao.updateVisit(updated)
        syncOutboxDao.insertEvent(
            SyncOutboxEntity(
                entityType = "VISIT",
                entityId = updated.id,
                operation = "UPDATE",
                payloadJson = """{"id":"${updated.id}","status":"${updated.status}","notes":"${updated.notes.take(100)}"}"""
            )
        )
    }

    suspend fun endVisit(
        visitId: String,
        latitude: Double? = null,
        longitude: Double? = null,
        followUpRequired: Boolean = false,
        nextFollowUpDate: Long? = null
    ): VisitEntity? {
        val visit = visitDao.getVisitById(visitId) ?: return null
        val now = System.currentTimeMillis()
        val completed = visit.copy(
            endTimestampUtc = now,
            endLatitude = latitude,
            endLongitude = longitude,
            status = "COMPLETED",
            followUpRequired = followUpRequired,
            nextFollowUpDate = nextFollowUpDate,
            updatedAt = now,
            localRevision = visit.localRevision + 1
        )
        visitDao.updateVisit(completed)

        syncOutboxDao.insertEvent(
            SyncOutboxEntity(
                entityType = "VISIT",
                entityId = completed.id,
                operation = "UPDATE",
                payloadJson = """{"id":"${completed.id}","status":"COMPLETED","durationMinutes":${completed.durationMinutes}}"""
            )
        )
        auditDao.logEvent(
            AuditEventEntity(
                entityType = "VISIT",
                entityId = completed.id,
                action = "COMPLETED",
                details = "Visit completed. Duration: ${completed.durationMinutes} min. Follow-up: $followUpRequired."
            )
        )
        return completed
    }

    fun getChecklistForVisit(visitId: String): Flow<List<ChecklistItemEntity>> =
        checklistDao.getChecklistForVisitFlow(visitId)

    suspend fun updateChecklistItem(item: ChecklistItemEntity) {
        checklistDao.updateItem(item)
    }

    fun getVisitsCompletedTodayFlow(): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return visitDao.getVisitsCompletedTodayFlow(calendar.timeInMillis)
    }

    fun getVisitsCompletedThisWeekFlow(): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return visitDao.getVisitsCompletedThisWeekFlow(calendar.timeInMillis)
    }

    fun getActiveVisitsCountFlow(): Flow<Int> = visitDao.getActiveVisitsCountFlow()

    suspend fun getAllVisitsList(): List<VisitEntity> = visitDao.getAllVisitsList()
}
