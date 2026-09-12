package com.example.cielocase.ui.payment

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cielocase.R
import com.example.cielocase.domain.model.PaymentRecord
import com.example.cielocase.ui.component.UiDefaults
import com.example.cielocase.ui.tickets.TicketCard
import com.example.cielocase.util.composable.typography
import com.example.cielocase.util.extensions.formatAsCurrency
import com.example.cielocase.util.extensions.formatAsDateTime

@Composable
fun PaymentReceiptScreen(
    onFinish: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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

        Text(stringResource(R.string.receipt_title), style = typography().titleLarge)
        Spacer(Modifier.size(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(purchase.eventTitle, style = typography().titleMedium)
                Spacer(Modifier.size(12.dp))
                ReceiptRow(
                    stringResource(R.string.receipt_purchase),
                    purchase.id.take(UiDefaults.SHORT_ID_LENGTH),
                )
                ReceiptRow(
                    stringResource(R.string.checkout_quantity),
                    purchase.quantity.toString(),
                )
                ReceiptRow(
                    stringResource(R.string.checkout_total),
                    purchase.totalInCents.formatAsCurrency(),
                )
                purchase.payment?.let { payment ->
                    Spacer(Modifier.size(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.size(8.dp))
                    PaymentDetails(payment)
                }
            }
        }

        if (state.tickets.isNotEmpty()) {
            Spacer(Modifier.size(24.dp))
            Text(stringResource(R.string.receipt_tickets), style = typography().titleMedium)
            Spacer(Modifier.size(8.dp))
            state.tickets.forEach { ticket ->
                TicketCard(ticket = ticket, total = state.tickets.size)
                Spacer(Modifier.size(12.dp))
            }
        }

        Spacer(Modifier.size(16.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.my_tickets))
        }
    }
}

@Composable
private fun PaymentDetails(payment: PaymentRecord) {
    payment.authCode?.let { ReceiptRow(stringResource(R.string.receipt_auth_code), it) }
    payment.cieloCode?.let { ReceiptRow(stringResource(R.string.receipt_cielo_code), it) }
    payment.productName?.let { ReceiptRow(stringResource(R.string.receipt_product), it) }
    val card = listOfNotNull(payment.brand, payment.maskedCard).joinToString(" ")
    if (card.isNotBlank()) ReceiptRow(stringResource(R.string.receipt_card), card)
    payment.terminal?.let { ReceiptRow(stringResource(R.string.receipt_terminal), it) }
    payment.paidAtEpochMillis?.let {
        ReceiptRow(stringResource(R.string.receipt_paid_at), it.formatAsDateTime())
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = typography().bodyMedium)
        Text(text = value, style = typography().bodyMedium)
    }
}
