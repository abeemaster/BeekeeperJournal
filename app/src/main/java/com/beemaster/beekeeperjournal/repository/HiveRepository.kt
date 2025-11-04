// HiveRepository.kt HiveRepository.kt та NoteRepository.kt
//Ці класи будуть керувати доступом до даних.

// Сюди винесено логіку роботи з даними про вулики (було у файлі MainActivity).
// Це забезпечить єдину точку доступу до даних і відокремить логіку збереження від логіки відображення.

package com.beemaster.beekeeperjournal.repository

import androidx.annotation.ColorInt
import com.beemaster.beekeeperjournal.db.dao.HiveDao
import com.beemaster.beekeeperjournal.db.dao.NoteDao
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторій для роботи з вуликами.
 * Відповідає за абстрагування джерела даних (HiveDao) від рівня ViewModel.
 * Здійснює всі операції, пов'язані з HiveEntity.
 */
@Singleton // ✅ Додаємо Singleton для коректної роботи Hilt
class HiveRepository @Inject constructor(
    private val hiveDao: HiveDao,
    private val noteDao: NoteDao
) {
    /**
     * Вставляє новий вулик у базу даних.
     * @param hive Об'єкт HiveEntity, який потрібно вставити.
     */
    suspend fun insertHive(hive: HiveEntity) {
        hiveDao.insertHive(hive)
    }

    /**
     * Отримує об'єкт вулика за його унікальним ID.
     * @param hiveId Унікальний ID вулика.
     * @return Об'єкт HiveEntity або null.
     */
    suspend fun getHiveById(hiveId: Int): HiveEntity? {
        return hiveDao.getHiveById(hiveId)
    }

    /**
     * Асинхронний виклик, що повертає список усіх вуликів один раз.
     * Отримує перше значення з потоку Flow.
     * @return Список усіх HiveEntity.
     */
    suspend fun getAllHives(): List<HiveEntity> {
        return hiveDao.getAllHives().first()
    }

    /**
     * Повертає Flow зі списком усіх вуликів.
     * Цей метод використовується для спостереження за даними в реальному часі.
     * @return Flow, що містить список усіх HiveEntity.
     */
    fun getAllHivesAsFlow(): Flow<List<HiveEntity>> {
        return hiveDao.getAllHives()
    }

    suspend fun updateHiveNumber(hiveId: Long, newNumber: String) {
        // Виклик методу DAO
        hiveDao.updateHiveNumber(hiveId, newNumber)
    }
    /**
     * Отримує об'єкт вулика за його унікальним номером.
     * Використовується для перевірки унікальності при додаванні нового вулика.
     * @param hiveNumber Номер вулика (String).
     * @return Об'єкт HiveEntity або null.
     */
    suspend fun getHiveByNumber(hiveNumber: String): HiveEntity? {
        return hiveDao.getHiveByNumber(hiveNumber)
    }

    /**
     * Видаляє вулик із бази даних.
     * @param hive Об'єкт HiveEntity для видалення.
     * Додано каскадне видалення нотаток.
     */
    suspend fun deleteHive(hive: HiveEntity) {
        // 1. Спочатку видаляємо всі залежні нотатки
        noteDao.deleteNotesByHiveId(hive.id)

        // 2. Потім видаляємо сам вулик
        hiveDao.deleteHive(hive.id)
    }
    suspend fun getHiveById(hiveId: Long): HiveEntity? {
        return hiveDao.getHiveById(hiveId)
    }


    /**
     * Імпортує список вуликів у базу даних, зазвичай, для відновлення.
     * @param hives Список HiveEntity для імпорту.
     */
    suspend fun importHives(hives: List<HiveEntity>) {
        // ВИКОРИСТОВУЙТЕ DAO-метод, що виконує очищення та вставку в ОДНІЙ транзакції.
        // Це забезпечує надійне відновлення/імпорт даних.
        hiveDao.clearAndInsertHives(hives)
    }
    /**
     * Оновлює основний колір вулика за ID.
     */
    suspend fun updatePrimaryColor(hiveId: Long, @ColorInt color: Int) {
        // Виклик методу DAO
        hiveDao.updatePrimaryColor(hiveId, color)
    }

    /**
     * Оновлює додатковий колір вулика за ID.
     */
    suspend fun updateSecondaryColor(hiveId: Long, @ColorInt color: Int) {
        // Виклик методу DAO
        hiveDao.updateSecondaryColor(hiveId, color)
    }
}