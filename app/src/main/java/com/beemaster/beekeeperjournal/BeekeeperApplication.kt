// У файлі BeekeeperApplication.kt

package com.beemaster.beekeeperjournal

import android.app.Application
import android.util.Log
import androidx.room.Room
import db.AppDatabase
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.android.StorageService

class BeekeeperApplication : Application() {

    // ✅ ІНІЦІАЛІЗАЦІЯ БАЗИ ДАНИХ
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this, // Використовуємо 'this' замість 'applicationContext'
            AppDatabase::class.java,
            "beekeeper_journal_database"
        ).build()
    }

    // ✅ ІНІЦІАЛІЗАЦІЯ РЕПОЗИТОРІЮ
    // Передаємо DAO в правильному порядку.
    val hiveRepository: HiveRepository by lazy {
        HiveRepository(database.noteDao(), database.hiveDao())
    }

    companion object {
        private const val TAG = "BeekeeperApplication"
        var voskModel: Model? = null
            private set

        private val voskModelReadyListeners = mutableListOf<() -> Unit>()

        fun addVoskModelReadyListener(listener: () -> Unit) {
            if (voskModel != null) {
                listener.invoke()
            } else {
                voskModelReadyListeners.add(listener)
            }
        }

        private fun notifyVoskModelReady() {
            voskModelReadyListeners.forEach { it.invoke() }
            voskModelReadyListeners.clear()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: BeekeeperApplication started. Initializing Vosk model globally.")
        initVoskModel()
    }

    override fun onTerminate() {
        super.onTerminate()
        // Важливо звільнити ресурси Vosk при завершенні роботи додатку
        Log.d(TAG, "onTerminate: Releasing Vosk model resources.")
        voskModel?.close()
        voskModel = null
    }

    private fun initVoskModel() {
        LibVosk.setLogLevel(LogLevel.INFO)
        Log.d(TAG, "initVoskModel: Starting Vosk model initialization.")
        Log.d(TAG, "initVoskModel: Attempting to unpack model from assets: 'vosk-model-small-uk-v3-small'")

        StorageService.unpack(this, "vosk-model-small-uk-v3-small", "model",
            { unpackedModel ->
                voskModel = unpackedModel
                notifyVoskModelReady()
                Log.d(TAG, "initVoskModel: Vosk model successfully loaded and unpacked globally.")
            },
            { exception ->
                val errorMessage = exception.message ?: "Невідома помилка розпакування моделі."
                Log.e(TAG, "initVoskModel: Error unpacking Vosk model: $errorMessage", exception)
            })
    }
}