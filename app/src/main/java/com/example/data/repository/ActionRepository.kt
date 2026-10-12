package com.example.data.repository

import com.example.data.local.dao.ActionDao
import com.example.data.local.dao.AuditDao
import com.example.data.local.dao.SyncOutboxDao
import com.example.data.local.entities.ActionEntity
import com.example.data.local.entities.AuditEventEntity
import com.example.data.local.entities.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

class ActionRepository(
    private val actionDao: ActionDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val auditDao: AuditDao
) {
    val allActions: Flow<List<ActionEntity>> = actionDao.getAllActionsFlow()

    fun getActionsForStore(storeId: String): Flow<List<ActionEntity>> =
        actionDao.getActionsForStoreFlow(storeId)

    fun getActionsForVisit(visitId: String): Flow<List<ActionEntity>> =
        actionDao.getActionsForVisitFlow(visitId)

    fun getActionFlow(id: String): Flow<ActionEntity?> = actionDao.getActionByIdFlow(id)

    suspend fun getAction(id: String): ActionEntity? = actionDao.getActionById(id)

    suspend fun getOpenActionsForStore(storeId: String): List<ActionEntity> =
        actionDao.getOpenActionsForStore(storeId)

    suspend fun createAction(
        storeId: String,
        originatingVisitId: String?,
        title: String,
        description: String,
        category: String = "Merchandising",
        priority: String = "NORMAL",
        assignedTo: String = "Store Manager",
        dueAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
    ): ActionEntity {
        val now = System.currentTimeMillis()
        val action = ActionEntity(
            id = UUID.randomUUID().toString(),
            storeId = storeId,
            originatingVisitId = originatingVisitId,
            title = title,
            description = description,
            category = category,
            priority = priority,
            status = "OPEN",
            assignedTo = assignedTo,
            createdAt = now,
            dueAt = dueAt,
            updatedAt = now,
            localRevision = 1
        )
        actionDao.insertAction(action)
        syncOutboxDao.insertEvent(
            SyncOutboxEntity(
                entityType = "ACTION",
                entityId = action.id,
                operation = "CREATE",
                payloadJson = """{"id":"${action.id}","title":"${action.title}","priority":"${action.priority}"}"""
            )
        )
        auditDao.logEvent(
            AuditEventEntity(
                entityType = "ACTION",
                entityId = action.id,
                action = "CREATED",
                details = "Action item '${action.title}' created with priority ${action.priority}."
            )
        )
        return action
    }

    suspend fun updateActionStatus(
        actionId: String,
        newStatus: String,
        completedBy: String? = null,
        completionNotes: String? = null,
        deferredReason: String? = null,
        newDueAt: Long? = null
    ): ActionEntity? {
        val action = actionDao.getActionById(actionId) ?: return null
        val now = System.currentTimeMillis()
        val updated = action.copy(
            status = newStatus,
            completedAt = if (newStatus == "COMPLETED") now else action.completedAt,
            completedBy = if (newStatus == "COMPLETED") completedBy else action.completedBy,
            completionNotes = completionNotes ?: action.completionNotes,
            deferredReason = deferredReason ?: action.deferredReason,
            dueAt = newDueAt ?: action.dueAt,
            updatedAt = now,
            localRevision = action.localRevision + 1
        )
        actionDao.updateAction(updated)
        syncOutboxDao.insertEvent(
            SyncOutboxEntity(
                entityType = "ACTION",
                entityId = updated.id,
                operation = "UPDATE",
                payloadJson = """{"id":"${updated.id}","status":"$newStatus"}"""
            )
        )
        auditDao.logEvent(
            AuditEventEntity(
                entityType = "ACTION",
                entityId = updated.id,
                action = "STATUS_CHANGED",
                details = "Action '${updated.title}' updated to $newStatus. Notes: ${completionNotes ?: deferredReason ?: "N/A"}"
            )
        )
        return updated
    }

    suspend fun updateAction(action: ActionEntity) {
        val updated = action.copy(
            updatedAt = System.currentTimeMillis(),
            localRevision = action.localRevision + 1
        )
        actionDao.updateAction(updated)
    }

    suspend fun deleteAction(actionId: String) {
        actionDao.deleteActionById(actionId)
        syncOutboxDao.insertEvent(
            SyncOutboxEntity(
                entityType = "ACTION",
                entityId = actionId,
                operation = "DELETE",
                payloadJson = """{"id":"$actionId"}"""
            )
        )
    }

    fun getOpenActionsCountFlow(): Flow<Int> = actionDao.getOpenActionsCountFlow()

    fun getOverdueActionsCountFlow(): Flow<Int> =
        actionDao.getOverdueActionsCountFlow(System.currentTimeMillis())

    fun getCompletedActionsThisMonthFlow(): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return actionDao.getCompletedActionsThisMonthFlow(calendar.timeInMillis)
    }

    suspend fun getAllActionsList(): List<ActionEntity> = actionDao.getAllActionsList()
}
