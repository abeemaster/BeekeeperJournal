//  NoteDiffCallback.kt  DiffUtil — це допоміжний клас, який обчислює різницю між двома списками даних (старим і новим)
//  і надає список конкретних оновлень. Замість того, щоб перемальовувати весь список, він каже RecyclerView,
//  які саме елементи були додані, видалені чи змінені. Це значно покращує продуктивність і прибирає блимання.
// Адаптер для RecyclerView, який відображає список нотаток.
// NotesAdapter.kt

package com.beemaster.beekeeperjournal.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
// import com.beemaster.beekeeperjournal.models.Note // Більше не потрібна у деяких місцях
import com.beemaster.beekeeperjournal.models.NoteDisplayModel // ✅ ВИКОРИСТОВУЄМО ТУТ
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Адаптер для відображення списку об'єктів [NoteDisplayModel] у RecyclerView.
 * Використовує [ListAdapter] та [NoteDiffCallback] для ефективного оновлення списку.
 *
 * @property onLongClick Лямбда-функція, що викликається при довгому натисканні на елемент. Приймає [NoteDisplayModel].
 */

class NotesAdapter(
    // ✅ ВИПРАВЛЕНО: onLongClick тепер приймає NoteDisplayModel
    private val onLongClick: (NoteDisplayModel) -> Unit,
    private val showHiveInfo: Boolean = false
// ✅ ВИПРАВЛЕНО: ListAdapter тепер працює з NoteDisplayModel
) : ListAdapter<NoteDisplayModel, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    /**
     * Внутрішній клас, що представляє елемент списку нотаток (ViewHolder).
     */
    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        private val hiveInfoTextView: TextView = itemView.findViewById(R.id.noteTypeAndHive)

        private val dateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())

        /**
         * Прив'язує об'єкт [NoteDisplayModel] до елементів інтерфейсу.
         */
        // ✅ ВИПРАВЛЕНО: Прив'язка до NoteDisplayModel
        fun bind(note: NoteDisplayModel) {
            // Використовуємо 'timestamp'
            dateTextView.text = dateFormat.format(Date(note.timestamp))

            // Використовуємо 'text'
            contentTextView.text = note.text

            // Керуємо видимістю залежно від прапорця
            if (showHiveInfo) {
                // Використовуємо агреговане поле hiveDisplayNumber (String), яке містить назву вулика
                hiveInfoTextView.text = itemView.context.getString(
                    R.string.hive_notes_format,
                    note.hiveDisplayNumber, // ✅ ВИКОРИСТОВУЄМО ТЕ, ЩО ТРЕБА (Назва вулика)
                    note.title
                )
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
// ✅ ВИПРАВЛЕНО: DiffUtil.ItemCallback тепер працює з NoteDisplayModel
private class NoteDiffCallback : DiffUtil.ItemCallback<NoteDisplayModel>() {

    /**
     * Порівнюємо за унікальним ID.
     */
    // ✅ ВИПРАВЛЕНО: Порівняння NoteDisplayModel
    override fun areItemsTheSame(oldItem: NoteDisplayModel, newItem: NoteDisplayModel): Boolean {
        return oldItem.id == newItem.id
    }

    /**
     * Порівнюємо весь вміст (якщо ID однакові).
     */
    // ✅ ВИПРАВЛЕНО: Порівняння NoteDisplayModel
    override fun areContentsTheSame(oldItem: NoteDisplayModel, newItem: NoteDisplayModel): Boolean {
        return oldItem == newItem
    }
}