package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AnimationSpeedOption
import com.example.model.BatteryRingStyle
import com.example.model.ButtonStyleOption
import com.example.model.CardAppearance
import com.example.model.ConnectionAnimStyle
import com.example.model.CustomizationSettings
import com.example.model.DarkModePreference
import com.example.model.DotMatrixStyle
import com.example.model.IconStyleOption

@Immutable
data class LuxThemeExtras(
    val isDark: Boolean,
    val accent: Color,
    val signalRed: Color,
    val secondaryAccent: Color,
    val dotMatrixFont: FontFamily,
    val dotMatrixStyle: DotMatrixStyle,
    val backgroundColor: Color,
    val backgroundBrush: Brush,
    val cardGradientBrush: Brush,
    val cardBorderColor: Color,
    val cardCornerRadius: Dp,
    val buttonCornerRadius: Dp,
    val cardAppearance: CardAppearance,
    val buttonStyle: ButtonStyleOption,
    val iconStyle: IconStyleOption,
    val animationSpeed: AnimationSpeedOption,
    val batteryRingStyle: BatteryRingStyle,
    val connectionAnimStyle: ConnectionAnimStyle,
    val hapticEnabled: Boolean,
)

val LocalLuxThemeExtras = staticCompositionLocalOf {
    LuxThemeExtras(
        isDark = true,
        accent = SignalRed,
        signalRed = SignalRed,
        secondaryAccent = NothingLightGray,
        dotMatrixFont = DotoMatrixFamily,
        dotMatrixStyle = DotMatrixStyle.DOTO,
        backgroundColor = AmoledBlack,
        backgroundBrush = Brush.verticalGradient(listOf(AmoledBlack, AmoledBlack)),
        cardGradientBrush = Brush.linearGradient(listOf(NothingSurface, NothingSurface)),
        cardBorderColor = NothingHairlineDark,
        cardCornerRadius = 22.dp,
        buttonCornerRadius = 28.dp,
        cardAppearance = CardAppearance.GLASS,
        buttonStyle = ButtonStyleOption.PILL,
        iconStyle = IconStyleOption.OUTLINED,
        animationSpeed = AnimationSpeedOption.SMOOTH_1X,
        batteryRingStyle = BatteryRingStyle.ORBITAL,
        connectionAnimStyle = ConnectionAnimStyle.SONIC_PULSE,
        hapticEnabled = true,
    )
}

