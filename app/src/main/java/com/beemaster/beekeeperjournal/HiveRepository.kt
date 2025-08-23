// HiveRepository.kt

// Сюди винесено логіку роботи з даними про вулики (було у файлі MainActivity). Це забезпечить єдину точку доступу до даних і відокремить логіку збереження від логіки відображення.

// HiveRepository.kt

package com.beemaster.beekeeperjournal

import db.NoteDao
import androidx.lifecycle.LiveData
import db.HiveDao
import db.NoteEntity
import db.HiveEntity

class HiveRepository(
    private val noteDao: NoteDao,
    private val hiveDao: HiveDao
) {
    private val TAG = "HiveRepository"

    val allNotes: LiveData<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun insert(note: NoteEntity) {
        noteDao.insert(note)
    }

    suspend fun update(note: NoteEntity) {
        noteDao.update(note)
    }

    fun findNoteById(id: String): LiveData<NoteEntity?> {
        return noteDao.findNoteById(id)
    }

    suspend fun insertNote(note: NoteEntity) {
        noteDao.insert(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.update(note)
    }

    suspend fun deleteNote(noteId: String) {
        noteDao.deleteNoteById(noteId)
    }

    suspend fun getAllNotes_suspend(): List<NoteEntity> {
        return noteDao.getAllNotes_suspend()
    }

    suspend fun getNotesByHiveNumber(hiveNumber: Int): List<NoteEntity> {
        return noteDao.getNotesByHiveNumber(hiveNumber)
    }

    suspend fun insertHive(hive: HiveEntity) {
        hiveDao.insert(hive)
    }

    suspend fun getAllHives(): List<HiveEntity> {
        return hiveDao.getAllHives()
    }

    suspend fun updateHive(hive: HiveEntity) {
        hiveDao.update(hive)
    }

    suspend fun deleteHive(hiveNumber: Int) {
        hiveDao.deleteHiveByNumber(hiveNumber)
    }

    suspend fun getHiveByNumber(hiveNumber: Int): HiveEntity? {
        return hiveDao.getHiveByNumber(hiveNumber)
    }
}