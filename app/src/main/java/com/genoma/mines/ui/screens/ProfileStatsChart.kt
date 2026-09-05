package com.genoma.mines.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class StatsPeriod {
    WEEK,
    MONTH,
    YEAR
}

private data class WinLossStats(
    val wins: Int,
    val losses: Int
) {
    val total: Int
        get() = wins + losses

    val winPercentage: Int
        get() = if (total == 0) 0 else (wins * 100) / total
}

@Composable
fun ProfileStatsChart(
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember {
        mutableStateOf(StatsPeriod.WEEK)
    }

    val stats = when (selectedPeriod) {
        StatsPeriod.WEEK -> WinLossStats(
            wins = 5,
            losses = 2
        )

        StatsPeriod.MONTH -> WinLossStats(
            wins = 18,
            losses = 7
        )

        StatsPeriod.YEAR -> WinLossStats(
            wins = 96,
            losses = 34
        )
    }


    val winColor = MaterialTheme.colorScheme.primary
    val lossColor = MaterialTheme.colorScheme.error

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PeriodOption(
                text = "Week",
                selected = selectedPeriod == StatsPeriod.WEEK,
                onClick = {
                    selectedPeriod = StatsPeriod.WEEK
                },
                modifier = Modifier.weight(1f)
            )

            PeriodOption(
                text = "Month",
                selected = selectedPeriod == StatsPeriod.MONTH,
                onClick = {
                    selectedPeriod = StatsPeriod.MONTH
                },
                modifier = Modifier.weight(1f)
            )

            PeriodOption(
                text = "Year",
                selected = selectedPeriod == StatsPeriod.YEAR,
                onClick = {
                    selectedPeriod = StatsPeriod.YEAR
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(190.dp)
            ) {
                if (stats.total > 0) {
                    val strokeWidth = 32.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )

                    val winSweep =
                        360f * stats.wins.toFloat() / stats.total.toFloat()

                    val lossSweep =
                        360f * stats.losses.toFloat() / stats.total.toFloat()

                    drawArc(
                        color = winColor,
                        startAngle = -90f,
                        sweepAngle = winSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(
                            diameter,
                            diameter
                        ),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )

                    drawArc(
                        color = lossColor,
                        startAngle = -90f + winSweep,
                        sweepAngle = lossSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(
                            diameter,
                            diameter
                        ),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${stats.total}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Games",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            ChartLegend(
                color = winColor,
                label = "Win",
                value = stats.wins
            )

            Spacer(
                modifier = Modifier.width(28.dp)
            )

            ChartLegend(
                color = lossColor,
                label = "Loss",
                value = stats.losses
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "${stats.winPercentage}% win rate",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun PeriodOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.surface
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            },
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun ChartLegend(
    color: Color,
    label: String,
    value: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )

        Spacer(
            modifier = Modifier.width(7.dp)
        )

        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}