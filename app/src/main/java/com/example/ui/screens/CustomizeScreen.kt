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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.model.FontScaleOption
import com.example.model.IconStyleOption
import com.example.ui.components.LuxActionButton
import com.example.ui.components.LuxGlassCard
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                title = "Luxury Studio Customizer",
                subtitle = "10 curated themes · RGB accents · Persistent DataStore layout",
                trailing = { Icon(Icons.Rounded.Palette, contentDescription = null, tint = extras.accent) },
            )
        }

        // Live Preview Card
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customization.themeId.displayName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = customization.themeId.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusBadgePill(label = customization.darkMode.label)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    LuxActionButton(text = "Primary Pill", onClick = {}, modifier = Modifier.weight(1f))
                    LuxActionButton(text = "Secondary", filled = false, onClick = {}, modifier = Modifier.weight(1f))
                }
            }
        }

        // 10 Premium Themes Selector
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "10 Luxury Themes", subtitle = "Tap any theme to preview & save immediately")
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(AppThemeId.entries, key = { it.name }) { theme ->
                        val selected = customization.themeId == theme
                        Column(
                            modifier = Modifier
                                .width(116.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(theme.darkBgHex), Color(theme.darkSurfaceHex)),
                                    ),
                                )
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Color(theme.accentHex) else Color(0xFF2D3646),
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .clickable {
                                    onUpdateCustomization { it.copy(themeId = theme) }
                                }
                                .padding(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(theme.primaryHex)),
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected theme",
                                        tint = Color(theme.accentHex),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                maxLines = 1,
                            )
                            Text(
                                text = theme.tagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB4BFCE),
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }

        // Dark / Light Mode & RGB Accent Studio
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Appearance & RGB Accent")
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DarkModePreference.entries.forEachIndexed { idx, pref ->
                        SegmentedButton(
                            selected = customization.darkMode == pref,
                            onClick = { onUpdateCustomization { it.copy(darkMode = pref) } },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = DarkModePreference.entries.size),
                        ) {
                            Text(pref.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                SwitchRow("Dynamic RGB Spectrum Cycle", customization.rgbCycleEnabled) { checked ->
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
                    subtitle = "Zero-permission Android Photo Picker",
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
                        icon = Icons.Rounded.Image,
                        onClick = {
                            wallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (customization.customWallpaperUri != null) {
                        LuxActionButton(
                            text = "Clear",
                            icon = Icons.Rounded.Delete,
                            filled = false,
                            onClick = { onUpdateCustomization { it.copy(customWallpaperUri = null) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Card, Button, Font, Icon, Animation, Battery Ring, Connection Anim
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "UI Surface, Typography & Motion")
                Spacer(Modifier.height(10.dp))

                ChipGroupSelector("Card Appearance", CardAppearance.entries, customization.cardAppearance, { it.label }) { v ->
                    onUpdateCustomization { it.copy(cardAppearance = v) }
                }
                ChipGroupSelector("Button Style", ButtonStyleOption.entries, customization.buttonStyle, { it.label }) { v ->
                    onUpdateCustomization { it.copy(buttonStyle = v) }
                }
                ChipGroupSelector("Font Size", FontScaleOption.entries, customization.fontScale, { it.label }) { v ->
                    onUpdateCustomization { it.copy(fontScale = v) }
                }
                ChipGroupSelector("Icon Style", IconStyleOption.entries, customization.iconStyle, { it.label }) { v ->
                    onUpdateCustomization { it.copy(iconStyle = v) }
                }
                ChipGroupSelector("Animation Speed", AnimationSpeedOption.entries, customization.animationSpeed, { it.label }) { v ->
                    onUpdateCustomization { it.copy(animationSpeed = v) }
                }
                ChipGroupSelector("Battery Design", BatteryRingStyle.entries, customization.batteryRingStyle, { it.label }) { v ->
                    onUpdateCustomization { it.copy(batteryRingStyle = v) }
                }
                ChipGroupSelector("Connection Animation", ConnectionAnimStyle.entries, customization.connectionAnimStyle, { it.label }) { v ->
                    onUpdateCustomization { it.copy(connectionAnimStyle = v) }
                }
            }
        }

        // Dashboard Cards & Navigation Layout
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Dashboard Cards, Widgets & Nav")
                Spacer(Modifier.height(8.dp))
                SwitchRow("Subwoofer Engine X Card", customization.showSubwooferCard) { v ->
                    onUpdateCustomization { it.copy(showSubwooferCard = v) }
                }
                SwitchRow("Mini Bass Visualizer Widget", customization.showMiniVisualizerWidget) { v ->
                    onUpdateCustomization { it.copy(showMiniVisualizerWidget = v) }
                }
                SwitchRow("Quick ANC & Sound Strip", customization.showQuickAncCard) { v ->
                    onUpdateCustomization { it.copy(showQuickAncCard = v) }
                }
                SwitchRow("Compatibility Tier Card", customization.showCompatibilityCard) { v ->
                    onUpdateCustomization { it.copy(showCompatibilityCard = v) }
                }
                SwitchRow("Firmware & Update Card", customization.showFirmwareCard) { v ->
                    onUpdateCustomization { it.copy(showFirmwareCard = v) }
                }
                SwitchRow("Show Bottom Navigation Labels", customization.showBottomNavLabels) { v ->
                    onUpdateCustomization { it.copy(showBottomNavLabels = v) }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RgbSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(56.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..255f,
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
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { item ->
                FilterChip(
                    selected = item == selected,
                    onClick = { onSelect(item) },
                    label = { Text(titleOf(item)) },
                )
            }
        }
    }
}
