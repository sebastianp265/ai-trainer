package com.github.sebastianp265.aitrainer.ui.progress

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.ByteArrayOutputStream

private const val SYSTEM_SCREEN_TIMEOUT_MS = 10_000L

class HealthConnectPermissionScreen {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val appPackage = instrumentation.targetContext.packageName
    private val allowAllSwitch = By.text("Allow all")
    private val allowButton = By.text("Allow")
    private val healthPermissions = listOf("android.permission.health.READ_NUTRITION")

    fun revokeAllPermissions() {
        healthPermissions.forEach { instrumentation.uiAutomation.revokeRuntimePermission(appPackage, it) }
    }

    fun allowAll() {
        tap(allowAllSwitch)
        tap(allowButton)
    }

    fun assertClosedAndAppInFront() {
        if (!device.wait(Until.gone(allowAllSwitch), SYSTEM_SCREEN_TIMEOUT_MS)) fail("Health Connect permission screen is still open")
        if (!device.wait(Until.hasObject(By.pkg(appPackage).depth(0)), SYSTEM_SCREEN_TIMEOUT_MS)) fail("the app is not back in front")
    }

    private fun tap(selector: BySelector) {
        val element = device.wait(Until.findObject(selector), SYSTEM_SCREEN_TIMEOUT_MS) ?: fail("$selector not found")
        element.click()
    }

    private fun fail(reason: String): Nothing = throw AssertionError("$reason. Screen:\n${screenDump()}")

    private fun screenDump(): String = ByteArrayOutputStream().also { device.dumpWindowHierarchy(it) }.toString()
}
