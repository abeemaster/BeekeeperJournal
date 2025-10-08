//  NoteDiffCallback.kt  DiffUtil — це допоміжний клас, який обчислює різницю між двома списками даних (старим і новим)
//  і надає список конкретних оновлень. Замість того, щоб перемальовувати весь список, він каже RecyclerView,
//  які саме елементи були додані, видалені чи змінені. Це значно покращує продуктивність і прибирає блимання.
// NotesAdapter.kt
// Адаптер для RecyclerView, який відображає список нотаток.
package com.beemaster.beekeeperjournal.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
// import com.beemaster.beekeeperjournal.db.entity.NoteEntity // Більше не потрібен
import com.beemaster.beekeeperjournal.models.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Адаптер для відображення списку об'єктів [Note] у RecyclerView.
 * Використовує [ListAdapter] та [NoteDiffCallback] для ефективного оновлення списку.
 *
 * @property onLongClick Лямбда-функція, що викликається при довгому натисканні на елемент.
 */
// ✅ ВИПРАВЛЕНО: Адаптер вже коректно використовує ListAdapter<Note, ...>
class NotesAdapter(
    private val onLongClick: (Note) -> Unit,
    private val showHiveInfo: Boolean = false
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    /**
     * Внутрішній клас, що представляє елемент списку нотаток (ViewHolder).
     */
    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        private val hiveInfoTextView: TextView = itemView.findViewById(R.id.noteTypeAndHive)

        private val dateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())

        /**
         * Прив'язує об'єкт [Note] до елементів інтерфейсу.
         */
        fun bind(note: Note) {
            // ✅ ВИПРАВЛЕНО: Використовуємо 'timestamp' замість 'createdAt'
            dateTextView.text = dateFormat.format(Date(note.timestamp))

            // ✅ ВИПРАВЛЕНО: Використовуємо 'text' замість 'content'
            contentTextView.text = note.text

            // Керуємо видимістю залежно від прапорця
            if (showHiveInfo) {
                // ПРИКЛАД: Відображаємо номер вулика та тип запису, якщо це потрібно.
                // Припускаємо, що R.string.hive_note_info_format існує.
                hiveInfoTextView.text = itemView.context.getString(R.string.hive_notes_format, note.hiveNumber, note.title)
            }
            hiveInfoTextView.visibility = if (showHiveInfo) View.VISIBLE else View.GONE

            itemView.setOnLongClickListener {
                onLongClick(note)
                true
            }
        }
    }

    /**
     * Створює новий ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_list_item, parent, false)
        return NoteViewHolder(view)
    }

    /**
     * Прив'язує дані до ViewHolder.
     */
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

/**
 * Допоміжний клас для обчислення різниці між списками нотаток.
 */
// ✅ ВИПРАВЛЕНО: Успадковуємося від DiffUtil.ItemCallback<Note>
private class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {

    /**
     * Порівнюємо за унікальним ID.
     */
    override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem.id == newItem.id
    }

    /**
     * Порівнюємо весь вміст (якщо ID однакові).
     */
    override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem == newItem
    }
}