# Release and verification status

The historical documentation review below was performed November 20, 2026. It did not rerun tests. Subsequent full-release preparation integrates the documentation and November application branches; new executed results and APK verification are recorded separately in [the 1.1.0 release record](releases/1.1.0.md). The original failures and historical counts remain unchanged.

## What each record describes

| Artifact / checkpoint | Revision | Documented JVM result | Documented Android result | Evidence |
| --- | --- | --- | --- | --- |
| Published v1.0.1 Preview 1 | `e9058f4` | 53 passed | Device UI testing not performed | [Release notes](https://github.com/DyFl/LabCalculator/releases/tag/v1.0.1-preview.1) |
| Published v1.0.2 Preview 1 | `35cff0a` | 68 passed | 12 passed on the exact preview APK, BlueStacks Android 9 / API 28 | [Release notes](https://github.com/DyFl/LabCalculator/releases/tag/v1.0.2-preview.1); [packaging record](audit.md#preview-packaging--2026-09-29) |
| September shared-UI update | `f3b3bd9` | 68 passed | 17 passed, Android 9, 360 dp / 1.3× text | [UI notes](ui-polish.md#verification) |
| Validation and preset follow-up | `7a50282` | 71 passed | 24 passed, Android 9, 360 dp / 1.3× text | [Audit record](audit.md) |
| Dilution display / preset-status pass | Included in `1dc95b0` | 83 passed | 29 passed; 3 focused tests also passed at 320 dp / 1.5× | [Pass notes](dilution-presets.md#verification) |
| Concentration-unit pass | `1dc95b0` | 100 passed | 38 passed; 11 focused tests also passed at 320 dp / 1.5× | [Unit notes](concentration-units.md#verification) |
| Default-branch workflow cleanup | `83bd243` | 100 passed | 48 passed on Android 9 at 360 dp / 1.3×; 21 focused tests also passed at 320 dp / 1.5× | [Workflow notes](workflow-cleanup.md#verification) |
| November design checkpoint | Recorded within `71b4733`, before responsiveness additions | 100 passed | 54 passed on Android 9 at 360 dp / 1.3× | [Design report](https://github.com/DyFl/LabCalculator/blob/71b4733906c618cd342c5a150fc865e422a40a89/docs/instrument-design.md#verification-during-the-design-pass) |
| Final November source preview | `71b4733` | 100 passed, including a documented forced rerun | 57/57 passed on Android 9 at 360 dp / 1.3× using the final app source and updated test dependencies | [Final preview verification](https://github.com/DyFl/LabCalculator/blob/71b4733906c618cd342c5a150fc865e422a40a89/docs/responsiveness.md#source-preview-verification) |

Counts refer to test methods, not every input case exercised inside those methods. Focused reruns overlap the complete suite and must not be added to its count. A source inspection during this documentation review counted 68/12 `@Test` declarations at the release tag, 100/48 on reviewed `main`, and 100/57 on the November branch. That corroborates suite size, not execution or a passing result.

The 83-test and 54-test records describe intermediate local development passes, not separate published versions. The public history does not provide an independent commit for every local checkpoint.

## November physical-device result

The responsiveness report records **51 of 57 tests passing** in the full Pixel 9 Pro / Android 17 run. Six late workflow tests failed with `No compose hierarchies found in the app`, before their scientific assertions could execute. The entire workflow class subsequently passed **10/10 in isolation**, including those six tests, with no assertions removed.

That isolated rerun is useful evidence but does not make the original full run a pass. The root cause of hierarchy loss was not established. At that historical checkpoint, a single uninterrupted complete Android 17 pass was outstanding. During the later release preparation, the full suite passed 57/57 after a first 56/57 attempt and a separate 9/9 state-class rerun; see [the release record](releases/1.1.0.md). Neither later result changes the original six-failure run into a pass. The reported performance measurements apply to specific warm workloads and local builds; they are not a responsiveness guarantee or certification.

For the final 57-test Android 9 run, the report documents an isolated application ID and a corresponding package-identity assertion adjustment in a temporary verification snapshot. Other app and test sources matched the reviewed checkout. Screenshot capture was disabled because of an emulator capture-service crash; functional, layout, clipping, clipboard, and state assertions still ran. These qualifications matter when interpreting the result.

## CI, lint, and evidence limits

Existing GitHub Actions runs were inspected during this review:

- [`83bd243` on main](https://github.com/DyFl/LabCalculator/actions/runs/36933823062): completed successfully.
- [`71b4733` on the November branch](https://github.com/DyFl/LabCalculator/actions/runs/44005008077): completed successfully.

The workflows run JVM tests and debug builds across Windows, macOS, and Linux. They do not execute the Android device suite. The main workflow uploads debug APK artifacts; the November branch restricts artifact upload to explicit manual opt-in. Neither is a new tagged software release.

Historical lint counts also belong to particular checkpoints: the published v1.0.2 record reports zero errors / 15 warnings; the reviewed main workflow record reports zero / 19; the November design pass reports zero / 13; the final November responsiveness record reports zero / 17 per checked variant. Do not combine these into a single current lint result.

The checked-in development notes describe local test execution. Most raw reports, device logs, and performance traces are ignored build outputs and are not available in the repository. They were not independently rerun or reconstructed here. Historical offline Gradle connected-test attempts lacked a cached UTP runner dependency; the documented Android suites ran through ADB / AndroidJUnitRunner. A successful direct runner invocation is distinct from a successful `connectedDebugAndroidTest` task.

TalkBack, the configured Android 7 minimum, additional physical devices, and other keyboard implementations still need hands-on coverage. Exact arithmetic and automated checks do not replace method-specific validation, measurement uncertainty, or laboratory QA review.

## Version and screenshot identity

The latest published APK is `v1.0.2-preview.1`, Android version code `3`, from `35cff0a`. Reviewed `main` and the November branch retain that base version despite later changes. The November branch additionally defines a local `1.0.2-preview.1-optimized-local` variant. A displayed version string alone therefore does not identify the source or signer of a local build.

At the presentation checkpoint, the README's November screenshots were pinned to `71b4733`, captured November 14 from a running development build. The release README now shows captures from the exact optimized 1.1.0 APK and retains links to those earlier portrait/dark-theme images. The older `*-current.png` names refer to the September 30 checkpoint. Original images and historical notes are retained.

The presentation-only update described above made no application or release changes. Subsequent, separately authorized 1.1.0 release preparation changes the version and optimized release configuration and prepares a draft; its new evidence is recorded separately.
