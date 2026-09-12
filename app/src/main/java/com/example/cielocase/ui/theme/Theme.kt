package com.example.cielocase.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = CieloBlue40,
    onPrimary = Color.White,
    primaryContainer = CieloBlueContainerLight,
    onPrimaryContainer = CieloBlueOnContainerLight,
    secondary = CieloBlue40,
    onSecondary = Color.White,
    secondaryContainer = CieloBlueContainerLight,
    onSecondaryContainer = CieloBlueOnContainerLight,
    tertiary = CieloTeal40,
    onTertiary = Color.White,
    background = NeutralLightBackground,
    onBackground = NeutralLightOnSurface,
    surface = NeutralLightBackground,
    onSurface = NeutralLightOnSurface,
    surfaceVariant = NeutralLightSurfaceVariant,
    onSurfaceVariant = NeutralLightOnSurfaceVariant,
    surfaceContainerLowest = NeutralLightContainerLowest,
    surfaceContainerLow = NeutralLightContainerLow,
    surfaceContainer = NeutralLightContainer,
    surfaceContainerHigh = NeutralLightContainerHigh,
    surfaceContainerHighest = NeutralLightContainerHighest,
    outline = NeutralLightOutline,
    outlineVariant = NeutralLightSurfaceVariant,
    error = ErrorLight,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = CieloBlue80,
    onPrimary = CieloBlueOnPrimaryDark,
    primaryContainer = CieloBlueContainerDark,
    onPrimaryContainer = CieloBlueContainerLight,
    secondary = CieloBlue80,
    onSecondary = CieloBlueOnPrimaryDark,
    secondaryContainer = CieloBlueContainerDark,
    onSecondaryContainer = CieloBlueContainerLight,
    tertiary = CieloTeal80,
    onTertiary = CieloBlueOnPrimaryDark,
    background = NeutralDarkBackground,
    onBackground = NeutralDarkOnSurface,
    surface = NeutralDarkBackground,
    onSurface = NeutralDarkOnSurface,
    surfaceVariant = NeutralDarkSurfaceVariant,
    onSurfaceVariant = NeutralDarkOnSurfaceVariant,
    surfaceContainerLowest = NeutralDarkContainerLowest,
    surfaceContainerLow = NeutralDarkContainerLow,
    surfaceContainer = NeutralDarkContainer,
    surfaceContainerHigh = NeutralDarkContainerHigh,
    surfaceContainerHighest = NeutralDarkContainerHighest,
    outline = NeutralDarkOutline,
    outlineVariant = NeutralDarkSurfaceVariant,
    error = ErrorDark,
    onError = ErrorOnDark,
)

/**
 * Dynamic color is disabled by default: this is a branded checkout flow, so the palette must
 * stay the Cielo blue instead of following the device wallpaper.
 */
@Composable
fun CieloCaseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
