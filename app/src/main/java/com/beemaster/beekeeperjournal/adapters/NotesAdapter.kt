//  NoteDiffCallback.kt  DiffUtil — це допоміжний клас, який обчислює різницю між двома списками даних (старим і новим)
//  і надає список конкретних оновлень. Замість того, щоб перемальовувати весь список, він каже RecyclerView,
//  які саме елементи були додані, видалені чи змінені. Це значно покращує продуктивність і прибирає блимання.

// Новий вміст для NotesAdapter.kt

package com.beemaster.beekeeperjournal.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter // КЛЮЧОВА ЗМІНА: використовуємо ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.db.entity.NoteEntity // Припускаємо, що використовується NoteEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(
    // Приймаємо колбек для обробки довгого натискання
    private val onLongClick: (NoteEntity) -> Unit
) : ListAdapter<NoteEntity, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    // 1. ViewHolder: зберігає посилання на елементи макета item_note.xml
    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)

        fun bind(note: NoteEntity) {
            val dateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
            dateTextView.text = dateFormat.format(Date(note.createdAt))
            contentTextView.text = note.content

            // Встановлення слухача для довгого натискання
            itemView.setOnLongClickListener {
                onLongClick(note) // Викликаємо колбек
                true
            }
        }
    }

    // 2. Створення нового View-елемента з макета
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false) // Використовуємо макет елемента, який ми обговорювали
        return NoteViewHolder(view)
    }

    // 3. Прив'язка даних до View-елемента
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

// 4. NoteDiffCallback (коректна реалізація, подібна до вашої ідеї)
// Вбудовуємо його в цей же файл, або залишаємо окремим класом, але він повинен наслідувати DiffUtil.ItemCallback
class NoteDiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
    override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
        // Порівнюємо за унікальним ID
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
        // Порівнюємо весь вміст
        return oldItem == newItem
    }
}