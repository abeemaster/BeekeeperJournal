// HiveRepository.kt HiveRepository.kt та NoteRepository.kt
//Ці класи будуть керувати доступом до даних.

// Сюди винесено логіку роботи з даними про вулики (було у файлі MainActivity).
// Це забезпечить єдину точку доступу до даних і відокремить логіку збереження від логіки відображення.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.HiveDao
import com.beemaster.beekeeperjournal.db.NoteDao
import com.beemaster.beekeeperjournal.db.HiveEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class HiveRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val hiveDao: HiveDao
) {
    suspend fun insertHive(hive: HiveEntity) {
        hiveDao.insertHive(hive)
    }

    suspend fun getHiveById(hiveId: Int): HiveEntity? {
        return hiveDao.getHiveById(hiveId)
    }

    /**
     * Асинхронний виклик, що повертає список вуликів один раз.
     * Цей метод є suspend, тому його потрібно викликати в корутині.
     */
    suspend fun getAllHives(): List<HiveEntity> {
        return hiveDao.getAllHives().first()
    }

    /**
     * Повертає Flow зі списком вуликів.
     * Цей метод не є suspend, і він автоматично надає оновлення.
     */
    fun getAllHivesAsFlow(): Flow<List<HiveEntity>> {
        return hiveDao.getAllHives()
    }

    // ✅ ДОДАНО: Метод для оновлення вулика
    suspend fun updateHive(hive: HiveEntity) {
        hiveDao.updateHive(hive)
    }


    // ✅ ДОДАНО: Метод для видалення вулика
    suspend fun deleteHive(hive: HiveEntity) {
        hiveDao.deleteHive(hive.id)
    }
}