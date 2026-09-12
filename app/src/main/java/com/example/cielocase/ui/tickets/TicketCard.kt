package com.example.cielocase.ui.tickets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cielocase.R
import com.example.cielocase.domain.model.Ticket
import com.example.cielocase.ui.component.QrCodeImage
import com.example.cielocase.ui.component.UiDefaults
import com.example.cielocase.util.composable.colorScheme
import com.example.cielocase.util.composable.typography

@Composable
fun TicketCard(ticket: Ticket, total: Int, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = ticket.eventTitle,
                style = typography().titleSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.ticket_sequence, ticket.sequence, total),
                style = typography().bodySmall,
                color = colorScheme().onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            QrCodeImage(payload = ticket.qrPayload)
            Spacer(Modifier.size(8.dp))
            Text(
                text = ticket.id.take(UiDefaults.SHORT_ID_LENGTH),
                style = typography().labelSmall,
                color = colorScheme().onSurfaceVariant,
            )
        }
    }
}
