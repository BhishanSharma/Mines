package com.genoma.mines.core.theme
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private fun darkColorSchemeFor(variant: MinesThemeVariant): ColorScheme = when (variant) {
    MinesThemeVariant.CLASSIC_TEAL -> darkColorScheme(
        primary = MinesTealDark,
        onPrimary = Color(0xFF00382F),
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        secondary = MinesTealDark,
        error = MinesCoral,
        errorContainer = MinesCoralContainerDark,
        onErrorContainer = Color(0xFFFFDAD3),
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnBackground,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline,
        outlineVariant = DarkOutline
    )
    MinesThemeVariant.DEEP_OCEAN -> darkColorScheme(
        primary = OceanPrimaryDark,
        onPrimary = Color(0xFF00202C),
        primaryContainer = OceanDarkPrimaryContainer,
        onPrimaryContainer = OceanDarkOnPrimaryContainer,
        secondary = OceanPrimaryDark,
        error = MinesCoral,
        errorContainer = MinesCoralContainerDark,
        onErrorContainer = Color(0xFFFFDAD3),
        background = OceanDarkBackground,
        onBackground = OceanDarkOnBackground,
        surface = OceanDarkSurface,
        onSurface = OceanDarkOnBackground,
        surfaceVariant = OceanDarkSurfaceVariant,
        onSurfaceVariant = OceanDarkOnSurfaceVariant,
        outline = OceanDarkOutline,
        outlineVariant = OceanDarkOutline
    )
}

private fun lightColorSchemeFor(variant: MinesThemeVariant): ColorScheme = when (variant) {
    MinesThemeVariant.CLASSIC_TEAL -> lightColorScheme(
        primary = MinesTealLight,
        onPrimary = Color.White,
        primaryContainer = LightPrimaryContainer,
        onPrimaryContainer = LightOnPrimaryContainer,
        secondary = MinesTealLight,
        error = MinesCoral,
        errorContainer = MinesCoralContainerLight,
        onErrorContainer = Color(0xFF410E01),
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightSurface,
        onSurface = LightOnBackground,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        outlineVariant = LightOutline
    )
    MinesThemeVariant.DEEP_OCEAN -> lightColorScheme(
        primary = OceanPrimaryLight,
        onPrimary = Color.White,
        primaryContainer = OceanLightPrimaryContainer,
        onPrimaryContainer = OceanLightOnPrimaryContainer,
        secondary = OceanPrimaryLight,
        error = MinesCoral,
        errorContainer = MinesCoralContainerLight,
        onErrorContainer = Color(0xFF410E01),
        background = OceanLightBackground,
        onBackground = OceanLightOnBackground,
        surface = OceanLightSurface,
        onSurface = OceanLightOnBackground,
        surfaceVariant = OceanLightSurfaceVariant,
        onSurfaceVariant = OceanLightOnSurfaceVariant,
        outline = OceanLightOutline,
        outlineVariant = OceanLightOutline
    )
}

@Composable
fun MinesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeVariant: MinesThemeVariant = MinesThemeVariant.CLASSIC_TEAL,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> darkColorSchemeFor(themeVariant)
        else -> lightColorSchemeFor(themeVariant)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}