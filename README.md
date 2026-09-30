# Lab Calculator

I work in an environmental laboratory and built this Android app for calculations I use or run into at work. It's a small personal tool that keeps them in one place.

It works offline, with no account, analytics, or internet permission. Android 7.0 or newer is required.

<img src="docs/screenshots/dilution-calculator-current.png" alt="Dilution calculator showing a 1 mL result, Copy result, and expandable calculation steps" width="360">

*Current development build running in BlueStacks, September 29, 2026.*

## What it does

- **Dilution:** stock volume from C₁V₁ = C₂V₂, with PPM/PPB conversion.
- **RPD:** relative percent difference between a sample and its replicate.
- **Unit conversions:** metric mass, volume, and mass concentration.
- **MS/MSD:** source concentration, spike recoveries, and RPD of the measured MS/MSD results.
- **Molarity / mass:** grams of reagent from molarity, final volume, and formula weight.

Each calculator displays its result in a card with a **Copy result** button that includes the units. Calculation steps can be expanded, selected, and copied. Editing an input clears its previous result. Tabs retain separate inputs during navigation and Android state restoration; this isn't a permanent calculation log. The app uses a light theme.

Named presets are saved on the device for dilution recipes, molarity/reagent recipes, MS/MSD preparation settings, and unit-conversion pairs. Use **Save preset** after entering valid settings, then **Load preset** to reuse them. MS/MSD presets contain units, dilution factor, and added spike concentration; loading one clears sample measurements and results. **Next sample** also clears those measurements and results while keeping preparation settings. **Clear all** resets the whole MS/MSD form. Presets can be deleted from the load dialog; names must be unique within each calculator, with up to 20 presets each. Presets survive app restarts, but are removed when app data is cleared or the app is uninstalled. No sample results or calculation history are stored in presets.

## Calculation assumptions

Dilution uses the **final solution volume**, not the volume of solvent added. Concentrations are converted to the same units first. Use a matching concentration basis: PPM/PPB should not be treated as mg/L/µg/L for arbitrary matrices or densities. Short repeating decimals end in `R`; long expansions are shown as exact fractions in mL.

RPD uses `|A − B| / |(A + B) / 2| × 100`. A zero average is undefined. Signed results are accepted, but near-zero results and non-detects need your method's rules. The display uses two decimal places.

The MS/MSD calculator assumes **the spike was added after dilution**. Enter uncorrected source, MS, and MSD results on the same dilution basis, with the spike as its final added concentration. The dilution factor only reconstructs the original source concentration. RPD compares measured concentrations, not percent recoveries. This assumes the spike volume does not materially change the native sample concentration. When the MS/MSD average is zero, RPD is shown as undefined; source concentration, recoveries, and their calculation steps remain available. There are no automatic QC pass/fail limits.

Molarity / mass uses `m = M × V × FW`, with volume converted to liters. Use the formula weight for the actual reagent, including hydration. No purity correction is applied. Five significant figures are a display choice, not an assessment of the precision of your measurements.

Arithmetic uses decimal numbers rather than binary floating point. Input accepts a decimal point and scientific notation, with a 256-character limit and decimal scale from −1000 to 1000. Commas and non-detect qualifiers such as `<0.1` aren't interpreted as numbers. Exact arithmetic doesn't replace measurement uncertainty or your SOP's rounding rules.

## Install or build

Download an APK from [Releases](https://github.com/DyFl/LabCalculator/releases), open it on your phone, and allow installation from that source if Android prompts you. Preview APKs are debug builds.

The v1.0.1 Preview 1 APK used a different debug signing certificate from v1.0.0 Preview 1. Updating between those previews requires uninstalling the old app first, which clears its state. Record anything you need before uninstalling.

To build from source, open the project in Android Studio with JDK 17 and Android SDK API 37 installed. The Gradle wrapper is included; the first build needs internet access for dependencies.

```powershell
# Windows
.\gradlew.bat test assembleDebug
```

```sh
# macOS / Linux
./gradlew test assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions also runs tests and builds debug APKs on Windows, macOS, and Linux.

With an Android device or emulator connected, run `connectedDebugAndroidTest` to exercise the UI tests. See [the audit notes](docs/audit.md) for calculation checks and remaining concerns.

## Accuracy

This is a convenience tool. Check results against your laboratory's approved SOPs, methods, QA system, and applicable regulatory methods before using them. It isn't an EPA-approved method or a validated laboratory information system.

## Development

This is an ongoing personal project. I'll adjust it as I find things that are useful in day-to-day lab work. Reports of wrong answers are especially helpful: include the calculator, inputs, units, and expected result.
