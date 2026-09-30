package com.dyfl.labcalculator

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.dyfl.labcalculator.calculation.ConcentrationUnit
import com.dyfl.labcalculator.calculation.DilutionInput
import com.dyfl.labcalculator.presets.CalculatorPresetStore
import com.dyfl.labcalculator.presets.MAX_PRESETS_PER_CALCULATOR
import com.dyfl.labcalculator.presets.PresetKind
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
        assertTrue(reopened.delete(first))
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
        assertTrue(store.save("", preparation) is SavePresetResult.Invalid)
        assertTrue(store.save("Invalid", preparation.copy(finalSpikeConcentration = "0")) is SavePresetResult.Invalid)
        assertTrue(store.load(PresetKind.MS_MSD).isEmpty())
        repeat(MAX_PRESETS_PER_CALCULATOR) { index ->
            assertTrue(store.save("Preset $index", preparation) is SavePresetResult.Saved)
        }
        val existing = store.load(PresetKind.MS_MSD)
        assertTrue(store.save("One more", preparation) is SavePresetResult.Invalid)
        assertEquals(existing, store.load(PresetKind.MS_MSD))
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
