package com.fuelmap.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable

/** Простой сплайн-график цены (спарклайн) с заливкой под линией. */
@Composable
fun PriceChart(
    points: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return
    val min = points.min()
    val max = points.max()
    val range = (max - min).coerceAtLeast(0.01f)

    Canvas(modifier) {
        val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
        fun x(i: Int) = i * stepX
        fun y(p: Float) = size.height - ((p - min) / range) * size.height * 0.86f - size.height * 0.07f

        val line = Path().apply {
            points.forEachIndexed { i, p ->
                if (i == 0) moveTo(x(i), y(p)) else lineTo(x(i), y(p))
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(x(points.lastIndex), size.height)
            lineTo(x(0), size.height)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.0f))
            )
        )
        drawPath(line, color = lineColor, style = Stroke(width = 6f))
        points.forEachIndexed { i, p ->
            drawCircle(lineColor, radius = 7f, center = Offset(x(i), y(p)))
        }
    }
}
