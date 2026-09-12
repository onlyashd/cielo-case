package com.example.cielocase.domain.payment

/**
 * Remembers which purchase is currently waiting for a payment callback.
 *
 * Cielo error payloads (`{"code":1,"reason":"..."}`) carry no order reference, so the app
 * has to keep it locally to be able to settle the right purchase. It survives process death
 * because the payment happens in another application.
 */
interface InFlightPaymentStore {
    fun get(): String?
    fun set(reference: String)
    fun clear()
}
