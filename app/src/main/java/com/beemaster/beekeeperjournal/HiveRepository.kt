// HiveRepository.kt

// Сюди винесено логіку роботи з даними про вулики (було у файлі MainActivity). Це забезпечить єдину точку доступу до даних і відокремить логіку збереження від логіки відображення.

package com.beemaster.beekeeperjournal

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class HiveRepository(private val context: Context) {

    private val gson = Gson()
    private val HIVE_DATA_FILE_NAME = "hives.json"
    private val NOTES_FILE_NAME = "notes.json"
    private val notesFileName = "notes.json"
    private val tag = "HiveRepository"

    fun readHivesFromJson(): MutableList<HiveData> {
        val file = File(context.filesDir, HIVE_DATA_FILE_NAME)
        if (!file.exists() || file.length() == 0L) {
            return mutableListOf()
        }
        return try {
            FileReader(file).use { reader ->
                val type = object : TypeToken<MutableList<HiveData>>() {}.type
                gson.fromJson(reader, type) ?: mutableListOf()
            }
        } catch (e: Exception) {
            Log.e(tag, "Помилка читання вуликів з файлу: ${e.message}", e)
            mutableListOf()
        }
    }

    fun writeHivesToJson(hives: List<HiveData>) {
        val file = File(context.filesDir, HIVE_DATA_FILE_NAME)
        try {
            FileWriter(file).use { writer ->
                gson.toJson(hives, writer)
            }
        } catch (e: Exception) {
            Log.e(tag, "Помилка запису вуликів до файлу: ${e.message}", e)
        }
    }

    fun readAllNotesFromJson(): MutableList<Note> {
        val file = File(context.filesDir, NOTES_FILE_NAME)
        if (!file.exists() || file.length() == 0L) {
            return mutableListOf()
        }
        return try {
            FileReader(file).use { reader ->
                val type = object : TypeToken<MutableList<Note>>() {}.type
                gson.fromJson(reader, type) ?: mutableListOf()
            }
        } catch (e: Exception) {
            Log.e(tag, "Помилка читання записів з файлу: ${e.message}", e)
            mutableListOf()
        }
    }

    // У файлі HiveRepository.kt

    // ✅ 1. ФУНКЦІЯ ДЛЯ ОНОВЛЕННЯ НОТАТКИ
    fun updateNoteInJson(updatedNote: Note) {
        val allNotes = readAllNotesFromJson()
        val noteIndexToUpdate = allNotes.indexOfFirst { it.id == updatedNote.id }
        if (noteIndexToUpdate != -1) {
            allNotes[noteIndexToUpdate] = updatedNote
            writeNotesToJson(allNotes)
        } else {
            Log.e(tag, "Нотатка з ID ${updatedNote.id} не знайдена для оновлення.")
        }
    }

    // ✅ 2. ПРИВАТНА ФУНКЦІЯ ДЛЯ ЗАПИСУ ВСІХ НОТАТОК
    private fun writeNotesToJson(notes: List<Note>) {
        val file = File(context.filesDir, notesFileName)
        try {
            FileWriter(file).use { writer ->
                gson.toJson(notes, writer)
            }
        } catch (e: Exception) {
            Log.e(tag, "Помилка запису нотаток: ${e.message}", e)
        }
    }
}