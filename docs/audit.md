# Repository audit — 2026-09-20

These notes describe the September 20 audit. The September 29 follow-up at the end records subsequent changes and emulator verification.

This audit covered the five calculation engines, all Compose screens and shared controls, activity and theme, resources and screenshots, unit/instrumentation tests, Gradle configuration, CI, README, and live GitHub description. Findings were presented before edits. No scientific formula was changed.

## Critical

| Location | Finding and consequence | Proposed fix / outcome |
| --- | --- | --- |
| `DilutionCalculator.kt`, `ExactFraction.toDisplayString` | Long division searched for the entire repeat cycle without a bound. A stock of 1000000007, target of 1 in the same units, and 1 mL final volume could freeze the UI while expanding 1/1000000007. | Fixed: stop expansion after 128 digits and show the reduced exact fraction. Terminating decimals and short repeating displays remain unchanged. A timeout regression test exercises the example. |
| Parsers in `DilutionCalculator.kt`, `RpdCalculator.kt`, `MsMsdCalculator.kt`, `UnitConverter.kt` | Valid BigDecimal syntax such as `1e2147483647` could cause arithmetic exceptions or enormous allocations later. User input and expanded steps were also saved into Android state without bounds. | Fixed: shared parser enforces 256 characters and scale −1000..1000, matching the existing molarity limits. All fields report invalid input. The UI replaces an oversized edit with an explicitly invalid short marker and error, rather than silently truncating a number. |

The freeze/crash risks are from implementation inspection and bounded regression tests; the audit did not deliberately exhaust the host's memory with the original code.

## Important

| Location | Finding and consequence | Proposed fix / outcome |
| --- | --- | --- |
| `DilutionCalculatorScreen.kt`, equation card | The substitution line displayed PPB divided by PPM without the conversion factor, though the engine correctly normalized them. Copying that line could give the wrong result. | Fixed: equation uses PPB for both terms and explicitly says conversion happens first. |
| `MsMsdCalculatorScreen.kt`, input help | Dilution-corrected MS/MSD values could be mixed with an uncorrected source value and produce plausible but incorrect recovery. | Fixed wording: enter uncorrected measured concentrations on the same dilution basis. Existing post-dilution spike and literal-result RPD conventions are preserved. |
| `MolarityMassCalculatorScreen.kt` | Five significant figures could be mistaken for measurement precision; purity and hydrate assumptions were unstated. | Fixed wording: display policy is independent of input precision; use the actual hydrated reagent's formula weight; no purity correction is applied. |
| All calculator screens | Successful calculation inserted long working above Calculate/Clear. Repeated use required scrolling past it. | Fixed: working follows the action buttons. |
| `LabComponents.kt` and callers | Most fields exposed the generic label “Value”; fixed button heights and one-line unit labels constrained large text. | Fixed: meaningful field labels, minimum button heights, and wrapping dropdown labels. Device/TalkBack verification remains necessary. |
| Tests | Existing 53 unit tests covered the main examples but not long repeat cycles, bounded input in every calculator, or every conversion pair. Instrumentation only checked the package name. | Added eight unit test methods containing many cases, plus three UI tests for restoration, tab isolation, stale-result clearing, errors, and oversized paste. |

## Minor

