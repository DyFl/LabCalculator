# Lab Calculator

An Android-only, offline lab-calculation app built with Kotlin and Jetpack Compose. It has no internet permission, login, database, analytics, or external API.

The app has four independent tabs:

- **Dilution** — solves `C1 × V1 = C2 × V2` for the required stock volume.
- **RPD** — calculates Relative Percent Difference for a sample and replicate.
- **Unit conversions** — performs exact metric conversions for mass, volume, and mass concentration.
- **MS/MSD** — calculates source concentration, Matrix Spike and Matrix Spike Duplicate recoveries, and RPD between the literal MS/MSD results.

After a successful calculation, every tab shows a selectable **Calculation Steps** card directly below its result. The steps use the same parsed values and intermediate values as the calculation engine.

## Clone, test, and build

You need Git, JDK 17, and the Android SDK with Android API 37 and Android SDK Build Tools 36.0.0. Android Studio can install the JDK and Android SDK for you. The repository contains the application source, Gradle wrapper scripts and wrapper JAR required to start a build. The first build still needs internet access to download Gradle and the declared dependencies; the finished Android app itself works offline.

Clone the repository and enter its folder:

```text
git clone https://github.com/DyFl/LabCalculator.git
cd LabCalculator
```

On Windows PowerShell or Command Prompt, run:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

On macOS or Linux, run:

```bash
./gradlew test
./gradlew assembleDebug
```

The generated APK is `app/build/outputs/apk/debug/app-debug.apk`.

Every push and pull request also runs both commands on Windows, macOS, and Linux in GitHub Actions. To download an APK built there:

1. Open the repository's **Actions** tab on GitHub.
2. Open a successful **Build** workflow run.
3. Scroll to **Artifacts** in the run summary.
4. Download the debug APK artifact for the operating system you want. Each archive contains `app-debug.apk`.

## Open and run the app in Android Studio

1. Install Android Studio if it is not already installed.
2. Open Android Studio and choose **Open**.
3. Select the `LabCalculator` project folder.
4. Wait for the message at the bottom of Android Studio to say the Gradle sync has finished. The first sync may download Android build tools; this is only for building the app. The installed app itself works offline.
5. Connect an Android phone with a USB cable, or open **Tools > Device Manager** and start an Android emulator.
6. Select that phone or emulator near the top of Android Studio.
7. Click the green **Run** triangle. Android Studio builds, installs, and opens the app.

For a physical phone, Android may ask you to enable **Developer options** and **USB debugging**:

1. On the phone, open **Settings > About phone**.
2. Tap **Build number** seven times.
3. Return to Settings, open **Developer options**, and enable **USB debugging**.
4. Reconnect the cable and accept the phone's trust prompt.

## Test each tab manually

### Dilution

1. Tap **Dilution** at the top of the app.
2. Enter `10` for Stock concentration and select **PPM**.
3. Enter `200` for Final concentration and select **PPB**.
4. Enter `50` mL for Final solution volume.
5. Tap **Calculate**. Volume from stock should show `1 mL`.
6. Scroll below the result. Calculation Steps should show `10 PPM = 10,000 PPB`, substitute `200 PPB × 50 mL`, cancel PPB, and finish with `1 mL`.
7. Tap **Clear all**. The inputs, result, and calculation steps should disappear.

The screen also displays the equation, converts PPM and PPB exactly, and marks repeating results with `R` after three repetitions.

### RPD

1. Tap **RPD** at the top of the app.
2. Enter `10` for Original Sample Result.
3. Enter `12` for Replicate Sample Result. Both values must use the same units.
4. Tap **Calculate**. Relative Percent Difference should show `18.18%`.
5. Scroll below the result. Calculation Steps should show a difference of `2`, an average of `11`, an absolute average of `11`, a display-only approximation of `18.181818%`, and the final `18.18%`.
6. Enter `0` in both fields and calculate. The steps should clear and the app should display: `RPD cannot be calculated when the average is zero.`
7. Tap **Clear** to reset this tab.

### Unit conversions

