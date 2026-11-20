# Technical information

## Project structure

Lab Calculator uses Kotlin and Jetpack Compose. Calculation and numeric-input code is in `app/src/main/java/com/dyfl/labcalculator/calculation/`, local preset handling in `presets/`, and screens and shared controls in `ui/`. JVM tests cover calculation, input, unit, and preset behavior; Android instrumentation tests cover UI and state workflows.

The main application manifest requests no internet permission and disables Android backup. There is no account, analytics service, or network client in the reviewed app source. Copying a result places it on the Android clipboard. Presets are local preparation settings, not a sample-results database.

## Presets and saved state

Named presets are saved on the device for dilution recipes, molarity/reagent recipes, MS/MSD preparation settings, and unit-conversion pairs. Use **Save preset** after entering valid settings, then **Load preset** to reuse them. MS/MSD presets contain units, dilution factor, and added spike concentration; loading one clears sample measurements, results, and obsolete errors. **Next sample** clears measurements, results, and measurement errors while keeping preparation settings and their validation errors, then focuses and scrolls to the source measurement. **Clear all** resets the whole MS/MSD form and clears all errors. Presets can be deleted from the load dialog; names must be unique within each calculator, with up to 20 presets each. Presets survive app restarts, but are removed when app data is cleared or the app is uninstalled. No sample results or calculation history are stored in presets.

Preset status distinguishes empty storage from records this app cannot load and an unreadable storage value. Available presets remain usable alongside unavailable records, including unsupported versions. Saving and deleting preserve unrelated raw records; unexpected storage types refuse changes. No automatic repair or recovery is performed.


Tabs retain their own inputs and results through navigation and Android state restoration. This is not permanent history or a guarantee of retention after dismissing the task or force-stopping the app. Editing an input clears its previous result. Calculation steps can be expanded and selected; the November source preview also adds a dedicated copy-all action for steps.

## Build from source

Use Android Studio with JDK 17 and Android SDK API 37. The Gradle wrapper is included; the first build needs internet access to obtain dependencies. Minimum supported Android is 7.0 / API 24; that minimum is a configuration setting, not evidence of a completed device test.

```powershell
# Windows
.\gradlew.bat test assembleDebug
```

```sh
# macOS / Linux
./gradlew test assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. With a configured Android device or emulator, `connectedDebugAndroidTest` runs the UI suite. Earlier offline attempts lacked a cached UTP dependency; the documented Android runs used ADB and AndroidJUnitRunner directly. See [verification](verification.md) for what was actually reported.

**Check signing before installing a local build over an existing app.** A new machine's debug key usually differs from the key used for a published APK. Check both signing certificate and version-code compatibility; do not assume that matching version text makes an APK interchangeable. Clearing data or uninstalling removes local presets and state.

The separate [November branch build guidance](https://github.com/DyFl/LabCalculator/blob/71b4733906c618cd342c5a150fc865e422a40a89/docs/responsiveness.md#safely-try-local-builds) describes its isolated Performance build and same-package optimized-local build. Those variants are absent from `main` at `83bd243` and are not published releases.

## Continuous integration

The workflow on `main` runs JVM tests and debug builds on Windows, macOS, and Linux, then uploads debug APK workflow artifacts. It does not run Android device tests. Workflow artifacts are separate from the APK attached to a tagged release and may use different debug signing keys.

The November preview branch changes artifact upload to an explicit manual opt-in. See [verification](verification.md) for the reviewed revisions and existing CI results. No workflow is changed by this documentation update.
