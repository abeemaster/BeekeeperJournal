// HiveDao.kt у вашому пакеті Цей DAO буде відповідати за всі операції з таблицею hives.
// Ці інтерфейси міститимуть методи для взаємодії з таблицями.

package com.beemaster.beekeeperjournal.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HiveDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHive(hive: HiveEntity)

    @Update
    suspend fun updateHive(hive: HiveEntity)

    @Query("DELETE FROM hives WHERE id = :hiveId")
    suspend fun deleteHive(hiveId: Int)

    @Query("SELECT * FROM hives ORDER BY hiveNumber ASC")
    fun getAllHives(): Flow<List<HiveEntity>> // ✅ Повертає Flow

    @Query("SELECT * FROM hives WHERE id = :hiveId")
    suspend fun getHiveById(hiveId: Int): HiveEntity?
}