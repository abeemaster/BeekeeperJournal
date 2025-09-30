// SearchResultsAdapter.kt Цей файл відповідає за відображення списку результатів пошуку.

package com.beemaster.beekeeperjournal.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.db.getFormattedDate // ✅ ВАЖЛИВИЙ ІМПОРТ: для використання функції форматування дати

/**
 * Адаптер для відображення результатів пошуку нотаток у RecyclerView.
 * Використовує NoteSearchResult як модель даних, що включає Note та назву вулика.
 * * @param searchResults Список результатів пошуку, що підлягає відображенню.
 * @param onItemLongClick Обробник довгого натискання на елемент (зазвичай для редагування/видалення).
 */
class SearchResultsAdapter(
    private val searchResults: MutableList<NoteSearchResult>,
    private val onItemLongClick: (Note) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.SearchResultViewHolder>() {

    /**
     * ViewHolder для елемента результату пошуку.
     * Містить посилання на всі View-елементи макета note_item_search_result.
     */
    class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // TextView для відображення дати нотатки
        val noteDate: TextView = itemView.findViewById(R.id.noteDate)
        // TextView для відображення вмісту (тексту) нотатки
        val noteText: TextView = itemView.findViewById(R.id.noteText)
        // TextView для відображення типу запису та назви вулика
        val noteTypeAndHive: TextView = itemView.findViewById(R.id.noteTypeAndHive)
    }

    /**
     * Створює та повертає новий екземпляр SearchResultViewHolder.
     * Надуває макет елемента списку з XML.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item_search_result, parent, false)
        return SearchResultViewHolder(view)
    }

    /**
     * Прив'язує дані до View-елементів у SearchResultViewHolder.
     * Цей метод викликається для кожного елемента списку.
     */
    @SuppressLint("StringFormatMatches")
    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val result = searchResults[position]
        val note = result.note
        val hiveNumber = result.hiveNumber
        val context = holder.itemView.context

        // ✅ ПОКРАЩЕННЯ: Використовуємо функцію-розширення для форматування дати.
        // Це забезпечує єдиний формат по всьому додатку.
        holder.noteDate.text = note.getFormattedDate()

        holder.noteText.text = note.text

        // Визначаємо локалізовану назву типу запису
        val displayText = when (note.type) {
            "general" -> context.getString(R.string.general_records_type_name) // Наприклад, "Загальні записи"

            // Якщо є номер вулика, використовуємо новий шаблон "Вулик №%s"
            else -> context.getString(R.string.hive_display_number, hiveNumber)
        }

        holder.noteTypeAndHive.text = displayText // Встановлюємо спрощений підпис

        // Форматуємо рядок "Тип: Назва вулика"
        holder.noteTypeAndHive.text = context.getString(
            R.string.hive_display_number,
            displayText,
            hiveNumber
        )

        // Обробник довгого натискання для взаємодії з нотаткою (наприклад, контекстне меню)
        holder.itemView.setOnLongClickListener {
            onItemLongClick(note)
            true // Повертаємо true, щоб вказати, що подія оброблена
        }
    }

    /**
     * Повертає загальну кількість елементів у списку.
     */
    override fun getItemCount(): Int = searchResults.size

    /**
     * Оновлює дані адаптера, використовуючи DiffUtil для ефективної анімації та оновлення UI.
     * Це значно краще, ніж просто викликати notifyDataSetChanged().
     *
     * @param newResults Новий список результатів пошуку.
     */
    fun updateData(newResults: List<NoteSearchResult>) {
        val diffCallback = SearchResultsDiffCallback(searchResults, newResults)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Очищаємо старий список і додаємо новий
        searchResults.clear()
        searchResults.addAll(newResults)

        // Повідомляємо адаптер про зміни, використовуючи результат DiffUtil
        diffResult.dispatchUpdatesTo(this)
    }
}

/**
 * Callback для DiffUtil, використовується для обчислення різниці між старим і новим списками.
 */
class SearchResultsDiffCallback(
    private val oldList: List<NoteSearchResult>,
    private val newList: List<NoteSearchResult>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    /**
     * Перевіряє, чи представляють два елементи один і той самий об'єкт.
     * Порівнюємо за унікальним ID нотатки.
     */
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].note.id == newList[newItemPosition].note.id
    }

    /**
     * Перевіряє, чи змінився вміст двох елементів, якщо вони є одним і тим самим об'єктом.
     * Порівнюємо весь вміст.
     */
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Порівнюємо вміст усього об'єкта NoteSearchResult
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

/**
 * Клас даних для одного результату пошуку.
 * Необхідний для передачі в адаптер як самої нотатки, так і пов'язаної з нею назви вулика.
 *
 * @param note Об'єкт нотатки (модель UI).
 * @param hiveName Назва вулика, до якого відноситься нотатка.
 */
data class NoteSearchResult(
    val note: Note,
    val hiveNumber: String // ✅ ЗМІНЕНО З hiveName НА hiveNumber
)