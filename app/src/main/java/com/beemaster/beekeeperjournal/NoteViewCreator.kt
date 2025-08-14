// NoteVievCreator має два методи, що відповідають за створення елементів інтерфейсу:
//       createNoteItem()
//       createActionButton()

package com.beemaster.beekeeperjournal

import android.widget.Toast
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible

/**
 * Клас, що відповідає за створення та налаштування візуальних елементів для нотаток.
 * Це дозволяє відокремити логіку UI від основної активності.
 */
class NoteViewCreator(private val context: Context, private val onEditNote: (String) -> Unit, private val onDeleteNote: (String) -> Unit) {

    private var currentlyVisibleActionsLayout: LinearLayout? = null
    private var currentlySelectedNoteItem: LinearLayout? = null

    /**
     * Створює повний елемент нотатки з датою, текстом та кнопками дій.
     */
    fun createNoteItem(note: Note): View {
        val noteItem = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.spacing_medium)
            }
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.note_item_background)
            setPadding(context.resources.getDimensionPixelSize(R.dimen.content_padding),
                context.resources.getDimensionPixelSize(R.dimen.content_padding),
                context.resources.getDimensionPixelSize(R.dimen.content_padding),
                context.resources.getDimensionPixelSize(R.dimen.content_padding))
            elevation = context.resources.getDimension(R.dimen.button_elevation)
            tag = note.id
        }

        val dateTextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.spacing_small)
            }
            this.text = note.date
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.note_date_text))
        }

        val noteTextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            this.text = note.text
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.note_text_color))
        }

        val actionsLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = context.resources.getDimensionPixelSize(R.dimen.spacing_medium)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
        }

        val editButton = createActionButton("Редагувати", R.color.edit_button_color) {
            onEditNote(note.id)
        }
        val deleteButton = createActionButton("Видалити", R.color.delete_button_color) {
            onDeleteNote(note.id)
        }

        actionsLayout.addView(editButton)
        actionsLayout.addView(deleteButton)
        noteItem.addView(dateTextView)
        noteItem.addView(noteTextView)
        noteItem.addView(actionsLayout)

        noteItem.setOnLongClickListener {
            toggleActionsLayout(noteItem, actionsLayout)
            true
        }

        noteItem.setOnClickListener {
            if (currentlyVisibleActionsLayout == actionsLayout && actionsLayout.isVisible) {
                hideActionsAndResetBackground()
                Toast.makeText(context, "Дії приховано.", Toast.LENGTH_SHORT).show()
            }
        }

        return noteItem
    }

    /**
     * Створює кнопку дії (Редагувати, Видалити).
     */
    private fun createActionButton(text: String, colorResId: Int, onClickListener: (View) -> Unit): Button {
        val params = LinearLayout.LayoutParams(
            context.resources.getDimensionPixelSize(R.dimen.action_button_min_width),
            context.resources.getDimensionPixelSize(R.dimen.action_button_min_height)
        ).apply {
            marginEnd = context.resources.getDimensionPixelSize(R.dimen.spacing_small)
        }

        return Button(context).apply {
            this.text = text
            textSize = context.resources.getDimension(R.dimen.action_button_text_size) / context.resources.displayMetrics.density
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            backgroundTintList = ContextCompat.getColorStateList(context, colorResId)
            setPadding(context.resources.getDimensionPixelSize(R.dimen.action_button_padding_horizontal),
                context.resources.getDimensionPixelSize(R.dimen.action_button_padding_vertical),
                context.resources.getDimensionPixelSize(R.dimen.action_button_padding_horizontal),
                context.resources.getDimensionPixelSize(R.dimen.action_button_padding_vertical))
            val shapeDrawable = GradientDrawable().apply {
                cornerRadius = context.resources.getDimension(R.dimen.action_button_corner_radius)
                setColor(ContextCompat.getColor(context, colorResId))
            }
            background = shapeDrawable
            elevation = context.resources.getDimension(R.dimen.button_elevation)
            stateListAnimator = null
            this.layoutParams = params
            setOnClickListener(onClickListener)
        }
    }

    /**
     * Показує/приховує кнопки дій для елемента нотатки.
     */
    private fun toggleActionsLayout(noteItem: LinearLayout, actionsLayout: LinearLayout) {
        if (currentlyVisibleActionsLayout != null && currentlyVisibleActionsLayout != actionsLayout) {
            hideActionsAndResetBackground()
        }

        if (actionsLayout.isVisible) {
            actionsLayout.isVisible = false
            noteItem.background = ContextCompat.getDrawable(context, R.drawable.note_item_background)
            currentlyVisibleActionsLayout = null
            currentlySelectedNoteItem = null
        } else {
            actionsLayout.visibility = View.VISIBLE
            noteItem.background = ContextCompat.getDrawable(context, R.drawable.note_item_background_selected)
            currentlyVisibleActionsLayout = actionsLayout
            currentlySelectedNoteItem = noteItem
        }
    }

    /**
     * Приховує кнопки дій та скидає фон для всіх нотаток.
     */
    fun hideActionsAndResetBackground() {
        currentlyVisibleActionsLayout?.visibility = View.GONE
        currentlySelectedNoteItem?.background = ContextCompat.getDrawable(context, R.drawable.note_item_background)
        currentlyVisibleActionsLayout = null
        currentlySelectedNoteItem = null
    }
}