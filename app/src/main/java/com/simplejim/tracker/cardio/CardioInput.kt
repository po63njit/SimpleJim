package com.simplejim.tracker.cardio

import java.math.BigDecimal
import java.math.RoundingMode

internal enum class DistanceUnit(internal val millimetersPerUnit: Long) {
    MILE(1_609_344),
    KILOMETER(1_000_000),
}

internal enum class CardioField { DURATION, DISTANCE, INCLINE, FLOORS, LEVEL }
internal enum class CardioError { REQUIRED, INVALID_FORMAT, OUT_OF_RANGE }
internal data class CardioValidationIssue(val field: CardioField, val error: CardioError)

internal sealed interface CardioInputResult<out T> {
    data class Valid<T>(val entry: T) : CardioInputResult<T>
    data class Invalid(val issues: List<CardioValidationIssue>) : CardioInputResult<Nothing>
}

// Retain the quantity and unit shown on the machine. Derived results are never source data.
internal data class DisplayedDistance(val thousandths: Long, val unit: DistanceUnit) {
    fun millimeters(): Long =
        BigDecimal.valueOf(thousandths, 3).multiply(BigDecimal.valueOf(unit.millimetersPerUnit))
            .setScale(0, RoundingMode.HALF_UP).longValueExact()
}

internal data class TreadmillEntry(
    val durationSeconds: Long,
    val distance: DisplayedDistance,
    val inclineThousandthsOfPercent: Long?,
) {
    // Average over the entire logged duration, in the same unit used in entry.
    fun averagePaceSecondsPerEnteredUnit(): Long =
        BigDecimal.valueOf(durationSeconds).divide(
            BigDecimal.valueOf(distance.thousandths, 3), 0, RoundingMode.HALF_UP,
        ).longValueExact()
}

internal data class StairMachineEntry(
    val durationSeconds: Long,
    val floorThousandths: Long,
    val displayedLevelThousandths: Long?,
) {
    fun averageFloorsPerMinute(): BigDecimal =
        BigDecimal.valueOf(floorThousandths, 3).multiply(BigDecimal.valueOf(60))
            .divide(BigDecimal.valueOf(durationSeconds), 1, RoundingMode.HALF_UP)
}

internal object CardioInput {
    private val clock = Regex("[0-9]{1,6}:[0-9]{2}(?::[0-9]{2})?")
    private val decimal = Regex("-?(?:[0-9]{1,9}(?:[.,][0-9]{1,3})?|[.,][0-9]{1,3})")

    fun treadmill(
        duration: String,
        distance: String,
        unit: DistanceUnit,
        incline: String = "",
    ): CardioInputResult<TreadmillEntry> {
        val issues = mutableListOf<CardioValidationIssue>()
        val seconds = duration(duration, issues)
        val amount = quantity(distance, CardioField.DISTANCE, required = true, minimum = 1, issues)
        val grade = quantity(incline, CardioField.INCLINE, required = false, minimum = null, issues)
        return if (issues.isEmpty() && seconds != null && amount != null) {
            CardioInputResult.Valid(TreadmillEntry(seconds, DisplayedDistance(amount, unit), grade))
        } else {
            CardioInputResult.Invalid(issues)
        }
    }

    fun stairs(
        duration: String,
        floors: String,
        level: String = "",
    ): CardioInputResult<StairMachineEntry> {
        val issues = mutableListOf<CardioValidationIssue>()
        val seconds = duration(duration, issues)
        val floorAmount = quantity(floors, CardioField.FLOORS, required = true, minimum = 1, issues)
        val machineLevel = quantity(level, CardioField.LEVEL, required = false, minimum = 0, issues)
        return if (issues.isEmpty() && seconds != null && floorAmount != null) {
            CardioInputResult.Valid(StairMachineEntry(seconds, floorAmount, machineLevel))
        } else {
            CardioInputResult.Invalid(issues)
        }
    }

    private fun duration(input: String, issues: MutableList<CardioValidationIssue>): Long? {
        val value = input.trim()
        if (value.isEmpty()) return reject(CardioField.DURATION, CardioError.REQUIRED, issues)
        if (!clock.matches(value)) return reject(CardioField.DURATION, CardioError.INVALID_FORMAT, issues)
        val parts = value.split(':').map(String::toLong)
        val total = when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            else -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        }
        if (total == 0L || parts.last() > 59 || (parts.size == 3 && parts[1] > 59)) {
            return reject(CardioField.DURATION, CardioError.OUT_OF_RANGE, issues)
        }
        return total
    }

    private fun quantity(
        input: String,
        field: CardioField,
        required: Boolean,
        minimum: Long?,
        issues: MutableList<CardioValidationIssue>,
    ): Long? {
        val value = input.trim()
        if (value.isEmpty()) return if (required) reject(field, CardioError.REQUIRED, issues) else null
        if (!decimal.matches(value)) return reject(field, CardioError.INVALID_FORMAT, issues)
        val scaled = value.replace(',', '.').toBigDecimal().movePointRight(3).longValueExact()
        if (minimum != null && scaled < minimum) return reject(field, CardioError.OUT_OF_RANGE, issues)
        return scaled
    }

    private fun reject(
        field: CardioField,
        error: CardioError,
        issues: MutableList<CardioValidationIssue>,
    ): Nothing? {
        issues += CardioValidationIssue(field, error)
        return null
    }
}