| Location | Finding and consequence | Proposed fix / outcome |
| --- | --- | --- |
| Dilution, RPD, MS/MSD, molarity screens | Field errors used `remember` while inputs used `rememberSaveable`; errors disappeared after leaving a tab or recreation. | Changed field error state to `rememberSaveable`. UI restoration tests added and compiled, but not executed here. |
| `MsMsdCalculator.kt`, zero-average return | Undefined RPD suppresses otherwise calculable recoveries and original-source concentration. | Documented. A future change could expose partial results with an undefined RPD; current behavior is explicit and does not show a false zero. |
| `LabComponents.kt`, decimal keyboard | Signed values and exponents parse correctly, but some decimal keyboards omit minus or exponent keys. Comma-decimal input is rejected. | Keep the existing keypad; document decimal-point/scientific-notation input. Check target phones before choosing a different keyboard or locale policy. |
| `LabCalculatorApp.kt`, state | Saved state supports tab switching and Android recreation, not permanent history or reliable retention after dismissing the task/force-stop. | README now describes the limit. Persisting previous bench values should be a separate decision because stale values can also cause errors. |
| `MainActivity.kt`, theme | App is intentionally light-only; default edge-to-edge system-bar styling may follow system dark mode. | Retained palette. Check status/navigation icon contrast on actual light/dark system settings and gesture/three-button navigation. |
| `ui/theme/Type.kt`, `RpdCalculator.kt` comments | Template styles were commented out; a comment restated the RPD arithmetic. | Removed those comments. Kept comments explaining exact arithmetic and resource bounds. |
| Resources, manifest, version catalog, shared composables | Lint reports 15 warnings: nine unused resource entries (seven colors and two XML templates), two dependency notices, two modifier-parameter ordering notices, a redundant activity label, and backup-rule configuration. | Recorded, not suppressed. No persisted database/preferences currently exist. Dependency upgrades and backup behavior should be checked separately; lint's cached version suggestions are not a current dependency/security audit. |

## GitHub authenticity

The old README was roughly 13 KB and repeated build, installation, test, and calculator explanations. It wasn't full of promotional adjectives or badges; repetition and tutorial-like detail were the main problems. The rewrite introduces the environmental-lab motivation in first person, keeps the five actual tools, explains assumptions, and consolidates setup instructions. It does not invent users, validation, credentials, or development history.

The four supplied screenshots are real and useful, but show an earlier interface. The README retains one with an honest caption; the others remain in `docs/screenshots`. New screenshots require a running device.

The GitHub description was checked: “Offline Android lab calculator built with Kotlin and Jetpack Compose.” It is factual and was left unchanged. No CONTRIBUTING file, badge collection, or marketing roadmap needed removal. No remote changes, commit, release, or APK publication were made.

## Implementation priority

1. Bound expensive calculations and pasted inputs without changing scientific results.
2. Correct misleading equation presentation and explain dilution/precision assumptions.
3. Add regression cases, preserve validation state, and improve repeated calculation workflow.
4. Shorten README and remove obvious template commentary.
5. Build app/test APKs, run unit tests and lint, and review the diff.

These steps are complete locally. Device-only checks below remain open.

## Calculation verification

Expected arithmetic was derived separately from the app, with rational arithmetic spot-checks using Python's `fractions.Fraction`. The added conversion tests use independent SI exponent positions rather than the production factor table. Tests check values as well as existing step/result consistency assertions.

