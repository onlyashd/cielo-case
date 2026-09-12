package com.example.cielocase.fake

import com.example.cielocase.domain.model.Event

object TestEvents {
    val openEvent = Event(
        id = "evt-1",
        title = "Rock na Praça",
        description = "Festival",
        startsAtEpochMillis = 1_760_000_000_000,
        location = "São Paulo - SP",
        unitPriceInCents = 10_000,
        availableTickets = 10,
        salesOpen = true,
    )

    val almostSoldOut = openEvent.copy(id = "evt-2", availableTickets = 2)

    val salesClosed = openEvent.copy(id = "evt-3", salesOpen = false)

    val all = listOf(openEvent, almostSoldOut, salesClosed)
}
