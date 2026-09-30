package com.dyfl.labcalculator.calculation

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class MolarityVolumeUnit(val label: String) {
    MILLILITER("mL"),
    LITER("L");

    internal fun toLiters(value: BigDecimal): BigDecimal = when (this) {
        MILLILITER -> value.movePointLeft(3)
        LITER -> value
    }
}

enum class MolarityMassField {
    MOLARITY,
    FINAL_SOLUTION_VOLUME,
    FORMULA_WEIGHT
}

data class MolarityMassInput(
    val molarity: String,
    val finalSolutionVolume: String,
    val volumeUnit: MolarityVolumeUnit,
    val formulaWeight: String
)

data class MolarityMassError(val field: MolarityMassField, val message: String)

sealed interface MolarityMassResult {
    data class Success(
        val exactMassGrams: BigDecimal,
        val volumeLiters: BigDecimal,
        val formattedMassGrams: String,
        val calculationSteps: List<String>
    ) : MolarityMassResult

    data class Invalid(val errors: List<MolarityMassError>) : MolarityMassResult
}

/** Solves m = M × V × FW with exact decimal arithmetic; only the display is rounded. */
object MolarityMassCalculator {
    private val displayPrecision = MathContext(5, RoundingMode.HALF_UP)

    fun calculate(input: MolarityMassInput): MolarityMassResult {
        val errors = mutableListOf<MolarityMassError>()
        val molarity = parseNumber(
            input.molarity, MolarityMassField.MOLARITY, "desired molarity", errors
        )
        val volume = parseNumber(
            input.finalSolutionVolume, MolarityMassField.FINAL_SOLUTION_VOLUME,
            "final solution volume", errors
        )
        val formulaWeight = parseNumber(
            input.formulaWeight, MolarityMassField.FORMULA_WEIGHT, "formula weight", errors
        )

        if (molarity != null && molarity < BigDecimal.ZERO) {
            errors += MolarityMassError(
                MolarityMassField.MOLARITY, "Desired molarity cannot be negative."
            )
        }
        if (volume != null && volume <= BigDecimal.ZERO) {
            errors += MolarityMassError(
                MolarityMassField.FINAL_SOLUTION_VOLUME,
                "Final solution volume must be greater than zero."
            )
        }
        if (formulaWeight != null && formulaWeight <= BigDecimal.ZERO) {
            errors += MolarityMassError(
                MolarityMassField.FORMULA_WEIGHT, "Formula weight must be greater than zero."
            )
        }
        if (errors.isNotEmpty()) return MolarityMassResult.Invalid(errors)

        checkNotNull(molarity)
        checkNotNull(volume)
        checkNotNull(formulaWeight)
        val volumeLiters = input.volumeUnit.toLiters(volume)
        val mass = molarity.multiply(volumeLiters).multiply(formulaWeight)
        val formattedMass = formatMass(mass)
        val steps = listOf(
            when (input.volumeUnit) {
                MolarityVolumeUnit.MILLILITER ->
                    "Convert volume: ${exactDisplay(volume)} mL ÷ 1,000 = " +
                        "${exactDisplay(volumeLiters)} L."
                MolarityVolumeUnit.LITER ->
                    "Volume is already in liters: ${exactDisplay(volumeLiters)} L."
            },
            "Mass = M × V × FW.",
            "Mass = ${exactDisplay(molarity)} mol/L × ${exactDisplay(volumeLiters)} L × " +
                "${exactDisplay(formulaWeight)} g/mol.",
            "Cancel mol and L: Mass = ${exactDisplay(mass)} g before display rounding.",
            "Required Mass = $formattedMass g" +
                if (mass.signum() == 0) "." else " (5 significant figures)."
        )
        return MolarityMassResult.Success(mass, volumeLiters, formattedMass, steps)
    }

    private fun formatMass(value: BigDecimal): String {
        if (value.signum() == 0) return "0"
        val rounded = value.round(displayPrecision)
        val exponent = rounded.precision() - rounded.scale() - 1
        return if (exponent < -6 || exponent >= 7) {
            val mantissa = rounded.movePointLeft(exponent).setScale(4)
            "$mantissa" + "E" + (if (exponent >= 0) "+" else "") + exponent
        } else {
            rounded.setScale(4 - exponent).toPlainString()
        }
    }

    private fun exactDisplay(value: BigDecimal): String {
        if (value.signum() == 0) return "0"
        val exponent = value.precision() - value.scale() - 1
        return if (exponent < -6 || exponent >= 7) value.stripTrailingZeros().toString()
        else value.toPlainString()
    }

    private fun parseNumber(
        text: String,
        field: MolarityMassField,
        fieldName: String,
        errors: MutableList<MolarityMassError>
    ): BigDecimal? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            errors += MolarityMassError(field, "Enter the $fieldName.")
            return null
        }
        return try {
            parseLabDecimal(trimmed)
        } catch (error: NumberFormatException) {
            errors += MolarityMassError(field, checkNotNull(error.message))
            null
        }
    }
}