| Calculator | Equation and variables | Dimensional check and independently checked examples |
| --- | --- | --- |
| Dilution | `V₁ = C₂ × V₂ / C₁`; C₁ stock concentration, C₂ target concentration, V₂ final solution volume, V₁ stock volume. PPM × 1000 → PPB. | Common concentration cancels, leaving mL. 10 ppm → 200 ppb in 50 mL = 1 mL. 2.5 ppm → 125 ppb in 12.5 mL = 0.625 mL. 1000 ppb → 1 ppm in 50 mL = 50 mL. Zero target = 0 mL. Equal 1e100 concentrations with 1e−100 mL final volume preserve that volume. 1/6 = 0.1666R; 1/7 preserves its six-digit repeating cycle. |
| RPD | `100 × abs(A−B) / abs((A+B)/2)`; A original, B replicate, same units/basis. | Concentration cancels. 10 and 12 → 200/11 percent → 18.18%. 0.1 and 0.12 give the same result, as do corresponding values near 1e−12 and 1e100. 0 and 0.1 → 200.00%; −1 and 2 → 600.00%; opposite equal values are undefined. |
| Metric conversions | `y = x × f_from / f_to`; x input amount, f unit size relative to the category base. | Base units are µg, µL, or ng/L. Every directed same-category pair, including identity, is checked: 34 pairs × four values = 136 cases. Values include zero, −1.25, 1e−100, and 1e100. All cross-category pairs are rejected. 25 mg → 25000 µg; 0.25 mg/L → 250 µg/L. |
| MS/MSD | Original source `S × D`; recovery `100 × (X−S)/A`; RPD `100 × abs(MS−MSD)/abs((MS+MSD)/2)`. S raw diluted source, D dimensionless dilution factor, A final added spike concentration, X measured MS or MSD. | Source remains concentration; percentage ratios cancel concentration. S=5, D=10, A=50, MS=55, MSD=50 → source 50, recoveries 100.00% and 90.00%, RPD 9.52%. With S=10, D=1, A=40, MS=48, MSD=50 → 95.00%, 100.00%, 4.08%. Decimal/large/small scaling and D=2.5 preserve percentages. Negative and >100% recoveries remain visible. |
| Molarity / mass | `m = M × V × FW`; M mol/L, V final solution liters, FW g/mol. mL / 1000 → L. | mol/L × L × g/mol = g. 0.02 M × 0.250 L × 58.44 g/mol = 0.2922 g, displayed 0.29220. 0.02 M × 0.500 L × 40.00 g/mol = 0.4 g, displayed 0.40000. Existing tests cover zero, mL/L equivalence, exact decimal products, rounding carry, scientific notation, and invalid fields. |

Displayed equations agree with engine operations after the dilution-label correction. No binary floating-point calculation is used. RPD and recovery round half up to two decimal places only at output; molarity rounds half up to five significant figures. Exact conversion/dilution output strips unnecessary trailing zeros. These policies do not propagate significant figures or uncertainty from source measurements. The unchanged `R` notation is explained, but an exact fraction is less ambiguous for long periods.

Every numeric field is covered for blanks, malformed text, NaN/Infinity, commas, oversized strings, and out-of-range exponents. Negative and zero domain checks remain calculator-specific: dilution requires positive stock/volume, molarity positive volume/formula weight, and MS/MSD positive spike/factor. Signed measured results and conversion values remain allowed. Neither `ND` nor `<RL` is converted to zero.

## Scientific choices to confirm with the analyst

