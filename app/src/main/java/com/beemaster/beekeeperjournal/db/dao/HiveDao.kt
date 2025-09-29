package com.beemaster.beekeeperjournal.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import kotlinx.coroutines.flow.Flow

/**
 * Інтерфейс доступу до даних (DAO) для роботи з таблицею "hives" (вулики).
 */
@Dao
interface HiveDao {

    /**
     * Вставляє новий вулик у базу даних або замінює його, якщо він вже існує (за ID).
     * @param hive Об'єкт HiveEntity для вставки.
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertHive(hive: HiveEntity)

    /**
     * Оновлює інформацію про існуючий вулик.
     * @param hive Об'єкт HiveEntity для оновлення.
     */
    @Update
    suspend fun updateHive(hive: HiveEntity)

    /**
     * Видаляє вулик за його унікальним ID.
     * @param hiveId ID вулика для видалення.
     */
    @Query("DELETE FROM hives WHERE id = :hiveId")
    suspend fun deleteHive(hiveId: Int)

    /**
     * Отримує всі вулики, відсортовані за номером. Повертає потік даних (Flow).
     * @return Flow, що містить список HiveEntity.
     */
    @Query("SELECT * FROM hives ORDER BY hiveNumber ASC")
    fun getAllHives(): Flow<List<HiveEntity>>

    /**
     * Отримує загальну кількість вуликів у таблиці. Повертає потік даних (Flow).
     * @return Flow, що містить кількість вуликів.
     */
    @Query("SELECT COUNT(*) FROM hives")
    fun getHivesCount(): Flow<Int>

    /**
     * Отримує вулик за його унікальним номером.
     * Використовується для перевірки унікальності.
     * @param number Номер вулика (String).
     * @return Об'єкт HiveEntity або null.
     */
    @Query("SELECT * FROM hives WHERE hiveNumber = :number")
    suspend fun getHiveByNumber(number: String): HiveEntity?

    /**
     * Отримує вулик за його внутрішнім унікальним ID.
     * Використовується для завантаження даних для відображення заголовка.
     * @param hiveId Внутрішній ID вулика (Int).
     * @return Об'єкт HiveEntity або null.
     */
    @Query("SELECT * FROM hives WHERE id = :hiveId")
    suspend fun getHiveById(hiveId: Int): HiveEntity?

    /**
     * Отримує всі вулики. Використовується у синхронному контексті (наприклад, для експорту).
     * @return Список усіх HiveEntity.
     */
    @Query("SELECT * FROM hives")
    suspend fun getAllHivesSuspend(): List<HiveEntity>

    /**
     * Видаляє всі записи з таблиці вуликів.
     */
    @Query("DELETE FROM hives")
    suspend fun deleteAllHives()

    /**
     * Вставляє список вуликів. Використовується для імпорту/відновлення даних.
     * @param hives Список HiveEntity для вставки.
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAllHives(hives: List<HiveEntity>)

    /**
     * Виконує очищення таблиці та подальшу вставку нового списку вуликів.
     * Це допоміжний метод для імпорту/відновлення.
     * @param hives Список HiveEntity для імпорту.
     */
    suspend fun clearAndInsertHives(hives: List<HiveEntity>) {
        deleteAllHives()
        insertAllHives(hives)
    }

    /**
     * Вставляє список вуликів, замінюючи існуючі за конфліктом ID.
     * Це дублюючий метод для insertAllHives, але використовується в репозиторії.
     * @param hives Список HiveEntity для імпорту.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHives(hives: List<HiveEntity>)
}