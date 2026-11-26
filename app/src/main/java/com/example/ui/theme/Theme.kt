package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SertaoDarkScheme = darkColorScheme(
    primary = EmberOrange,
    onPrimary = SertaoObsidian,
    primaryContainer = SertaoSurfaceVariant,
    onPrimaryContainer = SolarGold,
    secondary = SolarGold,
    onSecondary = SertaoObsidian,
    secondaryContainer = SertaoSurface,
    onSecondaryContainer = WarmParchment,
    tertiary = StyleMoonColor,
    onTertiary = WarmParchment,
    background = SertaoObsidian,
    onBackground = WarmParchment,
    surface = SertaoCharcoal,
    onSurface = WarmParchment,
    surfaceVariant = SertaoSurface,
    onSurfaceVariant = MutedSand,
    error = BossCrimson,
    onError = WarmParchment
)

private val SertaoShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun CacadoresSertaoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SertaoDarkScheme,
        typography = Typography,
        shapes = SertaoShapes,
        content = content
    )
}
