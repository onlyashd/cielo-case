package com.example.cielocase.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.cielocase.core.composable.NavBar
import com.example.cielocase.core.composable.NavigationStack
import com.example.cielocase.core.composable.TopBar
import com.example.cielocase.ui.theme.CieloCaseTheme
import com.example.cielocase.util.composable.colorScheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CieloCaseTheme {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeContent,
                    topBar = { TopBar() },
                    bottomBar = { NavBar() },
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.padding(innerPadding),
                        color = colorScheme().background
                    ) {
                        NavigationStack()
                    }
                }
            }
        }
    }
}
