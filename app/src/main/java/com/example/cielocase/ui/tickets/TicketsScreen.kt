package com.example.cielocase.ui.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cielocase.R

@Composable
fun TicketsScreen(viewModel: TicketsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.isLoading -> Centered { CircularProgressIndicator() }
        state.tickets.isEmpty() -> Centered { Text(stringResource(R.string.tickets_empty)) }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.ticketsByPurchase.values.forEach { tickets ->
                items(tickets, key = { it.id }) { ticket ->
                    TicketCard(ticket = ticket, total = tickets.size)
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
