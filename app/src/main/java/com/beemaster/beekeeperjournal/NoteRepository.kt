// NoteRepository.kt
// Сюди винесено всю логіку роботи з файлами нотаток.

package com.beemaster.beekeeperjournal

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class NoteRepository(private val context: Context) {

    private val gson = Gson()
    private val NOTES_FILE_NAME = "notes.json"

    fun getNotesFile(): File {
        return File(context.filesDir, NOTES_FILE_NAME)
    }

    fun readAllNotesFromJson(): MutableList<Note> {
        val file = getNotesFile()
        if (!file.exists() || file.length() == 0L) {
            return mutableListOf()
        }
        return try {
            FileReader(file).use { reader ->
                val type = object : TypeToken<MutableList<Note>>() {}.type
                gson.fromJson(reader, type) ?: mutableListOf()
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "Помилка читання записів з файлу: ${e.message}", e)
            mutableListOf()
        }
    }

    fun writeAllNotesToJson(notes: List<Note>) {
        val file = getNotesFile()
        try {
            FileWriter(file).use { writer ->
                gson.toJson(notes, writer)
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "Помилка запису записів до файлу: ${e.message}", e)
        }
    }
}