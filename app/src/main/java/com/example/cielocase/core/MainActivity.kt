package com.example.cielocase.core

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cielocase.core.composable.AppRoot
import com.example.cielocase.data.payment.cielo.CieloCallbackHandler
import com.example.cielocase.ui.theme.CieloCaseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity of the app. It is also the `order://response` contract declared in the
 * manifest: Cielo Smart returns the payment result as a deep link to this activity
 * (`launchMode="singleTask"`, so the result arrives in [onNewIntent]).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var callbackHandler: CieloCallbackHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handlePaymentCallback(intent)
        enableEdgeToEdge()
        setContent {
            CieloCaseTheme {
                AppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentCallback(intent)
    }

    private fun handlePaymentCallback(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        callbackHandler.handle(intent.data?.toString())
    }
}
