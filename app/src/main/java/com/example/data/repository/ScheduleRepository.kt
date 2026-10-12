package com.example.data.repository

import com.example.data.local.dao.AuditDao
import com.example.data.local.dao.ScheduleDao
import com.example.data.local.dao.SyncOutboxDao
import com.example.data.local.entities.AuditEventEntity
import com.example.data.local.entities.ScheduledVisitEntity
import com.example.data.local.entities.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

class ScheduleRepository(
    private val scheduleDao: ScheduleDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val auditDao: AuditDao
) {
    val allSchedules: Flow<List<ScheduledVisitEntity>> = scheduleDao.getAllSchedulesFlow()
    val upcomingSchedules: Flow<List<ScheduledVisitEntity>> = scheduleDao.getUpcomingSchedulesFlow()

    fun getSchedulesForStore(storeId: String): Flow<List<ScheduledVisitEntity>> =
        scheduleDao.getSchedulesForStoreFlow(storeId)

    suspend fun scheduleVisit(
        storeId: String,
        scheduledDate: Long,
        purpose: String = "Routine Inspection",
        preparationNotes: String = "",
        frequency: String = "ONCE"
    ): ScheduledVisitEntity {
        val schedule = ScheduledVisitEntity(
            id = UUID.randomUUID().toString(),
            storeId = storeId,
            scheduledDate = scheduledDate,
            purpose = purpose,
            preparationNotes = preparationNotes,
            frequency = frequency,
            isCompleted = false
        )
        scheduleDao.insertSchedule(schedule)

        syncOutboxDao.insertEvent(
            SyncOutboxEntity(
                entityType = "SCHEDULE",
                entityId = schedule.id,
                operation = "CREATE",
                payloadJson = """{"id":"${schedule.id}","storeId":"$storeId","scheduledDate":$scheduledDate}"""
            )
        )
        auditDao.logEvent(
            AuditEventEntity(
                entityType = "SCHEDULE",
                entityId = schedule.id,
                action = "SCHEDULED",
                details = "Scheduled visit planned for store $storeId at $scheduledDate."
            )
        )
        return schedule
    }

    suspend fun markCompleted(scheduleId: String, linkedVisitId: String?) {
        val schedule = scheduleDao.getScheduleById(scheduleId)
        scheduleDao.markCompleted(scheduleId, linkedVisitId)

        // If recurring, create the next recurrence safely
        if (schedule != null && schedule.frequency != "ONCE") {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = schedule.scheduledDate
            }
            when (schedule.frequency) {
                "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "BI_WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 2)
                "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            }
            val nextSchedule = schedule.copy(
                id = UUID.randomUUID().toString(),
                scheduledDate = calendar.timeInMillis,
                isCompleted = false,
                linkedVisitId = null,
                createdAt = System.currentTimeMillis()
            )
            scheduleDao.insertSchedule(nextSchedule)
        }
    }

    suspend fun deleteSchedule(scheduleId: String) {
        scheduleDao.deleteScheduleById(scheduleId)
    }

    fun getOverdueCountFlow(): Flow<Int> =
        scheduleDao.getOverdueSchedulesCountFlow(System.currentTimeMillis())

    fun getUpcomingCountFlow(): Flow<Int> =
        scheduleDao.getUpcomingSchedulesCountFlow()

    suspend fun getAllSchedulesList(): List<ScheduledVisitEntity> = scheduleDao.getAllSchedulesList()
}
