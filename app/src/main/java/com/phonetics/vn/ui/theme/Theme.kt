package com.phonetics.vn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Vietnamese flag palette: red #DA251D (field) and #FFDE00 (star),
// darkened for night-reading while keeping the flag's identity.
private val FlagGold = Color(0xFFFFD54F)
private val FlagGoldBright = Color(0xFFFFDE00)

private val PhoneticsDark = darkColorScheme(
    primary = FlagGold,
    onPrimary = Color(0xFF4A0000),
    primaryContainer = Color(0xFF8B1A13),
    onPrimaryContainer = Color(0xFFFFE9B0),
    secondary = FlagGoldBright,
    onSecondary = Color(0xFF3E0000),
    secondaryContainer = Color(0xFF6E1208),
    onSecondaryContainer = Color(0xFFFFDDB0),
    background = Color(0xFF1B0704),
    onBackground = Color(0xFFFFEFE0),
    surface = Color(0xFF26100A),
    onSurface = Color(0xFFFFEFE0),
    surfaceVariant = Color(0xFF3B1710),
    onSurfaceVariant = Color(0xFFF0C9A8),
    outline = Color(0xFFA8502A),
    outlineVariant = Color(0xFF481406),
    error = Color(0xFFFFB4A2),
    onError = Color(0xFF7A1200),
)

/**
 * Phở-netics theme. Deliberately dark by design (Vietnamese-flag palette),
 * per product preference.
 */
@Composable
fun PhoneticsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PhoneticsDark, content = content)
}
