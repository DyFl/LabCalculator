package com.dyfl.labcalculator.calculation

import java.math.BigDecimal
import java.math.RoundingMode

enum class MsMsdField {
    RAW_SOURCE_RESULT,
    DILUTION_FACTOR,
    FINAL_SPIKE_CONCENTRATION,
    MS_RESULT,
    MSD_RESULT
}

data class MsMsdInput(
    val rawSourceResult: String,
    val dilutionFactor: String,
    val finalSpikeConcentration: String,
    val msResult: String,
    val msdResult: String,
    val concentrationUnit: ConcentrationUnit = ConcentrationUnit.PPB
)

data class MsMsdError(
    val field: MsMsdField,
    val message: String
)

/** An exact percentage represented as a fraction, so no intermediate division is rounded. */
data class ExactPercentage(
    val numerator: BigDecimal,
    val denominator: BigDecimal
) {
    fun roundedValue(scale: Int = FINAL_PERCENT_SCALE): BigDecimal =
        numerator.divide(denominator, scale, RoundingMode.HALF_UP)

    fun formatted(scale: Int = FINAL_PERCENT_SCALE): String =
        "${roundedValue(scale).toPlainString()}%"

    private companion object {
        const val FINAL_PERCENT_SCALE = 2
    }
}

data class MsMsdCalculation(
    val concentrationUnit: ConcentrationUnit,
    val rawSourceResult: BigDecimal,
    val dilutionFactor: BigDecimal,
    val finalSpikeConcentration: BigDecimal,
    val msResult: BigDecimal,
    val msdResult: BigDecimal,
    val originalSourceConcentration: BigDecimal,
    val msRecoveredSpikeConcentration: BigDecimal,
    val msRecovery: ExactPercentage,
    val msdRecoveredSpikeConcentration: BigDecimal,
    val msdRecovery: ExactPercentage,
    val msMsdDifference: BigDecimal,
    val msMsdAverage: BigDecimal,
    val msMsdAbsoluteAverage: BigDecimal,
    val msMsdRpd: ExactPercentage
)

data class MsMsdCalculationSection(
    val title: String,
    val steps: List<String>
)

sealed interface MsMsdResult {
    data class Success(
        val calculation: MsMsdCalculation,
        val formattedOriginalSourceConcentration: String,
        val formattedMsRecovery: String,
        val formattedMsdRecovery: String,
        val formattedMsMsdRpd: String,
        val calculationSections: List<MsMsdCalculationSection>
    ) : MsMsdResult

    data class Invalid(val errors: List<MsMsdError>) : MsMsdResult

    data object ZeroAverage : MsMsdResult {
        const val MESSAGE =
            "MS/MSD RPD cannot be calculated when the average of the MS and MSD results is zero."
    }
}

/** Pure MS/MSD calculation. The dilution factor applies only to the native source sample. */
object MsMsdCalculator {
    fun calculate(input: MsMsdInput): MsMsdResult {
        val errors = mutableListOf<MsMsdError>()
        val rawSource = parseInput(
            input.rawSourceResult,
            MsMsdField.RAW_SOURCE_RESULT,
            "raw diluted source-sample result",
            errors
        )
        val dilutionFactor = parseInput(
            input.dilutionFactor,
            MsMsdField.DILUTION_FACTOR,
            "sample dilution factor",
            errors
        )
        val finalSpike = parseInput(
            input.finalSpikeConcentration,
            MsMsdField.FINAL_SPIKE_CONCENTRATION,
            "final spike concentration",
            errors
        )
        val msResult = parseInput(
            input.msResult,
            MsMsdField.MS_RESULT,
            "literal MS result",
            errors
        )
        val msdResult = parseInput(
            input.msdResult,
            MsMsdField.MSD_RESULT,
            "literal MSD result",
            errors
        )

        if (dilutionFactor != null && dilutionFactor <= BigDecimal.ZERO) {
            errors += MsMsdError(
                MsMsdField.DILUTION_FACTOR,
                "The sample dilution factor must be greater than zero."
            )
        }
        if (finalSpike != null && finalSpike <= BigDecimal.ZERO) {
            errors += MsMsdError(
                MsMsdField.FINAL_SPIKE_CONCENTRATION,
                "The final spike concentration must be greater than zero."
            )
        }
        if (errors.isNotEmpty()) return MsMsdResult.Invalid(errors)

        checkNotNull(rawSource)
        checkNotNull(dilutionFactor)
        checkNotNull(finalSpike)
        checkNotNull(msResult)
        checkNotNull(msdResult)

        val msMsdSum = msResult.add(msdResult)
        if (msMsdSum.compareTo(BigDecimal.ZERO) == 0) return MsMsdResult.ZeroAverage

        val originalSource = rawSource.multiply(dilutionFactor)
        val msRecoveredSpike = msResult.subtract(rawSource)
        val msdRecoveredSpike = msdResult.subtract(rawSource)
        val msRecovery = ExactPercentage(msRecoveredSpike.multiply(ONE_HUNDRED), finalSpike)
        val msdRecovery = ExactPercentage(msdRecoveredSpike.multiply(ONE_HUNDRED), finalSpike)
        val msMsdDifference = msResult.subtract(msdResult).abs()
        val msMsdAverage = msMsdSum.divide(TWO)
        val msMsdAbsoluteAverage = msMsdAverage.abs()
        val msMsdRpd = ExactPercentage(msMsdDifference.multiply(ONE_HUNDRED), msMsdAbsoluteAverage)

        val calculation = MsMsdCalculation(
            concentrationUnit = input.concentrationUnit,
            rawSourceResult = rawSource,
            dilutionFactor = dilutionFactor,
            finalSpikeConcentration = finalSpike,
            msResult = msResult,
            msdResult = msdResult,
            originalSourceConcentration = originalSource,
            msRecoveredSpikeConcentration = msRecoveredSpike,
            msRecovery = msRecovery,
            msdRecoveredSpikeConcentration = msdRecoveredSpike,
            msdRecovery = msdRecovery,
            msMsdDifference = msMsdDifference,
            msMsdAverage = msMsdAverage,
            msMsdAbsoluteAverage = msMsdAbsoluteAverage,
            msMsdRpd = msMsdRpd
        )

        return MsMsdResult.Success(
            calculation = calculation,
            formattedOriginalSourceConcentration =
                "${originalSource.toExactPlainString()} ${input.concentrationUnit.label}",
            formattedMsRecovery = msRecovery.formatted(),
            formattedMsdRecovery = msdRecovery.formatted(),
            formattedMsMsdRpd = msMsdRpd.formatted(),
            calculationSections = buildCalculationSections(calculation)
        )
    }

