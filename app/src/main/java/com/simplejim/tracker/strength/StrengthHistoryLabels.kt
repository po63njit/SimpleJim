package com.simplejim.tracker.strength

import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal fun exerciseDisplayName(history: ExerciseStrengthHistory): String =
    history.sessions.last().heaviestActual.exerciseName.trim().replace(Regex("\\s+"), " ")

internal fun sourceSetLabel(
    source: SavedStrengthSet,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault(),
): String {
    val amount = source.load.value.toString().removeSuffix(".0")
    val reps = if (source.reps == 1) "1 rep" else "${source.reps} reps"
    return "$amount ${source.load.unit.dimensionLabel}, $reps, ${strengthDateTime(source.performedAt, locale, timeZone)}"
}

internal fun estimateLabel(
    estimate: EstimatedOneRepMax,
    locale: Locale = Locale.getDefault(),
): String {
    val originalUnit = estimate.sourceSet.load.unit
    // A mathematically finite estimate in kg need not fit in Double when converted back to lb.
    val converted = originalUnit.fromKilograms(estimate.kilograms)
    val displayUnit = if (converted.isFinite()) originalUnit else LoadUnit.Kilograms
    val amount = if (converted.isFinite()) converted else estimate.kilograms
    val text = if (amount < 0.1 || amount >= 1e7) amount.toString() else NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = 1
    }.format(amount)
    return "$text ${displayUnit.dimensionLabel}"
}

internal fun strengthDateTime(
    timestamp: Long,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault(),
): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale).apply {
    this.timeZone = timeZone
}.format(Date(timestamp))

internal data class StrengthSessionRow(val ordinal: Int, val point: StrengthSessionPoint)

internal fun strengthSessionRows(history: ExerciseStrengthHistory, newestFirst: Boolean): List<StrengthSessionRow> {
    val chronological = history.sessions.mapIndexed { index, point -> StrengthSessionRow(index + 1, point) }
    return if (newestFirst) chronological.asReversed() else chronological
}

private val LoadUnit.dimensionLabel: String
    get() = if (this == LoadUnit.Pounds) "lb" else "kg"
