package com.example.cielocase.data.payment.cielo

import com.example.cielocase.di.ApplicationScope
import com.example.cielocase.domain.SettleResult
import com.example.cielocase.domain.usecase.RegisterPaymentResultUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry point of the payment callback.
 *
 * The result is persisted as soon as the deep link arrives (not when some screen happens to
 * be listening), so an approved payment is never lost if the process was restarted while the
 * Cielo application was in the foreground.
 */
@Singleton
class CieloCallbackHandler @Inject constructor(
    private val parser: CieloCallbackParser,
    private val registerPaymentResult: RegisterPaymentResultUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _lastResult = MutableStateFlow<SettleResult?>(null)

    /** Last settled payment, used by the UI to jump to the result screen. */
    val lastResult: StateFlow<SettleResult?> = _lastResult.asStateFlow()

    /** @return true when [uri] was a Cielo payment callback. */
    fun handle(uri: String?): Boolean {
        val outcome = parser.parse(uri) ?: return false
        scope.launch { _lastResult.value = registerPaymentResult(outcome) }
        return true
    }

    fun consumeLastResult() {
        _lastResult.value = null
    }
}
