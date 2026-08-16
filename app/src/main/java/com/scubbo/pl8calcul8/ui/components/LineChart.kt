package com.scubbo.pl8calcul8.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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

/** One line on the chart. */
data class ChartSeries(
    val label: String,
    val points: List<Pair<Long, Double>>,
    val color: Color,
)

private val DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d")

private fun dateLabel(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMAT)

private fun valueLabel(value: Double): String = value.toInt().toString()

/** Single-series convenience wrapper. */
@Composable
fun LineChart(
    points: List<Pair<Long, Double>>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    LineChart(series = listOf(ChartSeries("", points, lineColor)), modifier = modifier)
}

/**
 * A minimal line chart of values over time: polylines with point markers,
 * min/max labels on the y-axis, first/last date labels on the x-axis, and
 * a legend when charting more than one series.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
) {
    val allPoints = series.flatMap { it.points }
    if (allPoints.size < 2) {
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

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
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

            val minDate = allPoints.minOf { it.first }
            val maxDate = allPoints.maxOf { it.first }
            val minValue = allPoints.minOf { it.second }
            val maxValue = allPoints.maxOf { it.second }
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

            series.forEach { s ->
                if (s.points.isEmpty()) return@forEach
                val path = Path()
                s.points.forEachIndexed { i, (date, value) ->
                    val point = Offset(x(date), y(value))
                    if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(path, s.color, style = Stroke(width = 4f))
                s.points.forEach { (date, value) ->
                    drawCircle(s.color, radius = 7f, center = Offset(x(date), y(value)))
                }
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

        val labelled = series.filter { it.label.isNotEmpty() }
        if (labelled.size > 1) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                labelled.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .drawBehind { drawCircle(s.color) },
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(s.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
