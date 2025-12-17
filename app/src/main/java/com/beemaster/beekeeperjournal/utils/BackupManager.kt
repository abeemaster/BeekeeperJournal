package com.beemaster.beekeeperjournal.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
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
    private val prefsManager: BackupPrefsManager
) {
    private val gson = Gson()

    private companion object {
        private const val TAG = "BackupManager"
        private const val BACKUP_DIR_NAME = "auto_backups"
        private const val MIME_TYPE_JSON = "application/json"
    }

    @Volatile
    private var _dataSource: BackupDataSource? = null

    fun setDataSource(dataSource: BackupDataSource) {
        if (this._dataSource == null) {
            this._dataSource = dataSource
            Log.d(TAG, context.getString(R.string.log_datasource_initialized))

        }
    }

    private val dataSource: BackupDataSource
        get() = _dataSource ?: throw IllegalStateException(context.getString(R.string.error_datasource_not_initialized))

    // --------------------------------------------------------------------
    // МОДИФІКОВАНИЙ МЕТОД: АВТОМАТИЧНИЙ БЕКАП (Внутрішній + SAF Copy)
    // --------------------------------------------------------------------

    /**
     * Створює резервну копію у внутрішній пам'яті (надійно) та копіює її у SAF-каталог (доступно).
     */
    suspend fun createAutomaticBackup() = withContext(Dispatchers.IO) {

        // 1. Перевірка на зміни даних
        if (!dataSource.hasDataChanged()) {
            Log.d(TAG, context.getString(R.string.log_data_not_changed))
            // Ми не викликаємо getAndIncrementNextIndex, тому тут не потрібен decrement.
            return@withContext
        }

        // 2. Отримуємо наступний індекс та ім'я файлу для версіонування
        val nextIndex = prefsManager.getAndIncrementNextIndex()
        val backupFileName = prefsManager.getBackupFileName(nextIndex)

        // 3. Визначення фіксованого внутрішнього шляху
        val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        val internalFile = File(backupDir, backupFileName)

        try {
            // 4. Збір даних та запис у внутрішній файл
            val backupData = getBackupData()
            val json = gson.toJson(backupData)

            internalFile.outputStream().use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            }

            // 5. Валідація внутрішнього файлу
            if (!validateBackupFile(internalFile)) {
                Log.e(TAG, "Внутрішній автобекап не пройшов валідацію. Індекс відкочено.")
                prefsManager.decrementLastBackupIndex()
                // Якщо невалідний, далі не продовжуємо
                return@withContext
            }

            Log.d(TAG, "Внутрішній автобекап успішно створено: ${internalFile.absolutePath}")

            // 6. КРОК КОПІЮВАННЯ: Копіюємо файл у SAF-каталог
            val safDirectoryUri = prefsManager.getBackupDirectoryUri()
            if (safDirectoryUri != null) {
                copyFileToSaf(internalFile, safDirectoryUri)
            } else {
                Log.d(TAG, "SAF каталог для автобекапу не встановлено. Копіювання не відбулося.")
                // У цьому випадку бекап залишається лише у внутрішній пам'яті.
            }

            // 7. ОНОВЛЕННЯ ЧАСУ: Встановлюємо час останнього успішного бекапу
            // Цей рядок виконується, лише якщо внутрішній бекап успішно створено та провалідовано.
            prefsManager.updateLastBackupTime()

        } catch (e: Exception) {
            // Обробка JobCancellationException
            if (e is kotlinx.coroutines.CancellationException) {
                // Це не справжня помилка бекапу, а скасування Job.
                // Ми ігноруємо її, оскільки це нормально при завершенні роботи програми.
                Log.w(TAG, "Автобекап був скасований Job: ${e.message}")
            } else {
                // Це справжня помилка IO або інша проблема
                Log.e(TAG, "Помилка при створенні/копіюванні автобекапу.", e)
                prefsManager.decrementLastBackupIndex()
            }
        }
    }

    /**
     * Копіює внутрішній файл у вибраний користувачем SAF-каталог.
     */
    private fun copyFileToSaf(sourceFile: File, treeUri: Uri) {
        val fileName = sourceFile.name
        try {
            // 1. Отримуємо DocumentFile для каталогу SAF
            val documentTree = DocumentFile.fromTreeUri(context, treeUri)

            // 2. Створення або пошук файлу в SAF-каталозі
            val documentFile = documentTree?.findFile(fileName)
                ?: documentTree?.createFile(MIME_TYPE_JSON, fileName)

            if (documentFile?.uri == null) {
                Log.e(TAG, "Не вдалося створити або знайти файл $fileName в SAF каталозі.")
                return
            }

            // 3. Копіювання вмісту (використовуємо ContentResolver для запису в SAF Uri)
            context.contentResolver.openOutputStream(documentFile.uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.i(TAG, "Автобекап успішно скопійовано до SAF: ${documentFile.uri}")

        } catch (e: Exception) {
            Log.e(TAG, "Помилка при копіюванні файлу $fileName до SAF", e)
        }
    }

    // --------------------------------------------------------------------
    // ІНШІ МЕТОДИ (Залишаються без змін)
    // --------------------------------------------------------------------

    /**
     * Отримує об'єкт BackupData. Використовується для обох типів бекапу.
    Так було
     */
    private suspend fun getBackupData(): BackupData {
        // val incomes = dataSource.getAllIncomesSuspend()

        return BackupData(
            hives = dataSource.getAllHivesSuspend(),
            notes = dataSource.getAllNotesSuspend(),
            expenses = dataSource.getAllExpensesSuspend(),
            incomes = dataSource.getAllIncomesSuspend().map { it.toIncomeEntity() },
            // incomes = incomes.map { it.toIncomeEntity() }, з цим рядком працювало
            years = dataSource.getAllYearsSuspend()
        )
    }

    /**
     * Експортує дані в JSON-файл за вказаним Uri (для ручного експорту/архівування).
     * @param uri Document Uri, який повертається SAF.
     */
    suspend fun exportManualData(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val backupData = getBackupData()
            val json = gson.toJson(backupData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            }

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

    /**
     * Імпортує дані з JSON-файлу за вказаним Uri.
     */
    suspend fun importData(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext

            val backupDataType = object : TypeToken<BackupData>() {}.type
            val backupData: BackupData = gson.fromJson(json, backupDataType)

            val incomesToImport = backupData.incomes.map { it.toIncome() }

            dataSource.importYears(backupData.years)
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
     */
    private suspend fun validateBackupFile(file: File): Boolean {
        return validateBackupFile(Uri.fromFile(file))
    }

    /**
     * Перевіряє щойно створений файл бекапу на валідність (основний метод).
     */
    private suspend fun validateBackupFile(uri: Uri): Boolean {
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
}