    private fun buildCalculationSections(
        calculation: MsMsdCalculation
    ): List<MsMsdCalculationSection> {
        val rawSource = calculation.rawSourceResult.toGroupedExactString()
        val dilutionFactor = calculation.dilutionFactor.toGroupedExactString()
        val spike = calculation.finalSpikeConcentration.toGroupedExactString()
        val msResult = calculation.msResult.toGroupedExactString()
        val msdResult = calculation.msdResult.toGroupedExactString()
        val originalSource = calculation.originalSourceConcentration.toGroupedExactString()
        val msRecovered = calculation.msRecoveredSpikeConcentration.toGroupedExactString()
        val msdRecovered = calculation.msdRecoveredSpikeConcentration.toGroupedExactString()
        val difference = calculation.msMsdDifference.toGroupedExactString()
        val average = calculation.msMsdAverage.toGroupedExactString()
        val absoluteAverage = calculation.msMsdAbsoluteAverage.toGroupedExactString()
        val unit = calculation.concentrationUnit.label

        return listOf(
            MsMsdCalculationSection(
                title = "Dilution handling",
                steps = listOf(
                    "Original source concentration = $rawSource $unit × $dilutionFactor.",
                    "Original source concentration = $originalSource $unit.",
                    "The dilution factor applies only to the native sample contribution.",
                    "The $spike $unit spike was added after dilution and is not multiplied by " +
                        "$dilutionFactor."
                )
            ),
            MsMsdCalculationSection(
                title = "MS recovery",
                steps = listOf(
                    "Recovered spike = $msResult $unit − $rawSource $unit.",
                    "Recovered spike = $msRecovered $unit.",
                    "MS recovery = ($msRecovered $unit ÷ $spike $unit) × 100.",
                    "The $unit units cancel.",
                    "Final MS recovery = ${calculation.msRecovery.formatted()}."
                )
            ),
            MsMsdCalculationSection(
                title = "MSD recovery",
                steps = listOf(
                    "Recovered spike = $msdResult $unit − $rawSource $unit.",
                    "Recovered spike = $msdRecovered $unit.",
                    "MSD recovery = ($msdRecovered $unit ÷ $spike $unit) × 100.",
                    "The $unit units cancel.",
                    "Final MSD recovery = ${calculation.msdRecovery.formatted()}."
                )
            ),
            MsMsdCalculationSection(
                title = "MS/MSD RPD",
                steps = listOf(
                    "Difference = |$msResult $unit − $msdResult $unit| = " +
                        "$difference $unit.",
                    "Average = ($msResult $unit + $msdResult $unit) ÷ 2 = " +
                        "$average $unit.",
                    "Absolute average = |$average $unit| = $absoluteAverage $unit.",
                    "RPD = ($difference $unit ÷ $absoluteAverage $unit) × 100.",
                    "The $unit units cancel.",
                    formatIntermediatePercentage(calculation.msMsdRpd),
                    "Final RPD = ${calculation.msMsdRpd.formatted()}."
                )
            )
        )
    }

    private fun formatIntermediatePercentage(percentage: ExactPercentage): String = try {
        val exactValue = percentage.numerator.divide(percentage.denominator)
        "RPD = ${exactValue.toGroupedExactString()}% (exact)."
    } catch (_: ArithmeticException) {
        val readableValue = percentage.numerator.divide(
            percentage.denominator,
            INTERMEDIATE_DISPLAY_SCALE,
            RoundingMode.HALF_UP
        )
        "RPD ≈ ${readableValue.toPlainString()}% (rounded to " +
            "$INTERMEDIATE_DISPLAY_SCALE decimal places for display only)."
    }

    private fun parseInput(
        text: String,
        field: MsMsdField,
        fieldName: String,
        errors: MutableList<MsMsdError>
    ): BigDecimal? {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) {
            errors += MsMsdError(field, "Enter the $fieldName.")
            return null
        }

        return try {
            BigDecimal(trimmedText)
        } catch (_: NumberFormatException) {
            errors += MsMsdError(
                field,
                "Enter a finite number using digits and a decimal point."
            )
            null
        }
    }

    private val ONE_HUNDRED = BigDecimal("100")
    private val TWO = BigDecimal("2")
    private const val INTERMEDIATE_DISPLAY_SCALE = 6
}
