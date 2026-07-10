package dev.tactos.core.design

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

/**
 * The static tactos palette — used below Android 12, or when the user later
 * disables dynamic color. A calm teal ("tact") with a warm amber accent that
 * echoes the clipboard highlight color.
 */
private val TactosTeal = Color(0xFF00696D)
private val TactosTealLight = Color(0xFF9CF1F5)
private val TactosAmber = Color(0xFF9A4522)
private val TactosAmberLight = Color(0xFFFFDBCC)

private val LightColors = lightColorScheme(
    primary = TactosTeal,
    onPrimary = Color.White,
    primaryContainer = TactosTealLight,
    onPrimaryContainer = Color(0xFF002021),
    secondary = Color(0xFF4A6365),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8E9),
    onSecondaryContainer = Color(0xFF051F21),
    tertiary = TactosAmber,
    onTertiary = Color.White,
    tertiaryContainer = TactosAmberLight,
    onTertiaryContainer = Color(0xFF380D00),
    background = Color(0xFFFAFDFC),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFAFDFC),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE4E5),
    onSurfaceVariant = Color(0xFF3F4949),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D4D9),
    onPrimary = Color(0xFF003739),
    primaryContainer = Color(0xFF004F52),
    onPrimaryContainer = TactosTealLight,
    secondary = Color(0xFFB1CBCD),
    onSecondary = Color(0xFF1B3436),
    secondaryContainer = Color(0xFF324B4D),
    onSecondaryContainer = Color(0xFFCCE8E9),
    tertiary = Color(0xFFFFB693),
    onTertiary = Color(0xFF5B1B00),
    tertiaryContainer = Color(0xFF7B2F0C),
    onTertiaryContainer = TactosAmberLight,
    background = Color(0xFF191C1C),
    onBackground = Color(0xFFE0E3E2),
    surface = Color(0xFF191C1C),
    onSurface = Color(0xFFE0E3E2),
    surfaceVariant = Color(0xFF3F4949),
    onSurfaceVariant = Color(0xFFBEC8C9),
)

/**
 * tactos Material 3 theme. Uses Material You dynamic color on Android 12+
 * ([dynamicColor] permitting) and the static tactos palette otherwise —
 * light and dark in both cases.
 */
@Composable
fun TactosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
