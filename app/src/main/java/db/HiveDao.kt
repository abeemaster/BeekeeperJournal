// HiveDao.kt у вашому пакеті Цей DAO буде відповідати за всі операції з таблицею hives.

package db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface HiveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hive: HiveEntity)

    @Update
    suspend fun update(hive: HiveEntity)

    @Query("SELECT * FROM hives")
    suspend fun getAllHives(): List<HiveEntity>

    @Query("SELECT * FROM hives WHERE hiveNumber = :number")
    suspend fun getHiveByNumber(number: Int): HiveEntity?

    @Query("DELETE FROM hives WHERE hiveNumber = :number")
    suspend fun deleteHiveByNumber(number: Int)
}