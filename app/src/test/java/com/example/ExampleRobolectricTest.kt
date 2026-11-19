package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.compatibility.DeviceCompatibilityEngine
import com.example.data.BackupManager
import com.example.data.DefaultEqPresets
import com.example.data.SecurityHashUtil
import com.example.model.AppThemeId
import com.example.model.CompatibilityTier
import com.example.model.CustomizationSettings
import com.example.model.EarbudBrand
import com.example.model.SmartSettings
import com.example.model.SoundMode
import com.example.model.SubwooferConfig
import com.example.model.UiModeOption
import com.example.ota.GitHubReleaseService
import com.example.ota.OtaChannel
import com.example.ota.OtaErrorType
import com.example.ota.OtaSecurityVerifier
import com.example.ota.SemVerComparator
import com.example.ui.navigation.HardikBudsProRoot
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.HardikBudsProTheme
import com.example.viewmodel.BudsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.io.File
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
    fun all10NothingOsThemesAreConfigured() {
        assertEquals(10, AppThemeId.entries.size)
        val expectedNames = setOf(
            "Nothing Monochrome", "Black", "White", "Red", "Titanium",
            "Midnight", "Silver", "Neon", "AMOLED", "Custom",
        )
        assertEquals(expectedNames, AppThemeId.entries.map { it.displayName }.toSet())
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
    fun splashScreenDisplaysNothingOsBranding() {
        composeTestRule.setContent {
            HardikBudsProTheme(customization = CustomizationSettings()) {
                SplashScreen(onSplashFinished = {})
            }
        }
        composeTestRule.onNodeWithTag("splash_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("HARDIK BUDS PRO").assertIsDisplayed()
        composeTestRule.onNodeWithText("ULTRA PREMIUM").assertIsDisplayed()
    }

    @Test
    fun bottomNavigationTraversesAllFiveScreensAndShortcuts() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = BudsViewModel(app)
        composeTestRule.setContent {
            HardikBudsProRoot(viewModel = vm, skipSplash = true)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_sound").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sound_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_devices").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("devices_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_fast_pair_card").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_customize").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("customize_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    }

    @Test
    fun homeTheatreDetectionAndSmoothConnectionWork() {
        assertTrue(DeviceCompatibilityEngine.isHomeTheatreDevice("Sony HT-A7000 Soundbar"))
        assertTrue(DeviceCompatibilityEngine.isHomeTheatreDevice("JBL Bar 9.1 Home Theater"))
        assertFalse(DeviceCompatibilityEngine.isHomeTheatreDevice("Pixel Buds Pro"))

        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = BudsViewModel(app)
        composeTestRule.setContent {
            HardikBudsProRoot(viewModel = vm, skipSplash = true)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen")
            .performScrollToNode(hasTestTag("home_theatre_smooth_btn"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("google_fast_connect_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_theatre_smooth_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.homeTheatreSettings.value.theatreConnectedActive)
        vm.disconnectHomeTheatreSmooth()
        composeTestRule.waitForIdle()
        assertFalse(vm.homeTheatreSettings.value.theatreConnectedActive)
    }

    @Test
    fun uiModeEngineSwitchesHomeLayoutsAndCustomizeSelector() {
        assertEquals(5, UiModeOption.entries.size)
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = BudsViewModel(app)
        composeTestRule.setContent {
            HardikBudsProRoot(viewModel = vm, skipSplash = true)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ui_mode_strip").assertIsDisplayed()

        // Switch to Pro Telemetry HUD
        composeTestRule.onNodeWithTag("ui_mode_chip_PRO_TELEMETRY").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pro_telemetry_hud_card").assertIsDisplayed()

        // Switch to Theatre Cinema HUD
        composeTestRule.onNodeWithTag("ui_mode_chip_THEATRE_AMBIENT").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theatre_ambient_hud_banner").assertIsDisplayed()

        // Switch to Drive / Dock HUD
        composeTestRule.onNodeWithTag("ui_mode_strip")
            .performScrollToNode(hasTestTag("ui_mode_chip_DRIVE_DOCK"))
        composeTestRule.onNodeWithTag("ui_mode_chip_DRIVE_DOCK").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("drive_dock_grid").assertIsDisplayed()

        // Verify Customize screen displays UI Mode Engine selector
        composeTestRule.onNodeWithTag("nav_customize").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("customize_screen")
            .performScrollToNode(hasTestTag("ui_mode_selector_card"))
        composeTestRule.onNodeWithTag("ui_mode_selector_card").assertIsDisplayed()
    }

    @Test
    fun semVerComparisonAndSecurityValidatorsWorkCorrectly() {
        assertTrue(SemVerComparator.isNewerVersion("v1.0.1", "1.0.0"))
        assertTrue(SemVerComparator.isNewerVersion("V1.1.0", "1.0.1"))
        assertTrue(SemVerComparator.isNewerVersion("2.0.0", "1.1.0"))
        assertTrue(SemVerComparator.isNewerVersion("1.1.0", "1.1.0-beta.1"))
        assertFalse(SemVerComparator.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(SemVerComparator.isNewerVersion("1.0.0-beta.1", "1.0.0"))

        assertTrue(SemVerComparator.isTrustedGitHubDownloadUrl("https://github.com/XKarankumar/Hardik-Buds/releases/download/v1.0.1/app.apk"))
        assertTrue(SemVerComparator.isTrustedGitHubDownloadUrl("https://objects.githubusercontent.com/github-production-release-asset/app.apk"))
        assertFalse(SemVerComparator.isTrustedGitHubDownloadUrl("http://github.com/XKarankumar/Hardik-Buds/releases/download/v1.0.1/app.apk"))
        assertFalse(SemVerComparator.isTrustedGitHubDownloadUrl("https://untrusted.example.com/app.apk"))

        val sampleNotes = "SHA-256: a1b2c3d4e5f60718293a4b5c6d7e8f90123456789abcdef0123456789abcdef0"
        assertEquals(
            "a1b2c3d4e5f60718293a4b5c6d7e8f90123456789abcdef0123456789abcdef0",
            SemVerComparator.extractSha256FromText(sampleNotes),
        )
    }

    @Test
    fun gitHubReleasesJsonParsingFiltersStableVsBetaAndDetectsApk() {
        val sampleJson = """
            [
              {
                "tag_name": "v1.1.0-beta.1",
                "name": "Hardik Buds Pro Beta 1",
                "prerelease": true,
                "draft": false,
                "published_at": "2026-11-18T10:00:00Z",
                "html_url": "https://github.com/XKarankumar/Hardik-Buds/releases/tag/v1.1.0-beta.1",
                "body": "Beta spatial tuning. SHA-256: 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "assets": [
                  {
                    "name": "Hardik-Buds-Pro-Ultra-Premium-v1.1.0-beta.1.apk",
                    "size": 15728640,
                    "browser_download_url": "https://github.com/XKarankumar/Hardik-Buds/releases/download/v1.1.0-beta.1/Hardik-Buds-Pro-Ultra-Premium-v1.1.0-beta.1.apk"
                  }
                ]
              },
              {
                "tag_name": "v1.0.1",
                "name": "Hardik Buds Pro Bug Fix",
                "prerelease": false,
                "draft": false,
                "published_at": "2026-11-15T10:00:00Z",
                "html_url": "https://github.com/XKarankumar/Hardik-Buds/releases/tag/v1.0.1",
                "body": "Bug fixes",
                "assets": [
                  {
                    "name": "Hardik-Buds-Pro-Ultra-Premium-debug.apk",
                    "size": 14680064,
                    "browser_download_url": "https://github.com/XKarankumar/Hardik-Buds/releases/download/v1.0.1/Hardik-Buds-Pro-Ultra-Premium-debug.apk"
                  }
                ]
              }
            ]
        """.trimIndent()

        val stableRes = GitHubReleaseService.parseReleasesJson(sampleJson, OtaChannel.STABLE)
        assertNotNull(stableRes.latestForChannel)
        assertEquals("1.0.1", stableRes.latestForChannel?.versionName)
        assertEquals(14680064L, stableRes.latestForChannel?.apkSizeBytes)

        val betaRes = GitHubReleaseService.parseReleasesJson(sampleJson, OtaChannel.BETA)
        assertNotNull(betaRes.latestForChannel)
        assertEquals("1.1.0-beta.1", betaRes.latestForChannel?.versionName)
        assertEquals(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            betaRes.latestForChannel?.expectedSha256,
        )
    }

    @Test
    fun otaSha256VerificationAndInvalidApkRejectionWork() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeApk = File(context.cacheDir, "corrupt_test.apk")
        fakeApk.writeText("not-a-valid-android-apk-archive")

        val actualSha = OtaSecurityVerifier.computeFileSha256(fakeApk)
        assertEquals(64, actualSha.length)

        // Mismatched SHA-256 must fail
        val badShaReport = OtaSecurityVerifier.verifyDownloadedApk(
            context = context,
            apkFile = fakeApk,
            expectedSizeBytes = fakeApk.length(),
            expectedSha256 = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
            installedVersionCode = 1L,
            installedVersionName = "1.0.0",
        )
        assertFalse(badShaReport.isValid)
        assertEquals(OtaErrorType.INVALID_APK, badShaReport.errorInfo?.type)

        // Matching SHA-256 still rejects invalid/corrupt APK binary structure
        val corruptArchiveReport = OtaSecurityVerifier.verifyDownloadedApk(
            context = context,
            apkFile = fakeApk,
            expectedSizeBytes = fakeApk.length(),
            expectedSha256 = actualSha,
            installedVersionCode = 1L,
            installedVersionName = "1.0.0",
        )
        assertFalse(corruptArchiveReport.isValid)
        assertEquals(OtaErrorType.INVALID_APK, corruptArchiveReport.errorInfo?.type)
        fakeApk.delete()
    }

    @Test
    fun settingsSoftwareUpdateOpensUpdateCenter() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = BudsViewModel(app)
        composeTestRule.setContent {
            HardikBudsProRoot(viewModel = vm, skipSplash = true)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("open_update_center_button"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("open_update_center_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("update_center_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("update_center_status_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("check_updates_ota_btn").assertIsDisplayed()

        composeTestRule.onNodeWithTag("update_center_screen")
            .performScrollToNode(hasTestTag("ota_history_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ota_history_card").assertIsDisplayed()
    }
}
