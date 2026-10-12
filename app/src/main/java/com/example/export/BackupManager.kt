package com.example.export

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entities.ActionEntity
import com.example.data.local.entities.PhotoEntity
import com.example.data.local.entities.ScheduledVisitEntity
import com.example.data.local.entities.StoreEntity
import com.example.data.local.entities.VisitEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupStats(
    val storesCount: Int,
    val visitsCount: Int,
    val actionsCount: Int,
    val schedulesCount: Int,
    val photosCount: Int
)

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    suspend fun createFullBackup(): File = withContext(Dispatchers.IO) {
        val backupsDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(backupsDir, "storeflow_backup_$timeStamp.json")

        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("appVersion", "1.0")
        root.put("exportedAt", System.currentTimeMillis())

        // Stores
        val stores = database.storeDao().getAllStoresList()
        val storesArray = JSONArray()
        for (s in stores) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("code", s.code)
            obj.put("name", s.name)
            obj.put("branchName", s.branchName)
            obj.put("address", s.address)
            obj.put("city", s.city)
            obj.put("region", s.region)
            obj.put("contactPerson", s.contactPerson)
            obj.put("contactPhone", s.contactPhone)
            obj.put("contactEmail", s.contactEmail)
            obj.put("category", s.category)
            obj.put("assignedWorker", s.assignedWorker)
            obj.put("visitFrequency", s.visitFrequency)
            obj.put("preferredDays", s.preferredDays)
            obj.put("openingHours", s.openingHours)
            obj.put("remarks", s.remarks)
            obj.put("createdAt", s.createdAt)
            obj.put("updatedAt", s.updatedAt)
            obj.put("isActive", s.isActive)
            obj.put("isArchived", s.isArchived)
            storesArray.put(obj)
        }
        root.put("stores", storesArray)

        // Visits
        val visits = database.visitDao().getAllVisitsList()
        val visitsArray = JSONArray()
        for (v in visits) {
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("storeId", v.storeId)
            obj.put("workerName", v.workerName)
            obj.put("startTimestampUtc", v.startTimestampUtc)
            v.endTimestampUtc?.let { obj.put("endTimestampUtc", it) }
            obj.put("timeZoneId", v.timeZoneId)
            obj.put("notes", v.notes)
            obj.put("keyObservations", v.keyObservations)
            obj.put("issuesIdentified", v.issuesIdentified)
            obj.put("positiveObservations", v.positiveObservations)
            obj.put("recommendations", v.recommendations)
            obj.put("priority", v.priority)
            obj.put("followUpRequired", v.followUpRequired)
            v.nextFollowUpDate?.let { obj.put("nextFollowUpDate", it) }
            obj.put("status", v.status)
            obj.put("createdAt", v.createdAt)
            obj.put("updatedAt", v.updatedAt)
            visitsArray.put(obj)
        }
        root.put("visits", visitsArray)

        // Actions
        val actions = database.actionDao().getAllActionsList()
        val actionsArray = JSONArray()
        for (a in actions) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("storeId", a.storeId)
            a.originatingVisitId?.let { obj.put("originatingVisitId", it) }
            obj.put("title", a.title)
            obj.put("description", a.description)
            obj.put("category", a.category)
            obj.put("priority", a.priority)
            obj.put("status", a.status)
            obj.put("assignedTo", a.assignedTo)
            obj.put("createdAt", a.createdAt)
            obj.put("dueAt", a.dueAt)
            a.completedAt?.let { obj.put("completedAt", it) }
            a.completedBy?.let { obj.put("completedBy", it) }
            a.completionNotes?.let { obj.put("completionNotes", it) }
            a.deferredReason?.let { obj.put("deferredReason", it) }
            actionsArray.put(obj)
        }
        root.put("actions", actionsArray)

        // Schedules
        val schedules = database.scheduleDao().getAllSchedulesList()
        val schedulesArray = JSONArray()
        for (sc in schedules) {
            val obj = JSONObject()
            obj.put("id", sc.id)
            obj.put("storeId", sc.storeId)
            obj.put("scheduledDate", sc.scheduledDate)
            obj.put("purpose", sc.purpose)
            obj.put("preparationNotes", sc.preparationNotes)
            obj.put("frequency", sc.frequency)
            obj.put("isCompleted", sc.isCompleted)
            sc.linkedVisitId?.let { obj.put("linkedVisitId", it) }
            obj.put("createdAt", sc.createdAt)
            schedulesArray.put(obj)
        }
        root.put("schedules", schedulesArray)

        // Photos
        val photos = database.photoDao().getAllPhotosList()
        val photosArray = JSONArray()
        for (p in photos) {
            val obj = JSONObject()
            obj.put("id", p.id)
            p.visitId?.let { obj.put("visitId", it) }
            p.actionId?.let { obj.put("actionId", it) }
            obj.put("filePath", p.filePath)
            obj.put("caption", p.caption)
            obj.put("capturedAt", p.capturedAt)
            obj.put("fileSize", p.fileSize)
            obj.put("isEvidence", p.isEvidence)
            photosArray.put(obj)
        }
        root.put("photos", photosArray)

        FileOutputStream(backupFile).use { out ->
            out.write(root.toString(2).toByteArray(Charsets.UTF_8))
        }

        backupFile
    }

    suspend fun inspectBackup(backupFile: File): BackupStats? = withContext(Dispatchers.IO) {
        try {
            val jsonStr = FileInputStream(backupFile).bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val storesCount = root.optJSONArray("stores")?.length() ?: 0
            val visitsCount = root.optJSONArray("visits")?.length() ?: 0
            val actionsCount = root.optJSONArray("actions")?.length() ?: 0
            val schedulesCount = root.optJSONArray("schedules")?.length() ?: 0
            val photosCount = root.optJSONArray("photos")?.length() ?: 0
            BackupStats(storesCount, visitsCount, actionsCount, schedulesCount, photosCount)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun restoreFromBackup(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonStr = FileInputStream(backupFile).bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)

            // Restore stores
            val storesArray = root.optJSONArray("stores")
            if (storesArray != null) {
                val list = mutableListOf<StoreEntity>()
                for (i in 0 until storesArray.length()) {
                    val obj = storesArray.getJSONObject(i)
                    list.add(
                        StoreEntity(
                            id = obj.getString("id"),
                            code = obj.getString("code"),
                            name = obj.getString("name"),
                            branchName = obj.optString("branchName", ""),
                            address = obj.optString("address", ""),
                            city = obj.optString("city", ""),
                            region = obj.optString("region", ""),
                            contactPerson = obj.optString("contactPerson", ""),
                            contactPhone = obj.optString("contactPhone", ""),
                            contactEmail = obj.optString("contactEmail", ""),
                            category = obj.optString("category", "Supermarket"),
                            assignedWorker = obj.optString("assignedWorker", "Field Rep"),
                            visitFrequency = obj.optString("visitFrequency", "Weekly"),
                            preferredDays = obj.optString("preferredDays", "Mon, Thu"),
                            openingHours = obj.optString("openingHours", "08:00 - 20:00"),
                            remarks = obj.optString("remarks", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isActive = obj.optBoolean("isActive", true),
                            isArchived = obj.optBoolean("isArchived", false)
                        )
                    )
                }
                database.storeDao().insertAll(list)
            }

            // Restore visits
            val visitsArray = root.optJSONArray("visits")
            if (visitsArray != null) {
                val list = mutableListOf<VisitEntity>()
                for (i in 0 until visitsArray.length()) {
                    val obj = visitsArray.getJSONObject(i)
                    list.add(
                        VisitEntity(
                            id = obj.getString("id"),
                            storeId = obj.getString("storeId"),
                            workerName = obj.optString("workerName", "Field Rep"),
                            startTimestampUtc = obj.getLong("startTimestampUtc"),
                            endTimestampUtc = if (obj.has("endTimestampUtc")) obj.getLong("endTimestampUtc") else null,
                            timeZoneId = obj.optString("timeZoneId", "UTC"),
                            notes = obj.optString("notes", ""),
                            keyObservations = obj.optString("keyObservations", ""),
                            issuesIdentified = obj.optString("issuesIdentified", ""),
                            positiveObservations = obj.optString("positiveObservations", ""),
                            recommendations = obj.optString("recommendations", ""),
                            priority = obj.optString("priority", "NORMAL"),
                            followUpRequired = obj.optBoolean("followUpRequired", false),
                            nextFollowUpDate = if (obj.has("nextFollowUpDate")) obj.getLong("nextFollowUpDate") else null,
                            status = obj.optString("status", "COMPLETED"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                database.visitDao().insertAll(list)
            }

            // Restore actions
            val actionsArray = root.optJSONArray("actions")
            if (actionsArray != null) {
                val list = mutableListOf<ActionEntity>()
                for (i in 0 until actionsArray.length()) {
                    val obj = actionsArray.getJSONObject(i)
                    list.add(
                        ActionEntity(
                            id = obj.getString("id"),
                            storeId = obj.getString("storeId"),
                            originatingVisitId = if (obj.has("originatingVisitId")) obj.getString("originatingVisitId") else null,
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            category = obj.optString("category", "Merchandising"),
                            priority = obj.optString("priority", "NORMAL"),
                            status = obj.optString("status", "OPEN"),
                            assignedTo = obj.optString("assignedTo", "Store Manager"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            dueAt = obj.optLong("dueAt", System.currentTimeMillis() + 604800000L),
                            completedAt = if (obj.has("completedAt")) obj.getLong("completedAt") else null,
                            completedBy = if (obj.has("completedBy")) obj.getString("completedBy") else null,
                            completionNotes = if (obj.has("completionNotes")) obj.getString("completionNotes") else null,
                            deferredReason = if (obj.has("deferredReason")) obj.getString("deferredReason") else null
                        )
                    )
                }
                database.actionDao().insertAll(list)
            }

            // Restore schedules
            val schedulesArray = root.optJSONArray("schedules")
            if (schedulesArray != null) {
                val list = mutableListOf<ScheduledVisitEntity>()
                for (i in 0 until schedulesArray.length()) {
                    val obj = schedulesArray.getJSONObject(i)
                    list.add(
                        ScheduledVisitEntity(
                            id = obj.getString("id"),
                            storeId = obj.getString("storeId"),
                            scheduledDate = obj.getLong("scheduledDate"),
                            purpose = obj.optString("purpose", "Routine Inspection"),
                            preparationNotes = obj.optString("preparationNotes", ""),
                            frequency = obj.optString("frequency", "ONCE"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            linkedVisitId = if (obj.has("linkedVisitId")) obj.getString("linkedVisitId") else null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                database.scheduleDao().insertAll(list)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
