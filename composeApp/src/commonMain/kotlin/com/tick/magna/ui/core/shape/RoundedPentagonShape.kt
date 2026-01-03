package com.tick.magna.ui.core.shape

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RoundedPentagonShape(private val cornerRadius: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = minOf(centerX, centerY)
        val radiusPx = with(density) { cornerRadius.toPx() }

        // Calculate pentagon points (rotated 180 degrees)
        val points = (0..4).map { i ->
            val angle = (PI / 2 + PI + (2 * PI * i / 5)).toFloat()
            Offset(
                x = centerX + radius * cos(angle),
                y = centerY + radius * sin(angle)
            )
        }

        // Start at first point
        path.moveTo(points[0].x, points[0].y)

        // Draw lines between points with rounded corners
        for (i in 0..4) {
            val current = points[i]
            val next = points[(i + 1) % 5]
            val previous = points[(i + 4) % 5]

            // Calculate direction vectors
            val toCurrent = (current - previous).normalize()
            val toNext = (next - current).normalize()

            // Calculate corner points
            val cornerStart = current - toCurrent * radiusPx
            val cornerEnd = current + toNext * radiusPx

            // Line to corner start
            path.lineTo(cornerStart.x, cornerStart.y)

            // Rounded corner
            path.quadraticBezierTo(
                current.x, current.y,
                cornerEnd.x, cornerEnd.y
            )
        }

        path.close()
        return Outline.Generic(path)
    }
}

// Helper extension functions
private operator fun Offset.minus(other: Offset) =
    Offset(x - other.x, y - other.y)

private operator fun Offset.plus(other: Offset) =
    Offset(x + other.x, y + other.y)

private operator fun Offset.times(scalar: Float) =
    Offset(x * scalar, y * scalar)

private fun Offset.normalize(): Offset {
    val length = sqrt(x * x + y * y)
    return if (length > 0) Offset(x / length, y / length) else this
}