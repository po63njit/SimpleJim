package com.simplejim.tracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.simplejim.tracker.strength.StrengthChart
import com.simplejim.tracker.strength.StrengthChartDot
import com.simplejim.tracker.strength.yFraction
import com.simplejim.tracker.strength.xFraction
import java.text.NumberFormat

@Composable
internal fun StrengthChartPanel(chart: StrengthChart) {
    val foreground = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.onErrorContainer
    val top = if (chart.maximumKg in .01..1e7) NumberFormat.getNumberInstance().apply {
        maximumFractionDigits = 2
    }.format(chart.maximumKg) else chart.maximumKg.toString()
    val scale = if (chart.maximumKg == 0.0) "All observed loads are 0 kg."
        else "Shared scale in kg, from 0 to about $top."
    val range = if (chart.ordinals.first == chart.ordinals.last) "Session ${chart.ordinals.first}."
        else "Sessions ${chart.ordinals.first} to ${chart.ordinals.last}, oldest to newest."
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Strength chart", modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        Text(range)
        if (chart.ordinals.first != chart.ordinals.last) {
            Text("Even spacing shows session order, not time between workouts.")
        }
        Text(scale)
        Text("Observed heaviest load")
        StrengthDots(chart, chart.observed, foreground, estimate = false)
        Text("Provisional estimated 1RM")
        if (chart.estimated.isEmpty()) {
            Text("No qualifying estimates in these sessions.")
        } else {
            StrengthDots(chart, chart.estimated, accent, estimate = true)
            Text("A blank session has no qualifying estimate.")
        }
        Text("Values, dates, and source sets are listed below.")
    }
}

@Composable
private fun StrengthDots(chart: StrengthChart, dots: List<StrengthChartDot>, ink: Color, estimate: Boolean) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    val baseline = MaterialTheme.colorScheme.outline
    Canvas(Modifier.fillMaxWidth().height(72.dp).clearAndSetSemantics { }) {
        val inset = 8.dp.toPx()
        val availableX = (size.width - inset * 2).coerceAtLeast(0f)
        val availableY = size.height - inset * 2
        for (ordinal in chart.ordinals) {
            val x = inset + availableX * chart.xFraction(ordinal)
            drawLine(grid, Offset(x, inset), Offset(x, size.height - inset), strokeWidth = 1.dp.toPx())
        }
        // Both panels use identical guide positions; avoid implying a range for an all-zero history.
        val guides = if (chart.maximumKg > 0.0) listOf(0f, .5f, 1f) else listOf(1f)
        for (fraction in guides) {
            val y = inset + availableY * fraction
            drawLine(
                if (fraction == 1f) baseline else grid,
                Offset(inset, y),
                Offset(inset + availableX, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
        for (dot in dots) {
            val x = inset + availableX * chart.xFraction(dot.ordinal)
            val y = inset + availableY * chart.yFraction(dot.kilograms)
            if (estimate) {
                drawLine(ink, Offset(x - 5.dp.toPx(), y), Offset(x + 5.dp.toPx(), y), strokeWidth = 3.dp.toPx())
            } else {
                drawCircle(ink, radius = 4.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}
