// BackupData Цей клас простий контейнер, який тримає HiveEntity та NoteEntity

package com.beemaster.beekeeperjournal.models

import com.beemaster.beekeeperjournal.db.HiveEntity
import com.beemaster.beekeeperjournal.db.NoteEntity

/**
 * Клас-обгортка для резервної копії даних.
 * Містить список усіх вуликів та всіх записів.
 */
data class BackupData(
    val hives: List<HiveEntity>,
    val notes: List<NoteEntity>
)