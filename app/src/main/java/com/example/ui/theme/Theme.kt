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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AnimationSpeedOption
import com.example.model.BatteryRingStyle
import com.example.model.ButtonStyleOption
import com.example.model.CardAppearance
import com.example.model.ConnectionAnimStyle
import com.example.model.CustomizationSettings
import com.example.model.DarkModePreference
import com.example.model.IconStyleOption

@Immutable
data class LuxThemeExtras(
    val isDark: Boolean,
    val accent: Color,
    val secondaryAccent: Color,
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
)

val LocalLuxThemeExtras = staticCompositionLocalOf {
    LuxThemeExtras(
        isDark = true,
        accent = SilverChrome,
        secondaryAccent = SilverMuted,
        backgroundBrush = Brush.verticalGradient(listOf(TitaniumObsidian, TitaniumCharcoal)),
        cardGradientBrush = Brush.linearGradient(listOf(TitaniumCharcoal, TitaniumElevated)),
        cardBorderColor = TitaniumBorder,
        cardCornerRadius = 22.dp,
        buttonCornerRadius = 28.dp,
        cardAppearance = CardAppearance.GLASS,
        buttonStyle = ButtonStyleOption.PILL,
        iconStyle = IconStyleOption.FILLED,
        animationSpeed = AnimationSpeedOption.SMOOTH_1X,
        batteryRingStyle = BatteryRingStyle.ORBITAL,
        connectionAnimStyle = ConnectionAnimStyle.SONIC_PULSE,
    )
}

@Composable
fun HardikBudsProTheme(
    customization: CustomizationSettings = CustomizationSettings(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (customization.darkMode) {
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
        DarkModePreference.SYSTEM -> systemDark
    }

    val themeSpec = customization.themeId

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

    val rawAccent = when {
        rgbCycleHue != null -> Color.hsl(rgbCycleHue, 0.72f, if (useDark) 0.72f else 0.45f)
        customization.customAccentEnabled -> Color(
            red = customization.customAccentRed.coerceIn(0, 255) / 255f,
            green = customization.customAccentGreen.coerceIn(0, 255) / 255f,
            blue = customization.customAccentBlue.coerceIn(0, 255) / 255f,
        )
        else -> Color(themeSpec.accentHex)
    }

    val primaryColor by animateColorAsState(
        targetValue = if (customization.customAccentEnabled || rgbCycleHue != null) {
            rawAccent
        } else {
            if (useDark) Color(themeSpec.primaryHex) else darkenForLight(Color(themeSpec.primaryHex))
        },
        label = "primaryAnim",
    )

    val secondaryColor by animateColorAsState(
        targetValue = if (useDark) Color(themeSpec.secondaryHex) else darkenForLight(Color(themeSpec.secondaryHex)),
        label = "secondaryAnim",
    )

    val bgTop = if (useDark) Color(themeSpec.darkBgHex) else Color(themeSpec.lightBgHex)
    val surfaceBase = if (useDark) Color(themeSpec.darkSurfaceHex) else Color(themeSpec.lightSurfaceHex)
    val surfaceVariant = if (useDark) surfaceBase.copy(alpha = 0.88f) else Color(0xFFE7ECF3)
    val onBg = if (useDark) Color(0xFFF3F6FB) else Color(0xFF12161F)
    val onBgMuted = if (useDark) Color(0xFF9AA5B8) else Color(0xFF4F596B)

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color(0xFF0A0C10),
            primaryContainer = primaryColor.copy(alpha = 0.18f),
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
            onSecondary = Color(0xFF0A0C10),
            secondaryContainer = surfaceBase,
            onSecondaryContainer = onBg,
            tertiary = rawAccent,
            background = bgTop,
            onBackground = onBg,
            surface = surfaceBase,
            onSurface = onBg,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onBgMuted,
            outline = if (useDark) Color(0xFF2D3646) else Color(0xFFCBD3E0),
            error = StatusCritical,
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.14f),
            onPrimaryContainer = Color(0xFF12161F),
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE5EAF2),
            onSecondaryContainer = Color(0xFF12161F),
            tertiary = primaryColor,
            background = bgTop,
            onBackground = onBg,
            surface = surfaceBase,
            onSurface = onBg,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onBgMuted,
            outline = Color(0xFFD0D8E4),
            error = Color(0xFFD93848),
        )
    }

    val cardRadius = when (customization.cardAppearance) {
        CardAppearance.GLASS -> 24.dp
        CardAppearance.TITANIUM -> 18.dp
        CardAppearance.MINIMAL -> 14.dp
    }

    val buttonRadius = when (customization.buttonStyle) {
        ButtonStyleOption.PILL -> 28.dp
        ButtonStyleOption.SHARP -> 10.dp
        ButtonStyleOption.METALLIC -> 18.dp
    }

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            bgTop,
            surfaceBase.copy(alpha = if (useDark) 0.96f else 0.72f),
            bgTop,
        ),
    )

    val cardBrush = when (customization.cardAppearance) {
        CardAppearance.GLASS -> Brush.linearGradient(
            colors = listOf(
                surfaceBase.copy(alpha = if (useDark) 0.76f else 0.88f),
                surfaceBase.copy(alpha = if (useDark) 0.48f else 0.74f),
            ),
        )
        CardAppearance.TITANIUM -> Brush.linearGradient(
            colors = listOf(
                surfaceBase,
                if (useDark) surfaceBase.copy(alpha = 0.85f).compositeOverBlack(0.06f) else Color(0xFFF6F8FC),
            ),
        )
        CardAppearance.MINIMAL -> Brush.linearGradient(listOf(surfaceBase, surfaceBase))
    }

    val borderColor = when (customization.cardAppearance) {
        CardAppearance.GLASS -> primaryColor.copy(alpha = if (useDark) 0.24f else 0.30f)
        CardAppearance.TITANIUM -> if (useDark) Color(0xFF333C4D) else Color(0xFFCBD4E1)
        CardAppearance.MINIMAL -> if (useDark) Color(0xFF222834) else Color(0xFFE0E6EF)
    }

    val extras = LuxThemeExtras(
        isDark = useDark,
        accent = primaryColor,
        secondaryAccent = secondaryColor,
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
    )

    val shapes = Shapes(
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(cardRadius),
        large = RoundedCornerShape(28.dp),
    )

    CompositionLocalProvider(LocalLuxThemeExtras provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildLuxuryTypography(customization.fontScale.multiplier),
            shapes = shapes,
            content = content,
        )
    }
}

private fun darkenForLight(color: Color): Color {
    // Ensure high contrast on light silver backgrounds
    return Color(
        red = (color.red * 0.52f).coerceIn(0.08f, 0.72f),
        green = (color.green * 0.52f).coerceIn(0.08f, 0.72f),
        blue = (color.blue * 0.52f).coerceIn(0.08f, 0.72f),
        alpha = 1f,
    )
}

private fun Color.compositeOverBlack(amount: Float): Color = Color(
    red = (red * (1f - amount)).coerceIn(0f, 1f),
    green = (green * (1f - amount)).coerceIn(0f, 1f),
    blue = (blue * (1f - amount)).coerceIn(0f, 1f),
    alpha = alpha,
)
