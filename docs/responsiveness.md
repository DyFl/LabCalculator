# Responsiveness investigation — November 14, 2026

The reported redesign was saved locally under `build/responsiveness/redesign-original/` before editing. Pre-redesign source was exported from `83bd243` to `build/responsiveness/pre-redesign/`. Comparisons used separate application IDs; the existing `com.dyfl.labcalculator` installation and its presets were not replaced or cleared by the investigation. The user subsequently tested the signer-matched optimized update and accepted its design and responsiveness. This GitHub preview contains source and documentation; no APK or release is published.

## Findings and targeted changes

**Rapid return taps could be ignored.** The redesign's tab handler compared the requested tab with `selectedTab`, a value captured by the last composition. In a Dilution → RPD → Dilution sequence before recomposition, the last tap looked like a tap on the already-selected tab and was discarded. The handler now compares against the current saved state. A regression invokes both tab actions in one UI turn and checks that the final selection and entered value are correct. It fails on the saved reported redesign and passes after the change.

**Keyboard resizing invalidated the entire form.** The viewport height was a changing value in a static composition local. Publishing a new height during keyboard animation invalidated the provider's complete content. Every numeric field also read keyboard height during composition and restarted a keyed effect when it changed. The provider now publishes a stable state holder, and only the focused field observes keyboard/layout changes with `snapshotFlow`. Size observation and scrolling stay outside composition. Resizing, wrapped labels, validation scrolling, result scrolling, and accessibility semantics are retained.

**The shared Calculate callback unnecessarily invalidated content.** A new callback was published through another static composition local whenever `LabScreen` recomposed. It now keeps a stable identity and forwards to the current input callback. All five screens supply input-capturing lambdas. Freshness matters: an intermediate implementation with local function references retained old input snapshots; the existing workflow tests caught that, and it was corrected before delivery. Tests check that the stable action uses the latest callback/input version.

No screen-transition animation or intentional tab-selection delay was present. Scientific calculations are triggered by Calculate/Done and explicit unit operations; they are not performed for each ordinary numeric keystroke. Preset decoding/validation occurs when loading storage and already existed before the redesign. The palette, typography, shapes, elevation, selected pills, detailed disclosures, copy actions, and scientific code were retained. No speculative rendering or scientific changes were made.

## Measurements and limits

Controlled probes run production shared Compose components in an isolated Android test activity. A `SideEffect` counts form-content compositions after initial layout; counters are not added to the shipping app.

| Probe | Reported redesign | Revised components |
| --- | --- | --- |
| 11 viewport-height changes | 11 additional whole-form compositions | 0 |
| 10 input-callback changes | 10 additional whole-form compositions | 0 |
| Return tab tap before recomposition | Wrong tab remains selected | Last tap wins |
| Calculate after 10 callback changes | Latest callback invoked | Latest callback invoked |

These probes first ran on the existing Android 9 emulator. The three revised responsiveness tests also passed on the physical Pixel during the later full test run, while the reported redesign reproduced the ignored-return-tap failure. The callback probe verifies freshness; calculator workflows exercise real input snapshots and unit changes.

After the Pixel reconnected, **15 controlled trials** ran on the **Pixel 9 Pro, Android 17 / API 37**: three trials each for pre-redesign optimized, reported-redesign optimized/debug, and revised optimized/debug builds. The everyday installed app was confirmed debuggable. All benchmark interactions targeted separate comparison packages; it was not updated, uninstalled, cleared, or driven by the benchmark.

Each trial warmed the app, measured 30 Dilution/RPD tab selections, opened the numeric keyboard, warmed nine digits, and measured 27 more digits using actual Gboard key touches. All 36 entered digits were verified after every trial. Builds used the same toolchain, requested ART `speed` compilation, and alternated run order. Comparison preset storage was empty. Font scale, theme, density, and refresh settings were unchanged. Battery temperature ranged from 32.9 to 34.9 °C while USB charging. Screen timeout was temporarily extended and restored to its original 600,000 ms.

Perfetto captured FrameTimeline, UI-thread scheduling, and view/input traces. Analysis used its canonical `android.frames.timeline` module. All 45 workload phases were covered by their traces, with no reported buffer overwrite, wrap, overrun, or packet-loss statistics. The table reports the **median of three per-run p95 app-frame completion times**, in milliseconds; lower is better.

