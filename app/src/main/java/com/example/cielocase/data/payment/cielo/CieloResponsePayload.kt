package com.example.cielocase.data.payment.cielo

/** Successful `response` payload: the Cielo order with its payments. */
data class CieloOrderPayload(
    val id: String? = null,
    val reference: String? = null,
    val status: String? = null,
    val type: String? = null,
    val price: Long? = null,
    val paidAmount: Long? = null,
    val payments: List<CieloPaymentResponse>? = null,
)

data class CieloPaymentResponse(
    val id: String? = null,
    val amount: Long? = null,
    val authCode: String? = null,
    val cieloCode: String? = null,
    val brand: String? = null,
    val mask: String? = null,
    val installments: Int? = null,
    val terminal: String? = null,
    val requestDate: String? = null,
    val paymentFields: Map<String, String>? = null,
) {
    val statusCode: String? get() = paymentFields?.get("statusCode")
    val productName: String? get() = paymentFields?.get("productName")
}

/** Failure `response` payload: `{"code":1,"reason":"CANCELADO PELO USUÁRIO"}`. */
data class CieloErrorPayload(
    val code: Int? = null,
    val reason: String? = null,
    val message: String? = null,
) {
    val description: String? get() = reason ?: message
}
