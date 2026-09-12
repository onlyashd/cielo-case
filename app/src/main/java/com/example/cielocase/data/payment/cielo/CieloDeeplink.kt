package com.example.cielocase.data.payment.cielo

/**
 * Cielo Smart deep link contract.
 *
 * Request:  `lio://payment?request=<base64(json)>&urlCallback=order://response`
 * Response: `order://response?response=<base64(json)>&responsecode=0`
 *
 * Docs: https://docs.cielo.com.br/cielo-smart/docs/pagamento and
 * https://docs.cielo.com.br/cielo-smart/docs/recuperando-dados
 */
object CieloDeeplink {
    const val SCHEME = "lio"
    const val HOST_PAYMENT = "payment"
    const val HOST_PAYMENT_REVERSAL = "payment-reversal"

    const val PARAM_REQUEST = "request"
    const val PARAM_URL_CALLBACK = "urlCallback"
    const val PARAM_RESPONSE = "response"

    /** Must match the `intent-filter` declared for `MainActivity` in the manifest. */
    const val CALLBACK_SCHEME = "order"
    const val CALLBACK_HOST = "response"
    const val CALLBACK_URL = "$CALLBACK_SCHEME://$CALLBACK_HOST"

    /**
     * Packages that expose the `lio://` deep links. The documentation mentions
     * `com.ads.lio.uriappclient`, while current terminals/emulator builds ship
     * `br.com.cielosmart.orderservice`; both are declared in `<queries>`.
     */
    val HANDLER_PACKAGES = listOf(
        "com.ads.lio.uriappclient",
        "br.com.cielosmart.orderservice",
    )

    /** Cielo error codes returned in the `{"code":..,"reason":".."}` payload. */
    object ErrorCode {
        const val USER_CANCELLED = 1
        const val GENERIC = 2
        const val PAYMENT = 3
        const val AUTHENTICATION = 4
    }

    /** `paymentFields.statusCode`: 0 = Pix payment, 1 = authorized, 2 = reversal. */
    object StatusCode {
        const val PIX_PAYMENT = "0"
        const val AUTHORIZED = "1"
        const val REVERSED = "2"
    }
}
