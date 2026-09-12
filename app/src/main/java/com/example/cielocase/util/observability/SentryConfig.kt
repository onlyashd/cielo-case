package com.example.cielocase.util.observability

import android.content.Context
import com.example.cielocase.BuildConfig
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid

/**
 * Error reporting. Opt-in: nothing is initialized unless `SENTRY_DSN` is configured in
 * `local.properties`/environment (see [BuildConfig.SENTRY_DSN]).
 *
 * Auto-init is disabled in the manifest (`io.sentry.auto-init=false`) so initialization happens
 * here, where the DSN is checked and the payload scrubbing is installed.
 */
class SentryConfig(context: Context) {
    init {
        SentryAndroid.init(context) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.isDebug = BuildConfig.DEBUG
            options.environment = if (BuildConfig.DEBUG) "debug" else "release"
            options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}"
            // This app handles payments: never attach user/device identifiers or screenshots.
            options.isSendDefaultPii = false
            options.isAttachScreenshot = false
            options.isAttachViewHierarchy = false
            options.tracesSampleRate = 0.0
            // Only `sentry-android-core` is bundled, so there is no native (NDK) crash handler.
            options.isEnableNdk = false
            options.beforeSend = SentryOptions.BeforeSendCallback { event, _ -> event.scrubbed() }
        }
    }

    /** Last line of defense: strips Cielo credentials from anything leaving the device. */
    private fun SentryEvent.scrubbed(): SentryEvent = apply {
        message?.let { it.message = SensitiveData.redact(it.message) }
        exceptions?.forEach { it.value = SensitiveData.redact(it.value) }
        breadcrumbs?.forEach { it.message = SensitiveData.redact(it.message) }
    }
}
