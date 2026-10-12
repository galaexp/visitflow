package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ScheduledVisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM scheduled_visits ORDER BY scheduledDate ASC")
    fun getAllSchedulesFlow(): Flow<List<ScheduledVisitEntity>>

    @Query("SELECT * FROM scheduled_visits WHERE isCompleted = 0 ORDER BY scheduledDate ASC")
    fun getUpcomingSchedulesFlow(): Flow<List<ScheduledVisitEntity>>

    @Query("SELECT * FROM scheduled_visits WHERE isCompleted = 0 AND scheduledDate < :currentTime ORDER BY scheduledDate ASC")
    fun getOverdueSchedulesFlow(currentTime: Long): Flow<List<ScheduledVisitEntity>>

    @Query("SELECT * FROM scheduled_visits WHERE scheduledDate BETWEEN :startOfDay AND :endOfDay ORDER BY scheduledDate ASC")
    fun getSchedulesForDayFlow(startOfDay: Long, endOfDay: Long): Flow<List<ScheduledVisitEntity>>

    @Query("SELECT * FROM scheduled_visits WHERE storeId = :storeId ORDER BY scheduledDate ASC")
    fun getSchedulesForStoreFlow(storeId: String): Flow<List<ScheduledVisitEntity>>

    @Query("SELECT * FROM scheduled_visits WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: String): ScheduledVisitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduledVisitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<ScheduledVisitEntity>)

    @Update
    suspend fun updateSchedule(schedule: ScheduledVisitEntity)

    @Query("UPDATE scheduled_visits SET isCompleted = 1, linkedVisitId = :visitId WHERE id = :id")
    suspend fun markCompleted(id: String, visitId: String?)

    @Query("DELETE FROM scheduled_visits WHERE id = :id")
    suspend fun deleteScheduleById(id: String)

    @Query("SELECT COUNT(*) FROM scheduled_visits WHERE isCompleted = 0 AND scheduledDate < :currentTime")
    fun getOverdueSchedulesCountFlow(currentTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM scheduled_visits WHERE isCompleted = 0")
    fun getUpcomingSchedulesCountFlow(): Flow<Int>

    @Query("SELECT * FROM scheduled_visits ORDER BY scheduledDate ASC")
    suspend fun getAllSchedulesList(): List<ScheduledVisitEntity>
}
