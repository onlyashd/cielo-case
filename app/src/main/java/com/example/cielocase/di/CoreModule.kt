package com.example.cielocase.di

import android.content.Context
import com.example.cielocase.core.MainViewModel
import com.example.cielocase.database.AppDatabase
import com.example.cielocase.database.Database
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun provideMainViewModel(): MainViewModel = MainViewModel()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Database().getDatabase(context)
}
