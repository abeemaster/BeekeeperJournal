// BackupData Цей клас простий контейнер, який тримає HiveEntity та NoteEntity

package com.beemaster.beekeeperjournal.models

import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
import com.beemaster.beekeeperjournal.db.entity.NoteEntity

/**
 * Клас-обгортка для резервної копії даних.
 * Містить список усіх вуликів та всіх записів.
 */
data class BackupData(
    val hives: List<HiveEntity>,
    val notes: List<NoteEntity>,
    val expenses: List<ExpenseEntity>,
    val incomes: List<IncomeEntity>
)