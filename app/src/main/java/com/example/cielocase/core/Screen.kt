package com.example.cielocase.core

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Events : Screen("events")
    object Tickets : Screen("ticket")
    object PaymentReview : Screen("payment-review")
    object PaymentResult : Screen("payment-result")
    object PaymentReceipt : Screen("payment-receipt")
}
