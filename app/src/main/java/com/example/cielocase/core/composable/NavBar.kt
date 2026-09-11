package com.example.cielocase.core.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cielocase.R
import com.example.cielocase.core.MainViewModel
import com.example.cielocase.core.Screen
import com.example.cielocase.core.model.NavDrawerItem
import com.example.cielocase.util.extensions.getActivity
import com.example.cielocase.util.extensions.getDrawable
import com.example.cielocase.util.extensions.getString
import com.example.cielocase.util.extensions.popAllTo

@Composable
fun NavBar(viewModel: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val items = listOf(
        NavDrawerItem(Screen.Dashboard, R.drawable.ic_home, R.string.home),
        NavDrawerItem(Screen.Events, R.drawable.ic_calendar, R.string.events),
        NavDrawerItem(Screen.Tickets, R.drawable.ic_ticket, R.string.my_tickets),
    )
    val selectedItem = remember { mutableStateOf(items[0]) }

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(item.icon.getDrawable(), item.title.getString()) },
                label = { Text(item.title.getString()) },
                selected = item == selectedItem.value,
                onClick = {
                    selectedItem.value = item
                    context.getActivity()?.let { activity ->
                        viewModel.navController.observe(activity) { controller ->
                            controller.popAllTo(item.screen.route)
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewNavBar() {
    NavBar()
}