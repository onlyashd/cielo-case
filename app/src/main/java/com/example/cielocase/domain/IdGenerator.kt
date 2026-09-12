package com.example.cielocase.domain

import java.util.UUID

fun interface IdGenerator {
    fun newId(): String

    companion object {
        val Uuid = IdGenerator { UUID.randomUUID().toString() }
    }
}
