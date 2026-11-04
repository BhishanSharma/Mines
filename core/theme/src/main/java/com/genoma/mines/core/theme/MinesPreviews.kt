package com.genoma.mines.core.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview for full screens: light, dark, and a large font scale, so
 * contrast and text-overflow regressions show up in the IDE rather than
 * on a device. Wrap the preview body in [MinesTheme] (it follows the
 * preview's night mode).
 */
@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF0A0F0E,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
annotation class MinesPreviews
