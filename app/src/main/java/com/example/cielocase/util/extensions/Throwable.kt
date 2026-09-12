package com.example.cielocase.util.extensions

import android.util.Log
import com.example.cielocase.util.observability.SensitiveData
import io.sentry.Sentry

fun Throwable.log(tag: String) {
    Log.e(tag, SensitiveData.redact(stackTraceToString()).orEmpty())
    Sentry.captureException(this)
}

fun Throwable.log(tag: String, msg: String) {
    Log.e(tag, "$msg\n${SensitiveData.redact(stackTraceToString()).orEmpty()}")
    Sentry.captureException(this)
}
