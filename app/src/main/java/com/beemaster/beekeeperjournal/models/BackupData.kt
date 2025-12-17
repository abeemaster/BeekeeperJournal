// BackupData Цей клас простий контейнер, який тримає HiveEntity та NoteEntity

package com.beemaster.beekeeperjournal.models

import com.beemaster.beekeeperjournal.db.entity.BeekeepingYear
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity

/**
 * Клас-обгортка для резервної копії даних.
 * Містить список усіх вуликів та всіх записів.
 */
data class BackupData(
    val hives: List<HiveEntity>,
    val notes: List<Note>,
    val expenses: List<Expense>,
    val incomes: List<IncomeEntity>,
    val years: List<BeekeepingYear>
)