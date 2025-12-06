// ExpenseMappers.kt

package com.beemaster.beekeeperjournal.mappers

import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import com.beemaster.beekeeperjournal.models.Expense

/**
 * Функція розширення для конвертації сутності бази даних (Entity) в бізнес-модель (Model).
 * Використовується при читанні даних з репозиторію.
 */
fun ExpenseEntity.toExpense(): Expense {
    return Expense(
        id = this.id,
        hiveId = this.hiveId,
        date = this.date,
        name = this.name,
        quantityUnits = this.quantityUnits,
        nameQuantity = this.nameQuantity,
        amount = this.amount
    )
}

/**
 * Функція розширення для конвертації бізнес-моделі (Model) в сутність бази даних (Entity).
 * Використовується при записі даних через репозиторій.
 */
fun Expense.toExpenseEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = this.id,
        hiveId = this.hiveId,
        date = this.date,
        name = this.name,
        quantityUnits = this.quantityUnits,
        nameQuantity = this.nameQuantity,
        amount = this.amount,
        yearId = this.yearId
    )
}