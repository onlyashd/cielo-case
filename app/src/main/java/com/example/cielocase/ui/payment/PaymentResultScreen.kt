package com.example.cielocase.ui.payment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cielocase.R
import com.example.cielocase.domain.model.Purchase
import com.example.cielocase.domain.model.PurchaseStatus
import com.example.cielocase.ui.component.text
import com.example.cielocase.util.composable.colorScheme
import com.example.cielocase.util.composable.typography
import com.example.cielocase.util.extensions.formatAsCurrency
import com.example.cielocase.util.extensions.getDrawable

@Composable
fun PaymentResultScreen(
    onShowReceipt: (purchaseId: String) -> Unit,
    onRetryStarted: (purchaseId: String) -> Unit,
    onFinish: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.retryStarted.collect(onRetryStarted) }

    val purchase = state.purchase
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (purchase == null) {
            Text(stringResource(R.string.common_loading))
            return@Column
        }

        Spacer(Modifier.size(32.dp))
        StatusHeader(purchase)
        Spacer(Modifier.size(16.dp))
        Text(text = purchase.eventTitle, style = typography().titleMedium)
        Text(
            text = "${purchase.quantity}x - ${purchase.totalInCents.formatAsCurrency()}",
            style = typography().bodyMedium,
            color = colorScheme().onSurfaceVariant,
        )

        purchase.failure?.let { failure ->
            Spacer(Modifier.size(16.dp))
            Text(
                text = failure.toUiMessage().text(),
                style = typography().bodyMedium,
                textAlign = TextAlign.Center,
            )
            failure.detail()?.let { detail ->
                Text(
                    text = detail.text(),
                    style = typography().bodySmall,
                    color = colorScheme().onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        state.message?.let { message ->
            Spacer(Modifier.size(16.dp))
            Text(text = message.text(), color = colorScheme().error)
        }

        Spacer(Modifier.size(32.dp))
        when (purchase.status) {
            PurchaseStatus.PENDING -> Unit

            PurchaseStatus.APPROVED -> Button(
                onClick = { onShowReceipt(purchase.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.result_see_receipt))
            }

            PurchaseStatus.DENIED,
            PurchaseStatus.CANCELLED,
            PurchaseStatus.FAILED,
            -> Button(onClick = viewModel::onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.result_try_again))
            }
        }

        Spacer(Modifier.size(8.dp))
        OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.result_back_to_events))
        }
    }
}

@Composable
private fun StatusHeader(purchase: Purchase) {
    if (purchase.status == PurchaseStatus.PENDING) {
        CircularProgressIndicator()
        Spacer(Modifier.size(16.dp))
        Text(stringResource(R.string.result_waiting), style = typography().titleLarge)
        Text(
            text = stringResource(R.string.result_waiting_description),
            style = typography().bodyMedium,
            color = colorScheme().onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        return
    }

    val (titleRes, iconRes, tint) = when (purchase.status) {
        PurchaseStatus.APPROVED -> Triple(
            R.string.result_approved,
            R.drawable.ic_bookmark_check,
            colorScheme().primary,
        )
        PurchaseStatus.DENIED -> Triple(
            R.string.result_denied,
            R.drawable.ic_currency,
            colorScheme().error,
        )
        PurchaseStatus.CANCELLED -> Triple(
            R.string.result_cancelled,
            R.drawable.ic_currency,
            colorScheme().error,
        )
        else -> Triple(R.string.result_failed, R.drawable.ic_currency, colorScheme().error)
    }

    Icon(
        painter = iconRes.getDrawable(),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(48.dp),
    )
    Spacer(Modifier.size(12.dp))
    Text(text = stringResource(titleRes), style = typography().titleLarge, color = tint)
}
