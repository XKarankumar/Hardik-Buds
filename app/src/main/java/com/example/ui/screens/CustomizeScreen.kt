package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.AnimationSpeedOption
import com.example.model.AppThemeId
import com.example.model.BatteryRingStyle
import com.example.model.ButtonStyleOption
import com.example.model.CardAppearance
import com.example.model.ConnectionAnimStyle
import com.example.model.CustomizationSettings
import com.example.model.DarkModePreference
import com.example.model.DotMatrixStyle
import com.example.model.FontScaleOption
import com.example.model.IconStyleOption
import com.example.model.UiModeOption
import com.example.ui.components.LuxActionButton
import com.example.ui.components.LuxGlassCard
import com.example.ui.components.RedSignalDot
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalLuxThemeExtras

@Composable
fun CustomizeScreen(
    customization: CustomizationSettings,
    onUpdateCustomization: ((CustomizationSettings) -> CustomizationSettings) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current

    // Zero-permission Android Photo Picker for custom wallpaper
    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let { picked ->
                onUpdateCustomization { it.copy(customWallpaperUri = picked.toString()) }
            }
        },
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customize_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                title = "Customize OS",
                subtitle = "10 THEMES · DOT MATRIX · RGB ACCENTS · DATASTORE PERSISTENCE",
                trailing = { Icon(Icons.Outlined.Palette, contentDescription = null, tint = extras.signalRed) },
            )
        }

        // Live Preview Card
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth(), highlightRedBorder = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RedSignalDot(active = true, size = 8.dp)
                            Text(
                                text = customization.themeId.displayName.uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = customization.themeId.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusBadgePill(label = customization.uiMode.badge, isRedActive = true)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    LuxActionButton(text = "Active Action", useRedAccent = true, onClick = {}, modifier = Modifier.weight(1f))
                    LuxActionButton(text = "Monochrome", filled = false, onClick = {}, modifier = Modifier.weight(1f))
                }
            }
        }

        // UI Mode Engine (Studio / Minimal / Pro Telemetry HUD / Theatre Cinema / Drive Dock)
        item {
            LuxGlassCard(
                modifier = Modifier.fillMaxWidth().testTag("ui_mode_selector_card"),
                highlightRedBorder = customization.uiMode != UiModeOption.STUDIO,
            ) {
                SectionHeader(
                    title = "UI Mode Engine",
                    subtitle = "ADAPT HOME LAYOUT DENSITY, TELEMETRY HUD & DOCK CONTROLS",
                    trailing = {
                        StatusBadgePill(label = customization.uiMode.badge, isRedActive = true)
                    },
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UiModeOption.entries.forEach { mode ->
                        val selected = customization.uiMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) extras.signalRed.copy(alpha = 0.14f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                )
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) extras.signalRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable {
                                    onUpdateCustomization { it.copy(uiMode = mode) }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("ui_mode_option_${mode.name}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    RedSignalDot(active = selected, size = 7.dp)
                                    Text(
                                        text = mode.label.uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (selected) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            StatusBadgePill(label = mode.badge, isRedActive = selected)
                        }
                    }
                }
            }
        }

        // 10 Curated Themes
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "10 System Themes", subtitle = "BLACK · WHITE · RED · TITANIUM · MIDNIGHT · SILVER · NEON · AMOLED · CUSTOM")
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(AppThemeId.entries, key = { it.name }) { theme ->
                        val selected = customization.themeId == theme
                        val cardBg = if (theme == AppThemeId.WHITE) Color(theme.lightSurfaceHex) else Color(theme.darkBgHex)
                        val textCol = if (theme == AppThemeId.WHITE) Color(0xFF111113) else Color.White
                        Column(
                            modifier = Modifier
                                .width(122.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBg)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Color(theme.accentHex) else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .clickable {
                                    val nextDark = if (theme.prefersLightMode) DarkModePreference.LIGHT else customization.darkMode
                                    onUpdateCustomization { it.copy(themeId = theme, darkMode = nextDark) }
                                }
                                .padding(12.dp)
                                .testTag("theme_card_${theme.name}"),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(theme.primaryHex))
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(theme.accentHex)),
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = "Selected theme",
                                        tint = Color(theme.accentHex),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = theme.displayName.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = textCol,
                                maxLines = 1,
                            )
                            Text(
                                text = theme.tagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = textCol.copy(alpha = 0.68f),
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }

        // Display Mode, Dot Matrix Typography & Accent Studio
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Mode, Dot Matrix & Accent Color")
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DarkModePreference.entries.forEachIndexed { idx, pref ->
                        SegmentedButton(
                            selected = customization.darkMode == pref,
                            onClick = { onUpdateCustomization { it.copy(darkMode = pref) } },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = DarkModePreference.entries.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = extras.signalRed.copy(alpha = 0.16f),
                                activeContentColor = extras.signalRed,
                            ),
                        ) {
                            Text(pref.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                ChipGroupSelector("Dot-Matrix Style", DotMatrixStyle.entries, customization.dotMatrixStyle, { it.label }) { v ->
                    onUpdateCustomization { it.copy(dotMatrixStyle = v) }
                }

                Spacer(Modifier.height(6.dp))
                SwitchRow("Dynamic RGB Spectrum Accent", customization.rgbCycleEnabled) { checked ->
                    onUpdateCustomization { it.copy(rgbCycleEnabled = checked) }
                }
                SwitchRow("Custom RGB Accent Color", customization.customAccentEnabled) { checked ->
                    onUpdateCustomization { it.copy(customAccentEnabled = checked) }
                }

                if (customization.customAccentEnabled) {
                    RgbSlider("Red", customization.customAccentRed) { r ->
                        onUpdateCustomization { it.copy(customAccentRed = r) }
                    }
                    RgbSlider("Green", customization.customAccentGreen) { g ->
                        onUpdateCustomization { it.copy(customAccentGreen = g) }
                    }
                    RgbSlider("Blue", customization.customAccentBlue) { b ->
                        onUpdateCustomization { it.copy(customAccentBlue = b) }
                    }
                }
            }
        }

        // Custom Wallpaper (Android Photo Picker)
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Background Wallpaper",
                    subtitle = "ZERO-PERMISSION ANDROID PHOTO PICKER",
                )
                Spacer(Modifier.height(10.dp))
                if (customization.customWallpaperUri != null) {
                    AsyncImage(
                        model = customization.customWallpaperUri,
                        contentDescription = "Selected custom wallpaper preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LuxActionButton(
                        text = "Pick Wallpaper",
                        icon = Icons.Outlined.Image,
                        onClick = {
                            wallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (customization.customWallpaperUri != null) {
                        LuxActionButton(
                            text = "Clear",
                            icon = Icons.Outlined.Delete,
                            filled = false,
                            useRedAccent = true,
                            onClick = { onUpdateCustomization { it.copy(customWallpaperUri = null) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Surface, Icons, Typography, Indicators & Haptics
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Cards, Icons, Indicators & Motion")
                Spacer(Modifier.height(10.dp))

                ChipGroupSelector("Card Style", CardAppearance.entries, customization.cardAppearance, { it.label }) { v ->
                    onUpdateCustomization { it.copy(cardAppearance = v) }
                }
                ChipGroupSelector("Button Geometry", ButtonStyleOption.entries, customization.buttonStyle, { it.label }) { v ->
                    onUpdateCustomization { it.copy(buttonStyle = v) }
                }
                ChipGroupSelector("Font Scale", FontScaleOption.entries, customization.fontScale, { it.label }) { v ->
                    onUpdateCustomization { it.copy(fontScale = v) }
                }
                ChipGroupSelector("Icon Style", IconStyleOption.entries, customization.iconStyle, { it.label }) { v ->
                    onUpdateCustomization { it.copy(iconStyle = v) }
                }
                ChipGroupSelector("Battery Indicator", BatteryRingStyle.entries, customization.batteryRingStyle, { it.label }) { v ->
                    onUpdateCustomization { it.copy(batteryRingStyle = v) }
                }
                ChipGroupSelector("Connection Animation", ConnectionAnimStyle.entries, customization.connectionAnimStyle, { it.label }) { v ->
                    onUpdateCustomization { it.copy(connectionAnimStyle = v) }
                }
                ChipGroupSelector("Animation Speed", AnimationSpeedOption.entries, customization.animationSpeed, { it.label }) { v ->
                    onUpdateCustomization { it.copy(animationSpeed = v) }
                }
                Spacer(Modifier.height(4.dp))
                SwitchRow("Tactile Haptic Feedback", customization.hapticEnabled) { v ->
                    onUpdateCustomization { it.copy(hapticEnabled = v) }
                }
            }
        }

        // Home Layout & Widgets
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Home Layout & Widgets")
                Spacer(Modifier.height(8.dp))
                SwitchRow("Gaming Low-Latency Card", customization.showGamingCard) { v ->
                    onUpdateCustomization { it.copy(showGamingCard = v) }
                }
                SwitchRow("Subwoofer Engine X Card", customization.showSubwooferCard) { v ->
                    onUpdateCustomization { it.copy(showSubwooferCard = v) }
                }
                SwitchRow("Music Reactive Visualizer", customization.showMiniVisualizerWidget) { v ->
                    onUpdateCustomization { it.copy(showMiniVisualizerWidget = v) }
                }
                SwitchRow("Quick Sound & ANC Strip", customization.showQuickAncCard) { v ->
                    onUpdateCustomization { it.copy(showQuickAncCard = v) }
                }
                SwitchRow("Compatibility Engine Card", customization.showCompatibilityCard) { v ->
                    onUpdateCustomization { it.copy(showCompatibilityCard = v) }
                }
                SwitchRow("Firmware & Update Card", customization.showFirmwareCard) { v ->
                    onUpdateCustomization { it.copy(showFirmwareCard = v) }
                }
                SwitchRow("Bottom Navigation Labels", customization.showBottomNavLabels) { v ->
                    onUpdateCustomization { it.copy(showBottomNavLabels = v) }
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = extras.signalRed,
            ),
        )
    }
}

@Composable
private fun RgbSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    val extras = LocalLuxThemeExtras.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(56.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
            modifier = Modifier.weight(1f),
        )
        Text("$value", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(38.dp))
    }
}

@Composable
private fun <T> ChipGroupSelector(
    label: String,
    options: List<T>,
    selected: T,
    titleOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { item ->
                FilterChip(
                    selected = item == selected,
                    onClick = { onSelect(item) },
                    label = { Text(titleOf(item).uppercase(), style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                        selectedLabelColor = extras.signalRed,
                    ),
                )
            }
        }
    }
}
