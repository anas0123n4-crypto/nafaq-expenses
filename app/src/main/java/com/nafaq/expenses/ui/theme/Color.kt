package com.nafaq.expenses.ui.theme

import androidx.compose.ui.graphics.Color

val NafaqGold = Color(0xFFC9A227)
val NafaqGoldDeep = Color(0xFFA67C00)
val NafaqGoldSoft = Color(0xFFEBD9A6)
val NafaqGoldWash = Color(0xFFF7EFD8)
val NafaqBronze = Color(0xFF9C7A3C)
val NafaqCream = Color(0xFFF6F1E7)
val NafaqSurface = Color(0xFFFFFCF6)
val NafaqSurfaceVariant = Color(0xFFEFE7D8)
val NafaqInk = Color(0xFF241F17)
val NafaqInkSoft = Color(0xFF6B6151)
val NafaqOutline = Color(0xFFE0D6C2)
val NafaqOutlineSoft = Color(0xFFEDE4D3)
val NafaqError = Color(0xFFB3261E)
val NafaqOnGold = Color(0xFF2A2214)

fun hexToColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (error: Throwable) {
    Color(0xFF9A9186)
}
