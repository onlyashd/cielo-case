package com.example.cielocase.util.extensions

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats an amount in cents as Brazilian currency. */
fun Long.formatAsCurrency(): String =
    Formats.CURRENCY.format(this / Formats.CENTS_IN_UNIT)

fun Long.formatAsDateTime(zone: ZoneId = ZoneId.systemDefault()): String =
    Formats.DATE_TIME.format(Instant.ofEpochMilli(this).atZone(zone))

private object Formats {
    const val CENTS_IN_UNIT = 100.0

    private val LOCALE_BR: Locale = Locale.forLanguageTag("pt-BR")

    val CURRENCY: NumberFormat = NumberFormat.getCurrencyInstance(LOCALE_BR)

    val DATE_TIME: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", LOCALE_BR)
}
