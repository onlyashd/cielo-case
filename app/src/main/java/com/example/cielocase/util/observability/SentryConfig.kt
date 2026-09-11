package com.example.cielocase.util.observability

import android.content.Context
import com.example.cielocase.BuildConfig
import io.sentry.android.core.SentryAndroid

class SentryConfig(context: Context) {
    init {
        SentryAndroid.init(context) { options ->
            options.isDebug = BuildConfig.DEBUG
            options.dsn = BuildConfig.SENTRY_DSN
        }
    }
}
