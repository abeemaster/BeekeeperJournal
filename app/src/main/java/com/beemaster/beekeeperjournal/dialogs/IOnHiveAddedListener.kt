package com.beemaster.beekeeperjournal.dialogs

/**
 * Інтерфейс для обробки події додавання нового вулика.
 */
interface IOnHiveAddedListener {
    /**
     * Викликається після успішного додавання нового вулика.
     * @param hiveNumber Номер доданого вулика.
     */
    fun onHiveAdded(hiveNumber: String)
}