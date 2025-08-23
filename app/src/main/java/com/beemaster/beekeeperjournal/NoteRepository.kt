// NoteRepository.kt
// Сюди винесено всю логіку роботи з файлами нотаток.

// NoteRepository.kt
// NoteRepository.kt

package com.beemaster.beekeeperjournal

import androidx.lifecycle.liveData
import androidx.lifecycle.LiveData
import db.NoteEntity
import kotlinx.coroutines.Dispatchers
import db.NoteDao // ✅ Додаємо імпорт db.NoteDao

class NoteRepository(private val noteDao: NoteDao) { // ✅ ВИПРАВЛЕНО: тепер noteDao має правильний тип

    val allNotes: LiveData<List<NoteEntity>> = liveData(Dispatchers.IO) {
        val notes = noteDao.getAllNotes_suspend()
        emit(notes)
    }

    suspend fun insert(note: NoteEntity) {
        noteDao.insert(note)
    }

    suspend fun update(note: NoteEntity) {
        noteDao.update(note)
    }

    suspend fun deleteNoteById(noteId: String) {
        noteDao.deleteNoteById(noteId)
    }

    fun findNoteById(id: String): LiveData<NoteEntity?> {
        return noteDao.findNoteById(id)
    }

    fun getNotesByTypeAndHive(entryType: String, hiveNumber: Int): List<NoteEntity> {
        return noteDao.getNotesByTypeAndHive(entryType, hiveNumber)
    }

    fun getNoteByIdBlocking(noteId: String): NoteEntity? {
        return noteDao.findNoteById_blocking(noteId)
    }
}