1. Tap **Unit conversions** at the top of the app.
2. Choose **Mass** as the category.
3. Choose **milligrams (mg)** as the starting unit.
4. Choose **micrograms (µg)** as the destination unit.
5. Enter `25` and tap **Calculate**. The result should show `25000 µg`, and the explanation should say `1 mg = 1,000 µg.`
6. Scroll below the result. Calculation Steps should show the factor, `25 mg × (1,000 µg ÷ 1 mg)`, unit cancellation, and `25,000 µg`.
7. Tap **Swap starting and destination units**. With `25` still entered, calculate again; the result should show `0.025 mg`.
8. Try the Volume and Mass concentration categories. Each unit menu only contains units from its selected category.
9. Tap **Clear** to remove the entered value, result, and calculation steps.

### MS/MSD

1. Scroll the tab row if needed and tap **MS/MSD**.
2. Choose **PPB** as the shared concentration unit.
3. Enter `5` for Raw diluted source-sample result.
4. Enter `10` for Sample dilution factor.
5. Enter `50` for Final spike concentration added.
6. Enter `55` for Literal MS result and `50` for Literal MSD result. The shared selector applies PPB to all four concentration values.
7. Tap **Calculate**. The results should show an original source concentration of `50 PPB`, MS recovery of `100.00%`, MSD recovery of `90.00%`, and MS/MSD RPD of `9.52%`.
8. Review the selectable Calculation Steps. They should show PPB throughout, state that the spike was added after dilution, and show where PPB cancels in percentage calculations.
9. Change the shared unit to **PPM** and calculate again to see PPM applied consistently to the inputs, source result, and steps.
10. Tap **Clear**. The dilution factor returns to `1`, the shared unit returns to PPB, and the other inputs, results, and steps clear.

To copy calculation work, press and hold text inside a Calculation Steps card, adjust the selection handles if necessary, and tap **Copy**.

To check tab isolation, enter different values on two tabs and switch between them. Each tab keeps its own values and never copies them into another calculator.

To check field contrast, switch the phone or emulator to system dark mode and reopen the app. The app intentionally remains light, with black entered text on white fields.

## Run the automated tests

In Android Studio:

1. In the Project panel, open `app > src > test > java > com.example.labcalculator > calculation`.
2. Right-click the `calculation` folder.
3. Choose **Run 'Tests in calculation'**.
4. Confirm Android Studio shows all tests in green.

Alternatively, open the Terminal tab in Android Studio and run:

```powershell
.\gradlew.bat test
```

## Build an installable APK

1. In Android Studio, select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2. Wait for the build-completed notification.
3. Click **locate** in that notification, or open:

   `app/build/outputs/apk/debug/app-debug.apk`

## Install the APK manually

1. Copy `app-debug.apk` to the Android phone.
2. Open the APK from the phone's Files app.
3. If Android blocks it, follow the prompt to allow that Files app to **install unknown apps**.
4. Tap **Install**.

The debug APK is suitable for personal testing. Only install an APK that you built or received from someone you trust.

If Android's command-line tools are available, a connected phone can also be updated with:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Calculation rules

### Dilution

- `C1` is the stock concentration and must be greater than zero.
- `C2` is the desired final concentration and may be zero, but not negative.
- `V2` is the final solution volume in mL and must be greater than zero.
- After PPM/PPB conversion, `C2` cannot exceed `C1` because this calculator performs dilution, not concentration.
- Intermediate values are never rounded.

### RPD

- The equation is `|Original − Replicate| ÷ |((Original + Replicate) ÷ 2)| × 100`.
- Intermediate values are not rounded. Only the final result is rounded to two decimal places.
- The calculation is stopped when the average is zero.

### Unit conversions

- Only units in the same category can be converted.
- All conversion factors are exact powers of ten.
- Intermediate values are never rounded.
- Results omit unnecessary trailing zeros and use ordinary decimal notation for small values.

### MS/MSD

- One shared selector applies either PPB or PPM to the raw source, spike, MS, and MSD concentration values.
- The original source result and all concentration calculation steps display the selected unit. The common unit cancels when recovery and RPD percentages are calculated.
- The raw source result is multiplied by the sample dilution factor to calculate the original source concentration.
- The spike is added after sample dilution, so the dilution factor is not applied to the spike, literal MS/MSD results, recoveries, or RPD.
- Recovery is calculated against the raw diluted source result. MS/MSD RPD compares the two literal measured results using the absolute average denominator.
- Negative recoveries and recoveries above 100% remain visible; the app does not determine pass or fail.
- Intermediate values are not rounded. Final recovery and RPD percentages are rounded to two decimal places.
