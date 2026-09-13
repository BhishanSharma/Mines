package com.genoma.mines.settings.ui
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genoma.mines.core.theme.MinesTheme


enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromDarkThemeFlag(darkTheme: Boolean?): ThemePreference {
            return when (darkTheme) {
                null -> SYSTEM
                false -> LIGHT
                true -> DARK
            }
        }
    }
}


fun ThemePreference.toDarkThemeFlag(): Boolean? {
    return when (this) {
        ThemePreference.SYSTEM -> null
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
}

private data class ThemeOptionSpec(
    val preference: ThemePreference,
    val icon: ImageVector,
    val label: String,
    val subtitle: String
)

private val THEME_OPTIONS = listOf(
    ThemeOptionSpec(
        preference = ThemePreference.SYSTEM,
        icon = Icons.Filled.BrightnessAuto,
        label = "Sync with system",
        subtitle = "Matches your device's light/dark setting"
    ),
    ThemeOptionSpec(
        preference = ThemePreference.LIGHT,
        icon = Icons.Filled.LightMode,
        label = "Light",
        subtitle = "Always use the light theme"
    ),
    ThemeOptionSpec(
        preference = ThemePreference.DARK,
        icon = Icons.Filled.DarkMode,
        label = "Dark",
        subtitle = "Always use the dark theme"
    )
)


@Composable
fun ThemeToggle(
    selected: ThemePreference,
    onSelect: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        THEME_OPTIONS.forEach { option ->
            ThemeOptionRow(
                spec = option,
                selected = option.preference == selected,
                onClick = {
                    onSelect(option.preference)
                }
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(
    spec: ThemeOptionSpec,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 2.dp else 0.dp
        ),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = spec.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = spec.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            if (selected) {
                val isSync = spec.preference == ThemePreference.SYSTEM

                Icon(
                    imageVector = if (isSync) {
                        Icons.Filled.Sync
                    } else {
                        Icons.Filled.Check
                    },
                    contentDescription = if (isSync) {
                        "Synced with system"
                    } else {
                        "Selected"
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemeTogglePreview() {
    var selected by remember {
        mutableStateOf(ThemePreference.SYSTEM)
    }

    MinesTheme {
        ThemeToggle(
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}