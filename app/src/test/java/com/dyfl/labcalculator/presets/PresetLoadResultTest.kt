package com.dyfl.labcalculator.presets

import com.dyfl.labcalculator.calculation.ConcentrationUnit
import org.junit.Assert.*
import org.junit.Test

class PresetLoadResultTest {
    private val preset = CalculatorPreset("id", "Daily", PresetSettings.MsMsd("1", "50", ConcentrationUnit.PPB))
    private val valid = PresetCodec.encode(preset)
    private val future = "2|id|Future|MS_MSD|1|50|PPB"

    @Test fun `empty records are genuinely empty`() {
        assertEquals(PresetLoadResult.Empty, loadPresetRecords(emptySet(), PresetKind.MS_MSD))
        assertEquals("No saved presets", PresetLoadResult.Empty.message)
    }

    @Test fun `valid and noncanonical records remain available and sorted`() {
        val other = preset.copy(id = "other-id", name = "another")
        val raw = PresetCodec.encode(other).replace("another", "%61nother")
        val result = loadPresetRecords(setOf(valid, raw), PresetKind.MS_MSD)
        assertEquals(PresetLoadResult.Available(listOf(other, preset)), result)
        assertNull(result.message)
    }

    @Test fun `mixed records report unavailable count and keep valid presets usable`() {
        val wrongKind = "1|other|Other|UNIT_CONVERSION|GRAM|MILLIGRAM"
        val records = setOf(valid, future, "%ZZ", wrongKind)
        val original = records.toSet()
        val result = loadPresetRecords(records, PresetKind.MS_MSD)
        assertEquals(PresetLoadResult.PartiallyAvailable(listOf(preset), 3), result)
        assertEquals(original, records)
        assertTrue(checkNotNull(result.message).contains("Available presets can still be used"))
    }

    @Test fun `unsupported malformed and wrong kind records are stored but unavailable`() {
        for (record in listOf(future, "%ZZ", "1|id|name|UNKNOWN_KIND")) {
            val result = loadPresetRecords(setOf(record), PresetKind.MS_MSD)
            assertEquals(PresetLoadResult.Unavailable(1), result)
            assertTrue(result.presets.isEmpty())
            assertTrue(checkNotNull(result.message).contains("stored"))
            assertFalse(checkNotNull(result.message).contains("corrupt", ignoreCase = true))
        }
    }

    @Test fun `unreadable storage has its own status and plain language message`() {
        assertTrue(PresetLoadResult.Unreadable.presets.isEmpty())
        assertEquals("Saved preset storage could not be read. Existing data has been kept.",
            PresetLoadResult.Unreadable.message)
    }
}
