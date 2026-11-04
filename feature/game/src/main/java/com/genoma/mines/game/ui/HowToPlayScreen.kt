package com.genoma.mines.game.ui
import com.genoma.mines.core.ui.components.BoardMine
import com.genoma.mines.core.ui.components.BoardFlag
import com.genoma.mines.core.theme.MinesPreviews
import com.genoma.mines.core.ui.components.ScreenSurface
import com.genoma.mines.core.ui.components.ScreenTopBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genoma.mines.core.theme.HeartRed
import com.genoma.mines.core.theme.MinesTheme

private object HowToPlaySpacing {
    val screenHorizontal = 16.dp
    val screenBottom = 24.dp
    val sectionGap = 24.dp
    val rowGap = 10.dp
}

private data class HowToPlayStep(
    val title: String,
    val description: String,
    // Either a stock icon, or the board's own art (flag, mine) so the
    // guide shows exactly what the player will see on the board.
    val icon: ImageVector? = null,
    val art: (@Composable () -> Unit)? = null
)

private val rules = listOf(
    HowToPlayStep(
        icon = Icons.Filled.TouchApp,
        title = "Dig to reveal",
        description = "Tap a hidden cell to reveal what's underneath. Your first tap is always safe."
    ),
    HowToPlayStep(
        icon = Icons.Filled.Numbers,
        title = "Read the numbers",
        description = "A number tells you how many mines are in the 8 cells touching it."
    ),
    HowToPlayStep(
        art = { BoardFlag(modifier = Modifier.size(26.dp)) },
        title = "Flag the mines",
        description = "Mark cells you think hide a mine. Flagged cells can't be dug by accident."
    ),
    HowToPlayStep(
        art = { BoardMine(modifier = Modifier.size(28.dp)) },
        title = "Clear the board",
        description = "Dig up a mine and the game ends. Reveal every safe cell to win."
    )
)

private val controls = listOf(
    HowToPlayStep(
        icon = Icons.Filled.SwapHoriz,
        title = "Dig / Flag switch",
        description = "Use the switch above the board to choose what a tap does: dig a cell, or place a flag."
    ),
    HowToPlayStep(
        art = { BoardFlag(modifier = Modifier.size(26.dp)) },
        title = "Long-press to flag",
        description = "Long-press any hidden cell to flag or unflag it, whichever mode you're in."
    ),
    HowToPlayStep(
        icon = Icons.Filled.DoneAll,
        title = "Clear around a number",
        description = "Once a number has that many flags next to it, tap the number to dig all its other neighbors at once."
    )
)

private val heartsAndScoring = listOf(
    HowToPlayStep(
        icon = Icons.Filled.Favorite,
        title = "Each game costs a heart",
        description = "Win and you get the heart back. You hold up to 5, and one refills every hour."
    ),
    HowToPlayStep(
        icon = Icons.Filled.Timer,
        title = "Faster wins score more",
        description = "Harder boards and quicker clears earn more points. Losing early costs points; getting past halfway earns a few."
    )
)

@Composable
fun HowToPlayScreen(
    onBack: () -> Unit
) {
    ScreenSurface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            ScreenTopBar(
                title = "How to play",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HowToPlaySpacing.screenHorizontal)
                    .padding(top = 8.dp, bottom = HowToPlaySpacing.screenBottom)
            ) {
                HowToPlaySection(title = "RULES", steps = rules)

                Spacer(modifier = Modifier.height(HowToPlaySpacing.sectionGap))

                HowToPlaySection(title = "CONTROLS", steps = controls)

                Spacer(modifier = Modifier.height(HowToPlaySpacing.sectionGap))

                HowToPlaySection(title = "HEARTS & SCORING", steps = heartsAndScoring)
            }
        }
    }
}

@Composable
private fun HowToPlaySection(
    title: String,
    steps: List<HowToPlayStep>
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() }
    )

    Spacer(modifier = Modifier.height(HowToPlaySpacing.rowGap))

    Column(
        verticalArrangement = Arrangement.spacedBy(HowToPlaySpacing.rowGap)
    ) {
        steps.forEach { step ->
            HowToPlayRow(step = step)
        }
    }
}

@Composable
private fun HowToPlayRow(
    step: HowToPlayStep
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                when {
                    step.art != null -> step.art.invoke()
                    step.icon != null -> Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = if (step.icon == Icons.Filled.Favorite) {
                            HeartRed
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@MinesPreviews
@Composable
private fun HowToPlayScreenPreview() {
    MinesTheme {
        HowToPlayScreen(onBack = {})
    }
}
