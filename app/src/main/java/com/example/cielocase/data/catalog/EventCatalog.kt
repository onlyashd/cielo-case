package com.example.cielocase.data.catalog

import com.example.cielocase.domain.model.Event
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Hardcoded catalog (there is no backend in this sample). It is written into Room on first
 * launch so inventory can be reserved transactionally like it would be with a real catalog.
 */
object EventCatalog {

    private val ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")

    val events: List<Event> = listOf(
        event(
            id = "evt-rock-sp",
            title = "Rock na Praça - São Paulo",
            description = "Festival com três palcos e line-up nacional. Abertura dos portões às 14h.",
            dateTime = LocalDateTime.of(2026, 10, 18, 16, 0),
            location = "Parque da Independência, São Paulo - SP",
            unitPriceInCents = 18_990,
            availableTickets = 120,
        ),
        event(
            id = "evt-teatro-rj",
            title = "Peça: O Auto da Compadecida",
            description = "Montagem comemorativa com elenco convidado e sessão única.",
            dateTime = LocalDateTime.of(2026, 11, 7, 20, 30),
            location = "Teatro Municipal, Rio de Janeiro - RJ",
            unitPriceInCents = 12_000,
            availableTickets = 40,
        ),
        event(
            id = "evt-futebol-bh",
            title = "Clássico Mineiro - Rodada 34",
            description = "Setor coberto, entrada pelo portão B com ingresso digital.",
            dateTime = LocalDateTime.of(2026, 10, 25, 18, 45),
            location = "Estádio Governador Magalhães Pinto, Belo Horizonte - MG",
            unitPriceInCents = 9_500,
            availableTickets = 3,
        ),
        event(
            id = "evt-standup-poa",
            title = "Stand-up: Rir é o Melhor Remédio",
            description = "Duas horas de comédia com quatro humoristas no palco.",
            dateTime = LocalDateTime.of(2026, 12, 5, 21, 0),
            location = "Casa de Cultura, Porto Alegre - RS",
            unitPriceInCents = 7_000,
            availableTickets = 0,
        ),
        event(
            id = "evt-tech-rec",
            title = "Conferência Dev Nordeste",
            description = "Trilhas de mobile, dados e plataforma. Vendas abrem em breve.",
            dateTime = LocalDateTime.of(2026, 12, 12, 9, 0),
            location = "Centro de Convenções, Recife - PE",
            unitPriceInCents = 25_000,
            availableTickets = 200,
            salesOpen = false,
        ),
    )

    @Suppress("LongParameterList")
    private fun event(
        id: String,
        title: String,
        description: String,
        dateTime: LocalDateTime,
        location: String,
        unitPriceInCents: Long,
        availableTickets: Int,
        salesOpen: Boolean = true,
    ) = Event(
        id = id,
        title = title,
        description = description,
        startsAtEpochMillis = dateTime.atZone(ZONE).toInstant().toEpochMilli(),
        location = location,
        unitPriceInCents = unitPriceInCents,
        availableTickets = availableTickets,
        salesOpen = salesOpen,
    )
}
