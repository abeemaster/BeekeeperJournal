package com.beemaster.beekeeperjournal.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiveDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertHive(hive: HiveEntity)

    @Update
    suspend fun updateHive(hive: HiveEntity)

    @Query("DELETE FROM hives WHERE id = :hiveId")
    suspend fun deleteHive(hiveId: Int)

    @Query("SELECT * FROM hives ORDER BY hiveNumber ASC")
    fun getAllHives(): Flow<List<HiveEntity>>

    @Query("SELECT COUNT(*) FROM hives")
    fun getHivesCount(): Flow<Int> // ✅ Додаємо getHivesCount()

    @Query("SELECT * FROM hives WHERE hiveNumber = :number")
    suspend fun getHiveByNumber(number: Int): HiveEntity? // ✅ Додаємо getHiveByNumber()

    @Query("SELECT * FROM hives WHERE id = :hiveId")
    suspend fun getHiveById(hiveId: Int): HiveEntity?

    @Query("SELECT * FROM hives")
    suspend fun getAllHivesSuspend(): List<HiveEntity>

    @Query("DELETE FROM hives")
    suspend fun deleteAllHives()

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAllHives(hives: List<HiveEntity>)

    suspend fun clearAndInsertHives(hives: List<HiveEntity>) {
        deleteAllHives()
        insertAllHives(hives)
    }
    // ✅ Додано: метод для отримання всіх вуликів


    // ✅ Додано: метод для імпорту списку вуликів
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHives(hives: List<HiveEntity>)
}