package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionDao {
    @Query("SELECT * FROM actions ORDER BY dueAt ASC")
    fun getAllActionsFlow(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE storeId = :storeId ORDER BY status = 'COMPLETED', dueAt ASC")
    fun getActionsForStoreFlow(storeId: String): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE originatingVisitId = :visitId ORDER BY createdAt DESC")
    fun getActionsForVisitFlow(visitId: String): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE storeId = :storeId AND status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY dueAt ASC")
    suspend fun getOpenActionsForStore(storeId: String): List<ActionEntity>

    @Query("SELECT * FROM actions WHERE storeId = :storeId AND status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY dueAt ASC")
    fun getOpenActionsForStoreFlow(storeId: String): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE id = :id LIMIT 1")
    suspend fun getActionById(id: String): ActionEntity?

    @Query("SELECT * FROM actions WHERE id = :id LIMIT 1")
    fun getActionByIdFlow(id: String): Flow<ActionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: ActionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(actions: List<ActionEntity>)

    @Update
    suspend fun updateAction(action: ActionEntity)

    @Query("DELETE FROM actions WHERE id = :id")
    suspend fun deleteActionById(id: String)

    @Query("SELECT COUNT(*) FROM actions WHERE status NOT IN ('COMPLETED', 'CANCELLED')")
    fun getOpenActionsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM actions WHERE status NOT IN ('COMPLETED', 'CANCELLED') AND dueAt < :currentTime")
    fun getOverdueActionsCountFlow(currentTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM actions WHERE status = 'COMPLETED' AND completedAt >= :startOfMonth")
    fun getCompletedActionsThisMonthFlow(startOfMonth: Long): Flow<Int>

    @Query("SELECT * FROM actions ORDER BY createdAt DESC")
    suspend fun getAllActionsList(): List<ActionEntity>

    @Query("SELECT * FROM actions WHERE storeId = :storeId")
    suspend fun getActionsListForStore(storeId: String): List<ActionEntity>
}
