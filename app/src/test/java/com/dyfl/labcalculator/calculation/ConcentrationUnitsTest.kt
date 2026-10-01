package com.dyfl.labcalculator.calculation

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class ConcentrationUnitsTest {
    private val mg = ConcentrationUnit.MILLIGRAM_PER_LITER
    private val ug = ConcentrationUnit.MICROGRAM_PER_LITER

    private fun dilution(stock: String, stockUnit: ConcentrationUnit, target: String,
        targetUnit: ConcentrationUnit, volume: String = "50") =
        DilutionCalculator.calculate(DilutionInput(stock, stockUnit, target, targetUnit, volume))

    private fun success(stock: String, stockUnit: ConcentrationUnit, target: String,
        targetUnit: ConcentrationUnit) = dilution(stock, stockUnit, target, targetUnit) as DilutionResult.Success

    @Test fun `mg per L and ug per L dilution examples have independently expected transfer`() {
        // 0.2/10 * 50 mL = 1 mL, regardless of which compatible units are entered.
        listOf(success("10", mg, "0.2", mg), success("10000", ug, "200", ug),
            success("10", mg, "200", ug), success("10000", ug, "0.2", mg)).forEach {
            assertEquals("1", it.volumeFromStockMl.toExactString())
            assertEquals("Transfer 1 mL of stock and make up to 50 mL final solution volume.", it.preparationInstruction)
            assertTrue(it.calculationSteps.any { step -> step.contains("cancel µg/L") })
            assertFalse(it.calculationSteps.any { step -> step.contains("PPB") })
        }
    }

    @Test fun `legacy parts per calculations retain their interpretation`() {
        listOf(success("10", ConcentrationUnit.PPM, "200", ConcentrationUnit.PPB),
            success("10000", ConcentrationUnit.PPB, "0.2", ConcentrationUnit.PPM)).forEach {
            assertEquals("1", it.volumeFromStockMl.toExactString())
            assertTrue(it.calculationSteps.any { step -> step.contains("cancel PPB") })
        }
    }

    @Test fun `every dilution cross family pairing is rejected even for zero target`() {
        for (from in ConcentrationUnit.entries) for (to in ConcentrationUnit.entries) {
            if (from.family == to.family) continue
            for (target in listOf("0", "1")) {
                val result = dilution("10", from, target, to) as DilutionResult.Invalid
                assertTrue(result.errors.any { it.message == DilutionCalculator.INCOMPATIBLE_BASIS_MESSAGE })
            }
        }
    }

    @Test fun `mass concentration comparison occurs after exact normalization`() {
        assertTrue(dilution("1", mg, "1001", ug) is DilutionResult.Invalid)
        assertEquals("50", success("1000", ug, "1", mg).volumeFromStockMl.toExactString())
        assertEquals("0", success("1", mg, "0", ug).volumeFromStockMl.toExactString())
    }

    @Test fun `MS MSD mg and ug examples preserve expected recovery and concentration RPD`() {
        // (0.055 - 0.005)/0.05 = 100%; (0.050 - 0.005)/0.05 = 90%.
        val cases = listOf(MsMsdInput("0.005", "10", "0.05", "0.055", "0.05", mg),
            MsMsdInput("5", "10", "50", "55", "50", ug))
        cases.forEachIndexed { index, input ->
            val result = MsMsdCalculator.calculate(input) as MsMsdResult.Success
            assertEquals(if (index == 0) "0.05 mg/L" else "50 µg/L", result.formattedOriginalSourceConcentration)
            assertEquals("100.00%", result.formattedMsRecovery)
            assertEquals("90.00%", result.formattedMsdRecovery)
            assertEquals("9.52%", result.formattedMsMsdRpd)
            assertTrue(result.calculationSections.first().steps.any { it.contains("negligible spike volume") })
        }
    }

    @Test fun `compatible field conversion is exact beyond six significant digits`() {
        val changed = UnitChanges.concentrations(listOf("1.234567890123456789", "0.00000001", "-2", "0"), mg, ug)
            as UnitChangeResult.Converted
        assertEquals(listOf("1234.567890123456789", "0.00001", "-2000", "0"), changed.values)
        assertEquals(listOf("1.234567890123456789", "0.00000001", "-2", "0"),
            (UnitChanges.concentrations(changed.values, ug, mg) as UnitChangeResult.Converted).values)
    }

    @Test fun `PPB PPM unit changes preserve quantities exactly`() {
        assertEquals(listOf("0.005", "0.05", "0.055", "0.05"),
            (UnitChanges.concentrations(listOf("5", "50", "55", "50"), ConcentrationUnit.PPB,
                ConcentrationUnit.PPM) as UnitChangeResult.Converted).values)
    }

    @Test fun `blanks are preserved without preventing conversion of populated fields`() {
        assertEquals(listOf("", "500", " ", "0"),
            (UnitChanges.concentrations(listOf("", "0.5", " ", "0"), mg, ug) as UnitChangeResult.Converted).values)
    }

    @Test fun `one invalid field blocks whole form with no partial replacement`() {
        for (invalid in listOf("abc", "NaN", "1e1001", "9".repeat(257))) {
            val values = listOf("5", "50", invalid, "50")
            assertTrue(UnitChanges.concentrations(values, mg, ug) is UnitChangeResult.Blocked)
            assertEquals(listOf("5", "50", invalid, "50"), values)
        }
    }

    @Test fun `out of range converted input blocks the entire change`() {
        assertTrue(UnitChanges.concentrations(listOf("1", "1e-1000"), ug, mg) is UnitChangeResult.Blocked)
        assertTrue(UnitChanges.concentrations(listOf("1e1000"), mg, ug) is UnitChangeResult.Blocked)
    }

    @Test fun `large exact quantities use parseable scientific input instead of excessive plain text`() {
        val values = (UnitChanges.concentrations(listOf("1e900", "1e-900"), mg, ug) as UnitChangeResult.Converted).values
        assertTrue(values.all { it.length <= MAX_NUMBER_INPUT_LENGTH })
        assertEquals(0, BigDecimal("1e903").compareTo(parseLabDecimal(values[0])))
        assertEquals(0, BigDecimal("1e-897").compareTo(parseLabDecimal(values[1])))
    }

    @Test fun `incompatible families signal reset instead of attempting numeric conversion`() {
        for (from in ConcentrationUnit.entries) for (to in ConcentrationUnit.entries) {
            if (from.family == to.family) continue
            assertEquals(UnitChangeResult.ResetRequired, UnitChanges.concentrations(listOf("5", ""), from, to))
            assertEquals(UnitChangeResult.ResetRequired, UnitChanges.concentrations(listOf(""), from, to))
        }
    }

    @Test fun `direct cross family conversion cannot infer density`() {
        assertThrows(IllegalArgumentException::class.java) { mg.convert(BigDecimal.ONE, ConcentrationUnit.PPM) }
        assertEquals(0, BigDecimal("1000").compareTo(mg.convert(BigDecimal.ONE, ug)))
        val standalone = UnitConverter.convert("1", MetricUnit.MILLIGRAM_PER_LITER, MetricUnit.MICROGRAM_PER_LITER)
            as UnitConversionResult.Success
        assertEquals(0, standalone.exactValue.compareTo(mg.convert(BigDecimal.ONE, ug)))
    }

    @Test fun `metric changes used by molarity and converter preserve the input quantity`() {
        assertEquals(listOf("0.25"), (UnitChanges.metric(listOf("250"), MetricUnit.MILLILITER,
            MetricUnit.LITER) as UnitChangeResult.Converted).values)
        assertEquals(listOf("2000"), (UnitChanges.metric(listOf("2"), MetricUnit.GRAM,
            MetricUnit.MILLIGRAM) as UnitChangeResult.Converted).values)
        assertTrue(UnitChanges.metric(listOf("invalid"), MetricUnit.GRAM, MetricUnit.MILLIGRAM) is UnitChangeResult.Blocked)
        assertEquals(UnitChangeResult.ResetRequired, UnitChanges.metric(listOf("2"), MetricUnit.GRAM, MetricUnit.LITER))
    }
}
