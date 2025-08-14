// NoteManager.kt  відповідає за всю логіку роботи з даними (читання, запис, оновлення, видалення).

package com.beemaster.beekeeperjournal

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import android.util.Log


/**
 * Клас, що керує логікою збереження, оновлення, видалення та завантаження записів.
 * Він дозволяє HiveInfoActivity зосередитись лише на відображенні UI.
 */
class NoteManager(private val context: Context, private val noteRepository: NoteRepository) {

    private val tag = "NoteManager"

    /**
     * Оновлює існуючий запис.
     */
    fun updateNote(noteId: String, updatedNoteText: String, onUpdateComplete: () -> Unit) {
        val allNotes = noteRepository.readAllNotesFromJson()
        val noteIndex = allNotes.indexOfFirst { it.id == noteId }
        if (noteIndex != -1) {
            val updatedNote = allNotes[noteIndex].copy(
                text = updatedNoteText,
                timestamp = System.currentTimeMillis()
            )
            allNotes[noteIndex] = updatedNote
            noteRepository.writeAllNotesToJson(allNotes)
            Toast.makeText(context, "Запис оновлено!", Toast.LENGTH_SHORT).show()
            onUpdateComplete()
        } else {
            Toast.makeText(context, "Помилка: Запис для оновлення не знайдено.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Видаляє запис після підтвердження.
     */
    fun deleteNote(noteId: String, onDeleteComplete: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Видалити запис")
            .setMessage("Ви впевнені, що хочете видалити цей запис?")
            .setPositiveButton("Видалити") { dialog, _ ->
                val allNotes = noteRepository.readAllNotesFromJson()
                val initialSize = allNotes.size
                allNotes.removeIf { it.id == noteId }
                if (allNotes.size < initialSize) {
                    noteRepository.writeAllNotesToJson(allNotes)
                    Toast.makeText(context, "Запис видалено!", Toast.LENGTH_SHORT).show()
                    onDeleteComplete()
                } else {
                    Toast.makeText(context, "Помилка: Запис не знайдено.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Скасувати") { dialog, _ ->
                Toast.makeText(context, "Видалення скасовано.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Завантажує та фільтрує записи для відображення.
     */
    fun loadNotes(currentEntryType: String, currentHiveNumber: Int): List<Note> {
        val allNotes = noteRepository.readAllNotesFromJson()
        Log.d(tag, "NoteManager: Завантажено ${allNotes.size} нотаток з репозиторію.")

        val filteredNotes = allNotes.filter { note ->
            note.type == currentEntryType && note.hiveNumber == currentHiveNumber
        }.sortedByDescending { it.timestamp }

        Log.d(tag, "NoteManager: Знайдено ${filteredNotes.size} нотаток для hiveNumber: $currentHiveNumber і type: $currentEntryType")
        return filteredNotes
    }
}