package com.example.cielocase.core.composable

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.cielocase.R
import com.example.cielocase.core.Screen
import com.example.cielocase.core.model.NavDrawerItem
import com.example.cielocase.util.composable.colorScheme
import com.example.cielocase.util.extensions.getDrawable
import com.example.cielocase.util.extensions.getString

@Composable
fun NavBar(navController: NavHostController) {
    val items = listOf(
        NavDrawerItem(Screen.Events, R.drawable.ic_stadium, R.string.events),
        NavDrawerItem(Screen.Tickets, R.drawable.ic_ticket, R.string.my_tickets),
    )
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    NavigationBar(
        containerColor = colorScheme().primary,
        contentColor = colorScheme().onPrimary,
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon.getDrawable(), item.title.getString()) },
                label = { Text(item.title.getString()) },
                selected = currentRoute == item.screen.route,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colorScheme().primary,
                    selectedTextColor = colorScheme().onPrimary,
                    indicatorColor = colorScheme().onPrimary,
                    unselectedIconColor = colorScheme().onPrimary.copy(alpha = NavBarDefaults.UNSELECTED_ALPHA),
                    unselectedTextColor = colorScheme().onPrimary.copy(alpha = NavBarDefaults.UNSELECTED_ALPHA),
                ),
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(Screen.Events.route) {
                            inclusive = item.screen == Screen.Events
                        }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}

private object NavBarDefaults {
    const val UNSELECTED_ALPHA = 0.7f
}
