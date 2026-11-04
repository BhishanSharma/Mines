package com.genoma.mines.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.genoma.mines.core.theme.LocalMinesThemeVariant
import com.genoma.mines.core.theme.MinesThemeVariant

/**
 * Root container for a full screen. Paints the theme background, or the
 * ambient ocean backdrop when Deep Ocean is equipped, so every screen gets
 * the equipped theme's look — not just the game board.
 *
 * [classicAmbient] adds the animated Classic Teal backdrop in the given
 * style, for screens in the play flow (Home, Celebration). Leave it null
 * for settings/info screens, which read better on a plain background;
 * Deep Ocean is always animated.
 *
 * Drop-in replacement for
 * `Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background)`.
 */
@Composable
fun ScreenSurface(
    modifier: Modifier = Modifier,
    classicAmbient: ClassicAmbientStyle? = null,
    content: @Composable () -> Unit
) {
    val themeVariant = LocalMinesThemeVariant.current
    val isOcean = themeVariant == MinesThemeVariant.DEEP_OCEAN
    val isClassicAmbient = classicAmbient != null && themeVariant == MinesThemeVariant.CLASSIC_TEAL

    Box(modifier = modifier) {
        when {
            // Screens that ask for an ambient style (Home, Celebration) get
            // the lively ocean (rays, fish, bubbles); info screens stay calm.
            isOcean -> AmbientOceanBackdrop(
                themeVariant = themeVariant,
                modifier = Modifier.fillMaxSize(),
                lively = classicAmbient != null
            )

            isClassicAmbient -> ClassicAmbientBackdrop(
                modifier = Modifier.fillMaxSize(),
                // Smart-cast: isClassicAmbient already checked it's non-null.
                style = classicAmbient
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isOcean || isClassicAmbient) Color.Transparent else MaterialTheme.colorScheme.background,
            // Set explicitly: a transparent Surface can't infer it.
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}
