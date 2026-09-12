package com.example.cielocase.core

import androidx.lifecycle.ViewModel
import com.example.cielocase.data.payment.cielo.CieloCallbackHandler
import com.example.cielocase.domain.SettleResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class AppRootViewModel @Inject constructor(
    private val callbackHandler: CieloCallbackHandler,
) : ViewModel() {

    val settledPurchaseId: StateFlow<String?> = callbackHandler.lastResult
        .map { result ->
            when (result) {
                is SettleResult.Settled -> result.purchase.id
                is SettleResult.Duplicate -> result.purchase.id
                SettleResult.UnknownReference, null -> null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun onSettledResultHandled() = callbackHandler.consumeLastResult()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
