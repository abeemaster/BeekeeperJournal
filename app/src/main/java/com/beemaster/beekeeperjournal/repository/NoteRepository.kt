// NoteRepository.kt Ці класи будуть керувати доступом до даних.
// Сюди винесено всю логіку роботи з файлами нотаток.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.NoteDao
import com.beemaster.beekeeperjournal.db.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NoteRepository @Inject constructor(private val noteDao: NoteDao) {

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
}