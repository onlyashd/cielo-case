package com.example.cielocase.core

sealed class Screen(val route: String) {
    data object Events : Screen("events")
    data object Tickets : Screen("tickets")

    data object Checkout : Screen("checkout/{$ARG_EVENT_ID}") {
        fun of(eventId: String) = "checkout/$eventId"
    }

    data object PaymentReview : Screen("payment-review/{$ARG_PURCHASE_ID}") {
        fun of(purchaseId: String) = "payment-review/$purchaseId"
    }

    data object PaymentResult : Screen("payment-result/{$ARG_PURCHASE_ID}") {
        fun of(purchaseId: String) = "payment-result/$purchaseId"
    }

    data object PaymentReceipt : Screen("payment-receipt/{$ARG_PURCHASE_ID}") {
        fun of(purchaseId: String) = "payment-receipt/$purchaseId"
    }

    companion object {
        const val ARG_EVENT_ID = "eventId"
        const val ARG_PURCHASE_ID = "purchaseId"
    }
}
