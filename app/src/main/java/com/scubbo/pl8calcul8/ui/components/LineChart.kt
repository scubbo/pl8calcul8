package com.scubbo.pl8calcul8.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d")

private fun dateLabel(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMAT)

private fun valueLabel(value: Double): String = value.toInt().toString()

/**
 * A minimal line chart of values over time: polyline with point markers,
 * min/max labels on the y-axis and first/last date labels on the x-axis.
 */
@Composable
fun LineChart(
    points: List<Pair<Long, Double>>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Not enough data to chart yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(4.dp),
    ) {
        val yLabelWidth = 80f
        val xLabelHeight = 40f
        val chartLeft = yLabelWidth
        val chartRight = size.width
        val chartTop = 10f
        val chartBottom = size.height - xLabelHeight

        val minDate = points.first().first
        val maxDate = points.last().first
        val minValue = points.minOf { it.second }
        val maxValue = points.maxOf { it.second }
        // Pad the value range so a flat series doesn't sit on the border.
        val valuePad = ((maxValue - minValue) * 0.1).coerceAtLeast(1.0)
        val loValue = minValue - valuePad
        val hiValue = maxValue + valuePad

        fun x(date: Long): Float =
            if (maxDate == minDate) (chartLeft + chartRight) / 2
            else chartLeft + ((date - minDate).toFloat() / (maxDate - minDate)) * (chartRight - chartLeft)

        fun y(value: Double): Float =
            chartBottom - (((value - loValue) / (hiValue - loValue)).toFloat() * (chartBottom - chartTop))

        // Axes
        drawLine(axisColor, Offset(chartLeft, chartTop), Offset(chartLeft, chartBottom))
        drawLine(axisColor, Offset(chartLeft, chartBottom), Offset(chartRight, chartBottom))

        // Series
        val path = Path()
        points.forEachIndexed { i, (date, value) ->
            val point = Offset(x(date), y(value))
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, lineColor, style = Stroke(width = 4f))
        points.forEach { (date, value) ->
            drawCircle(lineColor, radius = 7f, center = Offset(x(date), y(value)))
        }

        // Y-axis labels: max at top, min at bottom
        drawText(
            textMeasurer.measure(valueLabel(maxValue), labelStyle),
            topLeft = Offset(0f, y(maxValue) - 14f),
        )
        drawText(
            textMeasurer.measure(valueLabel(minValue), labelStyle),
            topLeft = Offset(0f, y(minValue) - 14f),
        )

        // X-axis labels: first and last dates
        drawText(
            textMeasurer.measure(dateLabel(minDate), labelStyle),
            topLeft = Offset(chartLeft, chartBottom + 8f),
        )
        val lastLabel = textMeasurer.measure(dateLabel(maxDate), labelStyle)
        drawText(
            lastLabel,
            topLeft = Offset(chartRight - lastLabel.size.width, chartBottom + 8f),
        )
    }
}
