package com.dyfl.labcalculator.calculation

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

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
        val volumeFromStockMl: ExactFraction,
        val finalSolutionVolumeMl: BigDecimal,
        val preparationInstruction: String,
        val isApproximate: Boolean,
        val calculationSteps: List<String>
    ) : DilutionResult
    data class Invalid(val errors: List<DilutionError>) : DilutionResult
}

/**
 * Solves C1V1 = C2V2 for V1 without rounding any intermediate value.
 */
object DilutionCalculator {
    private const val FINAL_EXCEEDS_STOCK_MESSAGE =
        "Final concentration cannot exceed the stock concentration."
    internal const val INCOMPATIBLE_BASIS_MESSAGE =
        "Stock and target must use the same concentration family and basis. No parts-per to mass-per-volume conversion is assumed."

    /** Errors are saved as field/message pairs; only this target error also depends on stock. */
    internal fun errorsAfterEdit(
        errors: Map<DilutionField, String>,
        editedField: DilutionField
    ): Map<DilutionField, String> = errors.filterNot { (field, message) ->
        field == editedField ||
            (editedField == DilutionField.STOCK_CONCENTRATION &&
                field == DilutionField.FINAL_CONCENTRATION &&
                message in listOf(FINAL_EXCEEDS_STOCK_MESSAGE, INCOMPATIBLE_BASIS_MESSAGE))
    }

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

        if (input.stockUnit.family != input.finalUnit.family) {
            errors += DilutionError(DilutionField.FINAL_CONCENTRATION, INCOMPATIBLE_BASIS_MESSAGE)
        }
        if (errors.isNotEmpty()) {
            return DilutionResult.Invalid(errors)
        }

        checkNotNull(stockConcentration)
        checkNotNull(finalConcentration)
        checkNotNull(finalSolutionVolume)

        val stockInBase = input.stockUnit.toBase(stockConcentration)
        val finalInBase = input.finalUnit.toBase(finalConcentration)
        val baseUnit = input.stockUnit.baseUnit.label

        if (finalInBase > stockInBase) {
            return DilutionResult.Invalid(
                listOf(
                    DilutionError(
                        DilutionField.FINAL_CONCENTRATION,
                        FINAL_EXCEEDS_STOCK_MESSAGE
                    )
                )
            )
        }

        val numerator = finalInBase.multiply(finalSolutionVolume)
        val volumeFromStock = ExactFraction.fromRatio(numerator, stockInBase)
        val volumeDisplay = volumeFromStock.toExactString()
        // Select units before any display rounding, including values just below 1 mL.
        val useMicroliters = volumeFromStock.numerator.signum() > 0 &&
            volumeFromStock.numerator < volumeFromStock.denominator
        val transfer = if (useMicroliters) volumeFromStock.times(BigInteger.valueOf(1000))
            else volumeFromStock
        val transferUnit = if (useMicroliters) "µL" else "mL"
        val transferDisplay = transfer.forPreparation()
        val finalDisplay = ExactFraction.fromRatio(finalSolutionVolume, BigDecimal.ONE).forPreparation()
        val instruction = "Transfer ${transferDisplay.text} $transferUnit of stock and make up to " +
            "${finalDisplay.text} mL final solution volume."
        val calculationSteps = listOf(
            "Start with C₁V₁ = C₂V₂ and rearrange: V₁ = (C₂ × V₂) ÷ C₁.",
            "Concentration family: ${input.stockUnit.family.label}. Stock and target use the same basis; no density conversion is inferred.",
            concentrationNormalizationStep(
                symbol = "C₁",
                originalValue = stockConcentration,
                originalUnit = input.stockUnit,
                valueInBase = stockInBase
            ),
            concentrationNormalizationStep(
                symbol = "C₂",
                originalValue = finalConcentration,
                originalUnit = input.finalUnit,
                valueInBase = finalInBase
            ),
            "Substitute: V₁ = (${finalInBase.toGroupedExactString()} $baseUnit × " +
                "${finalSolutionVolume.toGroupedExactString()} mL) ÷ " +
                "${stockInBase.toGroupedExactString()} $baseUnit.",
            "Multiply the numerator: ${finalInBase.toGroupedExactString()} $baseUnit × " +
                "${finalSolutionVolume.toGroupedExactString()} mL = " +
                "${numerator.toGroupedExactString()} $baseUnit·mL.",
            "Divide and cancel $baseUnit: ${numerator.toGroupedExactString()} $baseUnit·mL ÷ " +
                "${stockInBase.toGroupedExactString()} $baseUnit = $volumeDisplay mL.",
            "Exact stock transfer: ${transfer.toExactString()} $transferUnit" +
                if ('/' in volumeDisplay) " (exact fraction; nonterminating decimal)." else ".",
            "Exact final solution volume: ${finalSolutionVolume.toExactPlainString()} mL. " +
                "Make up to this total volume, including the stock.",
            instruction,
            DILUTION_DISPLAY_POLICY
        )

