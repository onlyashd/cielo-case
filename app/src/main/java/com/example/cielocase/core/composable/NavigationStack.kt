package com.example.cielocase.core.composable

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cielocase.core.MainViewModel
import com.example.cielocase.core.Screen
import com.example.cielocase.events.EventsScreen
import com.example.cielocase.payment.PaymentReceiptScreen
import com.example.cielocase.payment.PaymentResultScreen
import com.example.cielocase.payment.PaymentReviewScreen
import com.example.cielocase.tickets.TicketsScreen

@Composable
fun NavigationStack(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) { Dashboard(navController) }
        composable(Screen.Events.route) { EventsScreen(navController) }
        composable(Screen.Tickets.route) { TicketsScreen(navController) }
        composable(Screen.PaymentReview.route) { PaymentReviewScreen(navController) }
        composable(Screen.PaymentResult.route) { PaymentResultScreen(navController) }
        composable(Screen.PaymentReceipt.route) { PaymentReceiptScreen(navController) }
    }

    viewModel.saveNavController(navController)
}