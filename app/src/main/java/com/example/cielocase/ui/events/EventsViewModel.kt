package com.example.cielocase.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cielocase.domain.EventRepository
import com.example.cielocase.domain.model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    eventRepository: EventRepository,
) : ViewModel() {

    val state: StateFlow<EventsUiState> = eventRepository.observeEvents()
        .map { EventsUiState(isLoading = false, events = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EventsUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

data class EventsUiState(
    val isLoading: Boolean = true,
    val events: List<Event> = emptyList(),
)
