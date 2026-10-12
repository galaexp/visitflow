package com.example.data.repository

import com.example.data.local.dao.AuditDao
import com.example.data.local.dao.StoreDao
import com.example.data.local.dao.SyncOutboxDao
import com.example.data.local.entities.AuditEventEntity
import com.example.data.local.entities.StoreEntity
import com.example.data.local.entities.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class StoreRepository(
    private val storeDao: StoreDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val auditDao: AuditDao
) {
    val activeStores: Flow<List<StoreEntity>> = storeDao.getActiveStoresFlow()
    val allStores: Flow<List<StoreEntity>> = storeDao.getAllStoresFlow()

    fun searchStores(query: String): Flow<List<StoreEntity>> = storeDao.searchStores(query)

    fun getStoreFlow(id: String): Flow<StoreEntity?> = storeDao.getStoreByIdFlow(id)

    suspend fun getStore(id: String): StoreEntity? = storeDao.getStoreById(id)

    suspend fun getStoreByCode(code: String): StoreEntity? = storeDao.getStoreByCode(code)

    suspend fun saveStore(store: StoreEntity) {
        val existing = storeDao.getStoreById(store.id)
        val isNew = existing == null
        val updatedStore = store.copy(
            updatedAt = System.currentTimeMillis(),
            localRevision = (existing?.localRevision ?: 0) + 1
        )
        storeDao.insertStore(updatedStore)

        val operation = if (isNew) "CREATE" else "UPDATE"
        syncOutboxDao.insertEvent(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entityType = "STORE",
                entityId = updatedStore.id,
                operation = operation,
                payloadJson = """{"id":"${updatedStore.id}","code":"${updatedStore.code}","name":"${updatedStore.name}"}"""
            )
        )
        auditDao.logEvent(
            AuditEventEntity(
                entityType = "STORE",
                entityId = updatedStore.id,
                action = if (isNew) "CREATED" else "MODIFIED",
                details = "Store ${updatedStore.name} (${updatedStore.code}) saved."
            )
        )
    }

    suspend fun setArchived(storeId: String, archived: Boolean) {
        storeDao.setArchived(storeId, archived)
        auditDao.logEvent(
            AuditEventEntity(
                entityType = "STORE",
                entityId = storeId,
                action = if (archived) "ARCHIVED" else "RESTORED",
                details = "Store archive status changed to $archived."
            )
        )
    }

    suspend fun deleteStore(storeId: String) {
        storeDao.deleteStoreById(storeId)
        syncOutboxDao.insertEvent(
            SyncOutboxEntity(
                entityType = "STORE",
                entityId = storeId,
                operation = "DELETE",
                payloadJson = """{"id":"$storeId"}"""
            )
        )
        auditDao.logEvent(
            AuditEventEntity(
                entityType = "STORE",
                entityId = storeId,
                action = "DELETED",
                details = "Store $storeId deleted."
            )
        )
    }

    suspend fun getAllStoresList(): List<StoreEntity> = storeDao.getAllStoresList()
}
