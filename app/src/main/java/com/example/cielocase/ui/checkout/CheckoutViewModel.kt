package com.example.cielocase.ui.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cielocase.R
import com.example.cielocase.core.Screen
import com.example.cielocase.domain.EventRepository
import com.example.cielocase.domain.model.Event
import com.example.cielocase.domain.usecase.StartCheckoutUseCase
import com.example.cielocase.ui.component.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    eventRepository: EventRepository,
    private val startCheckout: StartCheckoutUseCase,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle[Screen.ARG_EVENT_ID])

    private val quantity = MutableStateFlow(1)
    private val isSubmitting = MutableStateFlow(false)
    private val message = MutableStateFlow<UiMessage?>(null)

    private val checkoutStartedChannel = Channel<String>(Channel.BUFFERED)

    /** Emits the purchase id once the pending purchase is created. */
    val checkoutStarted: Flow<String> = checkoutStartedChannel.receiveAsFlow()

    val state: StateFlow<CheckoutUiState> = combine(
        eventRepository.observeEvent(eventId),
        quantity,
        isSubmitting,
        message,
    ) { event, selectedQuantity, submitting, uiMessage ->
        CheckoutUiState(
            isLoading = false,
            event = event,
            quantity = selectedQuantity,
            isSubmitting = submitting,
            message = uiMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CheckoutUiState())

    fun onQuantityChange(delta: Int) {
        val event = state.value.event ?: return
        val next = quantity.value + delta
        when {
            next < 1 -> Unit
            next > event.maxTicketsPerPurchase() -> message.value = quantityLimitMessage(event)
            else -> {
                message.value = null
                quantity.value = next
            }
        }
    }

    /** Creates the pending purchase. Repeated taps are ignored while one is in flight. */
    fun onContinue() {
        if (isSubmitting.value) return
        isSubmitting.value = true

        viewModelScope.launch {
            when (val result = startCheckout(eventId, quantity.value)) {
                is StartCheckoutUseCase.Result.Success ->
                    checkoutStartedChannel.send(result.purchase.id)

                is StartCheckoutUseCase.Result.InvalidQuantity ->
                    message.value = UiMessage(R.string.checkout_max_reached, result.max)

                is StartCheckoutUseCase.Result.NotEnoughTickets ->
                    message.value = UiMessage(R.string.checkout_not_enough, result.available)

                StartCheckoutUseCase.Result.SalesClosed ->
                    message.value = UiMessage(R.string.checkout_sales_closed)

                StartCheckoutUseCase.Result.EventNotFound ->
                    message.value = UiMessage(R.string.checkout_event_not_found)
            }
            isSubmitting.value = false
        }
    }

    fun onMessageShown() = message.update { null }

    private fun quantityLimitMessage(event: Event): UiMessage =
        if (event.availableTickets < Event.MAX_TICKETS_PER_PURCHASE) {
            UiMessage(R.string.checkout_not_enough, event.availableTickets)
        } else {
            UiMessage(R.string.checkout_max_reached, Event.MAX_TICKETS_PER_PURCHASE)
        }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

data class CheckoutUiState(
    val isLoading: Boolean = true,
    val event: Event? = null,
    val quantity: Int = 1,
    val isSubmitting: Boolean = false,
    val message: UiMessage? = null,
) {
    val totalInCents: Long get() = (event?.unitPriceInCents ?: 0) * quantity
}
