package com.example.cielocase.core.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cielocase.R
import com.example.cielocase.util.composable.colorScheme
import com.example.cielocase.util.extensions.getDrawable
import com.example.cielocase.util.extensions.getString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = R.drawable.ic_ticket.getDrawable(),
                    contentDescription = R.string.app_name.getString()
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.app_name))
            }
        },
        expandedHeight = 60.dp,
        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
        modifier = Modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme().primary,
            scrolledContainerColor = colorScheme().primary,
            navigationIconContentColor = colorScheme().onPrimary,
            titleContentColor = colorScheme().onPrimary,
            actionIconContentColor = colorScheme().onPrimary,
            subtitleContentColor = colorScheme().onPrimary
        )
    )
}

@Preview
@Composable
private fun PreviewTopBar() {
    TopBar()
}
