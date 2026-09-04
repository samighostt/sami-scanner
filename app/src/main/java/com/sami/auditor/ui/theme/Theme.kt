package com.sami.auditor.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SamiGold,
    onPrimary = SamiBackground,
    primaryContainer = SamiGoldDark,
    onPrimaryContainer = SamiText,
    secondary = SamiGoldVariant,
    onSecondary = SamiBackground,
    background = SamiBackground,
    onBackground = SamiText,
    surface = SamiCardBg,
    onSurface = SamiText,
    surfaceVariant = SamiCardElevated,
    onSurfaceVariant = SamiMuted,
    outline = SamiCardBorder,
    error = SamiBad,
    onError = SamiText
)

@Composable
fun SAMIAuditorTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SamiBackground.toArgb()
            window.navigationBarColor = SamiBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
