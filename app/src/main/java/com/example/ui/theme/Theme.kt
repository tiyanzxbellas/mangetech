package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = AmoledBlack,
    secondary = PureWhite,
    onSecondary = AmoledBlack,
    tertiary = MutedGrey,
    onTertiary = PureWhite,
    background = AmoledBlack,
    onBackground = PureWhite,
    surface = CardGrey,
    onSurface = PureWhite,
    surfaceVariant = HoverGrey,
    onSurfaceVariant = PureWhite,
    outline = BorderGrey
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
