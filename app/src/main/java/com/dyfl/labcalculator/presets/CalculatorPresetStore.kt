package com.dyfl.labcalculator.presets

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

internal sealed interface SavePresetResult {
    data class Saved(val preset: CalculatorPreset) : SavePresetResult
    data class Invalid(val message: String) : SavePresetResult
}

internal sealed interface DeletePresetResult {
    data object Deleted : DeletePresetResult
    data class Invalid(val message: String) : DeletePresetResult
}

internal class CalculatorPresetStore(
    context: Context,
    preferencesName: String = "calculator_presets"
) {
    private val preferences = context.applicationContext
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun load(kind: PresetKind): PresetLoadResult {
        val records = readRecords(kind) ?: return PresetLoadResult.Unreadable
        return loadPresetRecords(records, kind)
    }

    /** Null means the stored value cannot be safely rewritten as a set of records. */
    private fun readRecords(kind: PresetKind): Set<String>? {
        val value = preferences.all[key(kind)] ?: return emptySet()
        if (value !is Set<*> || value.any { it !is String }) return null
        // SharedPreferences collections must never be modified in place.
        return value.filterIsInstance<String>().toSet()
    }

    /** Call writes off the UI thread so disk completion can be checked. */
    fun save(name: String, settings: PresetSettings): SavePresetResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return SavePresetResult.Invalid("Enter a preset name.")
        if (trimmedName.length > MAX_PRESET_NAME_LENGTH) {
            return SavePresetResult.Invalid("Use a name of at most $MAX_PRESET_NAME_LENGTH characters.")
        }
        settings.validationError()?.let { return SavePresetResult.Invalid(it) }
        val records = readRecords(settings.kind)
            ?: return SavePresetResult.Invalid(UNREADABLE_STORAGE_MESSAGE)
        val existing = loadPresetRecords(records, settings.kind).presets
        if (existing.any { it.name.equals(trimmedName, ignoreCase = true) }) {
            return SavePresetResult.Invalid("A preset with this name already exists.")
        }
        if (existing.size >= MAX_PRESETS_PER_CALCULATOR) {
            return SavePresetResult.Invalid("Delete a preset before saving another (limit $MAX_PRESETS_PER_CALCULATOR).")
        }
        val preset = CalculatorPreset(UUID.randomUUID().toString(), trimmedName, settings)
        return if (write(settings.kind, records + PresetCodec.encode(preset))) SavePresetResult.Saved(preset)
        else SavePresetResult.Invalid("Could not save the preset. Please try again.")
    }

    fun delete(preset: CalculatorPreset): DeletePresetResult {
        val kind = preset.settings.kind
        val records = readRecords(kind)
            ?: return DeletePresetResult.Invalid(UNREADABLE_STORAGE_MESSAGE)
        val remaining = records.filterNot { record ->
            val decoded = PresetCodec.decode(record)
            decoded == preset
        }.toSet()
        return if (write(kind, remaining)) DeletePresetResult.Deleted
        else DeletePresetResult.Invalid("Could not delete the preset. Please try again.")
    }

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
    private fun write(kind: PresetKind, records: Set<String>): Boolean =
        preferences.edit().putStringSet(key(kind), records).commit()

    private companion object {
        const val UNREADABLE_STORAGE_MESSAGE =
            "Saved presets could not be read safely. No presets were changed."
    }
}
