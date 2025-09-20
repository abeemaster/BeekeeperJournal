// BackupManager - файл що відповідає за резервне копіювання та відновлення даних.

package com.beemaster.beekeeperjournal.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.beemaster.beekeeperjournal.models.BackupData
import com.beemaster.beekeeperjournal.viewmodel.MainActivityViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupManager(
    private val context: Context,
    private val viewModel: MainActivityViewModel
) {
    private val gson = Gson()

    suspend fun exportData(uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val hives = viewModel.getAllHivesSuspend()
                val notes = viewModel.getAllNotesSuspend()
                val expenses = viewModel.getAllExpensesSuspend()
                val incomes = viewModel.getAllIncomesSuspend()
                val backupData = BackupData(hives, notes, expenses, incomes)
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

                viewModel.importHives(backupData.hives)
                viewModel.importNotes(backupData.notes)
                viewModel.importExpenses(backupData.expenses)
                viewModel.importIncomes(backupData.incomes)

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
