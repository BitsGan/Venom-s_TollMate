package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = HighwayBlueLight,
    onPrimary = Slate900,
    primaryContainer = HighwayBlue,
    onPrimaryContainer = Color.White,
    secondary = FastlaneTealLight,
    onSecondary = Slate900,
    tertiary = TollAmberLight,
    background = Slate900,
    surface = Slate800,
    onBackground = Slate100,
    onSurface = Slate100,
    error = EmergencyRed
)

private val LightColorScheme = lightColorScheme(
    primary = HighwayBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = FastlaneTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = TollAmber,
    onTertiary = Color.White,
    background = Slate50,
    surface = Color.White,
    onBackground = Slate900,
    onSurface = Slate900,
    error = EmergencyRed
)

@Composable
fun TollMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to preserve our custom Highway branding
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

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
