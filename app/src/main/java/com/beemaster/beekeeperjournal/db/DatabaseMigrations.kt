// DatabaseMigrations.kt

package com.beemaster.beekeeperjournal.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Calendar

/**
 * Файл містить усі об'єкти міграції (схеми бази даних) для Room.
 * Кожна міграція забезпечує безпечне оновлення схеми бази даних між версіями.
 */

// --------------------------------------------------------------------------
// Оголошення окремих об'єктів міграції
// --------------------------------------------------------------------------

/**
 * Міграція з версії 1 на 2.
 * Зміна типу стовпця 'hiveNumber' у таблиці 'hives' з INTEGER на TEXT.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQL-запит для зміни стовпця hiveNumber з INT на TEXT
        db.execSQL("ALTER TABLE hives RENAME COLUMN hiveNumber TO hiveNumber_temp;")
        db.execSQL("ALTER TABLE hives ADD COLUMN hiveNumber TEXT NOT NULL DEFAULT '';")
        db.execSQL("UPDATE hives SET hiveNumber = hiveNumber_temp;")
        db.execSQL("ALTER TABLE hives DROP COLUMN hiveNumber_temp;")
    }
}

/**
 * Міграція з версії 2 на 3.
 * Створення нових таблиць 'expenses' та 'incomes'.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `name` TEXT NOT NULL, `amount` REAL NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `incomes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantity` REAL NOT NULL, `price` REAL NOT NULL, `totalAmount` REAL NOT NULL)"
        )
    }
}

/**
 * Міграція з версії 3 на 4.
 * Додавання стовпця 'quantityUnits' (REAL) до таблиці expenses.
 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN quantityUnits REAL NOT NULL DEFAULT 0.0")
    }
}

/**
 * Міграція з версії 4 на 5.
 * Додавання стовпця 'nameQuantity' (TEXT) до таблиці expenses.
 */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN nameQuantity TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * Міграція з версії 5 на 6.
 * Додавання стовпця 'unitName' (TEXT) до таблиці incomes.
 */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE incomes ADD COLUMN unitName TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * Міграція з версії 6 на 7.
 * Додавання стовпця 'hiveId' (INTEGER) до таблиць incomes та expenses.
 */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE incomes ADD COLUMN hiveId INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE expenses ADD COLUMN hiveId INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Міграція з версії 7 на 8.
 * Видалення стовпця 'name' з таблиці 'hives'.
 */
val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Створюємо тимчасову таблицю hives_new, яка відповідає новій HiveEntity
        db.execSQL(
            "CREATE TABLE hives_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "hiveNumber TEXT NOT NULL," +
                    "color INTEGER NOT NULL," +
                    "secondaryColor INTEGER NOT NULL" +
                    ")"
        )

        // 2. Копіюємо дані зі старої таблиці 'hives' (version 7) в нову 'hives_new' (version 8),
        // виключаючи стовпець 'name'.
        db.execSQL(
            "INSERT INTO hives_new (id, hiveNumber, color, secondaryColor) " +
                    "SELECT id, hiveNumber, color, secondaryColor FROM hives"
        )

        // 3. Видаляємо стару таблицю
        db.execSQL("DROP TABLE hives")

        // 4. Перейменовуємо нову таблицю на оригінальну назву
        db.execSQL("ALTER TABLE hives_new RENAME TO hives")
    }
}

//------------------- Нова міграція ---------------------

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- 1. Створюємо таблицю beekeeping_years ---
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `beekeeping_years` (" +
                    "`yearId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "`name` TEXT NOT NULL, " +
                    "`startDate` INTEGER NOT NULL)"
        )

        // Визначаємо поточний рік та час за допомогою Calendar (сумісний з API 24)
        val currentCalendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentTimeMillis = System.currentTimeMillis()

        // --- 2. Вставляємо початковий рік (ID 1) ---
        // Всі існуючі записи будуть прив'язані до цього року (DEFAULT 1).
        db.execSQL(
            "INSERT INTO beekeeping_years (yearId, name, startDate) " +
                    "VALUES (1, 'Початковий Рік $currentYear', $currentTimeMillis)"
        )

        // --- 3. Додавання стовпчика yearId до існуючих таблиць ---

        // notes
        db.execSQL("""
                    ALTER TABLE notes ADD COLUMN yearId INTEGER NOT NULL DEFAULT 1
                """.trimIndent())

        // incomes
        db.execSQL("""
                    ALTER TABLE incomes ADD COLUMN yearId INTEGER NOT NULL DEFAULT 1
                """.trimIndent())

        // expenses
        db.execSQL("""
                    ALTER TABLE expenses ADD COLUMN yearId INTEGER NOT NULL DEFAULT 1
                """.trimIndent())
    }
}

// --------------------------------------------------------------------------
//  Масив усіх міграцій (Оголошується після всіх об'єктів)
// --------------------------------------------------------------------------

/**
 * Масив, що містить усі об'єкти міграції. Використовується в AppModule.
 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,

)

/** УВАГА!!!
 * При створенні кожної нової міграції потрібно не забути змінити version у AppDatabase.
 * Також треба додати міграцію у масив ALL_MIGRATIONS.
 * При додаванні нового стовпчика треба не забути додати його дані у поле пошуку
 */