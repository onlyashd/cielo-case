package com.example.cielocase.data.payment.cielo

/**
 * Credentials issued by the Cielo developer portal (Cielo Smart - Order Manager API).
 * Provided through `local.properties`/environment variables, never hardcoded.
 */
data class CieloCredentials(
    val clientId: String,
    val accessToken: String,
    val merchantCode: String? = null,
) {
    val isConfigured: Boolean get() = clientId.isNotBlank() && accessToken.isNotBlank()
}
