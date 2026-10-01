package com.dyfl.labcalculator.presets

import java.util.Locale

/** Unavailable records may belong to a newer app; they are not necessarily corrupt. */
internal sealed interface PresetLoadResult {
    val presets: List<CalculatorPreset> get() = emptyList()

    data object Empty : PresetLoadResult
    data class Available(override val presets: List<CalculatorPreset>) : PresetLoadResult
    data class PartiallyAvailable(
        override val presets: List<CalculatorPreset>,
        val unavailableRecordCount: Int
    ) : PresetLoadResult
    data class Unavailable(val recordCount: Int) : PresetLoadResult
    data object Unreadable : PresetLoadResult
}

internal val PresetLoadResult.message: String? get() = when (this) {
    PresetLoadResult.Empty -> "No saved presets"
    is PresetLoadResult.Available -> null
    is PresetLoadResult.PartiallyAvailable ->
        "Some saved presets cannot be loaded by this app. Available presets can still be used."
    is PresetLoadResult.Unavailable ->
        "Saved presets are stored, but none can be loaded by this app."
    PresetLoadResult.Unreadable ->
        "Saved preset storage could not be read. Existing data has been kept."
}

internal fun loadPresetRecords(records: Set<String>, kind: PresetKind): PresetLoadResult {
    if (records.isEmpty()) return PresetLoadResult.Empty
    val presets = records.mapNotNull(PresetCodec::decode)
        .filter { it.settings.kind == kind }
        .sortedBy { it.name.lowercase(Locale.ROOT) }
    val unavailable = records.size - presets.size
    return when {
        presets.isEmpty() -> PresetLoadResult.Unavailable(records.size)
        unavailable > 0 -> PresetLoadResult.PartiallyAvailable(presets, unavailable)
        else -> PresetLoadResult.Available(presets)
    }
}
