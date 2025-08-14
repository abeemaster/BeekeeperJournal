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
    private val TAG = "HiveRepository"

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
            Log.e(TAG, "Помилка читання вуликів з файлу: ${e.message}", e)
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
            Log.e(TAG, "Помилка запису вуликів до файлу: ${e.message}", e)
        }
    }
}