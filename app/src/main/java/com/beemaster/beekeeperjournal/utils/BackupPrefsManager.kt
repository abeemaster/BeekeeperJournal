package com.beemaster.beekeeperjournal.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

/**
 * Клас-менеджер для роботи з SharedPreferences, що стосуються функціоналу бекапу.
 * Зберігає та отримує URI каталогу для копіювання автобекапу та індекс версії.
 */
@Singleton
class BackupPrefsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "backup_prefs"
        private const val KEY_BACKUP_DIRECTORY_URI = "backup_directory_uri"
        private const val KEY_LAST_BACKUP_INDEX = "last_backup_index"
        private const val MAX_BACKUP_VERSIONS = 3
        private const val KEY_LAST_DATA_MODIFIED = "last_data_modified_time"
        private const val KEY_LAST_BACKUP_SUCCESS_TIME = "last_backup_success_time"
    }

    // --- Часові мітки для відстеження змін ---

    fun getLastDataModifiedTime(): Long {
        // Повертаємо 0, якщо не встановлено
        return prefs.getLong(KEY_LAST_DATA_MODIFIED, 0L)
    }

    /** Оновлює час при будь-якій зміні даних у БД. */
    fun updateLastDataModifiedTime() {
        prefs.edit {
            putLong(KEY_LAST_DATA_MODIFIED, System.currentTimeMillis())
        }
    }

    fun getLastBackupTime(): Long {
        // Повертаємо 0, якщо бекап ще не створювався
        return prefs.getLong(KEY_LAST_BACKUP_SUCCESS_TIME, 0L)
    }

    /** Оновлює час при успішному створенні бекапу (в BackupManager). */
    fun updateLastBackupTime() {
        prefs.edit {
            putLong(KEY_LAST_BACKUP_SUCCESS_TIME, System.currentTimeMillis())
        }
    }

    // --- URI Каталогу SAF (Для копіювання) ---

    fun getBackupDirectoryUri(): Uri? {
        val uriString = prefs.getString(KEY_BACKUP_DIRECTORY_URI, null)
        return uriString?.toUri()
    }

    fun saveBackupDirectoryUri(uri: Uri) {
        prefs.edit {
            putString(KEY_BACKUP_DIRECTORY_URI, uri.toString())
        }
    }


    // --- Логіка Версіонування ---
    fun getLastBackupIndex(): Int {
        // Починаємо з 0, щоб перший бекап був "1"
        return prefs.getInt(KEY_LAST_BACKUP_INDEX, 0)
    }

    /**
     * Повертає наступний індекс для запису та оновлює його в Preferences.
     */
    fun getAndIncrementNextIndex(): Int {
        val lastIndex = getLastBackupIndex()
        val nextIndex = if (lastIndex < MAX_BACKUP_VERSIONS) lastIndex + 1 else 1

        prefs.edit {
            putInt(KEY_LAST_BACKUP_INDEX, nextIndex)
        }
        Log.d("BackupPrefsManager", "Індекс бекапу оновлено: $lastIndex -> $nextIndex")
        return nextIndex
    }

    /**
     * Зменшує індекс після невдалого бекапу (логіка відкату).
     */
    fun decrementLastBackupIndex() {
        val currentIndex = getLastBackupIndex()
        val previousIndex = if (currentIndex > 1) currentIndex - 1 else MAX_BACKUP_VERSIONS

        if (currentIndex != 0) {
            prefs.edit {
                putInt(KEY_LAST_BACKUP_INDEX, previousIndex)
            }
            Log.d("BackupPrefsManager", "Індекс бекапу відкочено: $currentIndex -> $previousIndex")
        }
    }

    /**
     * Повертає ім'я файлу для поточної версії.
     */
    fun getBackupFileName(index: Int): String {
        return "beekeeper_auto_backup_$index.json"
    }
}