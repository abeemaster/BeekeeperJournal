//  NoteDiffCallback.kt  DiffUtil — це допоміжний клас, який обчислює різницю між двома списками даних (старим і новим)
//  і надає список конкретних оновлень. Замість того, щоб перемальовувати весь список, він каже RecyclerView,
//  які саме елементи були додані, видалені чи змінені. Це значно покращує продуктивність і прибирає блимання.

package com.beemaster.beekeeperjournal

import androidx.recyclerview.widget.DiffUtil

class NoteDiffCallback(
    private val oldList: List<NoteSearchResult>,
    private val newList: List<NoteSearchResult>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Порівнюємо, чи це один і той самий об'єкт (наприклад, за унікальним ID).
        return oldList[oldItemPosition].note.id == newList[newItemPosition].note.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Порівнюємо вміст елементів, щоб визначити, чи вони були змінені.
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}