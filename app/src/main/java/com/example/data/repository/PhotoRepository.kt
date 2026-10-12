package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.dao.PhotoDao
import com.example.data.local.dao.SyncOutboxDao
import com.example.data.local.entities.PhotoEntity
import com.example.data.local.entities.SyncOutboxEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PhotoRepository(
    private val context: Context,
    private val photoDao: PhotoDao,
    private val syncOutboxDao: SyncOutboxDao
) {
    private val photosDir: File = File(context.filesDir, "photos").apply {
        if (!exists()) mkdirs()
    }

    fun getPhotosForVisit(visitId: String): Flow<List<PhotoEntity>> =
        photoDao.getPhotosForVisitFlow(visitId)

    fun getPhotosForAction(actionId: String): Flow<List<PhotoEntity>> =
        photoDao.getPhotosForActionFlow(actionId)

    suspend fun saveImageFromUri(
        uri: Uri,
        visitId: String?,
        actionId: String?,
        caption: String = "",
        isEvidence: Boolean = false,
        latitude: Double? = null,
        longitude: Double? = null
    ): PhotoEntity? = withContext(Dispatchers.IO) {
        try {
            val fileName = "photo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val targetFile = File(photosDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: return@withContext null
                // Compress bitmap responsibly (max 1600px width/height, 82% JPEG quality)
                val maxDim = 1600
                val scale = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val ratio = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * ratio).toInt(),
                        (bitmap.height * ratio).toInt(),
                        true
                    )
                } else bitmap

                FileOutputStream(targetFile).use { out ->
                    scale.compress(Bitmap.CompressFormat.JPEG, 82, out)
                }
            } ?: return@withContext null

            val photoEntity = PhotoEntity(
                id = UUID.randomUUID().toString(),
                visitId = visitId,
                actionId = actionId,
                filePath = targetFile.absolutePath,
                caption = caption,
                capturedAt = System.currentTimeMillis(),
                fileSize = targetFile.length(),
                isEvidence = isEvidence,
                latitude = latitude,
                longitude = longitude
            )
            photoDao.insertPhoto(photoEntity)

            syncOutboxDao.insertEvent(
                SyncOutboxEntity(
                    entityType = "PHOTO",
                    entityId = photoEntity.id,
                    operation = "CREATE",
                    payloadJson = """{"id":"${photoEntity.id}","fileSize":${photoEntity.fileSize},"caption":"$caption"}"""
                )
            )
            photoEntity
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createTempCameraFile(): File = withContext(Dispatchers.IO) {
        val fileName = "capture_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        File(photosDir, fileName)
    }

    suspend fun registerCapturedFile(
        file: File,
        visitId: String?,
        actionId: String?,
        caption: String = "",
        isEvidence: Boolean = false,
        latitude: Double? = null,
        longitude: Double? = null
    ): PhotoEntity = withContext(Dispatchers.IO) {
        val photoEntity = PhotoEntity(
            id = UUID.randomUUID().toString(),
            visitId = visitId,
            actionId = actionId,
            filePath = file.absolutePath,
            caption = caption,
            capturedAt = System.currentTimeMillis(),
            fileSize = file.length(),
            isEvidence = isEvidence,
            latitude = latitude,
            longitude = longitude
        )
        photoDao.insertPhoto(photoEntity)
        syncOutboxDao.insertEvent(
            SyncOutboxEntity(
                entityType = "PHOTO",
                entityId = photoEntity.id,
                operation = "CREATE",
                payloadJson = """{"id":"${photoEntity.id}","fileSize":${photoEntity.fileSize}}"""
            )
        )
        photoEntity
    }

    suspend fun deletePhoto(photoId: String) = withContext(Dispatchers.IO) {
        val photo = photoDao.getPhotoById(photoId)
        if (photo != null) {
            val file = File(photo.filePath)
            if (file.exists()) file.delete()
            photoDao.deletePhotoById(photoId)
        }
    }

    suspend fun getAllPhotosList(): List<PhotoEntity> = photoDao.getAllPhotosList()
}
