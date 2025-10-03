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
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Адаптер для відображення списку об'єктів [NoteEntity] у RecyclerView.
 * Використовує [ListAdapter] та [NoteDiffCallback] для ефективного оновлення списку.
 *
 * @property onLongClick Лямбда-функція, що викликається при довгому натисканні на елемент.
 */
class NotesAdapter(
    private val onLongClick: (NoteEntity) -> Unit,
    // ✅ ДОДАНО: Прапорець для керування відображенням інформації про вулик (для гнучкості)
    private val showHiveInfo: Boolean = false
) : ListAdapter<NoteEntity, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    /**
     * Внутрішній клас, що представляє елемент списку нотаток (ViewHolder).
     */
    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        private val hiveInfoTextView: TextView = itemView.findViewById(R.id.noteTypeAndHive) // Зроблено приватним

        // Створюємо SimpleDateFormat ОДИН РАЗ
        private val dateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())

        /**
         * Прив'язує об'єкт [NoteEntity] до елементів інтерфейсу.
         *
         * @param note Об'єкт нотатки, який потрібно відобразити.
         */
        fun bind(note: NoteEntity) {
            dateTextView.text = dateFormat.format(Date(note.createdAt))
            contentTextView.text = note.content

            // Керуємо видимістю залежно від прапорця
            hiveInfoTextView.visibility = if (showHiveInfo) View.VISIBLE else View.GONE

            itemView.setOnLongClickListener {
                onLongClick(note)
                true // Повертаємо true, що подія оброблена
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
private class NoteDiffCallback : DiffUtil.ItemCallback<NoteEntity>() { // Зроблено приватним

    /**
     * Порівнюємо за унікальним ID.
     */
    override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
        return oldItem.id == newItem.id
    }

    /**
     * Порівнюємо весь вміст (якщо ID однакові).
     */
    override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
        return oldItem == newItem
    }
}