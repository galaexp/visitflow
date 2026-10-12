package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ActionDao
import com.example.data.local.dao.AuditDao
import com.example.data.local.dao.ChecklistDao
import com.example.data.local.dao.PhotoDao
import com.example.data.local.dao.ScheduleDao
import com.example.data.local.dao.StoreDao
import com.example.data.local.dao.SyncOutboxDao
import com.example.data.local.dao.VisitDao
import com.example.data.local.entities.ActionEntity
import com.example.data.local.entities.AuditEventEntity
import com.example.data.local.entities.ChecklistItemEntity
import com.example.data.local.entities.PhotoEntity
import com.example.data.local.entities.ScheduledVisitEntity
import com.example.data.local.entities.StoreEntity
import com.example.data.local.entities.SyncOutboxEntity
import com.example.data.local.entities.VisitEntity

@Database(
    entities = [
        StoreEntity::class,
        VisitEntity::class,
        ActionEntity::class,
        PhotoEntity::class,
        ScheduledVisitEntity::class,
        ChecklistItemEntity::class,
        SyncOutboxEntity::class,
        AuditEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun visitDao(): VisitDao
    abstract fun actionDao(): ActionDao
    abstract fun photoDao(): PhotoDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "storeflow_offline.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
