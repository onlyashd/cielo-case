package com.example.cielocase.di

import android.content.Context
import com.example.cielocase.util.observability.SentryConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {
    @Provides
    @Singleton
    fun provideSentry(@ApplicationContext context: Context): SentryConfig =
        SentryConfig(context)
}
