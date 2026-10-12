package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE visitId = :visitId ORDER BY capturedAt ASC")
    fun getPhotosForVisitFlow(visitId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE actionId = :actionId ORDER BY capturedAt ASC")
    fun getPhotosForActionFlow(actionId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos ORDER BY capturedAt DESC")
    fun getAllPhotosFlow(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun getPhotoById(id: String): PhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>)

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deletePhotoById(id: String)

    @Query("SELECT * FROM photos ORDER BY capturedAt DESC")
    suspend fun getAllPhotosList(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE visitId = :visitId")
    suspend fun getPhotosListForVisit(visitId: String): List<PhotoEntity>
}
