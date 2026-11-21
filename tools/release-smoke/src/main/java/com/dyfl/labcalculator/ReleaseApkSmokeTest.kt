package com.dyfl.labcalculator

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Black-box checks also run against the signed R8 APK with a matching test certificate.
 * No Compose test host or calls into obfuscated calculation/preset implementations are used.
 */
@RunWith(AndroidJUnit4::class)
class ReleaseApkSmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val automation get() = instrumentation.uiAutomation
    private val presetName = "Release QA ${System.currentTimeMillis()}"

    private fun nodes(): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo?) {
            if (node == null) return
            result += node
            for (index in 0 until node.childCount) visit(node.getChild(index))
        }
        visit(automation.rootInActiveWindow)
        return result
    }

    private fun matches(node: AccessibilityNodeInfo, label: String) =
        node.text?.toString() == label || node.contentDescription?.toString() == label ||
            node.hintText?.toString() == label

    private fun find(label: String): AccessibilityNodeInfo {
        repeat(18) { attempt ->
            nodes().firstOrNull { matches(it, label) && it.isVisibleToUser &&
                !Rect().also(it::getBoundsInScreen).isEmpty }?.let { return it }
            val direction = if (attempt < 10) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            // The horizontal calculator tabs are also scrollable. Target the tall form.
            nodes().filter { it.isScrollable }.maxByOrNull {
                Rect().also(it::getBoundsInScreen).height()
            }?.performAction(direction)
            Thread.sleep(150)
        }
        error("Missing '$label'; visible text: ${nodes().mapNotNull { it.text?.toString() }}")
    }

    private fun click(label: String) {
        val target = find(label)
        var node: AccessibilityNodeInfo? = target
        while (node != null) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                Thread.sleep(200)
                return
            }
            node = node.parent
        }
        // Labels/icons can be separate virtual nodes from their touch target.
        tap(target)
        Thread.sleep(250)
    }

    private fun tap(node: AccessibilityNodeInfo) {
        val bounds = Rect().also(node::getBoundsInScreen)
        val time = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(time, SystemClock.uptimeMillis(), action,
                bounds.exactCenterX(), bounds.exactCenterY(), 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try { check(automation.injectInputEvent(event, true)) } finally { event.recycle() }
        }
    }

    private fun editor(label: String): AccessibilityNodeInfo {
        find(label)
        repeat(30) {
            // Use one snapshot: keyboard/layout animation can move the label between reads.
            val snapshot = nodes()
            val labels = snapshot.filter { matches(it, label) }
                .map { Rect().also(it::getBoundsInScreen) }.filterNot { it.isEmpty }
            snapshot.filter { candidate -> candidate.isEditable && labels.any { labelBounds ->
                Rect.intersects(labelBounds, Rect().also(candidate::getBoundsInScreen))
            } }.minByOrNull { Rect().also(it::getBoundsInScreen).let { r -> r.width() * r.height() } }
                ?.let { return it }
            Thread.sleep(100)
        }
        error("No editable node associated with $label after layout settled")
    }

    private fun fill(label: String, value: String) {
        val node = editor(label)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
            // Match physical keyboard editing when the label node has no SetText action.
            tap(node)
            for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
                automation.injectInputEvent(KeyEvent(0, SystemClock.uptimeMillis(), action,
                    KeyEvent.KEYCODE_A, 0, KeyEvent.META_CTRL_ON), true)
            }
            shell("input keyevent 67")
            check(value.matches(Regex("[A-Za-z0-9. ]+")))
            shell("input text ${value.replace(" ", "%s")}")
        }
        repeat(20) {
            if (editor(label).text?.toString() == value) return
            Thread.sleep(100)
        }
        assertEquals("Entered value: $label", value, editor(label).text?.toString())
    }

    private fun tab(label: String) {
        click("All calculators")
        click(label)
        click("Clear all")
    }

    private fun copy(label: String, expected: String) {
        click("Copy $label")
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        repeat(30) {
            if (clipboard.primaryClip?.getItemAt(0)?.text?.toString() == expected) return
            Thread.sleep(100)
        }
        assertEquals(expected, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use {
            it.readBytes().toString(Charsets.UTF_8)
        }

    private fun capture(name: String) {
        val directory = File(context.getExternalFilesDir(null), "release-smoke").apply { mkdirs() }
        // Shell screencap avoids the emulator's unreliable native instrumentation capture service.
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("screencap -p")).use {
            File(directory, "$name.png").writeBytes(it.readBytes())
        }
    }

    @Test fun allFiveCalculatorsOnTheInstalledBinary() {
        context.startActivity(Intent().apply {
            setClassName("com.dyfl.labcalculator", "com.dyfl.labcalculator.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        var presetCreated = false
        var failure: Throwable? = null
        try {
            tab("Dilution")
            fill("Stock concentration (C₁)", "10")
            fill("Final concentration (C₂)", "200")
            fill("Final solution volume (V₂)", "50")
            click("Save preset")
            fill("Preset name", presetName)
            click("Save")
            click("Clear all")
            click("Load preset")
            click(presetName)
            presetCreated = true
            click("Calculate")
            val instruction = "Transfer 1 mL of stock and make up to 50 mL final solution volume."
            find(instruction)
            copy("Preparation", instruction)
            capture("dilution-result")
            click("Copy calculation steps")
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            for (attempt in 0 until 30) {
                if (clipboard.primaryClip?.getItemAt(0)?.text?.startsWith("1.") == true) break
                Thread.sleep(100)
            }
            val copiedSteps = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
            assertTrue(copiedSteps.startsWith("1."))
            for (input in listOf("10", "200", "50")) assertTrue(copiedSteps.contains(input))

            tab("RPD")
            fill("Original Sample Result", "10")
            fill("Replicate Sample Result", "12")
            click("Calculate")
            find("18.18%")
            copy("Relative Percent Difference", "18.18%")
            capture("rpd-result")

            tab("Unit conversions")
            fill("Value to convert", "1")
            click("Calculate")
            find("1000 µg")
            copy("Converted result", "1000 µg")
            capture("conversion-result")
            click("Swap units")
            click("Calculate")
            find("1 mg")

            tab("MS/MSD")
            fill("Sample dilution factor", "10")
            fill("Final spike concentration added", "50")
            fill("Raw diluted source-sample result", "5")
            fill("Literal MS result", "55")
            fill("Literal MSD result", "50")
            click("Calculate")
            find("50 PPB")
            copy("Original source concentration", "50 PPB")
            copy("MS recovery", "100.00%")
            copy("MSD recovery", "90.00%")
            copy("MS/MSD RPD", "9.52%")
            capture("ms-msd-result")
            click("Next sample")
            assertEquals("", editor("Raw diluted source-sample result").text?.toString().orEmpty())
            assertEquals("", editor("Literal MS result").text?.toString().orEmpty())
            assertEquals("", editor("Literal MSD result").text?.toString().orEmpty())
            assertEquals("10", editor("Sample dilution factor").text?.toString())
            assertEquals("50", editor("Final spike concentration added").text?.toString())

            tab("Molarity")
            fill("Desired Molarity", "1")
            fill("Final Solution Volume", "1000")
            fill("Formula Weight", "58.44")
            click("Calculate")
            find("58.440 g")
            copy("Required Mass", "58.440 g")
            capture("molarity-result")
            val originalPid = shell("pidof com.dyfl.labcalculator").trim()
            shell("input keyevent 3")
            Thread.sleep(1000)
            shell("am kill com.dyfl.labcalculator")
            Thread.sleep(1000)
            shell("am start -n com.dyfl.labcalculator/.MainActivity")
            find("58.440 g")
            assertNotEquals(originalPid, shell("pidof com.dyfl.labcalculator").trim())
            assertEquals("58.44", editor("Formula Weight").text?.toString())
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            // Delete only the uniquely named preset created by this test.
            try {
                if (presetCreated) {
                    click("All calculators")
                    click("Dilution")
                    click("Load preset")
                    click("Delete $presetName")
                    for (attempt in 0 until 30) {
                        if (nodes().none { matches(it, presetName) }) break
                        Thread.sleep(100)
                    }
                    assertFalse("Created preset must be deleted", nodes().any { matches(it, presetName) })
                    click("Close")
                }
            } catch (cleanup: Throwable) {
                if (failure != null) failure.addSuppressed(cleanup) else throw cleanup
            } finally { shell("input keyevent 3") }
        }
    }
}
