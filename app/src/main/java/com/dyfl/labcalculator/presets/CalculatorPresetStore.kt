package com.dyfl.labcalculator.presets

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.util.Locale
import java.util.UUID

internal sealed interface SavePresetResult {
    data class Saved(val preset: CalculatorPreset) : SavePresetResult
    data class Invalid(val message: String) : SavePresetResult
}

internal class CalculatorPresetStore(
    context: Context,
    preferencesName: String = "calculator_presets"
) {
    private val preferences = context.applicationContext
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun load(kind: PresetKind): List<CalculatorPreset> {
        val records = try {
            preferences.getStringSet(key(kind), emptySet()).orEmpty()
        } catch (_: ClassCastException) {
            emptySet()
        }
        return records.mapNotNull(PresetCodec::decode)
            .filter { it.settings.kind == kind }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    /** Call writes off the UI thread so disk completion can be checked. */
    fun save(name: String, settings: PresetSettings): SavePresetResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return SavePresetResult.Invalid("Enter a preset name.")
        if (trimmedName.length > MAX_PRESET_NAME_LENGTH) {
            return SavePresetResult.Invalid("Use a name of at most $MAX_PRESET_NAME_LENGTH characters.")
        }
        settings.validationError()?.let { return SavePresetResult.Invalid(it) }
        val existing = load(settings.kind)
        if (existing.any { it.name.equals(trimmedName, ignoreCase = true) }) {
            return SavePresetResult.Invalid("A preset with this name already exists.")
        }
        if (existing.size >= MAX_PRESETS_PER_CALCULATOR) {
            return SavePresetResult.Invalid("Delete a preset before saving another (limit $MAX_PRESETS_PER_CALCULATOR).")
        }
        val preset = CalculatorPreset(UUID.randomUUID().toString(), trimmedName, settings)
        return if (write(settings.kind, existing + preset)) SavePresetResult.Saved(preset)
        else SavePresetResult.Invalid("Could not save the preset. Please try again.")
    }

    fun delete(preset: CalculatorPreset): Boolean =
        write(preset.settings.kind, load(preset.settings.kind).filterNot { it.id == preset.id })

    fun observe(kind: PresetKind, onChanged: () -> Unit): () -> Unit {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == null || changedKey == key(kind)) onChanged()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun key(kind: PresetKind) = "v1_${kind.name}"

    // Keep commit's return value: the KTX edit helper cannot report a failed disk write.
    @SuppressLint("UseKtx")
    private fun write(kind: PresetKind, presets: List<CalculatorPreset>): Boolean =
        preferences.edit().putStringSet(key(kind), presets.map(PresetCodec::encode).toSet()).commit()
}