        return DilutionResult.Success(
            volumeFromStockMl = volumeFromStock,
            finalSolutionVolumeMl = finalSolutionVolume,
            preparationInstruction = instruction,
            isApproximate = transferDisplay.isApproximate || finalDisplay.isApproximate,
            calculationSteps = calculationSteps
        )
    }

    private fun concentrationNormalizationStep(
        symbol: String,
        originalValue: BigDecimal,
        originalUnit: ConcentrationUnit,
        valueInBase: BigDecimal
    ): String = if (originalUnit == originalUnit.baseUnit)
        "$symbol is already in ${originalUnit.label}: ${originalValue.toGroupedExactString()} ${originalUnit.label}."
    else "Convert $symbol: ${originalValue.toGroupedExactString()} ${originalUnit.label} × " +
        "(${originalUnit.baseMultiplier.toGroupedExactString()} ${originalUnit.baseUnit.label} ÷ 1 ${originalUnit.label}) = " +
        "${valueInBase.toGroupedExactString()} ${originalUnit.baseUnit.label}."

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
            parseLabDecimal(trimmedText)
        } catch (error: NumberFormatException) {
            errors += DilutionError(
                field,
                checkNotNull(error.message)
            )
            null
        }
    }
}

/** Reduced rational quantity; presentation never becomes an arithmetic input. */
@ConsistentCopyVisibility
data class ExactFraction private constructor(
    val numerator: BigInteger,
    val denominator: BigInteger
) {
    fun toExactString(): String = terminatingDecimal()?.toExactPlainString()
        ?: "$numerator/$denominator"

    fun times(factor: BigInteger): ExactFraction = create(numerator.multiply(factor), denominator)

    internal fun terminatingDecimal(): BigDecimal? = try {
        BigDecimal(numerator).divide(BigDecimal(denominator)).stripTrailingZeros()
    } catch (_: ArithmeticException) {
        null // A nonterminating expansion stays a rational quantity.
    }

    companion object {
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

internal const val DILUTION_DISPLAY_POLICY =
    "Terminating values up to 12 significant digits are shown exactly. Other values are marked ≈ " +
        "and rounded to 6 significant digits (half up). Scientific notation keeps long values compact. " +
        "Exact values are in Calculation Steps. Display precision does not represent pipette capability " +
        "or measurement uncertainty."

private data class PreparationDisplay(val text: String, val isApproximate: Boolean)

private fun ExactFraction.forPreparation(): PreparationDisplay {
    val exact = terminatingDecimal()
    val approximate = exact == null || exact.precision() > 12
    val value = if (approximate) {
        BigDecimal(numerator).divide(BigDecimal(denominator), MathContext(6, RoundingMode.HALF_UP))
    } else checkNotNull(exact)
    val normalized = value.stripTrailingZeros()
    val plain = normalized.toPlainString()
    val compact = if (plain.length <= 16) plain else {
        val exponent = normalized.precision() - normalized.scale() - 1
        normalized.movePointLeft(exponent).toExactPlainString() + "E" +
            (if (exponent >= 0) "+" else "") + exponent
    }
    return PreparationDisplay((if (approximate) "≈ " else "") + compact, approximate)
}
