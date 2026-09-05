package com.dyfl.labcalculator.calculation

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MolarityMassCalculatorTest {
    private val example = MolarityMassInput("0.02", "500", MolarityVolumeUnit.MILLILITER, "40.00")

    @Test
    fun `requested examples use five significant figures`() {
        val cases = listOf(
            example to "0.40000",
            example.copy(finalSolutionVolume = "250", formulaWeight = "58.44") to "0.29220",
            example.copy(molarity = "0.1", finalSolutionVolume = "1",
                volumeUnit = MolarityVolumeUnit.LITER) to "4.0000",
            example.copy(molarity = "0.005", finalSolutionVolume = "100",
                formulaWeight = "180.16") to "0.090080"
        )
        cases.forEach { (input, expected) ->
            val result = success(input)
            assertEquals(expected, result.formattedMassGrams)
            assertEquals(0, BigDecimal(expected).compareTo(result.exactMassGrams))
            assertTrue(result.calculationSteps.last().contains("Required Mass = $expected g"))
        }
    }

    @Test
    fun `milliliters and liters give the same exact mass`() {
        val ml = success(example)
        val liters = success(example.copy(finalSolutionVolume = "0.5", volumeUnit = MolarityVolumeUnit.LITER))
        assertEquals(0, BigDecimal("0.5").compareTo(ml.volumeLiters))
        assertEquals(0, ml.exactMassGrams.compareTo(liters.exactMassGrams))
        assertEquals(ml.formattedMassGrams, liters.formattedMassGrams)
        assertTrue(ml.calculationSteps.first().contains("500 mL ÷ 1,000 = 0.500 L"))
        assertTrue(liters.calculationSteps.first().contains("already in liters"))
    }

    @Test
    fun `steps show the actual operands and exact mass`() {
        val result = success(example.copy(finalSolutionVolume = "250", formulaWeight = "58.44"))
        assertEquals("Mass = M × V × FW.", result.calculationSteps[1])
        assertEquals("Mass = 0.02 mol/L × 0.250 L × 58.44 g/mol.", result.calculationSteps[2])
        assertTrue(result.calculationSteps[3].contains("0.2922000 g before display rounding"))
    }

    @Test
    fun `formatting pads and rounds significant figures across magnitudes`() {
        val cases = mapOf(
            "2.922" to "2.9220", "12.345" to "12.345", "0.0025" to "0.0025000",
            "1.23456" to "1.2346", "9.99995" to "10.000",
            "0.000001" to "0.0000010000", "0.0000001" to "1.0000E-7",
            "9999999" to "1.0000E+7", "1e100" to "1.0000E+100"
        )
        cases.forEach { (mass, expected) ->
            val result = success(MolarityMassInput(mass, "1", MolarityVolumeUnit.LITER, "1"))
            assertEquals(expected, result.formattedMassGrams)
            assertEquals(0, BigDecimal(mass).compareTo(result.exactMassGrams))
        }
    }

    @Test
    fun `intermediate values are not rounded`() {
        val result = success(MolarityMassInput("1.234567", "1.234567", MolarityVolumeUnit.LITER, "1.234567"))
        assertEquals(BigDecimal("1.881672302290562263"), result.exactMassGrams)
        assertEquals("1.8817", result.formattedMassGrams)
    }

    @Test
    fun `zero molarity returns zero grams`() {
        assertEquals("0", success(example.copy(molarity = "0")).formattedMassGrams)
    }

    @Test
    fun `blank malformed and extreme inputs produce field errors`() {
        listOf("", " ", "abc", "1.2.3", "NaN", "Infinity", "1e2147483647", "1e-2147483647",
            "1".repeat(257)).forEach { value ->
            val result = MolarityMassCalculator.calculate(example.copy(
                molarity = value, finalSolutionVolume = value, formulaWeight = value
            ))
            assertTrue("Expected invalid input for $value", result is MolarityMassResult.Invalid)
            assertEquals(MolarityMassField.entries.toSet(),
                (result as MolarityMassResult.Invalid).errors.map { it.field }.toSet())
            assertTrue(result.errors.all { it.message.isNotBlank() })
        }
    }

    @Test
    fun `negative inputs and zero volume or formula weight are rejected`() {
        val negative = MolarityMassCalculator.calculate(example.copy(
            molarity = "-0.1", finalSolutionVolume = "-1", formulaWeight = "-1"
        )) as MolarityMassResult.Invalid
        assertEquals(3, negative.errors.size)
        assertEquals("Desired molarity cannot be negative.", negative.errors.first().message)
        val zero = MolarityMassCalculator.calculate(example.copy(
            finalSolutionVolume = "0", formulaWeight = "0"
        )) as MolarityMassResult.Invalid
        assertEquals(setOf(MolarityMassField.FINAL_SOLUTION_VOLUME, MolarityMassField.FORMULA_WEIGHT),
            zero.errors.map { it.field }.toSet())
        assertTrue(zero.errors.all { it.message.contains("must be greater than zero") })
    }

    @Test
    fun `surrounding whitespace is accepted`() {
        assertEquals("0.40000", success(example.copy(molarity = " 0.02 ")).formattedMassGrams)
    }

    private fun success(input: MolarityMassInput): MolarityMassResult.Success {
        val result = MolarityMassCalculator.calculate(input)
        assertTrue("Expected success but received $result", result is MolarityMassResult.Success)
        return result as MolarityMassResult.Success
    }
}
