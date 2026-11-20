# Development follow-up

These tasks come from the public source and verification records reviewed on November 20, 2026. They are separate from the documentation update. No new calculation defect was established by this presentation review; it was not a full source, security, or scientific-validation audit.

## 1. Resolve the full-suite Android 17 failure

**Evidence:** the [responsiveness report](https://github.com/DyFl/LabCalculator/blob/71b4733906c618cd342c5a150fc865e422a40a89/docs/responsiveness.md#initial-six-pixel-failures-and-separate-rerun) records six hierarchy-loss failures in a 57-test full run and a subsequent 10-test isolated pass.

Reproduce the complete suite on the physical Pixel with the final test dependencies. Capture activity/lifecycle and instrumentation diagnostics around the first hierarchy loss. Establish the cause before changing application or harness behavior; retain the assertions and original failure record.

**Completion evidence:** one uninterrupted complete run on the final candidate revision, with device/OS, application ID, runner, test counts, failures, and any snapshot differences recorded. A class-only pass is insufficient.

## 2. Review and integrate the November source preview

`codex/instrument-ui-preview` contains the newer UI, responsiveness changes, local optimized build variants, test-only dependency updates, and a different artifact-upload policy. It is not on `main` at the reviewed revision.

Review its application, configuration, and workflow diff separately from these documentation changes. Resolve documentation conflicts against the final chosen source. Run JVM tests, lint, device checks, and an optimized-build smoke check before proposing the next release.

**Completion evidence:** a reviewed source PR and an exact tested commit, with remaining warnings and device limitations recorded. Do not publish or increment versions as part of the presentation work.

## 3. Complete targeted device and accessibility checks

The records leave TalkBack, Android 7 compatibility, other physical devices, and keyboard variants unverified. Exercise focus order, labels and result announcements, signs/scientific-notation entry, light/dark system bars, large text, and keyboard-open scrolling. Check retained presets, exact unit changes, Next sample, and state restoration on the candidate build.

**Completion evidence:** a small device/OS/input-method matrix with actual outcomes and reproducible defects. Do not describe a configured minimum SDK or emulator layout pass as physical-device compatibility evidence.

## 4. Prepare reproducible release evidence and signing

Before the separately authorized software release, establish the release signing/upgrade approach without committing keys. Verify compatibility with the published APK and preservation of presets where an in-place upgrade is supported. Associate test reports and current screenshots with the exact candidate commit and built artifact; include checksums and clearly scoped release notes.

Review the recorded lint warnings about dependencies, resources, manifest and backup configuration. Confirm the intended behavior and fix justified findings without blanket suppression. Make the documented Gradle connected-test path reproducible with its required runner dependencies.

**Completion evidence:** traceable candidate build, recorded test/lint outcomes, verified signer and upgrade behavior, and no claims that exceed the tested platforms. Publication remains a separate decision.

## Optional investigation

The November measurements show improved number-entry and keyboard-opening performance but no measurable tab-rendering improvement from the targeted code changes. If tab responsiveness remains a practical concern, profile representative optimized-build workflows before changing rendering or caching. Preserve the distinction between the fixed ignored-tap defect and rendering speed.
