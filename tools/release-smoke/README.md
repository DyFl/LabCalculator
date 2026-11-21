# Release APK smoke check

This separate test application drives the installed `com.dyfl.labcalculator` APK through Android accessibility. It bundles its own runner and Kotlin runtime, without linking to the production APK's obfuscated classes. It is separate from the 57 Compose/application regression tests and must never be uploaded as a release asset.

Run on a disposable Android 9 test device/emulator. The test launches the actual main application, changes its current calculator inputs, exercises copying and a uniquely named preset, then deletes only that preset. It does not uninstall or clear application data. Do not run it against an everyday installation with inputs you need to retain. Clipboard inspection in this harness is scoped to Android 9; later Android clipboard restrictions require another approach.

From this directory, with `ANDROID_HOME` pointing to the SDK:

```powershell
..\..\gradlew.bat assembleDebug --max-workers=1
adb -s SERIAL install -r PATH_TO_EXACT_RELEASE_APK
adb -s SERIAL install -r -t "build/outputs/apk/debug/Lab Calculator release smoke-debug.apk"
adb -s SERIAL shell am instrument -w -r -e class com.dyfl.labcalculator.ReleaseApkSmokeTest com.dyfl.labcalculator.binarycheck/androidx.test.runner.AndroidJUnitRunner
```

Use the actual generated APK filename if AGP names it differently. The check covers all five numerical examples, units and swap, result and step copying, preset save/load/delete, MS/MSD Next sample, and saved-task restoration after background process termination. Screenshots are written under this test application's external-files `release-smoke` directory using shell screencap, avoiding the emulator's native instrumentation screenshot service.

A debug test APK from the main app cannot simply be paired with an R8 release APK: its runner may reference runtime classes removed/renamed in production. That bootstrap failure says nothing about the calculator assertions. This standalone runner avoids adding production keep rules solely for testing.
