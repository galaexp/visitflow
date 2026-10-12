package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.StoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores WHERE isArchived = 0 ORDER BY name ASC")
    fun getActiveStoresFlow(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores ORDER BY isArchived ASC, name ASC")
    fun getAllStoresFlow(): Flow<List<StoreEntity>>

    @Query("""
        SELECT * FROM stores 
        WHERE (name LIKE '%' || :query || '%' 
           OR code LIKE '%' || :query || '%' 
           OR city LIKE '%' || :query || '%' 
           OR contactPerson LIKE '%' || :query || '%')
        ORDER BY isArchived ASC, name ASC
    """)
    fun searchStores(query: String): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE id = :id LIMIT 1")
    suspend fun getStoreById(id: String): StoreEntity?

    @Query("SELECT * FROM stores WHERE id = :id LIMIT 1")
    fun getStoreByIdFlow(id: String): Flow<StoreEntity?>

    @Query("SELECT * FROM stores WHERE code = :code LIMIT 1")
    suspend fun getStoreByCode(code: String): StoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: StoreEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stores: List<StoreEntity>)

    @Update
    suspend fun updateStore(store: StoreEntity)

    @Query("UPDATE stores SET isArchived = :archived, updatedAt = :timestamp WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM stores WHERE id = :id")
    suspend fun deleteStoreById(id: String)

    @Query("SELECT COUNT(*) FROM stores WHERE isArchived = 0")
    suspend fun getActiveStoresCount(): Int

    @Query("SELECT * FROM stores")
    suspend fun getAllStoresList(): List<StoreEntity>
}
