// SearchResultsAdapter.kt
// Адаптер для відображення списку результатів пошуку, використовує ListAdapter для ефективного оновлення.

package com.beemaster.beekeeperjournal.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Адаптер для відображення результатів пошуку нотаток у RecyclerView.
 * Використовує NoteSearchResult як модель даних.
 *
 * @param onItemLongClick Обробник довгого натискання на елемент (для редагування/видалення).
 */
class SearchResultsAdapter(
    private val onItemLongClick: (Note) -> Unit
) : ListAdapter<NoteSearchResult, SearchResultsAdapter.SearchResultViewHolder>(SearchResultsDiffCallback()) {

    /**
     * ViewHolder для елемента результату пошуку.
     */
    inner class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // TextView для відображення дати нотатки
        val noteDate: TextView = itemView.findViewById(R.id.dateTextView)
        // TextView для відображення вмісту (тексту) нотатки
        val noteText: TextView = itemView.findViewById(R.id.contentTextView)
        // TextView для відображення типу запису та назви вулика
        val noteTypeAndHive: TextView = itemView.findViewById(R.id.noteTypeAndHive)
        // Форматування дати створюється один раз.
        private val dateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())

        /**
         * Прив'язує дані до View-елементів.
         */
        fun bind(result: NoteSearchResult) {
            val note = result.note
            val hiveNumber = result.hiveNumber
            val context = itemView.context

            noteDate.text = dateFormat.format(Date(note.timestamp))
            noteText.text = note.text

            val displayText = when (note.type) {
                "general" -> context.getString(R.string.general_records_type_name)

                else -> context.getString(R.string.hive_display_number, hiveNumber)
            }

            noteTypeAndHive.text = displayText

            // Обробник довгого натискання
            itemView.setOnLongClickListener {
                onItemLongClick(note)
                true // Повертаємо true, щоб вказати, що подія оброблена
            }
        }
    }

    /**
     * Створює та повертає новий екземпляр SearchResultViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_list_item, parent, false)
        return SearchResultViewHolder(view)
    }

    /**
     * Прив'язує дані до ViewHolder.
     */
    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        // ListAdapter надає елемент через getItem(position)
        holder.bind(getItem(position))
    }
}

/**
 * Об'єкт [DiffUtil.ItemCallback] для обчислення різниці між списками [NoteSearchResult].
 */
private class SearchResultsDiffCallback : DiffUtil.ItemCallback<NoteSearchResult>() { // ✅ Приватний клас

    override fun areItemsTheSame(oldItem: NoteSearchResult, newItem: NoteSearchResult): Boolean {
        return oldItem.note.id == newItem.note.id
    }

    override fun areContentsTheSame(oldItem: NoteSearchResult, newItem: NoteSearchResult): Boolean {
        return oldItem == newItem
    }
}

/**
 * Клас даних для одного результату пошуку.
 */
data class NoteSearchResult(
    val note: Note,
    val hiveNumber: String
)