| Build | Tab switching | Keyboard opening | Number entry |
| --- | ---: | ---: | ---: |
| Pre-redesign, optimized | 11.14 | 7.41 | 10.16 |
| Reported redesign, optimized | 13.91 | 9.19 | 11.84 |
| Revised, optimized | 14.02 | 6.44 | 8.21 |
| Reported redesign, debug | 53.81 | 18.51 | 31.64 |
| Revised, debug | 54.37 | 12.81 | 21.02 |

Number-entry p95 decreased about **34% in debug** and **31% in optimized** builds in this workload. Scheduled UI-thread CPU time for the same number-entry workload decreased from 839 to 615 ms in debug and 247 to 185 ms optimized. Keyboard-opening frame times also improved.

**Tab rendering did not measurably improve from the code changes.** Its optimized p95 remains above the pre-redesign value. The ignored-return-tap bug is fixed, but its correctness improvement is separate from frame rendering speed. Optimized tab p95 was about 14 ms versus 54 ms in debug; debug overhead is therefore a substantial part of the observed delay. No caching or visual simplification was added without further evidence.

These are warm, repeatable synthetic workloads with empty presets, not a direct measurement of finger-to-visible-response latency or a guarantee for every calculator/input length. FrameTimeline's app completion duration includes GPU/post work and ends before SurfaceFlinger's final screen presentation. See [Perfetto's metric definitions](https://perfetto.dev/docs/data-sources/frametimeline). Three repetitions provide a useful local comparison, not a broad performance certification. The [aggregate summary CSV](performance/pixel9-pro-2026-11-14.csv) is retained in this repository. Its `frames` and `app_late_frames` columns sum the three runs; timing columns are medians of the three per-run metrics, not pooled percentiles. `ui_wall_p95_ms` is the main-thread doFrame slice's wall-time p95; `ui_cpu_ms` is scheduled main-thread CPU time during that phase. Raw traces, phase timestamps, per-frame CSVs, trial results, and device metadata remain local under ignored `build/responsiveness/pixel/` and are not committed.

Android recommends a non-debuggable optimized build for performance assessment and warns about emulator benchmark results. See [Compose performance configuration](https://developer.android.com/develop/ui/compose/performance#properly-configure-your-app) and [Macrobenchmark measurement guidance](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview).

## Verification

| Check | Result |
| --- | --- |
| JVM calculation, parsing, unit, and preset tests | 100 passed |
| Earlier complete Android suite, Android 9, 360 dp / 1.3× text | 57 passed before the test-harness dependency update |
| UI, keyboard/viewport, and responsiveness subset, 320 dp / 1.5× text, animations disabled | 13 passed |
| Pixel Android 17 full UI run | 51/57 passed; six late tests lost the test Compose hierarchy |
| Pixel workflow class rerun, including those six | All 10 passed in isolation; no assertions removed |
| Updated test-harness compatibility on Android 9 | Three responsiveness regressions passed |
| Debug, separate performance, and same-package optimized APK builds | Passed |
| Debug, performance, and optimized-local lint | 0 errors; 17 dependency/configuration/resource maintenance warnings in each |
| Native optimized MainActivity smoke check on Android 9 | 10 PPM stock, 200 PPB target, 50 mL final volume produced the complete 1 mL transfer instruction; keyboard dismissed |

The investigation's Android tests ran against `com.dyfl.labcalculator.compareafter` to preserve the everyday installation. Only the temporary snapshot's application-ID smoke assertion was adapted to that isolated ID; functional tests were retained. All three new responsiveness regressions passed on both Android versions. The full and narrow runs cover both themes, input/error/result layouts, numeric Next/Done, clipboard, presets, units, and saved-state restoration. Emulator display, font, motion, and keyboard settings were restored.

### Source-preview verification

Before committing the preview, the complete UI suite was rerun from the final app source and updated test dependencies on **BlueStacks Android 9 / API 28**, at **360 dp width / 1.3× text**. It passed **57/57 in 207.925 seconds**, including all ten workflow tests and all three responsiveness regressions. The Pixel was not connected for this verification; this emulator pass does not establish a full Android 17 pass.

This run built a fresh ignored snapshot using application ID `com.dyfl.labcalculator.previewverify` and a distinct launcher label. The application-ID smoke assertion was adapted in that snapshot; all other test and production source files matched the reviewed checkout. Native screenshot capture was disabled with `captureScreenshots=false` because of the previously observed BlueStacks screenshot-service crash. All functional, layout, text-clipping, clipboard, and state assertions ran. No everyday app was replaced or cleared, and emulator size, density, font, keyboard, and timeout settings were restored. The runner reported `OK (57 tests)`; local output is `build/preview-verification/full-ui.txt`.

The practical Gradle check completed successfully:

```powershell
.\gradlew.bat --offline test assembleDebug assembleDebugAndroidTest assemblePerformance assembleOptimizedLocal lintDebug lintPerformance lintOptimizedLocal --no-configuration-cache
```

It accepted 100 JVM tests and built the debug, instrumentation, Performance, and optimized-local APKs locally. Lint reported **0 errors and 17 existing maintenance warnings per checked variant**. A separate `testDebugUnitTest --rerun-tasks` invocation then **passed all 100 JVM tests**, with no failures, errors, or skips, without relying on Gradle's up-to-date test task. Build outputs and raw verification logs stay ignored. CI on a push or pull request runs JVM tests and a debug build, with **APK upload disabled**; it does not run the on-device UI suite. Manual artifact upload is available only through an explicit workflow opt-in.

The Pixel initially could not run the old test harness because Espresso 3.5.1 reflected the removed `InputManager.getInstance` API. **Test-only** Espresso and AndroidX JUnit dependencies were updated to stable 3.7.0 / 1.3.0. The [Espresso release notes](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0) document the InputManager fix. App runtime dependencies did not change.

### Initial six Pixel failures and separate rerun

The updated-harness full Pixel run executed all 57 tests: **51 passed and six failed**. All six were late tests in `WorkflowCleanupTest`, with `IllegalStateException: No compose hierarchies found in the app`, before their scientific assertions could execute:

1. `emptyDilutionFamilyChangesKeepVolumeAndErrorsAndCheckBothAffectedFields`
2. `undefinedRpdExplainsAndDisablesOnlyItsCopyAndRestoresThenClearsOnEdit`
3. `lightAndDarkWorkflowLayoutsWrapWithAccessibleCopyTargets`
4. `converterSkipsEmptyResetButConfirmsInvalidAndPopulatedInputWithRestorableCancellation`
5. `populatedMsFamilyCancelPreservesSummaryAndConfirmedResetClearsRestorableResults`
6. `loadingPresetAndClearAllClearObsoletePreparationAndMeasurementErrors`

The complete `WorkflowCleanupTest` class was then rerun separately on the same Pixel: **10/10 passed in 67.869 seconds**, including those six tests. No assertions were removed and no application workaround was added. The crash log did not identify a Lab Calculator crash; the hierarchy loss's root cause was not established. An activity/lifecycle or foreground interaction is a possibility, not a proven diagnosis. The class-only success does **not** turn the original full-suite result into a pass. At this November 14 checkpoint, a single uninterrupted full-suite pass on Android 17 was unestablished. The later [1.1.0 release record](releases/1.1.0.md) preserves this history and records the new complete passing run and intervening failure.

Local logs are under ignored `build/responsiveness/`: `accepted-build.txt`, `emulator/original-probes.txt`, `emulator/improved-full.txt`, `emulator/improved-narrow.txt`, and the four before/after probe counter files. Pixel full-suite and isolated-class output is in `pixel/revised-full-tests-sdk37.txt` and `pixel/workflow-isolated-sdk37.txt`. The earlier implementation failure is retained locally as a diagnostic log, not a passing check. `optimized-smoke.png` records the native optimized calculation. The raw existing Pixel session is in `pixel-installed-gfx-initial.txt` and is not used as comparative evidence. Raw device logs and build reports are not part of the source preview.

During this investigation, calculation engines, numeric parsing, rounding/display policy, unit conversions, preset models/store, and the main manifest had no changes from pre-redesign HEAD. New local build variants provided an isolated comparison and a signer-matched optimized update; release configuration and signing were unchanged at that checkpoint. The later 1.1.0 release preparation enables optimization for `release`, explicitly disables debugging, and retains the historical key; see [the release record](releases/1.1.0.md).

## Safely try local builds

The optimized test build uses application ID **`com.dyfl.labcalculator.performance`**, launcher name **Lab Calculator · Performance**, and the existing local **debug certificate**. It is non-debuggable, R8 optimized, and shell-profileable on Android 10+. It is a local test build, not a production release. The main release signing configuration is unchanged. Optimization is enabled using AGP 9.3's [documented optimization DSL](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization).

The investigation's `optimizedLocal` variant built an **in-place optimized update** using the everyday application ID `com.dyfl.labcalculator`, version code 3, and version name `1.0.2-preview.1-optimized-local`. Before the user's Pixel test, its signing certificate was verified against the APK installed on that particular phone. The agent built and checked it; the user subsequently installed and tested it. Its UI/scientific source and R8 settings matched the optimized comparison, with the everyday package/label and without the comparison-only profiling manifest. Current release-branch local builds inherit the new 1.1.0 / code-4 base version; the full-release artifact uses `assembleRelease` without a local suffix.

That certificate match applies to the tested local build, not to every clone of this repository. Both local variants use the builder's standard Android debug key, which is **not committed**. A different machine normally has a different key. Before any in-place installation, compare the built APK's signer with the currently installed APK using Android SDK `apksigner verify --print-certs`, and check Android's version-code compatibility. A signer-matched update retains app storage and presets. If the signer differs, use the separate Performance copy; do not uninstall the everyday app or clear its data to get around a mismatch. No production signing credentials or new release signing configuration are included.

The separate **Performance** copy installs alongside the existing app and has independent, initially empty preset storage. Existing presets remain in the everyday app; their absence in that test copy does not mean they were deleted. Uninstalling the everyday app is unnecessary. The separate copy is the appropriate local test when the everyday app has a different signing certificate.

Build the ordinary debug APK and isolated optimized APK with:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assemblePerformance assembleOptimizedLocal lintDebug lintPerformance lintOptimizedLocal
```

- Ordinary debug output: `app/build/outputs/apk/debug/app-debug.apk`. It uses the everyday application ID; use an in-place update only after checking its signing certificate and version compatibility with the installed app.
- Separate optimized output: `app/build/outputs/apk/performance/app-performance.apk`.
- Same-package optimized update: `app/build/outputs/apk/optimizedLocal/app-optimizedLocal.apk` (`assembleOptimizedLocal`).
- The investigation also kept a local revised debug comparison under `build/responsiveness/`, application ID `com.dyfl.labcalculator.compareafter`. It used the same UI/scientific source with an isolated package and launcher label to keep debug overhead comparable. This temporary snapshot and its binaries are not shipped in the repository.

Build the separate Performance APK locally, then open it on the phone or use `adb -s SERIAL install -r PATH`. The comparison apps do not import or share the everyday app's presets. GitHub push/PR checks build and test source without uploading an APK; workflow artifact upload requires an explicit manual `upload_apk` opt-in. No release or APK publication is part of this preview.

## Physical-device comparison procedure

Keep the Pixel unlocked and connect a USB data cable. Enable Developer options/USB debugging and accept the computer's RSA prompt. Android Studio's [hardware-device connection guide](https://developer.android.com/studio/run/device) includes driver and connection troubleshooting. This Windows session required an isolated ADB listener and a numeric host address:

```powershell
adb -L tcp:53592 nodaemon server
# In a second terminal:
adb -H 127.0.0.1 -P 53592 devices -l
```

Use the explicit device serial for every command. The measured optimized comparison packages were `com.dyfl.labcalculator.comparepre.performance`, `com.dyfl.labcalculator.compareredesign.performance`, and `com.dyfl.labcalculator.performance`. Temporary snapshots and their APKs remain local. To repeat the pre-redesign comparison, export `83bd243` into an ignored comparison directory and apply the same performance build settings with an isolated application ID. The reported intermediate redesign was an uncommitted local snapshot, so it cannot be recovered from a Git revision. For a debug comparison, likewise use isolated reported-redesign/revised packages with independent storage and matching build settings.

Warm each app with the same tab sequence. Keep the font scale, IME, display refresh mode, and charging/thermal conditions comparable; alternate run order and repeat trials. Record rapid visible-tab switching separately from numeric entry with the keyboard open. Start with empty preset storage consistently; test representative preset counts separately if loading appears in traces.

Capture a low-overhead System Trace using Android Studio's profiler on the profileable comparison package. Inspect input events, main-thread work, and FrameTimeline rather than inferring responsiveness from APK size or build type. [Android's profiling guide](https://developer.android.com/studio/profile) explains profileable versus debuggable capture modes. `adb shell dumpsys gfxinfo PACKAGE reset`, followed by the scripted interaction and `adb shell dumpsys gfxinfo PACKAGE framestats`, can provide supporting frame diagnostics; these are not tap-to-display latency measurements by themselves.
