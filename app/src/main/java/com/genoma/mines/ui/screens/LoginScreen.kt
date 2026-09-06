package com.genoma.mines.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genoma.mines.R
import com.genoma.mines.ui.theme.CountOne
import com.genoma.mines.ui.theme.CountThree
import com.genoma.mines.ui.theme.CountTwo
import com.genoma.mines.ui.theme.MinesTheme

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

        // Small vertical taglines, top-right and bottom-left, matching the
        // reference layout.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(top = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.End
        ) {
            VerticalTagline(lines = listOf("CLEAR", "MINDS", "BRIGHTER", "DAYS"))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(bottom = 24.dp, start = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            VerticalTagline(lines = listOf("THINK", "UNCOVER", "WIN"), alignEnd = false)
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(horizontal = 24.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // App logo, replacing the flag from the reference design.
            Surface(
                modifier = Modifier.size(84.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.mine_logo),
                        contentDescription = "App logo",
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "MINES",
                fontSize = 34.sp,
                letterSpacing = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SAME LOGIC. NEW CHALLENGES.",
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            BoardPreview()

            Spacer(modifier = Modifier.height(40.dp))

            // "LOG IN TO SAVE YOUR PROGRESS" divider
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "LOG IN TO SAVE YOUR PROGRESS",
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                AuthOption(
                    label = "GOOGLE",
                    onClick = onGoogleSignInClick
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Continue with Google",
                        modifier = Modifier.size(26.dp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(48.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                AuthOption(
                    label = "GUEST",
                    onClick = onGuestClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Continue as guest",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "Sign in to keep your progress across devices.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VerticalTagline(
    lines: List<String>,
    alignEnd: Boolean = true
) {
    val alignment = if (alignEnd) Alignment.End else Alignment.Start
    Column(horizontalAlignment = alignment) {
        lines.forEach { line ->
            Text(
                text = line,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(
            modifier = Modifier.width(20.dp),
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun AuthOption(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

/**
 * A small, static preview of a mine board, purely decorative — it mirrors
 * the reference design's screenshot-style board without needing real game
 * state.
 */
@Composable
private fun BoardPreview() {
    // null = empty revealed cell, -1 = flagged covered cell, 0 = covered cell
    val board = listOf(
        listOf(null, null, 1, 0, 0),
        listOf(null, null, 0, 0, 0),
        listOf(null, 2, 3, -1, 1),
        listOf(null, null, 0, 0, 0),
        listOf(null, 1, 0, 0, 0)
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            board.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { value -> PreviewCell(value = value) }
                }
            }
        }
    }
}

@Composable
private fun PreviewCell(value: Int?) {
    val isCovered = value == 0 || value == -1
    val backgroundColor = if (isCovered) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            value == -1 -> Icon(
                imageVector = Icons.Filled.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )

            value != null && value > 0 -> Text(
                text = "$value",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = numberColor(value)
            )
        }
    }
}

private fun numberColor(value: Int): Color = when (value) {
    1 -> CountOne
    2 -> CountTwo
    3 -> CountThree
    else -> Color.Unspecified
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
                            flagAt -> Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )

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

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MinesTheme {
        LoginScreen(
            onGoogleSignInClick = {},
            onGuestClick = {}
        )
    }
}