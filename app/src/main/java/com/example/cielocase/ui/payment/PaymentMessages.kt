package com.example.cielocase.ui.payment

import com.example.cielocase.R
import com.example.cielocase.domain.model.PaymentFailure
import com.example.cielocase.domain.model.PaymentFailureKind
import com.example.cielocase.ui.component.UiMessage

/** Maps integration/payment failures to explicit, actionable messages. */
fun PaymentFailure.toUiMessage(): UiMessage = when (kind) {
    PaymentFailureKind.MISSING_CREDENTIALS -> UiMessage(R.string.error_missing_credentials)
    PaymentFailureKind.CIELO_APP_UNAVAILABLE -> UiMessage(R.string.error_cielo_unavailable)
    PaymentFailureKind.AUTHENTICATION -> UiMessage(R.string.error_authentication)
    PaymentFailureKind.INVALID_RESPONSE -> UiMessage(R.string.error_invalid_response)
    PaymentFailureKind.USER_CANCELLED -> UiMessage(R.string.result_cancelled)
    PaymentFailureKind.PAYMENT_DENIED -> UiMessage(R.string.result_denied)
    PaymentFailureKind.GENERIC -> UiMessage(R.string.error_generic)
}

fun PaymentFailure.detail(): UiMessage? = when {
    !reason.isNullOrBlank() -> UiMessage(R.string.error_reason, reason)
    code != null -> UiMessage(R.string.error_code, code)
    else -> null
}
