package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DotMatrixStyle

val DotoMatrixFamily = FontFamily(
    Font(R.font.doto, FontWeight.Normal),
    Font(R.font.doto, FontWeight.Medium),
    Font(R.font.doto, FontWeight.SemiBold),
    Font(R.font.doto, FontWeight.Bold),
)

val DotGothicFamily = FontFamily(
    Font(R.font.dotgothic16, FontWeight.Normal),
)

val OutfitDisplayFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold),
)

val JakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
)

fun dotMatrixFamilyFor(style: DotMatrixStyle): FontFamily = when (style) {
    DotMatrixStyle.DOTO -> DotoMatrixFamily
    DotMatrixStyle.DOT_GOTHIC -> DotGothicFamily
    DotMatrixStyle.CLEAN_MONO -> OutfitDisplayFamily
}

fun buildNothingTypography(
    scale: Float = 1.0f,
    dotStyle: DotMatrixStyle = DotMatrixStyle.DOTO,
): Typography {
    val matrixFont = dotMatrixFamilyFor(dotStyle)
    return Typography(
        displayLarge = TextStyle(
            fontFamily = matrixFont,
            fontWeight = FontWeight.Bold,
            fontSize = (40f * scale).sp,
            lineHeight = (46f * scale).sp,
            letterSpacing = 0.5.sp,
        ),
        displayMedium = TextStyle(
            fontFamily = matrixFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = (30f * scale).sp,
            lineHeight = (36f * scale).sp,
            letterSpacing = 0.4.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = matrixFont,
            fontWeight = FontWeight.Bold,
            fontSize = (23f * scale).sp,
            lineHeight = (29f * scale).sp,
            letterSpacing = 0.8.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = matrixFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = (19f * scale).sp,
            lineHeight = (25f * scale).sp,
            letterSpacing = 0.6.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = JakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15.5f * scale).sp,
            lineHeight = (21f * scale).sp,
            letterSpacing = 0.15.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = JakartaSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (13.5f * scale).sp,
            lineHeight = (19f * scale).sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = JakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * scale).sp,
            lineHeight = (22f * scale).sp,
            letterSpacing = 0.15.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = JakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5f * scale).sp,
            lineHeight = (19f * scale).sp,
            letterSpacing = 0.15.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = JakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12f * scale).sp,
            lineHeight = (16.5f * scale).sp,
            letterSpacing = 0.2.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (12.5f * scale).sp,
            lineHeight = (17f * scale).sp,
            letterSpacing = 0.8.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11.5f * scale).sp,
            lineHeight = (15f * scale).sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (10.5f * scale).sp,
            lineHeight = (14f * scale).sp,
            letterSpacing = 0.5.sp,
        ),
    )
}

val Typography = buildNothingTypography(1.0f, DotMatrixStyle.DOTO)
