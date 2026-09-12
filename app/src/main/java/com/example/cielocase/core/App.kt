package com.example.cielocase.core

import android.app.Application
import com.example.cielocase.BuildConfig
import com.example.cielocase.util.observability.SentryConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var sentryConfig: Provider<SentryConfig>

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.SENTRY_DSN.isNotBlank()) sentryConfig.get()
    }
}
