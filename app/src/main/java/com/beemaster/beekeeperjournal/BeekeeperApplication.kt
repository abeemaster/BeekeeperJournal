// BeekeeperApplication.kt Цей клас є точкою входу вашого додатка.
// Він ініціалізує глобальні ресурси, доступні з будь-якої точки програми.

package com.beemaster.beekeeperjournal

import android.app.Application
import android.util.Log
import com.beemaster.beekeeperjournal.voice.VoskModelManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

// Анотація @HiltAndroidApp дозволяє Hilt
// розпочати генерацію коду для ін'єкції залежностей.

@HiltAndroidApp
class BeekeeperApplication : Application() {

    // Hilt сам створить синглтон VoskModelManager
    @Inject
    lateinit var voskModelManager: VoskModelManager

    companion object {
        private const val TAG = "BeekeeperApplication"

    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: BeekeeperApplication started.")
        // initVoskModel() більше не викликається тут.
        // Він викликається в конструкторі VoskModelManager.
    }

    override fun onTerminate() {
        super.onTerminate()
        // ВИКОРИСТОВУЄМО МЕНЕДЖЕР для звільнення ресурсів
        Log.d(TAG, "onTerminate: Releasing Vosk model resources via manager.")
        voskModelManager.release()
    }
}