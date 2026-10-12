package com.example.sync

import com.example.data.local.dao.SyncOutboxDao
import com.example.data.local.entities.SyncOutboxEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

sealed class SyncResult {
    data class Success(val syncedCount: Int) : SyncResult()
    data class OfflineNoEndpoint(val message: String) : SyncResult()
    data class Error(val error: String, val failedCount: Int) : SyncResult()
}

class SyncManager(
    private val syncOutboxDao: SyncOutboxDao,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    val pendingCountFlow: Flow<Int> = syncOutboxDao.getPendingCountFlow()
    val pendingEventsFlow: Flow<List<SyncOutboxEntity>> = syncOutboxDao.getPendingEventsFlow()

    suspend fun performSync(workerUrl: String?, authToken: String?): SyncResult = withContext(Dispatchers.IO) {
        val pending = syncOutboxDao.getPendingEvents(50)
        if (pending.isEmpty()) {
            return@withContext SyncResult.Success(0)
        }

        if (workerUrl.isNullOrBlank()) {
            return@withContext SyncResult.OfflineNoEndpoint(
                "Operating in full Offline Mode. No Cloudflare Worker endpoint configured."
            )
        }

        var successCount = 0
        var errorCount = 0
        var lastErrorMessage = ""

        for (event in pending) {
            try {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = event.payloadJson.toRequestBody(mediaType)
                val requestBuilder = Request.Builder()
                    .url(workerUrl.trimEnd('/') + "/api/sync/${event.entityType.lowercase()}")
                    .post(body)
                    .addHeader("X-Event-ID", event.id)
                    .addHeader("X-Operation", event.operation)

                if (!authToken.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $authToken")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                if (response.isSuccessful) {
                    syncOutboxDao.markEventStatus(event.id, "SYNCED", null)
                    successCount++
                } else {
                    val code = response.code
                    val msg = "Server responded HTTP $code"
                    syncOutboxDao.markEventStatus(event.id, "FAILED", msg)
                    errorCount++
                    lastErrorMessage = msg
                }
                response.close()
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Network connection failed"
                syncOutboxDao.markEventStatus(event.id, "FAILED", errorMsg)
                errorCount++
                lastErrorMessage = errorMsg
            }
        }

        // Clean up completed sync events
        syncOutboxDao.purgeSyncedEvents()

        if (errorCount > 0 && successCount == 0) {
            SyncResult.Error(lastErrorMessage, errorCount)
        } else {
            SyncResult.Success(successCount)
        }
    }

    suspend fun purgeSynced() = withContext(Dispatchers.IO) {
        syncOutboxDao.purgeSyncedEvents()
    }

    suspend fun getAllAuditEvents() = withContext(Dispatchers.IO) {
        syncOutboxDao.getAllEvents()
    }
}
