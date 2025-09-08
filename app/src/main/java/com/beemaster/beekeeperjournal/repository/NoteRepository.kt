// NoteRepository.kt Ці класи будуть керувати доступом до даних.
// Сюди винесено всю логіку роботи з файлами нотаток.

package com.beemaster.beekeeperjournal.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.beemaster.beekeeperjournal.db.HiveDao
import com.beemaster.beekeeperjournal.db.HiveEntity
import com.beemaster.beekeeperjournal.db.NoteDao
import com.beemaster.beekeeperjournal.db.NoteEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val hiveDao: HiveDao
) {

    // Методи для нотаток
    fun getAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes()
    }

    fun getNotesByHiveAndType(hiveId: Int, noteType: String): Flow<List<NoteEntity>> {
        return noteDao.getNotesByHiveAndType(hiveId, noteType)
    }

    suspend fun insertNote(note: NoteEntity) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(id: Int) {
        noteDao.deleteNote(id)
    }

    suspend fun getNoteById(id: Int): NoteEntity? {
        return noteDao.getNoteById(id)
    }

    // ✅ Методи для роботи з вуликами
    fun getAllHives(): LiveData<List<HiveEntity>> {
        // ✅ asLiveData() перетворює Flow на LiveData, вирішуючи проблему
        return hiveDao.getAllHives().asLiveData()
    }

    // ✅ Додаємо getHivesCount()
    fun getHivesCount(): LiveData<Int> {
        return hiveDao.getHivesCount().asLiveData()
    }

    suspend fun insertHive(hive: HiveEntity) {
        hiveDao.insertHive(hive)
    }

    suspend fun updateHive(hive: HiveEntity) {
        hiveDao.updateHive(hive)
    }

    suspend fun deleteHive(hive: HiveEntity) {
        hiveDao.deleteHive(hive.id)
    }

    // ✅ Додаємо getHiveByNumber()
    suspend fun getHiveByNumber(number: Int): HiveEntity? {
        return hiveDao.getHiveByNumber(number)
    }

    // ✅ Додаємо методи для експорту
    suspend fun getAllHivesSuspend(): List<HiveEntity> {
        return hiveDao.getAllHivesSuspend()
    }

    suspend fun getAllNotesSuspend(): List<NoteEntity> {
        return noteDao.getAllNotesSuspend()
    }

    // ✅ Додаємо методи для імпорту
    suspend fun importHives(hives: List<HiveEntity>) {
        hiveDao.clearAndInsertHives(hives)
    }

    suspend fun importNotes(notes: List<NoteEntity>) {
        noteDao.clearAndInsertNotes(notes)
    }
}
