package com.dyfl.labcalculator.calculation

import java.math.BigDecimal
import java.math.BigInteger

enum class ConcentrationUnit(val label: String) {
    PPM("PPM"),
    PPB("PPB");

    internal fun toPartsPerBillion(value: BigDecimal): BigDecimal = when (this) {
        PPM -> value.multiply(BigDecimal("1000"))
        PPB -> value
    }
}

enum class DilutionField {
    STOCK_CONCENTRATION,
    FINAL_CONCENTRATION,
    FINAL_SOLUTION_VOLUME
}

data class DilutionInput(
    val stockConcentration: String,
    val stockUnit: ConcentrationUnit,
    val finalConcentration: String,
    val finalUnit: ConcentrationUnit,
    val finalSolutionVolumeMl: String
)

data class DilutionError(
    val field: DilutionField,
    val message: String
)

sealed interface DilutionResult {
    data class Success(
        val volumeFromStockMl: String,
        val calculationSteps: List<String>
    ) : DilutionResult
    data class Invalid(val errors: List<DilutionError>) : DilutionResult
}

/**
 * Solves C1V1 = C2V2 for V1 without rounding any intermediate value.
 */
object DilutionCalculator {
    fun calculate(input: DilutionInput): DilutionResult {
        val errors = mutableListOf<DilutionError>()

        val stockConcentration = parseNumber(
            text = input.stockConcentration,
            field = DilutionField.STOCK_CONCENTRATION,
            fieldName = "stock concentration",
            errors = errors
        )
        val finalConcentration = parseNumber(
            text = input.finalConcentration,
            field = DilutionField.FINAL_CONCENTRATION,
            fieldName = "final concentration",
            errors = errors
        )
        val finalSolutionVolume = parseNumber(
            text = input.finalSolutionVolumeMl,
            field = DilutionField.FINAL_SOLUTION_VOLUME,
            fieldName = "final solution volume",
            errors = errors
        )

        if (stockConcentration != null && stockConcentration <= BigDecimal.ZERO) {
            errors += DilutionError(
                DilutionField.STOCK_CONCENTRATION,
                "Stock concentration must be greater than zero."
            )
        }
        if (finalConcentration != null && finalConcentration < BigDecimal.ZERO) {
            errors += DilutionError(
                DilutionField.FINAL_CONCENTRATION,
                "Final concentration cannot be negative."
            )
        }
        if (finalSolutionVolume != null && finalSolutionVolume <= BigDecimal.ZERO) {
            errors += DilutionError(
                DilutionField.FINAL_SOLUTION_VOLUME,
                "Final solution volume must be greater than zero."
            )
        }

        if (errors.isNotEmpty()) {
            return DilutionResult.Invalid(errors)
        }

        checkNotNull(stockConcentration)
        checkNotNull(finalConcentration)
        checkNotNull(finalSolutionVolume)

        val stockInPpb = input.stockUnit.toPartsPerBillion(stockConcentration)
        val finalInPpb = input.finalUnit.toPartsPerBillion(finalConcentration)

        if (finalInPpb > stockInPpb) {
            return DilutionResult.Invalid(
                listOf(
                    DilutionError(
                        DilutionField.FINAL_CONCENTRATION,
                        "Final concentration cannot exceed the stock concentration."
                    )
                )
            )
        }

        val numerator = finalInPpb.multiply(finalSolutionVolume)
        val volumeFromStock = ExactFraction.fromRatio(numerator, stockInPpb)
        val volumeDisplay = volumeFromStock.toDisplayString()
        val calculationSteps = listOf(
            "Start with C₁V₁ = C₂V₂ and rearrange: V₁ = (C₂ × V₂) ÷ C₁.",
            concentrationNormalizationStep(
                symbol = "C₁",
                originalValue = stockConcentration,
                originalUnit = input.stockUnit,
                valueInPpb = stockInPpb
            ),
            concentrationNormalizationStep(
                symbol = "C₂",
                originalValue = finalConcentration,
                originalUnit = input.finalUnit,
                valueInPpb = finalInPpb
            ),
            "Substitute: V₁ = (${finalInPpb.toGroupedExactString()} PPB × " +
                "${finalSolutionVolume.toGroupedExactString()} mL) ÷ " +
                "${stockInPpb.toGroupedExactString()} PPB.",
            "Multiply the numerator: ${finalInPpb.toGroupedExactString()} PPB × " +
                "${finalSolutionVolume.toGroupedExactString()} mL = " +
                "${numerator.toGroupedExactString()} PPB·mL.",
            "Divide and cancel PPB: ${numerator.toGroupedExactString()} PPB·mL ÷ " +
                "${stockInPpb.toGroupedExactString()} PPB = $volumeDisplay mL.",
            if (volumeDisplay.endsWith('R')) {
                "Final Volume from stock = $volumeDisplay mL. R marks an exact repeating " +
                    "decimal; no rounding was applied."
            } else {
                "Final Volume from stock = $volumeDisplay mL."
            }
        )

        return DilutionResult.Success(
            volumeFromStockMl = volumeDisplay,
            calculationSteps = calculationSteps
        )
    }

