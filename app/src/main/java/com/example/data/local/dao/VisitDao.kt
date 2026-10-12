package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY startTimestampUtc DESC")
    fun getAllVisitsFlow(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE storeId = :storeId ORDER BY startTimestampUtc DESC")
    fun getVisitsForStoreFlow(storeId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE status = 'IN_PROGRESS' ORDER BY startTimestampUtc DESC LIMIT 1")
    fun getActiveVisitFlow(): Flow<VisitEntity?>

    @Query("SELECT * FROM visits WHERE status = 'IN_PROGRESS' ORDER BY startTimestampUtc DESC LIMIT 1")
    suspend fun getActiveVisit(): VisitEntity?

    @Query("SELECT * FROM visits WHERE storeId = :storeId AND status = 'IN_PROGRESS' LIMIT 1")
    suspend fun getActiveVisitForStore(storeId: String): VisitEntity?

    @Query("SELECT * FROM visits WHERE id = :id LIMIT 1")
    suspend fun getVisitById(id: String): VisitEntity?

    @Query("SELECT * FROM visits WHERE id = :id LIMIT 1")
    fun getVisitByIdFlow(id: String): Flow<VisitEntity?>

    @Query("SELECT * FROM visits WHERE storeId = :storeId AND status = 'COMPLETED' ORDER BY startTimestampUtc DESC LIMIT 1")
    suspend fun getLastCompletedVisitForStore(storeId: String): VisitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(visits: List<VisitEntity>)

    @Update
    suspend fun updateVisit(visit: VisitEntity)

    @Query("DELETE FROM visits WHERE id = :id")
    suspend fun deleteVisitById(id: String)

    @Query("SELECT COUNT(*) FROM visits WHERE status = 'COMPLETED' AND startTimestampUtc >= :startOfDayUtc")
    fun getVisitsCompletedTodayFlow(startOfDayUtc: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM visits WHERE status = 'COMPLETED' AND startTimestampUtc >= :startOfWeekUtc")
    fun getVisitsCompletedThisWeekFlow(startOfWeekUtc: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM visits WHERE status = 'IN_PROGRESS'")
    fun getActiveVisitsCountFlow(): Flow<Int>

    @Query("SELECT * FROM visits ORDER BY startTimestampUtc DESC")
    suspend fun getAllVisitsList(): List<VisitEntity>

    @Query("SELECT * FROM visits WHERE storeId = :storeId ORDER BY startTimestampUtc DESC")
    suspend fun getVisitsListForStore(storeId: String): List<VisitEntity>
}
