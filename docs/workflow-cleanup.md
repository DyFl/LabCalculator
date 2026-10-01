# Focused workflow cleanup — October 1, 2026

The completed dilution-output, preset-status and concentration-unit passes were reviewed and checkpointed locally as `1dc95b0`. The checkpoint contains their source, tests and documentation; attachments, generated reports/build outputs and signing material are excluded.

## Changes

- MS/MSD shows original-source concentration, MS recovery, MSD recovery and concentration-based RPD in one summary. Each value remains selectable and has its own labelled 48 dp copy action. Value and label text sizes are unchanged. Shared padding replaces four separate card paddings; rows and values grow/wrap at narrow widths or large text sizes. A zero MS/MSD average keeps the other three results available, shows the existing undefined-RPD explanation and disables only RPD copying. Calculation scrolls the summary into view; oversized summaries remain scrollable.
- Original-source results no longer repeat “Mass per volume” or the reconstruction formula. Formula and assumptions and Calculation Steps retain the explanation. Visible preparation guidance retains post-dilution spiking, uncorrected measurements on the same dilution/concentration basis and negligible spike volume. The spike field still specifies the final concentration added to each diluted aliquot. Repeated measurement descriptions are removed; unit-change mechanics are in expandable help. Blocked-conversion messages and reset explanations remain clear.
- Next sample clears source/MS/MSD, results, steps and measurement errors; it retains units, spike, dilution factor and their validation errors. It focuses and scrolls to the source measurement, including the measurement heading so a wrapped floating label stays below the tab bar. Preset loading and Clear all clear all obsolete errors through their own action policies. Preset storage is unchanged.
- Changing concentration families skips confirmation only when **every affected concentration is blank**: both dilution fields or all four MS/MSD fields. The selected family/unit replaces the old one, affected fields stay blank and unrelated volume/factor values and errors remain. Any nonblank text, including invalid text in the unselected field, requires confirmation. Converter category changes similarly skip confirmation for blank input. Cancel preserves values, units, errors and results; confirmed/immediate resets clear stale results and affected errors. Units, results, errors and pending dialogs remain saveable.

Formulas, rounding, exact arithmetic, compatible-unit conversion, scientific assumptions, preset storage, signing and release versions are unchanged. No QC evaluation, history or new calculator is added.

## Verification

Executed on the final source:

- `testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest --offline --console=plain` passed. All **100 JVM tests** passed with zero failures/errors. Both debug app/test APKs built. Lint reports **zero errors and 19 existing warnings**, with no new warnings. No dependency, signing or release-version changes were needed.
- All **48 Android tests** passed at a measured **360 dp / 1.3× text** on BlueStacks Android 9, run directly through ADB / AndroidJUnitRunner. All **21 focused tests** (`WorkflowCleanupTest`, `ConcentrationUnitsUiTest`, `DilutionPresetUiTest`) passed at a measured **320 dp / 1.5× text**. This includes the 10 new workflow regressions and the previous 11 unit/dilution/preset UI tests.
- Regressions cover retained numeric and invalid preparation errors, cleared measurement errors, fully visible source focus/heading, preset loading and Clear all, all four summary values and individual clipboard contents, undefined RPD and its disabled copy, compatible-unit stale-result clearing, blank/whitespace/populated/invalid resets, cancellation and saved-state restoration. Existing exact calculation, dilution instruction and preset-storage checks remain green.
- Automated text-layout scans and manual light/dark screenshot inspection cover preparation guidance, measurements, defined/undefined summaries, long values, retained errors, reset dialogs, expandable help and Next sample. Copy targets remain 48 dp and text sizes are unchanged. Visual inspection caught the wrapped source label under the tab bar after Next sample; including the measurement heading in the scroll request corrected it, and the regression now checks both heading and field bounds. Screenshot capture waits for native dialog animations to settle.
- `git diff --check` passed. Emulator size, density and font scale were restored after each run to **1280 × 720, density 240, font scale 1.0**.

Unavailable checks:

- Gradle's offline `connectedDebugAndroidTest` was attempted but cannot resolve the uncached `com.android.tools.utp:android-test-plugin-host-additional-test-output:32.3.1` runner dependency. The Android suites above executed successfully through ADB; no dependency downloads were made.
- Physical devices, TalkBack, other Android versions and other keyboard implementations were not exercised. No remaining functional issue was found in the executed checks.

Local logs are `build/workflow-cleanup/android-360-1.3/tests.txt` and `build/workflow-cleanup/android-320-1.5/tests.txt`. Final screenshots and measured viewport text are under each run's `screenshots/workflow-cleanup/` directory; the parent directories retain earlier captures. Lint and JVM reports remain under `app/build/reports/`. These generated reports, screenshots and the local verification script are ignored and excluded from commits. Nothing was pushed or published.
