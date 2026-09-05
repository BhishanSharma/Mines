package com.genoma.mines.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.genoma.mines.ui.theme.MinesTheme

private object ProfileSpacing {
    val screenHorizontal = 24.dp
    val screenTop = 12.dp
    val screenBottom = 24.dp
    val small = 8.dp
    val medium = 14.dp
    val large = 22.dp
}

private data class ProfileStats(
    val gamesPlayed: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    val currentStreak: Int,
    val bestStreak: Int
) {
    val winRate: Int
        get() = if (gamesPlayed == 0) 0 else (gamesWon * 100) / gamesPlayed
}

@Composable
fun ProfileScreen(
    username: String = "Player",
    selectedAvatar: AvatarOption = AvatarOption.Default,
    onAvatarSelected: (AvatarOption) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var showAvatarPicker by remember {
        mutableStateOf(false)
    }

    val stats = ProfileStats(
        gamesPlayed = 42,
        gamesWon = 29,
        gamesLost = 13,
        currentStreak = 4,
        bestStreak = 8
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = ProfileSpacing.screenHorizontal,
                        vertical = ProfileSpacing.screenTop
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ProfileSpacing.screenHorizontal)
                    .padding(
                        bottom = WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding() + ProfileSpacing.screenBottom
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(
                    modifier = Modifier.height(ProfileSpacing.medium)
                )

                Box(
                    modifier = Modifier.size(112.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                            .border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(
                                id = selectedAvatar.drawableRes
                            ),
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Edit badge — bottom-right, overlapping the avatar's
                    // own edge, opens the avatar picker below.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.background,
                                shape = CircleShape
                            )
                            .clickable {
                                showAvatarPicker = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Change profile picture",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.medium)
                )

                Text(
                    text = username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.large)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        ProfileSpacing.small
                    )
                ) {
                    ProfileStatCard(
                        value = stats.gamesPlayed.toString(),
                        label = "Played",
                        modifier = Modifier.weight(1f)
                    )

                    ProfileStatCard(
                        value = stats.gamesWon.toString(),
                        label = "Won",
                        modifier = Modifier.weight(1f)
                    )

                    ProfileStatCard(
                        value = stats.gamesLost.toString(),
                        label = "Lost",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.small)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        ProfileSpacing.small
                    )
                ) {
                    ProfileStatCard(
                        value = "${stats.winRate}%",
                        label = "Win rate",
                        modifier = Modifier.weight(1f)
                    )

                    ProfileStatCard(
                        value = stats.currentStreak.toString(),
                        label = "Streak",
                        modifier = Modifier.weight(1f)
                    )

                    ProfileStatCard(
                        value = stats.bestStreak.toString(),
                        label = "Best streak",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.large)
                )

                Text(
                    text = "Performance",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.medium)
                )

                ProfileStatsChart()

                Spacer(
                    modifier = Modifier.height(ProfileSpacing.medium)
                )
            }
        }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentSelection = selectedAvatar,
            onConfirm = { picked ->
                onAvatarSelected(picked)
                showAvatarPicker = false
            },
            onDismiss = {
                showAvatarPicker = false
            }
        )
    }
}

@Composable
private fun AvatarPickerDialog(
    currentSelection: AvatarOption,
    onConfirm: (AvatarOption) -> Unit,
    onDismiss: () -> Unit
) {
    // Local, unconfirmed choice — nothing is persisted (and the caller
    // isn't notified) until "Confirm" is tapped.
    var pendingSelection by remember {
        mutableStateOf(currentSelection)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Choose your avatar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                // 2 rows x 3 columns, as requested — plain Rows rather
                // than a lazy grid since the set is small and fixed.
                AvatarOption.entries
                    .chunked(3)
                    .forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowOptions.forEach { option ->
                                AvatarGridItem(
                                    option = option,
                                    selected = option == pendingSelection,
                                    onClick = {
                                        pendingSelection = option
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )
                    }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Button(
                        onClick = {
                            onConfirm(pendingSelection)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarGridItem(
    option: AvatarOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = option.drawableRes),
            contentDescription = option.contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ProfileStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 14.dp,
                    horizontal = 8.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun ProfileScreenPreview() {
    MinesTheme {
        ProfileScreen(
            username = "Alex",
            onBack = {}
        )
    }
}