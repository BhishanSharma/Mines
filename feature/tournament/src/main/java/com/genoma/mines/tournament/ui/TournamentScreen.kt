package com.genoma.mines.tournament.ui
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import com.genoma.mines.core.ui.components.ScreenTopBar
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun TournamentScreen(
    onBackClick: () -> Unit,
    // Something useful to do while the season is on its way, so the screen
    // isn't a dead end.
    onPracticeClick: () -> Unit = onBackClick
) {
    val infiniteTransition = rememberInfiniteTransition(
        label = "snow_animation"
    )

    val snowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 9000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_offset"
    )

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {

        // Falling snow animation
        FallingSnow(
            progress = snowOffset
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {

            ScreenTopBar(
                title = "Tournament",
                onBack = onBackClick,
                horizontalInset = false
            )

            // Main winter card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(
                            color = surfaceColor,
                            shape = RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = "Winter",
                        tint = primaryColor,
                        modifier = Modifier
                            .size(92.dp)
                            .rotate(
                                degrees = sin(
                                    snowOffset * (2f * PI.toFloat())
                                ) * 6f
                            )
                    )
                }

                Spacer(
                    modifier = Modifier.height(32.dp)
                )

                Text(
                    text = "THE WINTER",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = onSurfaceColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "CUP",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryColor,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = "Our first tournament season is on its way.",
                    style = MaterialTheme.typography.titleMedium,
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Compete with other players for the top spot.\nWarm up with a practice game in the meantime.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(
                    modifier = Modifier.height(30.dp)
                )

                // Coming soon badge
                Box(
                    modifier = Modifier
                        .background(
                            color = primaryColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(
                            horizontal = 24.dp,
                            vertical = 12.dp
                        )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "COMING SOON",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                Button(onClick = onPracticeClick) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Play a practice game")
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun FallingSnow(
    progress: Float
) {
    val flakes = remember {
        List(35) {
            Snowflake(
                x = Random.nextFloat(),
                startY = Random.nextFloat(),
                size = Random.nextFloat() * 4f + 2f,
                speed = Random.nextFloat() * 0.7f + 0.3f,
                alpha = Random.nextFloat() * 0.45f + 0.25f
            )
        }
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        flakes.forEach { flake ->

            val y = (
                    flake.startY +
                            progress * flake.speed
                    ) % 1f

            val wave = sin(
                (progress * 2f * PI.toFloat()) +
                        flake.x * 8f
            ) * 18f

            val x = flake.x * size.width + wave

            val yPosition = y * size.height

            drawCircle(
                color = Color.White.copy(
                    alpha = flake.alpha
                ),
                radius = flake.size,
                center = Offset(
                    x = x,
                    y = yPosition
                )
            )
        }
    }
}

private data class Snowflake(
    val x: Float,
    val startY: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)
