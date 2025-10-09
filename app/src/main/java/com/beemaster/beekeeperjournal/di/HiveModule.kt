// HiveModule.kt

package com.beemaster.beekeeperjournal.di

import android.content.Context
import androidx.core.content.ContextCompat
import com.beemaster.beekeeperjournal.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiveModule {

    // ✅ Надаємо константу default кольору вулика як просте Int
    @Provides
    @Singleton
    @Named("DefaultHiveColor")
    fun provideDefaultHiveColor(@ApplicationContext context: Context): Int {
        // Отримуємо значення кольору тут, де це дозволено (в DI Module)
        return ContextCompat.getColor(context, R.color.color_white)
    }
}