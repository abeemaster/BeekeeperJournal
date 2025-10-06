// IncomeMappers.kt

package com.beemaster.beekeeperjournal.mappers

import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
import com.beemaster.beekeeperjournal.models.Income

/**
 * Конвертує сутність бази даних (IncomeEntity) в доменну модель (Income).
 */
fun IncomeEntity.toIncome(): Income {
    return Income(
        id = this.id,
        hiveId = this.hiveId,
        date = this.date,
        productName = this.productName,
        quantity = this.quantity,
        unitName = this.unitName,
        price = this.price,
        totalAmount = this.totalAmount
    )
}

/**
 * Конвертує доменну модель (Income) в сутність бази даних (IncomeEntity).
 */
fun Income.toIncomeEntity(): IncomeEntity {
    return IncomeEntity(
        id = this.id,
        hiveId = this.hiveId,
        date = this.date,
        productName = this.productName,
        quantity = this.quantity,
        unitName = this.unitName,
        price = this.price,
        totalAmount = this.totalAmount
    )
}