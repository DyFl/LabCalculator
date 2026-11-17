package com.dyfl.labcalculator

import androidx.test.platform.app.InstrumentationRegistry

/** Layout assertions still run when an emulator's native screenshot service is unavailable. */
internal fun captureScreenshotsEnabled(): Boolean =
    InstrumentationRegistry.getArguments().getString("captureScreenshots", "true") != "false"
