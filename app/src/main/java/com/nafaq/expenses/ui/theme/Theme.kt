package com.nafaq.expenses.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NafaqColors = lightColorScheme(
    primary = NafaqGold,
    onPrimary = NafaqOnGold,
    primaryContainer = NafaqGoldSoft,
    onPrimaryContainer = NafaqInk,
    inversePrimary = NafaqGoldDeep,
    secondary = NafaqBronze,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E4CE),
    onSecondaryContainer = Color(0xFF4A3A10),
    tertiary = NafaqGoldDeep,
    onTertiary = Color.White,
    tertiaryContainer = NafaqGoldWash,
    onTertiaryContainer = NafaqInk,
    background = NafaqCream,
    onBackground = NafaqInk,
    surface = NafaqSurface,
    onSurface = NafaqInk,
    surfaceVariant = NafaqSurfaceVariant,
    onSurfaceVariant = NafaqInkSoft,
    surfaceTint = NafaqGold,
    inverseSurface = NafaqInk,
    inverseOnSurface = NafaqCream,
    outline = NafaqOutline,
    outlineVariant = NafaqOutlineSoft,
    error = NafaqError,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    scrim = Color(0x66000000)
)

private val NafaqShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun NafaqTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NafaqColors,
        typography = NafaqTypography,
        shapes = NafaqShapes,
        content = content
    )
}
