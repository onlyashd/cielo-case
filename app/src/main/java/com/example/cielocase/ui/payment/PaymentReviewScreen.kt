package com.example.cielocase.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cielocase.R
import com.example.cielocase.data.payment.simulator.SimulatedCallbackFactory
import com.example.cielocase.domain.payment.PaymentRequest
import com.example.cielocase.ui.component.UiDefaults
import com.example.cielocase.ui.component.text
import com.example.cielocase.util.composable.colorScheme
import com.example.cielocase.util.composable.typography
import com.example.cielocase.util.extensions.formatAsCurrency

@Composable
fun PaymentReviewScreen(
    onBack: () -> Unit,
    onPaymentStarted: (purchaseId: String) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.paymentStarted.collect(onPaymentStarted) }

    val purchase = state.purchase
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (purchase == null) {
            Text(stringResource(R.string.common_loading))
            return@Column
        }

        Text(stringResource(R.string.review_title), style = typography().titleLarge)
        Spacer(Modifier.size(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(purchase.eventTitle, style = typography().titleMedium)
                Spacer(Modifier.size(12.dp))
                ReviewRow(
                    stringResource(R.string.review_order),
                    purchase.id.take(UiDefaults.SHORT_ID_LENGTH),
                )
                ReviewRow(
                    stringResource(R.string.checkout_quantity),
                    purchase.quantity.toString(),
                )
                ReviewRow(
                    stringResource(R.string.checkout_unit_price),
                    purchase.unitPriceInCents.formatAsCurrency(),
                )
                Spacer(Modifier.size(8.dp))
                HorizontalDivider()
                Spacer(Modifier.size(8.dp))
                ReviewRow(
                    label = stringResource(R.string.checkout_total),
                    value = purchase.totalInCents.formatAsCurrency(),
                    emphasized = true,
                )
            }
        }

        Spacer(Modifier.size(24.dp))
        Text(stringResource(R.string.review_payment_method), style = typography().titleMedium)
        Spacer(Modifier.size(8.dp))
        PaymentMethodPicker(
            selected = state.paymentCode,
            onSelect = viewModel::onPaymentCodeSelected,
        )

        state.message?.let { message ->
            Spacer(Modifier.size(16.dp))
            Text(
                text = message.text(),
                color = colorScheme().error,
                style = typography().bodyMedium,
            )
        }

        Spacer(Modifier.size(24.dp))
        Button(
            onClick = viewModel::onPay,
            enabled = !state.isLaunching && !purchase.status.isTerminal,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (state.isLaunching) R.string.review_paying else R.string.review_pay,
                ),
            )
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.common_back))
        }

        if (state.isSimulatorEnabled) {
            Spacer(Modifier.size(24.dp))
            SimulatorSection(onSimulate = viewModel::onSimulate)
        }
    }
}

@Composable
private fun PaymentMethodPicker(selected: String, onSelect: (String) -> Unit) {
    val methods = listOf(
        PaymentRequest.PAYMENT_CODE_CREDIT_UP_FRONT to R.string.review_credit,
        PaymentRequest.PAYMENT_CODE_DEBIT_UP_FRONT to R.string.review_debit,
        PaymentRequest.PAYMENT_CODE_PIX to R.string.review_pix,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        methods.forEach { (code, labelRes) ->
            FilterChip(
                selected = selected == code,
                onClick = { onSelect(code) },
                label = { Text(stringResource(labelRes)) },
            )
        }
    }
}

@Composable
private fun SimulatorSection(onSimulate: (SimulatedCallbackFactory.Outcome) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.review_simulator_title), style = typography().titleSmall)
            Spacer(Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.review_simulator_description),
                style = typography().bodySmall,
                color = colorScheme().onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSimulate(SimulatedCallbackFactory.Outcome.APPROVED) }) {
                    Text(stringResource(R.string.review_simulate_approved))
                }
                OutlinedButton(onClick = { onSimulate(SimulatedCallbackFactory.Outcome.DENIED) }) {
                    Text(stringResource(R.string.review_simulate_denied))
                }
                OutlinedButton(onClick = { onSimulate(SimulatedCallbackFactory.Outcome.CANCELLED) }) {
                    Text(stringResource(R.string.review_simulate_cancelled))
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String, emphasized: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val style = if (emphasized) typography().titleMedium else typography().bodyMedium
        Text(text = label, style = style)
        Text(text = value, style = style)
    }
}
