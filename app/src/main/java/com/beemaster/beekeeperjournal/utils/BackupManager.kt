package com.beemaster.beekeeperjournal.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.mappers.toIncome
import com.beemaster.beekeeperjournal.mappers.toIncomeEntity
import com.beemaster.beekeeperjournal.models.BackupData
import com.beemaster.beekeeperjournal.viewmodel.BackupDataSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val prefsManager: BackupPrefsManager // Залишаємо для очищення старих налаштувань
) {
    private val gson = Gson()

    private companion object {
        // --- Константи для АВТОМАТИЧНОГО БЕКАПУ (Приватний шлях) ---
        private const val BACKUP_DIR_NAME = "auto_backups" // Новий каталог у приватній пам'яті
        private const val AUTO_BACKUP_FILENAME = "beekeeper_auto_backup.json"

        // --- Константи для SAF (Лише для ручного експорту) ---
        // private const val MIME_TYPE_JSON = "application/json" // Видалено, оскільки не використовується тут
        private const val TAG = "BackupManager"
    }

    @Volatile
    private var _dataSource: BackupDataSource? = null

    fun setDataSource(dataSource: BackupDataSource) {
        if (this._dataSource == null) {
            this._dataSource = dataSource
            Log.d(TAG, context.getString(R.string.log_datasource_initialized))
            // ✅ ОЧИЩЕННЯ СТАРИХ URI при першій ініціалізації
            // Примусово очищаємо старі, некоректні URI, оскільки автобекап більше їх не використовує.
            prefsManager.clearBackupDirectoryUri()
        }
    }

    private val dataSource: BackupDataSource
        get() = _dataSource ?: throw IllegalStateException(context.getString(R.string.error_datasource_not_initialized))

    // --------------------------------------------------------------------
    // НОВІ МЕТОДИ: АВТОМАТИЧНИЙ БЕКАП (ВНУТРІШНЯ ПАМ'ЯТЬ)
    // --------------------------------------------------------------------

    /**
     * Створює автоматичну резервну копію у **приватній пам'яті програми**
     * (фіксований шлях). Це надійніше і не залежить від SAF.
     */
    suspend fun createAutomaticBackup() = withContext(Dispatchers.IO) {
        // 1. Перевірка на зміни даних
        if (!dataSource.hasDataChanged()) {
            Log.d(TAG, context.getString(R.string.log_data_not_changed))
            return@withContext
        }

        // 2. Визначення фіксованого шляху
        val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        val backupFile = File(backupDir, AUTO_BACKUP_FILENAME)

        // 3. Збір даних
        val backupData = getBackupData()
        val json = gson.toJson(backupData)

        try {
            // 4. Запис даних у файл (стандартний Java/Kotlin File IO)
            backupFile.outputStream().use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            }

            // 5. Валідація
            if (validateBackupFile(backupFile)) {
                Log.d(TAG, "Автобекап успішно створено у: ${backupFile.absolutePath}")
            } else {
                // Якщо файл невалідний, логіка виведення Toast не потрібна, оскільки це автобекап.
                Log.e(TAG, "Автобекап створено, але валідація не вдалася: ${backupFile.absolutePath}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Помилка при створенні автобекапу у внутрішній пам'яті", e)
        }
    }

    /**
     * Отримує об'єкт BackupData. Використовується для обох типів бекапу.
     */
    private suspend fun getBackupData(): BackupData {
        val incomes = dataSource.getAllIncomesSuspend()

        return BackupData(
            hives = dataSource.getAllHivesSuspend(),
            notes = dataSource.getAllNotesSuspend(),
            expenses = dataSource.getAllExpensesSuspend(),
            incomes = incomes.map { it.toIncomeEntity() }
        )
    }

    // --------------------------------------------------------------------
    // МОДИФІКОВАНИЙ МЕТОД: РУЧНИЙ ЕКСПОРТ (SAF)
    // --------------------------------------------------------------------

    /**
     * ✅ ПЕРЕЙМЕНОВАНО: Експортує дані в JSON-файл за вказаним Uri (для ручного експорту/архівування).
     * @param uri Document Uri, який повертається SAF.
     */
    suspend fun exportManualData(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val backupData = getBackupData()
            val json = gson.toJson(backupData)

            // 1. Запис даних у файл через ContentResolver (SAF)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            }

            // 2. КРОК ВАЛІДАЦІЇ: Перевіряємо щойно створений файл
            val isValid = validateBackupFile(uri)

            withContext(Dispatchers.Main) {
                if (isValid) {
                    Toast.makeText(context, context.getString(R.string.toast_export_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_export_error_validation),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, context.getString(R.string.log_error_export), e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.toast_backup_error) + ": ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --------------------------------------------------------------------
    // МОДИФІКОВАНИЙ МЕТОД: ІМПОРТ (ПІДТРИМУЄ І ВНУТРІШНІЙ, І SAF)
    // --------------------------------------------------------------------

    /**
     * Імпортує дані з JSON-файлу за вказаним Uri.
     * Якщо ви хочете імпортувати з автобекапу, передайте його File URI (за допомогою Uri.fromFile(File)).
     */
    suspend fun importData(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            // ... (Ваша оригінальна логіка імпорту залишається незмінною)
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext

            val backupDataType = object : TypeToken<BackupData>() {}.type
            val backupData: BackupData = gson.fromJson(json, backupDataType)

            val incomesToImport = backupData.incomes.map { it.toIncome() }

            dataSource.importHives(backupData.hives)
            dataSource.importNotes(backupData.notes)
            dataSource.importExpenses(backupData.expenses)
            dataSource.importIncomes(incomesToImport)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.toast_restore_success), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, context.getString(R.string.log_error_import), e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.toast_restore_error) + ": ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Перевіряє щойно створений файл бекапу на валідність.
     * Перевантажений метод для підтримки File (внутрішній бекап)
     */
    private suspend fun validateBackupFile(file: File): Boolean {
        return validateBackupFile(Uri.fromFile(file))
    }

    /**
     * Перевіряє щойно створений файл бекапу на валідність (основний метод).
     * @param uri Uri щойно створеного файлу бекапу (може бути File Uri або Content Uri).
     * @return true, якщо файл успішно читається та парситься, false в іншому випадку.
     */
    private suspend fun validateBackupFile(uri: Uri): Boolean {
        // ... (Ваша оригінальна логіка валідації)
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val json = reader.readText()
                        val backupDataType = object : TypeToken<BackupData>() {}.type
                        gson.fromJson<BackupData>(json, backupDataType)
                    }
                }
                Log.d(TAG, "Резервна копія успішно пройшла валідацію.")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Валідація резервної копії не вдалася: ", e)
                false
            }
        }
    }

    // ✅ ВИДАЛЕНО: findOrCreateFile більше не потрібен для автобекапу, оскільки ми використовуємо File IO.
    // Якщо ви плануєте використовувати його для ручного експорту, його треба перенести в Activity/Fragment
    // і використовувати DocumentsContract.createDocument лише там.
}