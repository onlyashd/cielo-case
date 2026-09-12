package com.example.cielocase.data.payment.cielo

import com.google.gson.annotations.SerializedName

/** Request body of `lio://payment`, encoded as Base64 in the `request` parameter. */
data class CieloPaymentPayload(
    val accessToken: String,
    @SerializedName("clientID")
    val clientId: String,
    val reference: String,
    val merchantCode: String? = null,
    val email: String? = null,
    val installments: Int,
    val items: List<CieloPaymentItemPayload>,
    val paymentCode: String,
    /** Total amount in cents, as string - required by the Cielo contract. */
    val value: String,
)

data class CieloPaymentItemPayload(
    val name: String,
    val quantity: Int,
    val sku: String,
    val unitOfMeasure: String,
    /** Unit price in cents. */
    val unitPrice: Long,
)
