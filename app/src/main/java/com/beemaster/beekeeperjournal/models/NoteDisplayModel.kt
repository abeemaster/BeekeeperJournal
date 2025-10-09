// NoteDisplayModel.kt

package com.beemaster.beekeeperjournal.models

/**
 * Модель, спеціально розроблена для відображення нотаток у списках (наприклад, у NotesAdapter).
 * Містить назву вулика, агреговану з HiveEntity, що дозволяє UI коректно відображати дані.
 */
data class NoteDisplayModel(
    val id: Int,
    val text: String,
    val type: String,
    val hiveId: Int,
    val hiveDisplayNumber: String,
    val timestamp: Long,
    val title: String
)