# Concentration units and basis — October 1, 2026

This separate pass builds on the dilution preparation and preset-status work. Existing exact rational dilution outputs, µL/mL instruction formatting, approximation marks, and all five preset load statuses remain in place.

## Supported concentration families

| Family | Supported units | Exact relation |
| --- | --- | --- |
| Parts per, on the same underlying ratio basis | PPM, PPB | 1 PPM = 1,000 PPB |
| Mass per volume | mg/L, µg/L | 1 mg/L = 1,000 µg/L |

These families are distinct. There is no PPM↔mg/L or PPB↔µg/L conversion, water/density assumption, or implicit choice of mass/mass versus another parts-per basis. The legacy PPM/PPB interpretation remains the same; the operator must supply concentrations on a matching basis appropriate to the dilution relationship. No additional concentration units are added to dilution or MS/MSD.

Dilution requires stock and target to have compatible families. Within the family, it normalizes to PPB or µg/L using exact decimal factors before solving `V₁ = C₂ × V₂ / C₁`. The stock transfer remains an exact reduced rational quantity. For example, 10 mg/L stock, 200 µg/L target, and 50 mL final volume require exactly 1 mL of stock. A zero target still requires compatible units.

## Unit-change interaction policy

Unit selectors preserve an existing quantity by converting its number exactly. They do not relabel populated numbers or reinterpret them as a different physical quantity. To enter a different quantity in a chosen compatible unit, select that unit and then edit its number.

- **Dilution:** changing one compatible selector converts that field; the other concentration keeps its own compatible unit. Different units within a family are intentional and normalized during calculation.
- **MS/MSD:** the one **Shared concentration unit** selector is in **Preparation settings**. It converts source, spike, MS and MSD as one operation. The dimensionless dilution factor is untouched. Static unit suffixes appear beside all concentrations; the original-source result and exact working carry the selected unit. Recoveries and RPD remain percentages.
- **Blank fields:** stay blank, including during a shared conversion. Selecting a unit on an empty field specifies the unit for a future entry.
- **Invalid numeric text or range:** blocks the complete change. Units and every affected value stay unchanged; the UI reports the block. Conversions must fit the calculation parser's 256-character and decimal-scale bounds. Long exact numbers may use scientific notation; conversion never rounds.
- **Numeric values outside a calculation rule:** zero/negative quantities remain exactly zero/negative after conversion. Existing validation errors remain, and calculation continues to enforce its original sign rules. Signed MS/MSD measurements remain supported.
- **Incompatible family:** **Clear and change** requires an explicit confirmation. It clears both dilution concentrations or all four MS/MSD concentrations, sets the selected new unit, clears results/steps and concentration errors, and retains volume or dilution factor and its validation errors. Re-enter concentrations using the new basis. **Cancel**, back, and dismissal preserve the entire form. The pending dialog survives Android restoration.
- **Compatible changes:** clear calculated results and working, retain still-relevant field errors, and require a fresh Calculate. Selecting the current unit keeps state.

The same policy fixes two closely related inconsistencies: molarity volume changes preserve the exact mL/L quantity; the standalone converter's starting selector and swap preserve its input quantity. Invalid input blocks these changes. Converter destination changes only select the output unit and retain input errors. Converter category changes use the same explicit reset/re-entry dialog. Re-selecting a category keeps state.

## MS/MSD preparation and formulas

The preparation basis appears above the form, even when Formula and assumptions is collapsed:

- Spike added **after sample dilution**.
- Source, literal MS and literal MSD measurements are **uncorrected**, on the same dilution and concentration basis.
- Spike is the **final concentration added to each diluted aliquot**.
- Spike volume is **negligible**, so it does not materially change the native sample concentration.

The formulas are unchanged: original source = raw source × dilution factor; recoveries = `(MS or MSD − raw source) / spike × 100`; RPD = `|MS − MSD| / |(MS + MSD) / 2| × 100`. RPD compares measured concentrations. A zero average keeps recoveries available and shows undefined RPD. No pre-dilution spike mode, density conversion, QC limits, or pass/fail evaluation was introduced.

