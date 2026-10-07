package it.lampada.bibbia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Toni caldi, come la luce di una lampada a olio su carta.
private val Light = lightColorScheme(
    primary = Color(0xFF8A5A12),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB0),
    onPrimaryContainer = Color(0xFF2C1800),
    secondary = Color(0xFF6B5D3F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E1BB),
    onSecondaryContainer = Color(0xFF241A04),
    tertiary = Color(0xFF4A6547),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCEBC5),
    onTertiaryContainer = Color(0xFF07200A),
    background = Color(0xFFFBF6EE),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFBF6EE),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFEFE2D0),
    onSurfaceVariant = Color(0xFF4F4539),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7EFE3),
    surfaceContainer = Color(0xFFF2E9DC),
    surfaceContainerHigh = Color(0xFFECE2D4),
    surfaceContainerHighest = Color(0xFFE6DCCD),
    outline = Color(0xFF817567),
    outlineVariant = Color(0xFFD3C4B4),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFF3BC6A),
    onPrimary = Color(0xFF482A00),
    primaryContainer = Color(0xFF684000),
    onPrimaryContainer = Color(0xFFFFDDB0),
    secondary = Color(0xFFD7C5A0),
    onSecondary = Color(0xFF3A2F15),
    secondaryContainer = Color(0xFF52452A),
    onSecondaryContainer = Color(0xFFF3E1BB),
    tertiary = Color(0xFFB0CFAA),
    onTertiary = Color(0xFF1C361C),
    tertiaryContainer = Color(0xFF334D31),
    onTertiaryContainer = Color(0xFFCCEBC5),
    background = Color(0xFF17130F),
    onBackground = Color(0xFFEAE1D7),
    surface = Color(0xFF17130F),
    onSurface = Color(0xFFEAE1D7),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    surfaceContainerLowest = Color(0xFF120E0A),
    surfaceContainerLow = Color(0xFF1F1B16),
    surfaceContainer = Color(0xFF241F1A),
    surfaceContainerHigh = Color(0xFF2E2924),
    surfaceContainerHighest = Color(0xFF39342E),
    outline = Color(0xFF9C8F80),
    outlineVariant = Color(0xFF4F4539),
)

@Composable
fun LampadaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}

/** Colore dell'evidenziazione adattato al tema scuro (più tenue, per non abbagliare). */
fun highlightColor(argb: Long, dark: Boolean): Color {
    val c = Color(argb)
    return if (dark) c.copy(alpha = 0.35f) else c
}
