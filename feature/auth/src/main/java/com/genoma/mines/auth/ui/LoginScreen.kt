package com.genoma.mines.auth.ui
import com.genoma.mines.core.theme.MinesPreviews
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genoma.mines.auth.R
import com.genoma.mines.core.theme.CountOne
import com.genoma.mines.core.theme.LocalMinesThemeVariant
import com.genoma.mines.core.theme.MinesTheme
import com.genoma.mines.core.theme.MinesThemeVariant
import com.genoma.mines.core.ui.R as CoreUiR
import com.genoma.mines.core.ui.components.BoardFlag

/*
 * One message, one brand: the same hero art and tagline as Home. (This
 * screen used to carry three different taglines, two stacked-word
 * decorations, separate logo art, and a second demo board.)
 */
@Composable
fun LoginScreen(
    onGoogleSignInClick: () -> Unit,
    onGuestClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative mine-tile grids tucked into two corners, echoing the
        // board without competing with the actual content.
        CornerMineGrid(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-28).dp, y = (-14).dp)
        )
        CornerMineGrid(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 28.dp, y = 14.dp),
            flagAt = 0 to 2,
            numberAt = 1 to 0
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Same hero art as Home, so the first screen already looks like
            // the game the player is about to play.
            val banner = if (LocalMinesThemeVariant.current == MinesThemeVariant.DEEP_OCEAN) {
                CoreUiR.drawable.home_banner_ocean
            } else {
                CoreUiR.drawable.home_banner
            }
            Image(
                painter = painterResource(id = banner),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .widthIn(max = 300.dp)
                    .aspectRatio(1.1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Mines",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Think. Uncover. Win.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            GoogleSignInButton(
                onClick = onGoogleSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign in to save your progress across devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(onClick = onGuestClick) {
                Text(
                    text = "Continue as guest",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Say what guests give up, up front.
            Text(
                text = "Guest progress is saved on this device only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * The standard "Sign in with Google" button, per Google's branding
 * guidelines for Android: G logo plus that exact label, on Google's
 * light or dark neutral surface with its outline color. (This used to be
 * an unlabeled icon tile, which the guidelines don't allow.)
 */
@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val container = if (isDark) Color(0xFF131314) else Color.White
    val content = if (isDark) Color(0xFFE3E3E3) else Color(0xFF1F1F1F)
    val outline = if (isDark) Color(0xFF8E918F) else Color(0xFF747775)

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_google_logo),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Sign in with Google",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CornerMineGrid(
    modifier: Modifier = Modifier,
    rows: Int = 3,
    columns: Int = 3,
    flagAt: Pair<Int, Int> = 1 to 1,
    numberAt: Pair<Int, Int> = 0 to 0
) {
    Column(
        modifier = modifier.rotate(-14f),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(rows) { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(columns) { c ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (r to c) {
                            // Same flag art as the game board.
                            flagAt -> BoardFlag(modifier = Modifier.size(20.dp))

                            numberAt -> Text(
                                text = "1",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CountOne
                            )
                        }
                    }
                }
            }
        }
    }
}

@MinesPreviews
@Composable
private fun LoginScreenPreview() {
    MinesTheme {
        LoginScreen(
            onGoogleSignInClick = {},
            onGuestClick = {}
        )
    }
}
