package com.beemaster.beekeeperjournal.utils

import android.content.Context
import androidx.core.content.edit
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Клас-менеджер для роботи з SharedPreferences, що стосуються функціоналу бекапу.
 * Зберігає та отримує індекс поточної версії бекапу.
 */
@Singleton
class BackupPrefsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    // Використовуємо приватні налаштування для бекапу
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "backup_prefs"
        // ВИДАЛЕНО: private const val KEY_BACKUP_DIRECTORY_URI = "backup_directory_uri"
        private const val KEY_BACKUP_DIRECTORY_URI_OLD = "backup_directory_uri" // Залишаємо для очищення
        private const val KEY_LAST_BACKUP_INDEX = "last_backup_index"
        // Визначаємо кількість версій
        private const val MAX_BACKUP_VERSIONS = 3
    }

    // --- ЛОГІКА ОЧИЩЕННЯ СТАРИХ НАЛАШТУВАНЬ (Викликається BackupManager) ---
    /**
     * Очищує збережений старий URI каталогу (якщо він існував), щоб усунути проблеми.
     * Після переходу на внутрішній автобекап цей метод більше не використовується для функціоналу,
     * але потрібен для міграції/очищення.
     */
    fun clearBackupDirectoryUri() {
        prefs.edit {
            remove(KEY_BACKUP_DIRECTORY_URI_OLD)
            Log.d("BackupPrefsManager", "Старий URI каталогу бекапу очищено.")
        }
    }

    // --- Логіка Версіонування ---
    fun getLastBackupIndex(): Int {
        // Починаємо з 0, щоб перший бекап був "1"
        return prefs.getInt(KEY_LAST_BACKUP_INDEX, 0)
    }

    /**
     * Повертає наступний індекс для запису та оновлює його в Preferences.
     * Реалізує круговий буфер (1, 2, 3, 1, 2, 3...).
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
     * Зменшує індекс після невдалого бекапу (логіка відкату), щоб спроба була повторена на тому ж індексі.
     */
    fun decrementLastBackupIndex() {
        val currentIndex = getLastBackupIndex()
        val previousIndex = if (currentIndex > 1) currentIndex - 1 else MAX_BACKUP_VERSIONS

        // Відкат індексу лише якщо він був оновлений до невдалої спроби
        if (currentIndex != 0) {
            prefs.edit {
                putInt(KEY_LAST_BACKUP_INDEX, previousIndex)
            }
            Log.d("BackupPrefsManager", "Індекс бекапу відкочено: $currentIndex -> $previousIndex")
        }
    }

    /**
     * Повертає ім'я файлу для поточної версії.
     * @param index Індекс версії (1, 2 або 3).
     */
    fun getBackupFileName(index: Int): String {
        return "beekeeper_auto_backup_$index.json"
    }
}