package com.example.cielocase.data.payment.cielo

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.cielocase.domain.model.PaymentFailure
import com.example.cielocase.domain.model.PaymentFailureKind
import com.example.cielocase.domain.payment.PaymentGateway
import com.example.cielocase.domain.payment.PaymentLaunchResult
import com.example.cielocase.domain.payment.PaymentRequest
import com.example.cielocase.util.extensions.log
import com.example.cielocase.util.observability.SensitiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cielo Smart payment through the `lio://payment` deep link.
 *
 * Every failure that happens *before* the terminal takes over is reported explicitly, so the
 * UI can tell "nothing was charged" apart from "payment was refused".
 */
@Singleton
class CieloDeeplinkGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentials: CieloCredentials,
    private val requestFactory: CieloPaymentRequestFactory,
) : PaymentGateway {

    override fun start(request: PaymentRequest): PaymentLaunchResult {
        if (!credentials.isConfigured) {
            return notLaunched(
                PaymentFailureKind.MISSING_CREDENTIALS,
                "CIELO_CLIENT_ID/CIELO_ACCESS_TOKEN are not configured in local.properties",
            )
        }

        val intent = Intent(Intent.ACTION_VIEW, requestFactory.deeplink(request).toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        if (context.packageManager.resolveActivity(intent, 0) == null) {
            return notLaunched(
                PaymentFailureKind.CIELO_APP_UNAVAILABLE,
                "no application on this device handles ${CieloDeeplink.SCHEME}://" +
                    CieloDeeplink.HOST_PAYMENT,
            )
        }

        return runCatching {
            context.startActivity(intent)
            PaymentLaunchResult.Launched
        }.getOrElse { error ->
            error.log(TAG, "failed to launch the Cielo payment deep link")
            // The exception message may echo the deep link (which embeds the access token).
            notLaunched(PaymentFailureKind.GENERIC, SensitiveData.redact(error.message))
        }
    }

    private fun notLaunched(kind: PaymentFailureKind, reason: String?) =
        PaymentLaunchResult.NotLaunched(PaymentFailure(kind = kind, reason = reason))

    private companion object {
        const val TAG = "CieloDeeplinkGateway"
    }
}
