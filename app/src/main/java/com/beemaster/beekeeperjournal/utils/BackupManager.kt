package com.beemaster.beekeeperjournal.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import com.beemaster.beekeeperjournal.mappers.toIncome
import com.beemaster.beekeeperjournal.mappers.toIncomeEntity
import com.beemaster.beekeeperjournal.models.BackupData
import com.beemaster.beekeeperjournal.viewmodel.BackupDataSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext // Використовуємо Context рівня програми
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Клас, відповідальний за логіку резервного копіювання (експорту)
 * та відновлення (імпорту) даних програми.
 *
 * Використовує Setter Injection для BackupDataSource, щоб обійти обмеження Hilt.
 */
@Singleton // ✅ Зберігаємо Singleton, щоб уникнути конфліктів у SingletonC
class BackupManager @Inject constructor(
    // 1. Використовуємо ApplicationContext, оскільки це Singleton
    @ApplicationContext
    private val context: Context,
    // 2. BackupDataSource видалено з конструктора
    private val prefsManager: BackupPrefsManager
) {
    private val gson = Gson()
    private val BACKUP_FILENAME = "beekeeper_auto_backup.json"

    // 🚀 НОВА ВЛАСТИВІСТЬ: Зберігаємо BackupDataSource, переданий через ініціалізатор
    @Volatile // Забезпечуємо потокобезпечність
    private var _dataSource: BackupDataSource? = null

    /**
     * 🚀 НОВИЙ ІНІЦІАЛІЗАТОР: Викликається з Activity/Fragment, де ViewModel доступний.
     * Це обхідний шлях для Hilt.
     */
    fun setDataSource(dataSource: BackupDataSource) {
        if (this._dataSource == null) {
            this._dataSource = dataSource
            Log.d("BackupManager", "BackupDataSource успішно ініціалізовано через setDataSource().")
        }
    }

    // Допоміжна властивість для гарантованого доступу до dataSource
    private val dataSource: BackupDataSource
        get() = _dataSource ?: throw IllegalStateException("BackupDataSource не ініціалізовано. Викличте setDataSource() з Activity.")

    /**
     * Експортує всі дані програми в JSON-файл за вказаним Uri (для ручного бекапу).
     */
    suspend fun exportData(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            // ✅ Використовуємо dataSource
            val incomes = dataSource.getAllIncomesSuspend()

            val backupData = BackupData(
                hives = dataSource.getAllHivesSuspend(),
                notes = dataSource.getAllNotesSuspend(),
                expenses = dataSource.getAllExpensesSuspend(),
                incomes = incomes.map { it.toIncomeEntity() }
            )

            val json = gson.toJson(backupData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Резервну копію створено успішно!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Помилка експорту даних", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Помилка при створенні резервної копії: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Імпортує дані з JSON-файлу за вказаним Uri (для ручного відновлення).
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

            // 🚀 Викликаємо через dataSource
            dataSource.importHives(backupData.hives)
            dataSource.importNotes(backupData.notes)
            dataSource.importExpenses(backupData.expenses)
            dataSource.importIncomes(incomesToImport)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Дані відновлено успішно!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Помилка імпорту даних", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Помилка при відновленні даних: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    /**
     * Створює автоматичну резервну копію у вибраному каталозі.
     */
    suspend fun createAutomaticBackup() = withContext(Dispatchers.IO) {
        val directoryUri = prefsManager.getBackupDirectoryUri()
        if (directoryUri == null) {
            Log.d("BackupManager", "Каталог для автоматичного бекапу не встановлено.")
            return@withContext
        }

        try {
            val incomes = dataSource.getAllIncomesSuspend()

            val backupData = BackupData(
                hives = dataSource.getAllHivesSuspend(),
                notes = dataSource.getAllNotesSuspend(),
                expenses = dataSource.getAllExpensesSuspend(),
                incomes = incomes.map { it.toIncomeEntity() }
            )
            val json = gson.toJson(backupData)

            val fileUri = findOrCreateFile(directoryUri, "application/json", BACKUP_FILENAME)

            if (fileUri == null) {
                Log.e("BackupManager", "Не вдалося створити або знайти файл бекапу.")
                return@withContext
            }

            context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
            }

            Log.i("BackupManager", "Автоматичний бекап успішно створено: $BACKUP_FILENAME")

        } catch (e: Exception) {
            Log.e("BackupManager", "Помилка при створенні автоматичного бекапу", e)
        }
    }

    /**
     * Шукає існуючий файл або створює новий.
     */
    private fun findOrCreateFile(
        treeUri: Uri,
        mimeType: String,
        displayName: String
    ): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )

        context.contentResolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            "${DocumentsContract.Document.COLUMN_DISPLAY_NAME} = ?",
            arrayOf(displayName),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val docId = cursor.getString(0)
                return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            }
        }

        return DocumentsContract.createDocument(
            context.contentResolver,
            treeUri,
            mimeType,
            displayName
        )
    }
}