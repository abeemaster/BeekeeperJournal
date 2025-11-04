// Файл SharedPreferencesModule.kt потрібен виключно для того,
// щоб навчити Hilt створювати та надавати об'єкт SharedPreferences у програмі.

package com.beemaster.beekeeperjournal.di

import android.content.Context
import android.content.SharedPreferences
import com.beemaster.beekeeperjournal.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedPreferencesModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        // Надаємо SharedPreferences з іменем, яке використовується для налаштувань
        return context.getSharedPreferences(
            Constants.SETTINGS_PREFS_NAME, // Наприклад: "app_settings"
            Context.MODE_PRIVATE
        )
    }
}