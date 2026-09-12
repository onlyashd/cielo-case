package com.example.cielocase.tickets

import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cielocase.database.entity.TicketsEntity

@Composable
fun TicketCard(
    ticket: TicketsEntity,
) {
    ElevatedCard() {

    }
}

@Preview
@Composable
private fun PreviewTicketCard() {
    TicketCard()
}