@Composable
fun HardikBudsProTheme(
    customization: CustomizationSettings = CustomizationSettings(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val themeSpec = customization.themeId

    val useDark = when (customization.darkMode) {
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
        DarkModePreference.SYSTEM -> if (themeSpec.prefersLightMode) false else systemDark
    }

    val rgbCycleHue = if (customization.rgbCycleEnabled &&
        customization.animationSpeed != AnimationSpeedOption.REDUCED
    ) {
        val infinite = rememberInfiniteTransition(label = "rgbCycle")
        val hue by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 9000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "hue",
        )
        hue
    } else null

    val resolvedAccentTarget = when {
        rgbCycleHue != null -> Color.hsl(rgbCycleHue, 0.80f, if (useDark) 0.62f else 0.48f)
        customization.customAccentEnabled -> Color(
            red = customization.customAccentRed.coerceIn(0, 255) / 255f,
            green = customization.customAccentGreen.coerceIn(0, 255) / 255f,
            blue = customization.customAccentBlue.coerceIn(0, 255) / 255f,
        )
        else -> Color(themeSpec.accentHex)
    }

    val accentColor by animateColorAsState(targetValue = resolvedAccentTarget, label = "accentAnim")
    val secondaryColor by animateColorAsState(
        targetValue = if (useDark) Color(themeSpec.secondaryHex) else Color(0xFF52525B),
        label = "secondaryAnim",
    )

    val bgColor = if (useDark) Color(themeSpec.darkBgHex) else Color(themeSpec.lightBgHex)
    val surfaceColor = if (useDark) Color(themeSpec.darkSurfaceHex) else Color(themeSpec.lightSurfaceHex)
    val surfaceVariant = if (useDark) Color(0xFF141418) else Color(0xFFE4E4E7)
    val onBg = if (useDark) Color(0xFFFFFFFF) else Color(0xFF09090B)
    val onBgMuted = if (useDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    val outlineHairline = if (useDark) Color(0xFF26262C) else Color(0xFFD4D4D8)

    val primarySchemeColor = if (useDark) Color(themeSpec.primaryHex) else Color(0xFF111113)

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.16f),
            onPrimaryContainer = primarySchemeColor,
            secondary = secondaryColor,
            onSecondary = Color.Black,
            secondaryContainer = surfaceColor,
            onSecondaryContainer = onBg,
            tertiary = accentColor,
            background = bgColor,
            onBackground = onBg,
            surface = surfaceColor,
            onSurface = onBg,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onBgMuted,
            outline = outlineHairline,
            error = SignalRed,
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.12f),
            onPrimaryContainer = Color(0xFF09090B),
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE4E4E7),
            onSecondaryContainer = Color(0xFF09090B),
            tertiary = accentColor,
            background = bgColor,
            onBackground = onBg,
            surface = surfaceColor,
            onSurface = onBg,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onBgMuted,
            outline = outlineHairline,
            error = SignalRed,
        )
    }

    val cardRadius = when (customization.cardAppearance) {
        CardAppearance.GLASS -> 22.dp
        CardAppearance.TITANIUM -> 16.dp
        CardAppearance.MINIMAL -> 12.dp
    }

    val buttonRadius = when (customization.buttonStyle) {
        ButtonStyleOption.PILL -> 28.dp
        ButtonStyleOption.SHARP -> 8.dp
        ButtonStyleOption.METALLIC -> 18.dp
    }

    // Minimalist Nothing aesthetic: pure AMOLED flat or ultra-subtle translucent glass panels, no gaudy gradients
    val bgBrush = Brush.verticalGradient(listOf(bgColor, bgColor))
    val cardBrush = when (customization.cardAppearance) {
        CardAppearance.GLASS -> Brush.linearGradient(
            listOf(
                surfaceColor.copy(alpha = if (useDark) 0.78f else 0.92f),
                surfaceColor.copy(alpha = if (useDark) 0.62f else 0.84f),
            ),
        )
        CardAppearance.TITANIUM -> Brush.linearGradient(listOf(surfaceColor, surfaceColor))
        CardAppearance.MINIMAL -> Brush.linearGradient(listOf(bgColor, bgColor))
    }

    val borderColor = when (customization.cardAppearance) {
        CardAppearance.GLASS -> if (useDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f)
        CardAppearance.TITANIUM -> outlineHairline
        CardAppearance.MINIMAL -> outlineHairline.copy(alpha = 0.7f)
    }

    val extras = LuxThemeExtras(
        isDark = useDark,
        accent = accentColor,
        signalRed = accentColor,
        secondaryAccent = secondaryColor,
        dotMatrixFont = dotMatrixFamilyFor(customization.dotMatrixStyle),
        dotMatrixStyle = customization.dotMatrixStyle,
        backgroundColor = bgColor,
        backgroundBrush = bgBrush,
        cardGradientBrush = cardBrush,
        cardBorderColor = borderColor,
        cardCornerRadius = cardRadius,
        buttonCornerRadius = buttonRadius,
        cardAppearance = customization.cardAppearance,
        buttonStyle = customization.buttonStyle,
        iconStyle = customization.iconStyle,
        animationSpeed = customization.animationSpeed,
        batteryRingStyle = customization.batteryRingStyle,
        connectionAnimStyle = customization.connectionAnimStyle,
        hapticEnabled = customization.hapticEnabled,
    )

    val shapes = Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(cardRadius),
        large = RoundedCornerShape(28.dp),
    )

    CompositionLocalProvider(LocalLuxThemeExtras provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildNothingTypography(
                scale = customization.fontScale.multiplier,
                dotStyle = customization.dotMatrixStyle,
            ),
            shapes = shapes,
            content = content,
        )
    }
}
