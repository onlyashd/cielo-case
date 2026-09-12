package com.example.cielocase.tickets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.cielocase.core.MainViewModel
import com.example.cielocase.database.entity.TicketsEntity
import kotlinx.coroutines.launch

@Composable
fun TicketsScreen(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val sampleHolderId = "296.526.720-41"
    var tickets = listOf<TicketsEntity>()

    LaunchedEffect(null) {
        scope.launch {
            tickets = viewModel.db.value?.ticketsDao()?.getTickets(sampleHolderId) ?: listOf()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn {
            items(tickets) { ticket ->
                TicketCard(ticket)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewTicketsScreen() {
    TicketsScreen()
}
