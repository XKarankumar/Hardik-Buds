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
import com.example.audio.DspAudioProcessor
import com.example.compatibility.DeviceCompatibilityEngine
import com.example.data.BackupManager
import com.example.data.DefaultEqPresets
import com.example.data.SecurityHashUtil
import com.example.model.AppThemeId
import com.example.model.CompatibilityTier
import com.example.model.CustomizationSettings
import com.example.model.DspConfig
import com.example.model.EarbudBrand
import com.example.model.EqBandMode
import com.example.model.FilterSlope
import com.example.model.ParametricEqNode
import com.example.model.SmartSettings
import com.example.model.SoundMode
import com.example.model.SubwooferConfig
import com.example.model.UiModeOption
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
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
            HardikBudsProTheme(customization = CustomizationSettings()) {
                com.example.ui.screens.UpdateCenterScreen(
                    otaUiState = vm.otaUiState.value,
                    otaSettings = vm.otaSettings.value,
                    downloadState = vm.otaDownloadState.value,
                    updateHistory = emptyList(),
                    onNavigateBack = {},
                    onCheckForUpdates = {},
                    onStartDownload = {},
                    onPauseDownload = {},
                    onResumeDownload = {},
                    onCancelDownload = {},
                    onRetryDownload = {},
                    onInstallUpdate = { null },
                    onUpdateOtaSettings = {},
                    onClearHistory = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("update_center_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("update_center_status_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("check_updates_ota_btn").assertIsDisplayed()
    }

    @Test
    fun dspAudioProcessorMathFiltersBalanceLimiterAndSampleRatesWork() {
        fun generateSineStereo(freqHz: Float, sampleRate: Int, frames: Int, amplitude: Float = 0.5f): FloatArray {
            val out = FloatArray(frames * 2)
            for (i in 0 until frames) {
                val s = amplitude * sin(2.0 * PI * freqHz * i / sampleRate).toFloat()
                out[i * 2] = s
                out[i * 2 + 1] = s
            }
            return out
        }

        fun rms(buf: FloatArray, channelOffset: Int): Float {
            var sum = 0.0
            val count = buf.size / 2
            val skip = count / 4 // skip transient settle window
            for (i in skip until count) {
                val s = buf[i * 2 + channelOffset]
                sum += s * s
            }
            return sqrt(sum / (count - skip)).toFloat()
        }

        val processor = DspAudioProcessor(48000)

        // 1. Multi-sample rate stability + Anti-clipping Peak Limiter bounds
        for (sr in listOf(44100, 48000, 96000)) {
            processor.setSampleRate(sr)
            assertEquals(sr, processor.getSampleRateHz())
            processor.updateConfiguration(
                dspConfig = DspConfig(
                    preampDb = 10f,
                    masterGainDb = 8f,
                    bassEngineEnabled = true,
                    deepBassPercent = 90,
                    subBassPercent = 90,
                    limiterEnabled = true,
                    clippingPreventionEnabled = true,
                ),
                soundMode = SoundMode.BASS,
                profileBands15 = List(15) { if (it < 4) 10f else 0f },
            )
            processor.resetStates()
            val hotWave = generateSineStereo(63f, sr, frames = sr / 4, amplitude = 0.9f)
            processor.processStereoInterleaved(hotWave)
            for (sample in hotWave) {
                assertFalse("NaN detected at sample rate $sr", sample.isNaN())
                assertTrue("Sample exceeded [-1.0, 1.0] at $sr: $sample", abs(sample) <= 1.0001f)
            }
        }

        // 2. High-Pass Filter (24 dB/oct @ 400Hz) attenuates 60Hz sub-bass significantly
        processor.setSampleRate(48000)
        processor.updateConfiguration(
            dspConfig = DspConfig(
                highPassEnabled = true,
                highPassCutoffHz = 400f,
                highPassSlope = FilterSlope.SLOPE_24DB,
                limiterEnabled = false,
                clippingPreventionEnabled = false,
            ),
            soundMode = SoundMode.MUSIC,
            profileBands15 = List(15) { 0f },
        )
        processor.resetStates()
        val lowWave = generateSineStereo(60f, 48000, frames = 8192, amplitude = 0.5f)
        val rawLowRms = rms(lowWave, 0)
        processor.processStereoInterleaved(lowWave)
        val filteredLowRms = rms(lowWave, 0)
        assertTrue("Expected HPF to attenuate 60Hz (<20% remaining), was $filteredLowRms vs $rawLowRms", filteredLowRms < rawLowRms * 0.20f)

        // 3. Low-Pass Filter (24 dB/oct @ 500Hz) attenuates 6000Hz highs significantly
        processor.updateConfiguration(
            dspConfig = DspConfig(
                lowPassEnabled = true,
                lowPassCutoffHz = 500f,
                lowPassSlope = FilterSlope.SLOPE_24DB,
                limiterEnabled = false,
                clippingPreventionEnabled = false,
            ),
            soundMode = SoundMode.MUSIC,
            profileBands15 = List(15) { 0f },
        )
        processor.resetStates()
        val highWave = generateSineStereo(6000f, 48000, frames = 8192, amplitude = 0.5f)
        val rawHighRms = rms(highWave, 0)
        processor.processStereoInterleaved(highWave)
        val filteredHighRms = rms(highWave, 0)
        assertTrue("Expected LPF to attenuate 6kHz (<15% remaining), was $filteredHighRms vs $rawHighRms", filteredHighRms < rawHighRms * 0.15f)

        // 4. Stereo Balance L100% / R100% / Center verification
        processor.updateConfiguration(
            dspConfig = DspConfig(stereoBalance = -1f, stereoWidthPercent = 100),
            soundMode = SoundMode.MUSIC,
            profileBands15 = List(15) { 0f },
        )
        processor.resetStates()
        val balWaveLeftOnly = generateSineStereo(440f, 48000, frames = 4096, amplitude = 0.4f)
        processor.processStereoInterleaved(balWaveLeftOnly)
        assertTrue("Right channel must be near zero on L100% balance", rms(balWaveLeftOnly, 1) < 0.02f)
        assertTrue("Left channel must stay active on L100% balance", rms(balWaveLeftOnly, 0) > 0.2f)

        // 5. Parametric EQ boosts response around target frequency & Real Goertzel spectrum detects tone
        val peqConfig = DspConfig(
            parametricNodes = listOf(ParametricEqNode(1, enabled = true, frequencyHz = 1000f, qFactor = 1.4f, gainDb = 8f)),
        )
        processor.updateConfiguration(peqConfig, SoundMode.MUSIC, List(15) { 0f })
        val curve = processor.computeFrequencyResponseCurve(listOf(20f, 1000f, 16000f))
        assertTrue("1kHz curve point (${curve[1]} dB) should be boosted > 5 dB by Parametric EQ", curve[1] > 5.0f)

        val silentSpec = processor.analyzeRealPcmSpectrum(FloatArray(2048))
        assertTrue("Silent audio spectrum must remain strictly 0", silentSpec.all { it == 0f })
        val tone63 = generateSineStereo(63f, 48000, frames = 1024, amplitude = 0.6f)
        val toneSpec = processor.analyzeRealPcmSpectrum(tone63)
        assertTrue("63Hz bin (index 2 = ${toneSpec[2]}) should exceed ultrasonic bin (${toneSpec[15]})", toneSpec[2] > toneSpec[15] + 0.25f)
    }

    @Test
    fun defaultPresetsIncludeAllNewDspProfilesAnd15BandExpansion() {
        assertEquals(15, DspConfig.BANDS_15_SPECS.size)
        assertEquals("20Hz", DspConfig.BANDS_15_SPECS.first().shortLabel)
        assertEquals("16kHz", DspConfig.BANDS_15_SPECS.last().shortLabel)

        val defaultDsp = DspConfig()
        assertEquals(15, defaultDsp.bands15Db.size)
        assertTrue(defaultDsp.bands15Db.all { it == 0f })

        val builtIn = DefaultEqPresets.allBuiltIn()
        val names = builtIn.map { it.name }.toSet()
        val required = setOf(
            "Deep Bass", "Bass Head", "Balanced", "Vocal", "Clarity",
            "Studio", "Cinematic", "Gaming", "Rock", "Pop", "EDM",
        )
        assertTrue("Missing required DSP presets: ${required - names}", names.containsAll(required))
        builtIn.forEach { preset ->
            assertEquals("${preset.name} must resolve cleanly to 15 bands", 15, preset.resolved15Bands().size)
            assertEquals("${preset.name} must resolve cleanly to 10 bands", 10, preset.resolved10Bands().size)
        }
    }

    @Test
    fun soundStudio15BandDeepBassAndFiltersUiWork() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = BudsViewModel(app)
        composeTestRule.setContent {
            HardikBudsProTheme(customization = CustomizationSettings()) {
                com.example.ui.screens.SoundScreen(
                    activeDevice = null,
                    dspConfig = vm.dspConfig.value,
                    dspTelemetry = vm.dspTelemetry.value,
                    eqCurveDb = vm.eqCurveDb.value,
                    spectrumBands = vm.spectrumBands.value,
                    subwoofer = vm.subwooferConfig.value,
                    eqProfiles = vm.eqProfiles.value,
                    activeEqProfile = vm.activeEqProfile.value,
                    activeSoundMode = vm.activeSoundMode.value,
                    preferredAncMode = vm.preferredAncMode.value,
                    touchConfig = vm.touchConfig.value,
                    capabilities = vm.audioCapabilities.value,
                    smartSettings = vm.smartSettings.value,
                    bassEnergy = 0.5f,
                    soundTestRunning = null,
                    micPermissionGranted = true,
                    initialTab = 0,
                    onRequestMicPermission = {},
                    onUpdateDspConfig = { transform -> vm.updateDspConfig(transform) },
                    onResetAllDsp = { vm.resetAllDspToDefaults() },
                    onUpdateSubwoofer = { transform -> vm.updateSubwoofer(transform) },
                    onResetSubwoofer = {},
                    onSelectEqPreset = {},
                    onUpdateEqBand15 = { idx, db -> vm.updateActiveEqBand15(idx, db) },
                    onUpdateEqBand = { idx, db -> vm.updateActiveEqBand(idx, db) },
                    onUpdateParametricNode = { id, transform -> vm.updateParametricNode(id, transform) },
                    onSaveCustomEq = {},
                    onRenameCustomEq = { _, _ -> },
                    onDeleteCustomEq = {},
                    onExportEqJson = { "" },
                    onImportEqJson = { true },
                    onSelectSoundMode = {},
                    onSelectAncMode = {},
                    onToggleGamingMode = {},
                    onUpdateTouchConfig = {},
                    onRunSoundTest = {},
                    onStopSoundTest = {},
                    onShowToast = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sound_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sound_engine_status_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("animated_eq_graph_card").assertIsDisplayed()

        // Switch to Deep Bass tab (index 1)
        composeTestRule.onNodeWithTag("sound_tab_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("deep_bass_hero_card").assertIsDisplayed()

        // Switch to Filters & Mix tab (index 2)
        composeTestRule.onNodeWithTag("sound_tab_2").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sound_screen")
            .performScrollToNode(hasTestTag("stereo_balance_card"))
        composeTestRule.onNodeWithTag("stereo_balance_card").assertIsDisplayed()
    }
}
