package com.dyfl.labcalculator.presets

import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.DilutionCalculator
import com.dyfl.labcalculator.calculation.DilutionInput
import com.dyfl.labcalculator.calculation.DilutionResult
import com.dyfl.labcalculator.calculation.MetricUnit
import com.dyfl.labcalculator.calculation.MolarityMassCalculator
import com.dyfl.labcalculator.calculation.MolarityMassInput
import com.dyfl.labcalculator.calculation.MolarityMassResult
import com.dyfl.labcalculator.calculation.MolarityVolumeUnit
import com.dyfl.labcalculator.calculation.parseLabDecimal
import java.math.BigDecimal
import java.net.URLDecoder
import java.net.URLEncoder

internal const val MAX_PRESET_NAME_LENGTH = 60
internal const val MAX_PRESETS_PER_CALCULATOR = 20

internal enum class PresetKind {
    DILUTION, MS_MSD, MOLARITY_MASS, UNIT_CONVERSION
}

/** Preparation settings only: measured sample results and calculated outputs are never saved. */
internal sealed interface PresetSettings {
    val kind: PresetKind

    data class Dilution(val input: DilutionInput) : PresetSettings {
        override val kind = PresetKind.DILUTION
    }

    data class MsMsd(
        val dilutionFactor: String,
        val finalSpikeConcentration: String,
        val concentrationUnit: ConcentrationUnit
    ) : PresetSettings {
        override val kind = PresetKind.MS_MSD
    }

    data class MolarityMass(val input: MolarityMassInput) : PresetSettings {
        override val kind = PresetKind.MOLARITY_MASS
    }

    data class Conversion(val fromUnit: MetricUnit, val toUnit: MetricUnit) : PresetSettings {
        override val kind = PresetKind.UNIT_CONVERSION
    }
}

internal fun PresetSettings.validationError(): String? = when (this) {
    is PresetSettings.Dilution -> when (val result = DilutionCalculator.calculate(input)) {
        is DilutionResult.Success -> null
        is DilutionResult.Invalid -> result.errors.first().message
    }
    is PresetSettings.MolarityMass -> when (val result = MolarityMassCalculator.calculate(input)) {
        is MolarityMassResult.Success -> null
        is MolarityMassResult.Invalid -> result.errors.first().message
    }
    is PresetSettings.MsMsd -> positiveNumberError(dilutionFactor, "sample dilution factor")
        ?: positiveNumberError(finalSpikeConcentration, "final spike concentration")
    is PresetSettings.Conversion -> if (fromUnit.category == toUnit.category) null
        else "Choose starting and destination units from the same category."
}

private fun positiveNumberError(text: String, label: String): String? {
    if (text.isBlank()) return "Enter the $label."
    return try {
        if (parseLabDecimal(text.trim()) > BigDecimal.ZERO) null
        else "The $label must be greater than zero."
    } catch (error: NumberFormatException) {
        error.message
    }
}

internal data class CalculatorPreset(val id: String, val name: String, val settings: PresetSettings)

/** Versioned, escaped records use only JVM APIs, including on Android 7. */
internal object PresetCodec {
    fun encode(preset: CalculatorPreset): String {
        val values = when (val settings = preset.settings) {
            is PresetSettings.Dilution -> with(settings.input) {
                listOf(stockConcentration, stockUnit.name, finalConcentration,
                    finalUnit.name, finalSolutionVolumeMl)
            }
            is PresetSettings.MsMsd -> with(settings) {
                listOf(dilutionFactor, finalSpikeConcentration, concentrationUnit.name)
            }
            is PresetSettings.MolarityMass -> with(settings.input) {
                listOf(molarity, finalSolutionVolume, volumeUnit.name, formulaWeight)
            }
            is PresetSettings.Conversion -> listOf(settings.fromUnit.name, settings.toUnit.name)
        }
        return (listOf("1", preset.id, preset.name, preset.settings.kind.name) + values)
            .joinToString("|") { URLEncoder.encode(it, "UTF-8") }
    }

    fun decode(record: String): CalculatorPreset? {
        if (record.length > 8192) return null
        return try {
            val parts = record.split('|').map { URLDecoder.decode(it, "UTF-8") }
            if (parts.size < 4 || parts[0] != "1" || parts[1].isBlank() ||
                parts[1].length > 36 || parts[2].isBlank() ||
                parts[2].length > MAX_PRESET_NAME_LENGTH) return null
            val values = parts.drop(4)
            val settings = when (PresetKind.valueOf(parts[3])) {
                PresetKind.DILUTION -> {
                    if (values.size != 5) return null
                    PresetSettings.Dilution(DilutionInput(values[0],
                        ConcentrationUnit.valueOf(values[1]), values[2],
                        ConcentrationUnit.valueOf(values[3]), values[4]))
                }
                PresetKind.MS_MSD -> {
                    if (values.size != 3) return null
                    PresetSettings.MsMsd(values[0], values[1], ConcentrationUnit.valueOf(values[2]))
                }
                PresetKind.MOLARITY_MASS -> {
                    if (values.size != 4) return null
                    PresetSettings.MolarityMass(MolarityMassInput(values[0], values[1],
                        MolarityVolumeUnit.valueOf(values[2]), values[3]))
                }
                PresetKind.UNIT_CONVERSION -> {
                    if (values.size != 2) return null
                    PresetSettings.Conversion(MetricUnit.valueOf(values[0]), MetricUnit.valueOf(values[1]))
                }
            }
            if (settings.validationError() != null) null
            else CalculatorPreset(parts[1], parts[2], settings)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
