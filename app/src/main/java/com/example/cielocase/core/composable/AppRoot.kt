package com.example.cielocase.core.composable

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cielocase.core.AppRootViewModel
import com.example.cielocase.core.Screen
import com.example.cielocase.ui.checkout.CheckoutScreen
import com.example.cielocase.ui.events.EventsScreen
import com.example.cielocase.ui.payment.PaymentReceiptScreen
import com.example.cielocase.ui.payment.PaymentResultScreen
import com.example.cielocase.ui.payment.PaymentReviewScreen
import com.example.cielocase.ui.tickets.TicketsScreen
import com.example.cielocase.util.composable.colorScheme

@Composable
fun AppRoot(viewModel: AppRootViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val settledPurchaseId by viewModel.settledPurchaseId.collectAsStateWithLifecycle()

    // A payment callback may arrive while any screen is visible (or after a process restart):
    // whenever a purchase is settled the result screen takes over.
    LaunchedEffect(settledPurchaseId) {
        val purchaseId = settledPurchaseId ?: return@LaunchedEffect
        navController.navigate(Screen.PaymentResult.of(purchaseId)) { launchSingleTop = true }
        viewModel.onSettledResultHandled()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeContent,
        topBar = { TopBar() },
        bottomBar = { NavBar(navController) },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = colorScheme().background,
        ) {
            AppNavHost(navController)
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Events.route) {
        composable(Screen.Events.route) {
            EventsScreen(
                onEventSelected = { eventId -> navController.navigate(Screen.Checkout.of(eventId)) },
            )
        }
        composable(Screen.Checkout.route) {
            CheckoutScreen(
                onBack = navController::popBackStack,
                onCheckoutStarted = { purchaseId ->
                    navController.navigate(Screen.PaymentReview.of(purchaseId))
                },
            )
        }
        composable(Screen.PaymentReview.route) {
            PaymentReviewScreen(
                onBack = navController::popBackStack,
                onPaymentStarted = { purchaseId ->
                    navController.navigate(Screen.PaymentResult.of(purchaseId)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Screen.PaymentResult.route) {
            PaymentResultScreen(
                onShowReceipt = { purchaseId ->
                    navController.navigate(Screen.PaymentReceipt.of(purchaseId))
                },
                onRetryStarted = { purchaseId ->
                    navController.navigate(Screen.PaymentReview.of(purchaseId)) {
                        launchSingleTop = true
                    }
                },
                onFinish = {
                    navController.navigate(Screen.Events.route) {
                        popUpTo(Screen.Events.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.PaymentReceipt.route) {
            PaymentReceiptScreen(
                onFinish = {
                    navController.navigate(Screen.Tickets.route) {
                        popUpTo(Screen.Events.route)
                    }
                },
            )
        }
        composable(Screen.Tickets.route) { TicketsScreen() }
    }
}
