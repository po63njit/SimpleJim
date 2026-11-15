package com.simplejim.tracker.strength

internal data class StrengthChartDot(
    val ordinal: Int,
    val kilograms: Double,
    val sourceSet: SavedStrengthSet,
)

internal data class StrengthChart(
    val ordinals: IntRange,
    val maximumKg: Double,
    val observed: List<StrengthChartDot>,
    val estimated: List<StrengthChartDot>,
)

// Preserve every session slot, including those without estimates. Both series share a scale.
internal fun strengthChart(history: ExerciseStrengthHistory): StrengthChart {
    val recent = strengthSessionRows(history, newestFirst = false).takeLast(12)
    val observed = recent.map { row ->
        val source = row.point.heaviestActual
        StrengthChartDot(row.ordinal, source.load.toKilograms(), source)
    }
    val estimated = recent.mapNotNull { row ->
        row.point.bestEstimate?.let { StrengthChartDot(row.ordinal, it.kilograms, it.sourceSet) }
    }
    return StrengthChart(
        ordinals = if (recent.isEmpty()) IntRange.EMPTY else recent.first().ordinal..recent.last().ordinal,
        maximumKg = (observed + estimated).maxOfOrNull { it.kilograms } ?: 0.0,
        observed = observed,
        estimated = estimated,
    )
}

internal fun StrengthChart.xFraction(ordinal: Int): Float =
    if (ordinals.first == ordinals.last) .5f
    else (ordinal - ordinals.first).toFloat() / (ordinals.last - ordinals.first)

internal fun StrengthChart.yFraction(kilograms: Double): Float =
    if (maximumKg > 0.0) (1 - kilograms / maximumKg).toFloat() else 1f
