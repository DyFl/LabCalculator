# Dilution preparation and preset status — October 1, 2026

This records the first October 1 pass. The subsequent [concentration-units pass](concentration-units.md) adds explicit mg/L/µg/L support and quantity-preserving unit changes while retaining the output and storage behavior described here.

Dilution now presents a preparation instruction: “Transfer 625 µL of stock and make up to 12.5 mL final solution volume.” The result copy button copies the complete instruction with units and approximation marks. Final solution volume includes the stock; it is never presented as solvent to add. PPM/PPB assumptions, validation, presets, and mathematical results are unchanged.

## Display policy

- Stock transfers strictly between zero and 1 mL use µL. Transfers at or above 1 mL, and zero, use mL. Final volume always uses mL.
- The result retains a reduced rational quantity in mL and the exact final-volume decimal. Conversion to µL multiplies the rational value by exactly 1000. Display strings are never parsed back into quantities.
- Terminating quantities with at most 12 significant digits are displayed exactly after stripping trailing zeros. Other quantities use six significant digits, rounded half up and prefixed with `≈`. Approximation is marked independently on stock and final volumes.
- Plain notation is used up to 16 characters; longer displays use scientific notation. Very small values do not round to a false zero. At the unit threshold, an exact value just below 1 mL can display as `≈ 1000 µL`; the unit still reflects the exact quantity.
- For example, 1/3 mL displays as `≈ 333.333 µL`, and 1/6 mL as `≈ 166.667 µL`. Expand Calculation Steps for `1/3 mL = 1000/3 µL` or `1/6 mL = 500/3 µL`, along with the concentration conversion, substitution, and exact final volume. Repeating decimals no longer use the ambiguous `R` suffix, and long repeat cycles do not need decimal expansion.
- Display precision does not represent pipette capability, measurement uncertainty, or an SOP's required rounding. No equipment limits or serial-dilution planning were added.

The instruction uses the shared result card at the theme's title size so the sentence can wrap at narrow widths and large font scales. Calculation Steps remain expandable, selectable, and copyable by text selection. Tab and Android restoration rebuild exact working from the original input strings and a saved successful-calculation flag. Editing any input or unit, clearing, or applying a preset clears the previous result and working.

## Preset storage

`CalculatorPresetStore.load()` now returns one of five explicit statuses:

| Status | User-visible behavior |
| --- | --- |
| Empty (absent key or empty record set) | “No saved presets”; Load is disabled |
| Available | Presets can be loaded and deleted |
| Partly available | “Some saved presets cannot be loaded by this app. Available presets can still be used.” Load remains enabled |
| Stored records unavailable | “Saved presets are stored, but none can be loaded by this app.” Load is disabled |
| Underlying stored value unreadable | “Saved preset storage could not be read. Existing data has been kept.” Load is disabled; saves and deletes refuse replacement |

Unavailable does not mean corrupt: unsupported versions, malformed records, unknown units or kinds, invalid settings, and records stored under a different calculator's key are unavailable to this app. The load result retains usable presets and counts unavailable records. These messages appear beside the controls and inside an open load dialog, including after deletion or an observed storage change.

Saves add to the original raw set. Deletes remove only records that decode to the selected complete preset, preserving different records even when they share its ID. Unrelated unsupported, malformed, wrong-kind, and noncanonical records retain their original encoding. The v1 codec, name rules, capacity, and preparation-only storage remain compatible. No preset editing, export/import, repair, or destructive recovery was added.

## Verification

- `gradlew.bat --offline testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest --console=plain` passed. All **83 JVM tests** passed with zero failures, errors, or skips; app and instrumentation APKs built successfully.
- Lint passed with **zero errors and the same 19 existing warnings** for dependencies, manifest configuration, and unused resources.
- All **29 Android tests** passed on BlueStacks Android 9 / API 28 at a Compose-measured **360 dp width with 1.3× text**. Coverage includes exact/approximate preparation copying, exact working after tab/state restoration, stale-result clearing, all five storage statuses, preset usability and deletion beside unavailable records, unrelated raw-record preservation, and existing calculator workflows. Existing shared tests also inspect light/dark layouts.
- The three focused dilution/preset UI tests additionally passed at a measured **320 dp width with 1.5× text**. Text-layout assertions and screenshot inspection cover the preparation instruction, approximation note, exact fraction working, unavailable/unreadable messages, and the partial-load dialog.
- The offline `connectedDebugAndroidTest` Gradle task could not resolve the uncached `android-test-plugin-host-additional-test-output:32.3.1` UTP dependency. The compiled APKs were installed through ADB and tests executed directly through `AndroidJUnitRunner`; this is an executed Android suite, not a successful Gradle connected-test task.
- Logs, viewport measurements, and inspected screenshots are under `app/build/reports/dilution-presets/`, including `android-tests-360dp-1.3.txt` and `android-tests-320dp-1.5.txt`. An initial run passed at 480 dp; the 360 dp run was repeated after checking the actual Compose viewport. Original emulator settings (1280 × 720, density 240, font scale 1.0) were restored.
- `git diff --check` passed. Physical devices, TalkBack, other Android versions, and keyboard implementations remain unverified.

No commit, release, version bump, signing change, or concentration-unit expansion is part of this round.
