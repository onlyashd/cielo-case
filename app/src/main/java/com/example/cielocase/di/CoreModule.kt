package com.example.cielocase.di

import android.content.Context
import androidx.room.Room
import com.example.cielocase.data.local.AppDatabase
import com.example.cielocase.domain.Clock
import com.example.cielocase.domain.IdGenerator
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = IdGenerator.Uuid

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
