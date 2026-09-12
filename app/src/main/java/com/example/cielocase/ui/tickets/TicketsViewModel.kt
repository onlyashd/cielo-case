package com.example.cielocase.ui.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cielocase.domain.PurchaseRepository
import com.example.cielocase.domain.model.Ticket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TicketsViewModel @Inject constructor(
    purchaseRepository: PurchaseRepository,
) : ViewModel() {

    val state: StateFlow<TicketsUiState> = purchaseRepository.observeTickets()
        .map { tickets -> TicketsUiState(isLoading = false, tickets = tickets) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TicketsUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

data class TicketsUiState(
    val isLoading: Boolean = true,
    val tickets: List<Ticket> = emptyList(),
) {
    val ticketsByPurchase: Map<String, List<Ticket>> get() = tickets.groupBy { it.purchaseId }
}
