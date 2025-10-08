// BackupManager.kt

package com.beemaster.beekeeperjournal.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.beemaster.beekeeperjournal.mappers.toIncome
import com.beemaster.beekeeperjournal.mappers.toIncomeEntity
import com.beemaster.beekeeperjournal.models.BackupData
import com.beemaster.beekeeperjournal.viewmodel.MainActivityViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Клас, відповідальний за логіку резервного копіювання (експорту)
 * та відновлення (імпорту) даних програми.
 *
 * Використовує Gson для серіалізації/десеріалізації об'єкта [BackupData] у форматі JSON.
 * Робота з файлами здійснюється через [ContentResolver] та [Uri].
 *
 * @param context Контекст програми, необхідний для роботи з ContentResolver.
 * @param viewModel Посилання на ViewModel для доступу до методів імпорту/експорту даних.
 */
class BackupManager(
    private val context: Context,
    private val viewModel: MainActivityViewModel
) {
    private val gson = Gson()

    /**
     * Експортує всі дані з бази даних у JSON-файл за вказаним URI.
     * Операція виконується в потоці IO для запобігання блокуванню UI.
     *
     * @param uri URI, наданий системою для запису, куди буде збережено файл резервної копії.
     */
    suspend fun exportData(uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                // Отримуємо всі необхідні дані
                val hives = viewModel.getAllHivesSuspend()       // List<HiveEntity>
                val notes = viewModel.getAllNotesSuspend()       // List<Note>
                val expenses = viewModel.getAllExpensesSuspend() // List<ExpenseEntity>
                val incomes = viewModel.getAllIncomesSuspend()   // List<Income>

                // Конвертуємо Income у IncomeEntity для експорту (згідно зі структурою BackupData)
                val incomesToExport = incomes.map { it.toIncomeEntity() }

                val backupData = BackupData(hives, notes, expenses, incomesToExport)
                val json = gson.toJson(backupData)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Резервна копія створена успішно!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("BackupManager", "Помилка експорту даних", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Помилка при створенні резервної копії: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Імпортує дані з JSON-файлу за вказаним URI, десеріалізує їх
     * та передає у ViewModel для послідовного відновлення бази даних.
     * Операція виконується в потоці IO.
     *
     * @param uri URI файлу резервної копії, наданий системою.
     */
    suspend fun importData(uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        reader.readText()
                    }
                } ?: return@withContext

                val backupDataType = object : TypeToken<BackupData>() {}.type
                val backupData: BackupData = gson.fromJson(json, backupDataType)

                // Конвертуємо IncomeEntity у Income для імпорту у ViewModel
                val incomesToImport = backupData.incomes.map { it.toIncome() }

                // 🚀 ЗАБЕЗПЕЧЕННЯ ПОРЯДКУ: Імпорт Вуликів має бути ПЕРШИМ,
                // щоб зовнішні ключі нотаток були задоволені.
                viewModel.importHives(backupData.hives)
                viewModel.importNotes(backupData.notes)
                viewModel.importExpenses(backupData.expenses)
                viewModel.importIncomes(incomesToImport)

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
    }
}