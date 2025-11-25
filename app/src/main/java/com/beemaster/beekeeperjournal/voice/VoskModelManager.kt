// VoskModelManager.kt Це Менеджер або Репозиторій для моделі Vosk. Його головна мета — звільнити VoiceManager від важкої роботи з великим файлом моделі.
package com.beemaster.beekeeperjournal.voice

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.android.StorageService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton-клас, відповідальний за ініціалізацію, зберігання та керування
 * життєвим циклом моделі Vosk для розпізнавання мовлення.
 *
 * Використовується для забезпечення асинхронного доступу до моделі
 * та її звільнення Hilty.
 */
@Singleton
class VoskModelManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val TAG = "VoskModelManager"

    private var voskModel: Model? = null
    // Змінюю mutableList на CopyOnWriteArrayList, щоб уникнути ConcurrentModificationException
    // при виклику invoke()
    private val voskModelReadyListeners = mutableListOf<() -> Unit>()

    // Константа для імені моделі в assets
    private val VOSK_MODEL_NAME = "vosk-model-small-uk-v3-small"

    val isModelReady: Boolean
        get() = voskModel != null

    init {
        // Ініціалізація Vosk відбувається одразу при створенні синглтона Hilt
        initVoskModel()
    }

    /**
     * Повертає модель Vosk.
     * @return Об'єкт Model або null, якщо модель ще не готова.
     */
    fun getModel(): Model? = voskModel

    /**
     * Додає слухача, який буде викликаний, коли модель Vosk буде готова.
     */
    fun addModelReadyListener(listener: () -> Unit) {
        if (voskModel != null) {
            listener.invoke()
        } else {
            // Додаємо слухача
            voskModelReadyListeners.add(listener)
        }
    }

    private fun notifyModelReady() {
        Log.d(TAG, "notifyModelReady: Notifying ${voskModelReadyListeners.size} listeners.")
        // Викликаємо слухачів
        voskModelReadyListeners.forEach { it.invoke() }
        // Очищаємо список слухачів після сповіщення
        voskModelReadyListeners.clear()
    }

    /**
     * Асинхронно розпаковує та завантажує модель Vosk.
     */
    private fun initVoskModel() {
        LibVosk.setLogLevel(LogLevel.INFO)
        Log.d(TAG, "initVoskModel: Starting Vosk model initialization.")

        // Якщо модель вже була завантажена, пропускаємо розпакування
        if (isModelReady) {
            notifyModelReady()
            return
        }

        StorageService.unpack(context, VOSK_MODEL_NAME, "model",
            { unpackedModel ->
                voskModel = unpackedModel
                notifyModelReady()
                Log.d(TAG, "initVoskModel: Vosk model successfully loaded.")
            },
            { exception ->
                val errorMessage = exception.message ?: "Невідома помилка розпакування моделі."
                Log.e(TAG, "initVoskModel: Error unpacking Vosk model: $errorMessage", exception)
                // Можна додати тут логіку сповіщення про помилку
                // Наприклад, тимчасово використовувати Toast або Log.e
            }
        )
    }

    // Метод, який Activity викликатиме для повторної спроби або первинного запуску
    // У вашій реалізації це просто перезапуск initVoskModel
    fun startModelSetup() {
        initVoskModel()
    }

    // Метод для перевірки стану
    // fun isModelReady(): Boolean = isModelReady


    /**
     * Звільняє ресурси моделі Vosk.
     * Цей метод буде викликаний Hilty при завершенні життєвого циклу (onDestroy).
     */
    fun release() {
        Log.d(TAG, "release: Releasing Vosk model resources.")
        voskModel?.close()
        voskModel = null
    }
}