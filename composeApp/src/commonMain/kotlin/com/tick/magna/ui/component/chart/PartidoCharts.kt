package com.tick.magna.ui.component.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tick.magna.ui.core.theme.LocalDimensions

/**
 * The two charts the party screen draws. They were inline in a 793-line screen file and
 * have nothing to do with parties, so they live here as plain drawing components.
 */

@Composable
fun GenderChart(
    maleCount: Int,
    femaleCount: Int,
    labelMale: String,
    labelFemale: String,
    isLoading: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current
    val total = maleCount + femaleCount

    if (total == 0 && !isLoading) return

    val maleColor = colorScheme.primary
    val femaleColor = colorScheme.tertiary

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid24),
    ) {
        Box(modifier = Modifier.size(140.dp)) {
            if (total > 0) {
                val maleFraction = maleCount.toFloat() / total
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val diameter = size.minDimension
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f,
                    )
                    val arcSize = Size(diameter, diameter)
                    val maleSweep = 360f * maleFraction
                    drawArc(
                        color = maleColor,
                        startAngle = -90f,
                        sweepAngle = maleSweep,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize,
                    )
                    drawArc(
                        color = femaleColor,
                        startAngle = -90f + maleSweep,
                        sweepAngle = 360f - maleSweep,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize,
                    )
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid8)) {
            LegendItem(color = maleColor, label = labelMale, count = maleCount, total = total)
            LegendItem(color = femaleColor, label = labelFemale, count = femaleCount, total = total)
            if (isLoading) {
                Text(
                    text = "…",
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, count: Int, total: Int) {
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val pct = if (total > 0) (count * 100 / total) else 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label ($count — $pct%)",
            style = typography.bodySmall.copy(color = colorScheme.onSurface),
        )
    }
}

@Composable
fun HorizontalBarChart(
    entries: List<Pair<String, Int>>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current
    val maxValue = entries.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    if (entries.isEmpty() && !isLoading) {
        Text(
            text = "—",
            style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(dimensions.grid2),
                color = colorScheme.secondary,
                trackColor = colorScheme.onSecondary,
            )
        }
        entries.forEach { (label, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
            ) {
                Text(
                    text = label,
                    style = typography.labelMedium.copy(color = colorScheme.onSurfaceVariant),
                    modifier = Modifier.width(48.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colorScheme.surfaceDim),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(value.toFloat() / maxValue)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colorScheme.primary),
                    )
                }
                Text(
                    text = "$value",
                    style = typography.labelMedium.copy(
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.width(28.dp),
                )
            }
        }
    }
}
