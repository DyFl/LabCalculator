package com.dyfl.labcalculator

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.DilutionInput
import com.dyfl.labcalculator.presets.CalculatorPresetStore
import com.dyfl.labcalculator.presets.CalculatorPreset
import com.dyfl.labcalculator.presets.DeletePresetResult
import com.dyfl.labcalculator.presets.MAX_PRESETS_PER_CALCULATOR
import com.dyfl.labcalculator.presets.PresetKind
import com.dyfl.labcalculator.presets.PresetCodec
import com.dyfl.labcalculator.presets.PresetSettings
import com.dyfl.labcalculator.presets.SavePresetResult
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferencesName = "test_presets_${UUID.randomUUID()}"
    private val store = CalculatorPresetStore(context, preferencesName)
    private val preparation = PresetSettings.MsMsd("10", "50", ConcentrationUnit.PPM)

    @After
    fun cleanUp() {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun savedPreparationSurvivesNewStoreAndDeletionKeepsOtherPresets() {
        val first = (store.save(" Metals | daily ", preparation) as SavePresetResult.Saved).preset
        val second = (store.save("Other", preparation.copy(dilutionFactor = "2.5"))
            as SavePresetResult.Saved).preset
        val reopened = CalculatorPresetStore(context, preferencesName)
        assertEquals(listOf(first, second), reopened.load(PresetKind.MS_MSD))
        assertEquals(DeletePresetResult.Deleted, reopened.delete(first))
        assertEquals(listOf(second), CalculatorPresetStore(context, preferencesName).load(PresetKind.MS_MSD))
    }

    @Test
    fun duplicateNamesDoNotOverwriteAndDifferentCalculatorsAreIsolated() {
        val original = (store.save("Daily", preparation) as SavePresetResult.Saved).preset
        assertTrue(store.save(" daily ", preparation.copy(dilutionFactor = "5")) is SavePresetResult.Invalid)
        assertEquals(listOf(original), store.load(PresetKind.MS_MSD))
        val dilution = PresetSettings.Dilution(DilutionInput(
            "10", ConcentrationUnit.PPM, "200", ConcentrationUnit.PPB, "50"))
        assertTrue(store.save("Daily", dilution) is SavePresetResult.Saved)
        assertEquals(listOf(original), store.load(PresetKind.MS_MSD))
        assertEquals(dilution, store.load(PresetKind.DILUTION).single().settings)
    }

    @Test
    fun invalidSettingsAndCapacityLimitDoNotEraseExistingPresets() {
        val unreadable = "2|future-id|Future preparation|MS_MSD|1|50|PPB"
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        assertTrue(preferences.edit().putStringSet("v1_MS_MSD", setOf(unreadable)).commit())
        assertTrue(store.save("", preparation) is SavePresetResult.Invalid)
        assertTrue(store.save("Invalid", preparation.copy(finalSpikeConcentration = "0")) is SavePresetResult.Invalid)
        assertTrue(store.load(PresetKind.MS_MSD).isEmpty())
        repeat(MAX_PRESETS_PER_CALCULATOR) { index ->
            assertTrue(store.save("Preset $index", preparation) is SavePresetResult.Saved)
        }
        val existing = store.load(PresetKind.MS_MSD)
        assertTrue(store.save("One more", preparation) is SavePresetResult.Invalid)
        assertEquals(existing, store.load(PresetKind.MS_MSD))
        assertTrue(preferences.getStringSet("v1_MS_MSD", emptySet())!!.contains(unreadable))
    }

    @Test
    fun savesAndValidDeletionsPreserveUnrelatedRawRecords() {
        val first = CalculatorPreset("first-id", "Daily", preparation)
        val second = CalculatorPreset("second-id", "Other", preparation)
        val firstRecord = PresetCodec.encode(first)
        // A readable but noncanonical encoding must also survive byte-for-byte.
        val secondRecord = PresetCodec.encode(second).replace("Other", "%4fther")
        val dilution = CalculatorPreset(first.id, "Dilution", PresetSettings.Dilution(DilutionInput(
            "10", ConcentrationUnit.PPM, "200", ConcentrationUnit.PPB, "50")))
        val dilutionRecord = PresetCodec.encode(dilution)
        val unrelated = setOf(
            "1|corrupt-id|%ZZ|MS_MSD|1|50|PPB",
            "2|first-id|Future version|MS_MSD|1|50|PPB",
            "1|invalid-id|Invalid settings|MS_MSD|0|50|PPB",
            "1|unknown-id|Unknown units|MS_MSD|1|50|FUTURE_UNIT",
            dilutionRecord
        )
        val original = unrelated + firstRecord + secondRecord
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        assertTrue(preferences.edit().putStringSet("v1_MS_MSD", original)
            .putStringSet("v1_DILUTION", setOf(dilutionRecord)).commit())
        assertEquals(listOf(first, second), store.load(PresetKind.MS_MSD))

        val saved = (store.save("New", preparation) as SavePresetResult.Saved).preset
        val savedRecord = PresetCodec.encode(saved)
        assertEquals(original + savedRecord, preferences.getStringSet("v1_MS_MSD", emptySet()))
        assertTrue(store.save(" daily ", preparation) is SavePresetResult.Invalid)
        assertEquals(original + savedRecord, preferences.getStringSet("v1_MS_MSD", emptySet()))

        assertEquals(DeletePresetResult.Deleted, store.delete(first))
        assertEquals(unrelated + secondRecord + savedRecord,
            preferences.getStringSet("v1_MS_MSD", emptySet()))
        assertEquals(listOf(saved, second), CalculatorPresetStore(context, preferencesName).load(PresetKind.MS_MSD))
        assertEquals(setOf(dilutionRecord), preferences.getStringSet("v1_DILUTION", emptySet()))
        assertEquals(listOf(dilution), store.load(PresetKind.DILUTION))
    }

    @Test
    fun unexpectedStoredValueTypesRefuseMutationsWithoutReplacement() {
        val preset = (store.save("Daily", preparation) as SavePresetResult.Saved).preset
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        for (value in listOf("unreadable collection", 42, true)) {
            val editor = preferences.edit()
            when (value) {
                is String -> editor.putString("v1_MS_MSD", value)
                is Int -> editor.putInt("v1_MS_MSD", value)
                is Boolean -> editor.putBoolean("v1_MS_MSD", value)
            }
            assertTrue(editor.commit())
            assertTrue(store.load(PresetKind.MS_MSD).isEmpty())
            val saveResult = store.save("New", preparation) as SavePresetResult.Invalid
            assertTrue(saveResult.message.contains("could not be read safely"))
            val deleteResult = store.delete(preset) as DeletePresetResult.Invalid
            assertTrue(deleteResult.message.contains("could not be read safely"))
            assertEquals(value, preferences.all["v1_MS_MSD"])
        }
    }

    @Test
    fun changesFromAnotherStoreRefreshSubscribers() {
        val changed = CountDownLatch(1)
        val unsubscribe = store.observe(PresetKind.MS_MSD) { changed.countDown() }
        try {
            val writer = CalculatorPresetStore(context, preferencesName)
            assertTrue(writer.save("Daily", preparation) is SavePresetResult.Saved)
            assertTrue(changed.await(5, TimeUnit.SECONDS))
            assertEquals(preparation, store.load(PresetKind.MS_MSD).single().settings)
        } finally {
            unsubscribe()
        }
    }
}
