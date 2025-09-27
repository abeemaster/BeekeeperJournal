// SearchResultsAdapter.kt Цей файл відповідає за відображення списку результатів пошуку.

package com.beemaster.beekeeperjournal.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SearchResultsAdapter(
    private val searchResults: MutableList<NoteSearchResult>,
    private val onItemLongClick: (Note) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.SearchResultViewHolder>() {

    class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteDate: TextView = itemView.findViewById(R.id.noteDate)
        val noteText: TextView = itemView.findViewById(R.id.noteText)
        val noteTypeAndHive: TextView = itemView.findViewById(R.id.noteTypeAndHive)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item_search_result, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val result = searchResults[position]
        val note = result.note
        val hiveName = result.hiveName

        val dateFormat = SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(note.timestamp))

        holder.noteDate.text = formattedDate
        holder.noteText.text = note.text

        val typeText = when (note.type) {
            "general" -> "Загальні записи"
            "hive" -> "Інформація"
            "queen" -> "Матка"
            "notes" -> "Примітки"
            else -> note.type
        }
        holder.noteTypeAndHive.text = holder.itemView.context.getString(
            R.string.note_type_and_hive_name,
            typeText,
            hiveName
        )

        holder.itemView.setOnLongClickListener {
            onItemLongClick(note)
            true // Повертаємо true, щоб вказати, що подія оброблена
        }
    }

    override fun getItemCount(): Int = searchResults.size

    fun updateData(newResults: List<NoteSearchResult>) {
        val diffCallback = SearchResultsDiffCallback(searchResults, newResults)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        searchResults.clear()
        searchResults.addAll(newResults)

        diffResult.dispatchUpdatesTo(this)
    }
}

class SearchResultsDiffCallback(
    private val oldList: List<NoteSearchResult>,
    private val newList: List<NoteSearchResult>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Порівнюємо за унікальним ID нотатки
        return oldList[oldItemPosition].note.id == newList[newItemPosition].note.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Порівнюємо вміст усього об'єкта NoteSearchResult
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

// Клас даних для результату пошуку, що включає назву вулика
data class NoteSearchResult(
    val note: Note,
    val hiveName: String
)
