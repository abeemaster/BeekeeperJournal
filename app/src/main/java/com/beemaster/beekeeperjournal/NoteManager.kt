// NoteManager.kt  відповідає за всю логіку роботи з даними (читання, запис, оновлення, видалення).

package com.beemaster.beekeeperjournal

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import db.NoteEntity

/**
 * Клас, що керує логікою збереження, оновлення, видалення та завантаження записів.
 * Він дозволяє HiveInfoActivity зосередитись лише на відображенні UI.
 */
class NoteManager(private val context: Context, private val noteRepository: NoteRepository) {

    private val tag = "NoteManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    // ✅ ДОДАЄМО МЕТОД ДЛЯ ЗАВАНТАЖЕННЯ НОТАТОК
    fun loadNotes(entryType: String, hiveNumber: Int): List<NoteEntity> {
        return noteRepository.getNotesByTypeAndHive(entryType, hiveNumber)
    }

    // ✅ ДОДАЄМО МЕТОД ДЛЯ ОТРИМАННЯ НОТАТКИ ЗА ID
    fun getNoteById(noteId: String): NoteEntity? {
        return noteRepository.getNoteByIdBlocking(noteId)
    }

    // ✅ ІСНУЮЧИЙ МЕТОД ДЛЯ ВИДАЛЕННЯ НОТАТКИ
    fun deleteNote(noteId: String, onComplete: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Видалити запис")
            .setMessage("Ви впевнені, що хочете видалити цей запис? Цю дію не можна скасувати.")
            .setPositiveButton("Видалити") { _, _ ->
                scope.launch {
                    try {
                        noteRepository.deleteNoteById(noteId)
                        Log.d(tag, "Запис з ID $noteId успішно видалено.")
                        (context as? HiveInfoActivity)?.runOnUiThread {
                            onComplete()
                            Toast.makeText(context, "Запис видалено.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Помилка при видаленні запису: ${e.message}", e)
                        (context as? HiveInfoActivity)?.runOnUiThread {
                            Toast.makeText(context, "Помилка при видаленні.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }
}