For independently checked equivalent examples, 0.005 mg/L source, factor 10, 0.05 mg/L spike, 0.055 mg/L MS and 0.05 mg/L MSD give 0.05 mg/L original source, 100.00%/90.00% recovery, and 9.52% concentration RPD. The equivalent µg/L entries are 5, 50, 55 and 50, giving 50 µg/L original source and the same percentages.

Copied dilution instructions retain volume units and approximation marks. Expanded dilution working names the concentration family, exact normalization and unit cancellation. Copied original-source concentrations include mg/L, µg/L, PPM or PPB as selected; copied percentages keep `%`. MS/MSD working states the basis and uses the shared unit in every concentration term.

## Presets and saved state

The storage shape and version remain **v1**; fields already contain explicit enum unit names, so no migration or version bump is needed. PPM and PPB enum names, storage keys, and numerical interpretation are unchanged. New records use `MILLIGRAM_PER_LITER` and `MICROGRAM_PER_LITER` in the same slots. Builds that do not recognize these names cannot load those records; the current store retains unavailable raw records and reports accurate availability instead of discarding them.

Loading a dilution preset replaces its concentrations, units and final volume and clears results. Loading MS/MSD replaces the shared unit, spike and dilution factor, and clears sample measurements/results. **Next sample** keeps shared units, spike and dilution factor. **Clear all** restores the original defaults (dilution PPM/PPB; MS/MSD PPB/factor 1). Tab switching and Android saved state retain new units, converted inputs, errors, results and pending reset dialogs. Presets contain no measurements or calculated outputs.

Existing unavailable, unsupported-version, wrong-kind and noncanonical unrelated records are still preserved by save/delete. Mixed-family dilution recipes fail validation and are unavailable on load. Valid old and new recipes remain usable beside unavailable records.

## Verification

- **JVM:** all 100 tests passed, with zero failures/errors. The 17 added tests cover independently expected mg/L/µg/L dilution and MS/MSD results, exact compatible changes, every cross-family dilution pairing (including zero), blank/invalid/range behavior, long exact inputs, legacy PPM/PPB, and literal old/new preset round trips.
- **Build/lint:** `testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest --offline --console=plain` passed. Lint reports zero errors and the same 19 existing warnings. No dependency or toolchain changes were needed.
- **Android:** the complete 38-test suite passed at 360 dp / 1.3× text on the connected emulator. All 11 focused new-unit and prior dilution/preset UI tests also passed at 320 dp / 1.5× text. Checks cover atomic shared conversion, invalid/blank forms, reset/cancel, stale-result clearing, independent errors, copying, Next sample, Clear all, old/new presets beside unavailable records, tabs, and Android restoration. The 320 dp run caught dialog text clipping; shorter scrollable reset explanations corrected it. Automated text-layout checks and screenshot inspection confirm the final reset dialogs fit.
- **Runner limitation:** Gradle's offline connected task was attempted and still cannot resolve the uncached `android-test-plugin-host-additional-test-output:32.3.1` UTP dependency. APKs were installed and tests executed directly with ADB / AndroidJUnitRunner; no dependency downloads were made.
- **Local reports:** `app/build/reports/concentration-units/android-tests-360dp-1.3.txt` and `android-tests-320dp-1.5.txt`. Final screenshots and measured viewport text are in `screenshots-360dp-1.3/concentration-units/` and `screenshots-320dp-1.5/concentration-units/` beneath that directory; the parent screenshot directories retain earlier captures.

Emulator size, density and font scale were restored after each run. Physical-device, TalkBack, other Android versions and keyboard implementations remain unverified. The app can enforce unit-family compatibility, but the operator must supply the correct underlying parts-per and preparation basis. No commits, pushes, publication, release-version or signing changes were performed.
