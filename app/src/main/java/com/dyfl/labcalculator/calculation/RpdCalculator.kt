package com.dyfl.labcalculator.calculation

import java.math.BigDecimal
import java.math.RoundingMode

enum class RpdField {
    ORIGINAL_SAMPLE,
    REPLICATE_SAMPLE
}

data class RpdError(
    val field: RpdField,
    val message: String
)

sealed interface RpdResult {
    data class Success(
        val formattedPercent: String,
        val calculationSteps: List<String>
    ) : RpdResult
    data class Invalid(val errors: List<RpdError>) : RpdResult
    data object ZeroAverage : RpdResult {
        const val MESSAGE = "RPD cannot be calculated when the average is zero."
    }
}

/** Pure RPD calculation. Only the final displayed percentage is rounded. */
object RpdCalculator {
    fun calculate(originalText: String, replicateText: String): RpdResult {
        val errors = mutableListOf<RpdError>()
        val original = parseInput(
            text = originalText,
            field = RpdField.ORIGINAL_SAMPLE,
            fieldName = "original sample result",
            errors = errors
        )
        val replicate = parseInput(
            text = replicateText,
            field = RpdField.REPLICATE_SAMPLE,
            fieldName = "replicate sample result",
            errors = errors
        )

        if (errors.isNotEmpty()) return RpdResult.Invalid(errors)

        checkNotNull(original)
        checkNotNull(replicate)

        val sum = original.add(replicate)
        if (sum.compareTo(BigDecimal.ZERO) == 0) return RpdResult.ZeroAverage

        val difference = original.subtract(replicate).abs()
        val average = sum.divide(TWO)
        val absoluteAverage = average.abs()
        val exactNumerator = difference.multiply(ONE_HUNDRED)
        val displayedPercent = exactNumerator.divide(
            absoluteAverage,
            DISPLAY_SCALE,
            RoundingMode.HALF_UP
        )
        val formattedPercent = "${displayedPercent.toPlainString()}%"
        val intermediateDisplay = formatIntermediatePercentage(exactNumerator, absoluteAverage)
        val calculationSteps = listOf(
            "Difference = |${original.toGroupedExactString()} − " +
                "${replicate.toGroupedExactString()}| = ${difference.toGroupedExactString()}.",
            "Average = (${original.toGroupedExactString()} + " +
                "${replicate.toGroupedExactString()}) ÷ 2 = ${average.toGroupedExactString()}.",
            "Absolute average = |${average.toGroupedExactString()}| = " +
                "${absoluteAverage.toGroupedExactString()}.",
            "RPD = (${difference.toGroupedExactString()} ÷ " +
                "${absoluteAverage.toGroupedExactString()}) × 100.",
            intermediateDisplay,
            "Final RPD = $formattedPercent."
        )

        return RpdResult.Success(
            formattedPercent = formattedPercent,
            calculationSteps = calculationSteps
        )
    }

    private fun formatIntermediatePercentage(
        exactNumerator: BigDecimal,
        denominator: BigDecimal
    ): String = try {
        val exactPercentage = exactNumerator.divide(denominator).toGroupedExactString()
        "RPD = $exactPercentage% (exact)."
    } catch (_: ArithmeticException) {
        val readablePercentage = exactNumerator.divide(
            denominator,
            INTERMEDIATE_DISPLAY_SCALE,
            RoundingMode.HALF_UP
        )
        "RPD ≈ ${readablePercentage.toPlainString()}% (rounded to " +
            "$INTERMEDIATE_DISPLAY_SCALE decimal places for display only)."
    }

    private fun parseInput(
        text: String,
        field: RpdField,
        fieldName: String,
        errors: MutableList<RpdError>
    ): BigDecimal? {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) {
            errors += RpdError(field, "Enter the $fieldName.")
            return null
        }

        return try {
            parseLabDecimal(trimmedText)
        } catch (error: NumberFormatException) {
            errors += RpdError(
                field,
                checkNotNull(error.message)
            )
            null
        }
    }

    private val ONE_HUNDRED = BigDecimal("100")
    private val TWO = BigDecimal("2")
    private const val DISPLAY_SCALE = 2
    private const val INTERMEDIATE_DISPLAY_SCALE = 6
}
