package com.simplejim.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = PaperSurface,
    primaryContainer = PaperRaised,
    onPrimaryContainer = Ink,
    secondary = Forest,
    tertiary = TapeInk,
    onSecondary = PaperSurface,
    secondaryContainer = PaleGreen,
    onSecondaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = PaperSurface,
    onSurface = Ink,
    surfaceVariant = PaperRaised,
    onSurfaceVariant = MutedInk,
    surfaceTint = Ink,
    outline = InkOutline,
    outlineVariant = PaperDivider,
    error = Burgundy,
    onError = PaperSurface,
    errorContainer = PaleRose,
    onErrorContainer = Ink,
)

private val DarkColors = darkColorScheme(
    primary = WarmChalk,
    onPrimary = Graphite,
    primaryContainer = GraphiteRaised,
    onPrimaryContainer = Chalk,
    secondary = Green,
    tertiary = TapeRed,
    onSecondary = Graphite,
    secondaryContainer = GreenContainer,
    onSecondaryContainer = Chalk,
    background = Graphite,
    onBackground = Chalk,
    surface = GraphiteSurface,
    onSurface = Chalk,
    surfaceVariant = GraphiteRaised,
    onSurfaceVariant = Steel,
    surfaceTint = WarmChalk,
    outline = GraphiteOutline,
    outlineVariant = GraphiteDivider,
    error = Rose,
    onError = RoseContainer,
    errorContainer = RoseContainer,
    onErrorContainer = Chalk,
)

private val SimpleJimTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
    ),
)

@Composable
fun SimpleJimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SimpleJimTypography,
        content = content,
    )
}
