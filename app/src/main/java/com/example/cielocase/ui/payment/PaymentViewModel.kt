package com.example.cielocase.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cielocase.BuildConfig
import com.example.cielocase.R
import com.example.cielocase.core.Screen
import com.example.cielocase.data.payment.simulator.PaymentSimulator
import com.example.cielocase.data.payment.simulator.SimulatedCallbackFactory
import com.example.cielocase.domain.PurchaseRepository
import com.example.cielocase.domain.model.Purchase
import com.example.cielocase.domain.model.Ticket
import com.example.cielocase.domain.payment.PaymentRequest
import com.example.cielocase.domain.usecase.StartCheckoutUseCase
import com.example.cielocase.domain.usecase.StartPaymentUseCase
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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the review/result/receipt screens of a single purchase.
 *
 * The screens are stateless with respect to the payment: the source of truth is the purchase
 * row, which is updated by the callback handler. That is what makes the flow resilient to the
 * app being backgrounded (or killed) while the Cielo application is in the foreground.
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    purchaseRepository: PurchaseRepository,
    private val startPayment: StartPaymentUseCase,
    private val startCheckout: StartCheckoutUseCase,
    private val paymentSimulator: PaymentSimulator,
) : ViewModel() {

    private val purchaseId: String = checkNotNull(savedStateHandle[Screen.ARG_PURCHASE_ID])

    private val isLaunching = MutableStateFlow(false)
    private val message = MutableStateFlow<UiMessage?>(null)
    private val paymentCode = MutableStateFlow(PaymentRequest.PAYMENT_CODE_CREDIT_UP_FRONT)

    private val paymentStartedChannel = Channel<String>(Channel.BUFFERED)
    private val retryChannel = Channel<String>(Channel.BUFFERED)

    /** Emitted when the Cielo application was launched for this purchase. */
    val paymentStarted: Flow<String> = paymentStartedChannel.receiveAsFlow()

    /** Emitted with the id of the *new* purchase created for a retry. */
    val retryStarted: Flow<String> = retryChannel.receiveAsFlow()

    val state: StateFlow<PaymentUiState> = combine(
        purchaseRepository.observePurchase(purchaseId),
        purchaseRepository.observeTicketsOfPurchase(purchaseId),
        isLaunching,
        message,
        paymentCode,
    ) { purchase, tickets, launching, uiMessage, selectedPaymentCode ->
        PaymentUiState(
            isLoading = false,
            purchase = purchase,
            tickets = tickets,
            isLaunching = launching,
            message = uiMessage,
            paymentCode = selectedPaymentCode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PaymentUiState())

    fun onPaymentCodeSelected(code: String) {
        paymentCode.value = code
    }

    /**
     * Starts the payment. Duplicate submissions are dropped here (UI guard) and rejected again
     * by [StartPaymentUseCase] (domain guard), so a re-sent action can never charge twice.
     */
    fun onPay() {
        if (isLaunching.value) return
        isLaunching.value = true

        viewModelScope.launch {
            when (val result = startPayment(purchaseId, paymentCode.value)) {
                is StartPaymentUseCase.Result.Launched ->
                    paymentStartedChannel.send(purchaseId)

                is StartPaymentUseCase.Result.AlreadyInProgress ->
                    message.value = UiMessage(R.string.review_in_progress)

                is StartPaymentUseCase.Result.AlreadySettled ->
                    paymentStartedChannel.send(purchaseId)

                is StartPaymentUseCase.Result.NotLaunched ->
                    message.value = result.failure.toUiMessage()

                StartPaymentUseCase.Result.PurchaseNotFound ->
                    message.value = UiMessage(R.string.error_generic)
            }
            isLaunching.value = false
        }
    }

    /**
     * A settled purchase is never paid again: the retry creates a brand new purchase
     * (new `reference`) for the same event and quantity.
     */
    fun onRetry() {
        val purchase = state.value.purchase ?: return
        viewModelScope.launch {
            val result = startCheckout(purchase.eventId, purchase.quantity)
            if (result is StartCheckoutUseCase.Result.Success) {
                retryChannel.send(result.purchase.id)
            } else {
                message.value = UiMessage(R.string.error_generic)
            }
        }
    }

    fun onSimulate(outcome: SimulatedCallbackFactory.Outcome) {
        val purchase = state.value.purchase ?: return
        paymentSimulator.simulate(purchase.id, purchase.totalInCents, outcome)
    }

    fun onMessageShown() {
        message.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

data class PaymentUiState(
    val isLoading: Boolean = true,
    val purchase: Purchase? = null,
    val tickets: List<Ticket> = emptyList(),
    val isLaunching: Boolean = false,
    val message: UiMessage? = null,
    val paymentCode: String = PaymentRequest.PAYMENT_CODE_CREDIT_UP_FRONT,
    val isSimulatorEnabled: Boolean = BuildConfig.PAYMENT_SIMULATOR_ENABLED,
)
