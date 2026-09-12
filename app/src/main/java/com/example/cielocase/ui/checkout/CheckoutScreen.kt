package com.example.cielocase.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cielocase.R
import com.example.cielocase.ui.component.text
import com.example.cielocase.util.composable.colorScheme
import com.example.cielocase.util.composable.typography
import com.example.cielocase.util.extensions.formatAsCurrency
import com.example.cielocase.util.extensions.formatAsDateTime

@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onCheckoutStarted: (purchaseId: String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkoutStarted.collect(onCheckoutStarted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        val event = state.event
        if (event == null) {
            Text(
                text = stringResource(
                    if (state.isLoading) R.string.common_loading else R.string.checkout_event_not_found,
                ),
            )
            return@Column
        }

        Text(text = event.title, style = typography().titleLarge)
        Spacer(Modifier.size(4.dp))
        Text(
            text = event.startsAtEpochMillis.formatAsDateTime(),
            style = typography().bodyMedium,
            color = colorScheme().onSurfaceVariant,
        )
        Text(
            text = event.location,
            style = typography().bodyMedium,
            color = colorScheme().onSurfaceVariant,
        )

        Spacer(Modifier.size(24.dp))
        Text(text = stringResource(R.string.checkout_title), style = typography().titleMedium)
        Spacer(Modifier.size(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                QuantitySelector(
                    quantity = state.quantity,
                    onDecrement = { viewModel.onQuantityChange(-1) },
                    onIncrement = { viewModel.onQuantityChange(1) },
                )
                Spacer(Modifier.size(16.dp))
                HorizontalDivider()
                Spacer(Modifier.size(16.dp))
                SummaryRow(
                    label = stringResource(R.string.checkout_unit_price),
                    value = event.unitPriceInCents.formatAsCurrency(),
                )
                Spacer(Modifier.size(8.dp))
                SummaryRow(
                    label = stringResource(R.string.checkout_total),
                    value = state.totalInCents.formatAsCurrency(),
                    emphasized = true,
                )
            }
        }

        state.message?.let { message ->
            Spacer(Modifier.size(12.dp))
            Text(
                text = message.text(),
                color = colorScheme().error,
                style = typography().bodyMedium,
            )
        }

        Spacer(Modifier.size(24.dp))
        Button(
            onClick = viewModel::onContinue,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.checkout_continue))
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.common_back))
        }
    }
}

@Composable
private fun QuantitySelector(quantity: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = stringResource(R.string.checkout_quantity), style = typography().bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = onDecrement, enabled = quantity > 1) { Text("-") }
            Text(
                text = quantity.toString(),
                style = typography().titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            FilledIconButton(onClick = onIncrement) { Text("+") }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasized: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (emphasized) typography().titleMedium else typography().bodyMedium,
        )
        Text(
            text = value,
            style = if (emphasized) typography().titleMedium else typography().bodyMedium,
        )
    }
}
