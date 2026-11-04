package com.genoma.mines.core.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user has turned animations off (Settings > Accessibility >
 * Remove animations, which sets the animator duration scale to 0).
 *
 * Compose's own animation APIs already honor that setting, but hand-rolled
 * `withFrameNanos` loops (the ambient backdrops) don't — they must check
 * this and hold a still frame instead.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}

/**
 * A still moment to draw when motion is reduced: far enough in that
 * phase-offset elements (tiles, bubbles) are spread across the screen.
 */
const val STILL_FRAME_MS = 7_000L
