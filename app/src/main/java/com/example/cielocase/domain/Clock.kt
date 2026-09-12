package com.example.cielocase.domain

fun interface Clock {
    fun nowEpochMillis(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}