    private fun concentrationNormalizationStep(
        symbol: String,
        originalValue: BigDecimal,
        originalUnit: ConcentrationUnit,
        valueInPpb: BigDecimal
    ): String = when (originalUnit) {
        ConcentrationUnit.PPM ->
            "Convert $symbol: ${originalValue.toGroupedExactString()} PPM × " +
                "(1,000 PPB ÷ 1 PPM) = ${valueInPpb.toGroupedExactString()} PPB."

        ConcentrationUnit.PPB ->
            "$symbol is already in PPB: ${originalValue.toGroupedExactString()} PPB."
    }

    private fun parseNumber(
        text: String,
        field: DilutionField,
        fieldName: String,
        errors: MutableList<DilutionError>
    ): BigDecimal? {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) {
            errors += DilutionError(field, "Enter the $fieldName.")
            return null
        }

        return try {
            BigDecimal(trimmedText)
        } catch (_: NumberFormatException) {
            errors += DilutionError(
                field,
                "Enter a valid number using digits and a decimal point."
            )
            null
        }
    }
}

private class ExactFraction private constructor(
    val numerator: BigInteger,
    val denominator: BigInteger
) {
    fun toDisplayString(): String {
        if (numerator == BigInteger.ZERO) return "0"

        if (hasTerminatingDecimal()) {
            return BigDecimal(numerator)
                .divide(BigDecimal(denominator))
                .stripTrailingZeros()
                .toPlainString()
        }

        val wholePart = numerator.divide(denominator)
        var remainder = numerator.remainder(denominator)
        val digits = StringBuilder()
        val remainderPositions = mutableMapOf<BigInteger, Int>()

        while (remainder != BigInteger.ZERO && remainder !in remainderPositions) {
            remainderPositions[remainder] = digits.length
            remainder = remainder.multiply(BigInteger.TEN)
            digits.append(remainder.divide(denominator))
            remainder = remainder.remainder(denominator)
        }

        val repeatStart = checkNotNull(remainderPositions[remainder])
        val nonRepeatingDigits = digits.substring(0, repeatStart)
        val repeatingDigits = digits.substring(repeatStart)

        return buildString {
            append(wholePart)
            append('.')
            append(nonRepeatingDigits)
            repeat(REPETITION_COUNT) { append(repeatingDigits) }
            append('R')
        }
    }

    private fun hasTerminatingDecimal(): Boolean {
        var remainingDenominator = denominator
        while (remainingDenominator.mod(TWO) == BigInteger.ZERO) {
            remainingDenominator = remainingDenominator.divide(TWO)
        }
        while (remainingDenominator.mod(FIVE) == BigInteger.ZERO) {
            remainingDenominator = remainingDenominator.divide(FIVE)
        }
        return remainingDenominator == BigInteger.ONE
    }

    companion object {
        private const val REPETITION_COUNT = 3
        private val TWO = BigInteger.valueOf(2)
        private val FIVE = BigInteger.valueOf(5)

        fun fromRatio(numerator: BigDecimal, denominator: BigDecimal): ExactFraction {
            val numeratorFraction = numerator.toExactFraction()
            val denominatorFraction = denominator.toExactFraction()

            return create(
                numerator = numeratorFraction.numerator.multiply(denominatorFraction.denominator),
                denominator = numeratorFraction.denominator.multiply(denominatorFraction.numerator)
            )
        }

        private fun BigDecimal.toExactFraction(): ExactFraction {
            val normalized = stripTrailingZeros()
            return if (normalized.scale() >= 0) {
                create(
                    numerator = normalized.unscaledValue(),
                    denominator = BigInteger.TEN.pow(normalized.scale())
                )
            } else {
                create(
                    numerator = normalized.unscaledValue()
                        .multiply(BigInteger.TEN.pow(-normalized.scale())),
                    denominator = BigInteger.ONE
                )
            }
        }

        private fun create(numerator: BigInteger, denominator: BigInteger): ExactFraction {
            require(denominator != BigInteger.ZERO) { "Denominator cannot be zero." }

            val positiveDenominator = denominator.abs()
            val signedNumerator = if (denominator.signum() < 0) numerator.negate() else numerator
            val commonDivisor = signedNumerator.gcd(positiveDenominator)

            return ExactFraction(
                numerator = signedNumerator.divide(commonDivisor),
                denominator = positiveDenominator.divide(commonDivisor)
            )
        }
    }
}
