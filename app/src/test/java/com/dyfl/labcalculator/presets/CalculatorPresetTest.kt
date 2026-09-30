package com.dyfl.labcalculator.presets

import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.DilutionInput
import com.dyfl.labcalculator.calculation.MetricUnit
import com.dyfl.labcalculator.calculation.MolarityMassInput
import com.dyfl.labcalculator.calculation.MolarityVolumeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorPresetTest {
    private val dilution = PresetSettings.Dilution(DilutionInput(
        "1e1", ConcentrationUnit.PPM, "200", ConcentrationUnit.PPB, "50"))
    private val molarity = PresetSettings.MolarityMass(MolarityMassInput(
        "0.02", "250", MolarityVolumeUnit.MILLILITER, "58.44"))

    @Test
    fun `all preset types preserve exact inputs units and escaped Unicode names`() {
        val settings = listOf(dilution, molarity,
            PresetSettings.MsMsd("2.5", "5e1", ConcentrationUnit.PPM),
            PresetSettings.Conversion(MetricUnit.MILLIGRAM_PER_LITER, MetricUnit.MICROGRAM_PER_LITER))
        settings.forEach {
            val preset = CalculatorPreset("a-test-id", "Cu | 50% + µg/L — daily", it)
            assertEquals(preset, PresetCodec.decode(PresetCodec.encode(preset)))
        }
    }

    @Test
    fun `unsupported corrupt or incomplete saved records cannot be loaded`() {
        listOf("", "2|id|name|MS_MSD|1|50|PPB", "1|id|%ZZ|MS_MSD|1|50|PPB",
            "1|id|name|MS_MSD|1|50|UNKNOWN", "1|id|name|MS_MSD|1|50",
            "1|id|name|FUTURE_CALCULATOR", "1|id|name|MS_MSD|1|50|PPB|unexpected",
            "x".repeat(8193)).forEach { assertNull(PresetCodec.decode(it)) }
    }

    @Test
    fun `invalid saved preparation settings are rejected on load`() {
        listOf(
            dilution.copy(input = dilution.input.copy(finalConcentration = "20000")),
            molarity.copy(input = molarity.input.copy(formulaWeight = "0")),
            PresetSettings.MsMsd("1", "0", ConcentrationUnit.PPB),
            PresetSettings.Conversion(MetricUnit.GRAM, MetricUnit.LITER)
        ).forEach {
            assertNotNull(it.validationError())
            assertNull(PresetCodec.decode(PresetCodec.encode(CalculatorPreset("id", "invalid", it))))
        }
    }

    @Test
    fun `MS MSD presets validate preparation without requiring sample measurements`() {
        assertNull(PresetSettings.MsMsd("0.5", "1e-10", ConcentrationUnit.PPB).validationError())
        listOf("", "0", "-1", "NaN", "1e1001", "9".repeat(257)).forEach { invalid ->
            assertNotNull(PresetSettings.MsMsd(invalid, "50", ConcentrationUnit.PPB).validationError())
            assertNotNull(PresetSettings.MsMsd("1", invalid, ConcentrationUnit.PPB).validationError())
        }
    }

    @Test
    fun `blank and oversized preset names are rejected on load`() {
        listOf("", " ", "x".repeat(MAX_PRESET_NAME_LENGTH + 1)).forEach { name ->
            assertNull(PresetCodec.decode(PresetCodec.encode(CalculatorPreset("id", name, dilution))))
        }
    }

    @Test
    fun `conversion presets preserve pairs across every category`() {
        listOf(MetricUnit.GRAM to MetricUnit.MICROGRAM,
            MetricUnit.LITER to MetricUnit.MILLILITER,
            MetricUnit.MILLIGRAM_PER_LITER to MetricUnit.NANOGRAM_PER_LITER).forEach { (from, to) ->
            val preset = CalculatorPreset("id", "daily", PresetSettings.Conversion(from, to))
            assertNull(preset.settings.validationError())
            assertEquals(preset, PresetCodec.decode(PresetCodec.encode(preset)))
        }
    }
}
