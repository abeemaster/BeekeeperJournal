// VoskModule.kt

package com.beemaster.beekeeperjournal.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object VoskModule {


    }

    /**
     * Обробляє звільнення ресурсів VoskModelManager при завершенні роботи додатка.
     * Хоча VoskModelManager є Singleton, ми використовуємо Provides для
     * забезпечення, що release() буде викликаний, якщо Hilt це підтримує (на практиці,
     * фінальне звільнення часто залишається за onDestroy Application).
     * Однак, для чистоти, ми можемо це залишити.
     */
    // Примітка: Room, OkHttp та інші ресурси зазвичай звільняються тут.
    // Оскільки VoskModelManager є @Singleton і ініціалізується Hilt'ом,
    // ми не можемо напряму керувати його onDestroy. Залишимо це в Application.
    // Фактично, ми просто повертаємо створений менеджером об'єкт.
