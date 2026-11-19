package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.compatibility.DeviceCompatibilityEngine
import com.example.data.BackupManager
import com.example.data.DefaultEqPresets
import com.example.data.SecurityHashUtil
import com.example.model.CompatibilityTier
import com.example.model.CustomizationSettings
import com.example.model.EarbudBrand
import com.example.model.SmartSettings
import com.example.model.SoundMode
import com.example.model.SubwooferConfig
import com.example.ui.navigation.HardikBudsProRoot
import com.example.viewmodel.BudsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appNameMatchesHardikBudsPro() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Hardik Buds Pro", context.getString(R.string.app_name))
    }

    @Test
    fun compatibilityEngineDetectsAllMajorBrandsAndTiers() {
        assertEquals(
            EarbudBrand.ONEPLUS,
            DeviceCompatibilityEngine.detectBrand("OnePlus Buds Pro 2", "78:9F:70:11:22:33"),
        )
        assertEquals(
            EarbudBrand.SAMSUNG,
            DeviceCompatibilityEngine.detectBrand("Galaxy Buds2 Pro", "24:5E:BE:00:11:22"),
        )
        assertEquals(
            EarbudBrand.APPLE_AIRPODS,
            DeviceCompatibilityEngine.detectBrand("AirPods Pro", "18:1D:EA:11:22:33"),
        )
        assertEquals(
            EarbudBrand.SONY,
            DeviceCompatibilityEngine.detectBrand("WF-1000XM5", "74:45:CE:11:22:33"),
        )
        assertEquals(
            EarbudBrand.NOTHING,
            DeviceCompatibilityEngine.detectBrand("Nothing Ear (2)", "38:E6:0A:11:22:33"),
        )
        assertEquals(
            EarbudBrand.BOAT,
            DeviceCompatibilityEngine.detectBrand("boAt Airdopes 141", "AA:BB:CC:11:22:33"),
        )
        assertEquals(
            EarbudBrand.GOOGLE_PIXEL_BUDS,
            DeviceCompatibilityEngine.detectBrand("Pixel Buds Pro", "E0:9D:FA:11:22:33"),
        )
        assertEquals(
            EarbudBrand.GENERIC,
            DeviceCompatibilityEngine.detectBrand("Unknown Headset", "00:11:22:33:44:55"),
        )

        val genericReport = DeviceCompatibilityEngine.evaluateCompatibility(EarbudBrand.GENERIC)
        assertEquals(CompatibilityTier.TIER_1_STANDARD, genericReport.tier)
        assertFalse(genericReport.supportsHardwareAncControl)
    }

    @Test
    fun defaultPresetsContainAll14RequiredProfiles() {
        val names = DefaultEqPresets.allBuiltIn().map { it.name }.toSet()
        val expected = setOf(
            "Deep Bass", "Subwoofer", "EDM", "Punjabi", "Bollywood",
            "Pop", "Rock", "Hip-Hop", "Classical", "Gaming",
            "Cinema", "Vocal Boost", "Balanced", "Custom",
        )
        assertTrue(names.containsAll(expected))
    }

    @Test
    fun backupRoundTripAndPinHashVerificationWorkCorrectly() {
        val hash = SecurityHashUtil.hashSecret("1947")
        assertTrue(SecurityHashUtil.verifySecret("1947", hash))
        assertFalse(SecurityHashUtil.verifySecret("0000", hash))

        val eq = DefaultEqPresets.allBuiltIn().first { it.name == "Punjabi" }
        val eqJson = BackupManager.exportEqProfileJson(eq)
        val importedEq = BackupManager.importEqProfileJson(eqJson).getOrThrow()
        assertEquals("Punjabi", importedEq.name)
        assertEquals(10, importedEq.bandsDb.size)

        val backupJson = BackupManager.exportFullBackupJson(
            customization = CustomizationSettings(),
            subwoofer = SubwooferConfig(bassIntensity = 80),
            soundMode = SoundMode.BASS,
            smartSettings = SmartSettings(),
            customEqProfiles = listOf(importedEq),
        )
        val restored = BackupManager.importFullBackupJson(backupJson).getOrThrow()
        assertEquals(80, restored.subwoofer.bassIntensity)
        assertEquals(SoundMode.BASS, restored.soundMode)
    }

    @Test
    fun bottomNavigationTraversesAllFiveScreens() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = BudsViewModel(app)
        composeTestRule.setContent {
            HardikBudsProRoot(viewModel = vm)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_sound").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sound_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_devices").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("devices_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_customize").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("customize_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    }
}
