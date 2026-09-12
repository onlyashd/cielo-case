package com.example.cielocase.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cielocase.R
import com.example.cielocase.domain.model.Event
import com.example.cielocase.util.composable.colorScheme
import com.example.cielocase.util.composable.typography
import com.example.cielocase.util.extensions.formatAsCurrency
import com.example.cielocase.util.extensions.formatAsDateTime
import com.example.cielocase.util.extensions.getDrawable

@Composable
fun EventsScreen(
    onEventSelected: (eventId: String) -> Unit,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.isLoading -> CenteredContent { CircularProgressIndicator() }
        state.events.isEmpty() -> CenteredContent { Text(stringResource(R.string.events_empty)) }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.events, key = { it.id }) { event ->
                EventCard(event = event, onSelect = { onEventSelected(event.id) })
            }
        }
    }
}

@Composable
private fun EventCard(event: Event, onSelect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.title, style = typography().titleMedium)
            Spacer(Modifier.size(4.dp))
            Text(
                text = event.description,
                style = typography().bodySmall,
                color = colorScheme().onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            IconLabel(R.drawable.ic_calendar, event.startsAtEpochMillis.formatAsDateTime())
            IconLabel(R.drawable.ic_location, event.location)
            IconLabel(R.drawable.ic_money, event.unitPriceInCents.formatAsCurrency())
            Spacer(Modifier.size(12.dp))
            AvailabilityChip(event)
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = onSelect,
                enabled = event.isPurchasable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.events_buy))
            }
        }
    }
}

@Composable
private fun AvailabilityChip(event: Event) {
    val label = when {
        !event.salesOpen -> stringResource(R.string.events_sales_closed)
        event.isSoldOut -> stringResource(R.string.events_sold_out)
        else -> stringResource(R.string.events_available_tickets, event.availableTickets)
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = if (event.isPurchasable) {
                colorScheme().onSurface
            } else {
                colorScheme().error
            },
        ),
    )
}

@Composable
private fun IconLabel(iconRes: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = iconRes.getDrawable(),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colorScheme().onSurfaceVariant,
        )
        Spacer(Modifier.size(8.dp))
        Text(text = text, style = typography().bodyMedium)
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