- **Spike timing/basis:** current recovery subtracts raw diluted source and assumes spiking after dilution with negligible native-sample concentration change. Spiking before digestion/dilution or adding significant spike volume requires a different model. Recommend a separate explicitly named mode if needed, not a silent change. It would change recoveries.
- **MS/MSD RPD:** current app compares literal concentrations. Recovery-based RPD is an alternative used in some methods. In the 5/50/55/50 example it would be 10.53%, rather than 9.52%. The archived [EPA SOM02.3 equations, equation 15](https://19january2021snapshot.epa.gov/sites/static/files/2015-11/documents/som02.3equations_0.pdf) illustrate recovery-based RPD; that document is not a claim about which method this lab currently follows. Recommend retaining explicit labeling until the applicable SOP is identified.
- **Recovery equation:** subtracting unspiked from spiked concentration and dividing by added concentration agrees with the structure in [EPA's QA/QC explanation](https://www.epa.gov/choose-fish-and-shellfish-wisely/lab-quality-assurance-and-quality-control). This supports the arithmetic, not validation of the app for a particular analytical method.
- **Signed results/near zero:** absolute-average RPD is the existing convention. Restricting it to positive results, using absolute concentrations in the average, or replacing non-detects would change results. Keep the math visible and follow the lab's rules.
- **Dilution factor below 1:** currently any positive factor is allowed, including 0.5. That can represent a concentration correction, but is not ordinary dilution. Confirm whether the app should enforce ≥1 or rename this field; enforcing ≥1 would reject previously accepted workflows.
- **Preparation and rounding:** confirm reagent purity/hydrate handling, volumetric concentration basis, desired significant figures, and SOP rounding. None were silently inferred or changed.

## Architecture, build, and device limits

The architecture fits the size of the app: one activity, five independent screens, pure calculation objects, and a few shared controls. There is no database, network client, service, background work, or unnecessary dependency-injection layer. Some UI structure is repeated, but a generic form framework would add complexity. The small shared numeric parser removes the useful duplication. Encoded calculation steps contain generated text only, so delimiter collisions from arbitrary prose input aren't currently an issue.

`rememberSaveableStateHolder` preserves each tab's inputs/results; editing an input or unit clears stale results. Android saved state has limited capacity, so the input/expansion bounds matter beyond calculation speed. See [Android's saved-state guidance](https://developer.android.com/develop/ui/compose/state-saving). It still should not be treated as durable storage.

Build uses minSdk 24, compile/target API 37, AGP 9.3.1, Gradle 9.5.0, and a JDK 17 daemon. Java source compatibility 11 is separate from the daemon requirement. The host default Java was 8; validation explicitly selected the installed JDK 17 and existing Gradle cache. Wrapper checksum is configured. Dependencies are pinned; no version migration was needed to compile/test locally. Release optimization is off and existing preview distribution is debug-signed. A stable release signing key is needed for consistent future upgrades. CI definitions were inspected, but remote CI was not run during this audit.

The layout uses vertical scrolling, keyboard/navigation insets, and a scrollable tab row. Supplied screenshots have readable field contrast. Large fonts, narrow screens, landscape, TalkBack focus order, decimal keyboard variants, Android 7 compatibility, and system-bar contrast still require device testing. No emulator was configured and no device was attached. No current screenshots or successful instrumentation run are claimed.

## Validation

- `test`: 61 JUnit tests passed, zero failures/errors (53 existing + eight new methods with multiple cases).
- `assembleDebug`: passed; APK at `app/build/outputs/apk/debug/app-debug.apk`.
- `assembleDebugAndroidTest`: passed. Three added Compose UI tests compiled; not executed without a device/emulator.
- `lintDebug`: passed with zero errors and 15 warnings listed above. An earlier invocation crashed in the Kotlin lint analyzer; retry completed without a toolchain change.
- Final diff reviewed and `git diff --check` run. No version bumps, dependency changes, signing changes, or generated build artifacts were included in source changes. Added `.kotlin/` to `.gitignore` after the build produced local compiler cache files.

## Follow-up — 2026-09-29

The development build now has shared result cards with copying that includes units, expandable calculation steps, and persistent local presets for preparation settings. MS/MSD adds **Next sample**, which keeps preparation settings while clearing measurements and results. A zero MS/MSD average now leaves source concentration and recoveries available while displaying an undefined RPD. Presets contain settings only; they do not store sample results or history. The calculation assumptions above remain unchanged.

- **Unit tests:** 68 passed, zero failures or errors.
- **Build and lint:** debug APK and instrumentation APK built successfully; lint completed with zero errors and the 15 existing warnings.
- **Android tests:** all 12 instrumentation tests passed on BlueStacks Pie64, Android 9 / API 28. Coverage includes copying with units, expanding steps, saving/loading/deleting presets, duplicate and invalid preset handling, persistence between store instances, Next sample, undefined RPD, stale-result clearing, tab navigation, and Android state restoration.
- **Visual checks:** inspected dilution results, expanded steps, and the preset save dialog at 720 × 1280 with normal and 150% text size; inspected the scrolling forms and MS/MSD action controls at the original 1280 × 720 landscape size. The original display size, density, and font scale were restored. The README screenshot now comes from this build.

Gradle's offline `connectedDebugAndroidTest` task required an uncached Android UTP dependency. The already-built app and test APKs were instead installed through ADB, and `androidx.test.runner.AndroidJUnitRunner` was run directly. Its output reports `OK (12 tests)` and is saved locally at `app/build/reports/bluestacks/android-tests.txt`.

This verification covers one Android 9 emulator. Physical-device behavior, TalkBack focus order, other keyboard implementations, Android 7 compatibility, and other system navigation modes remain unverified.
