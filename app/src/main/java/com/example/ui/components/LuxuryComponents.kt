package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BatteryRingStyle
import com.example.model.ButtonStyleOption
import com.example.model.ConnectionAnimStyle
import com.example.ui.theme.LocalLuxThemeExtras
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LuxGlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    highlightRedBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(extras.cardCornerRadius)
    val borderCol = if (highlightRedBorder) extras.signalRed.copy(alpha = 0.72f) else extras.cardBorderColor

    val clickMod = if (onClick != null) {
        Modifier.clickable {
            if (extras.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(extras.cardGradientBrush)
            .border(width = 1.dp, color = borderCol, shape = shape)
            .then(clickMod),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun LuxActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = true,
    useRedAccent: Boolean = false,
    enabled: Boolean = true,
) {
    val extras = LocalLuxThemeExtras.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(extras.buttonCornerRadius)
    val useOutline = !filled || extras.buttonStyle == ButtonStyleOption.METALLIC

    if (useOutline) {
        OutlinedButton(
            onClick = {
                if (extras.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            enabled = enabled,
            shape = shape,
            border = BorderStroke(
                1.dp,
                if (useRedAccent) extras.signalRed else MaterialTheme.colorScheme.outline,
            ),
            modifier = modifier.minimumInteractiveComponentSize().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (useRedAccent) extras.signalRed else MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        val bg = if (useRedAccent) extras.signalRed else MaterialTheme.colorScheme.onBackground
        val fg = if (useRedAccent) Color.White else MaterialTheme.colorScheme.background
        Button(
            onClick = {
                if (extras.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            enabled = enabled,
            shape = shape,
            modifier = modifier.minimumInteractiveComponentSize().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = bg,
                contentColor = fg,
            ),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun RedSignalDot(
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
) {
    val extras = LocalLuxThemeExtras.current
    val dotColor = if (active) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(dotColor),
    )
}

@Composable
fun StatusBadgePill(
    label: String,
    isRedActive: Boolean = false,
    color: Color? = null,
    modifier: Modifier = Modifier,
) {
    val extras = LocalLuxThemeExtras.current
    val resolved = color ?: if (isRedActive) extras.signalRed else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier,
        color = if (isRedActive) extras.signalRed.copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            1.dp,
            if (isRedActive) extras.signalRed.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isRedActive) {
                RedSignalDot(active = true, size = 6.dp)
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = resolved,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun DotMatrixLogoMark(
    modifier: Modifier = Modifier,
    dotSize: Dp = 6.dp,
    animatePulse: Boolean = true,
) {
    val extras = LocalLuxThemeExtras.current
    val speedFactor = extras.animationSpeed.factor
    val pulse = if (animatePulse && speedFactor > 0f) {
        val infinite = rememberInfiniteTransition(label = "dotLogo")
        val p by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween((1400 * speedFactor).toInt().coerceAtLeast(500), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dotAlpha",
        )
        p
    } else 0.75f

    // 7x5 geometric Nothing-style earbuds/glyph dot pattern
    val grid = listOf(
        listOf(0, 1, 1, 0, 1, 1, 0),
        listOf(1, 0, 0, 1, 0, 0, 1),
        listOf(1, 0, 2, 0, 2, 0, 1),
        listOf(1, 0, 0, 0, 0, 0, 1),
        listOf(0, 1, 0, 2, 0, 1, 0),
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        grid.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { cell ->
                    val col = when (cell) {
                        2 -> extras.signalRed.copy(alpha = 0.55f + 0.45f * pulse)
                        1 -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                    }
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(col),
                    )
                }
            }
        }
    }
}

@Composable
fun Earbuds3DShowcase(
    isConnected: Boolean,
    bassEnergy: Float,
    modifier: Modifier = Modifier,
) {
    val extras = LocalLuxThemeExtras.current
    val speedFactor = extras.animationSpeed.factor
    val neutralRing = MaterialTheme.colorScheme.onBackground

    val floatOffset: Float
    val pulseProgress: Float

    if (speedFactor > 0f) {
        val infinite = rememberInfiniteTransition(label = "nothingDeviceDisplay")
        val f by infinite.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween((2600 * speedFactor).toInt().coerceAtLeast(900), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "floatY",
        )
        val p by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween((2200 * speedFactor).toInt().coerceAtLeast(800), easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "pulse",
        )
        floatOffset = f
        pulseProgress = p
    } else {
        floatOffset = 0f
        pulseProgress = 0.35f
    }

    val animatedBass by animateFloatAsState(
        targetValue = bassEnergy.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 320f),
        label = "bassScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(216.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.40f
            val dotStroke = PathEffect.dashPathEffect(floatArrayOf(4f, 10f), phase = 0f)

            // Outer dotted Nothing OS telemetry ring
            drawCircle(
                color = neutralRing.copy(alpha = 0.25f),
                radius = baseRadius * 1.12f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = dotStroke),
            )

            when (extras.connectionAnimStyle) {
                ConnectionAnimStyle.SONIC_PULSE -> {
                    if (isConnected) {
                        drawCircle(
                            color = extras.signalRed.copy(alpha = (1f - pulseProgress) * 0.55f),
                            radius = baseRadius + (pulseProgress * 26.dp.toPx()) + (animatedBass * 10.dp.toPx()),
                            center = center,
                            style = Stroke(width = 1.6.dp.toPx()),
                        )
                    }
                    drawCircle(
                        color = if (isConnected) extras.signalRed.copy(alpha = 0.7f) else neutralRing.copy(alpha = 0.28f),
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx()),
                    )
                }
                ConnectionAnimStyle.ORBIT_GLOW -> {
                    drawArc(
                        color = if (isConnected) extras.signalRed else neutralRing.copy(alpha = 0.4f),
                        startAngle = pulseProgress * 360f,
                        sweepAngle = 72f,
                        useCenter = false,
                        topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                        size = Size(baseRadius * 2, baseRadius * 2),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                ConnectionAnimStyle.MINIMAL -> {
                    drawCircle(
                        color = if (isConnected) extras.signalRed.copy(alpha = 0.65f) else neutralRing.copy(alpha = 0.25f),
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(164.dp)
                .graphicsLayer {
                    translationY = floatOffset
                    val scale = 1f + animatedBass * 0.03f
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (isConnected) extras.signalRed.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_nothing_buds),
                contentDescription = stringResource(R.string.cd_earbuds_hero),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun AnimatedBatteryIndicator(
    label: String,
    percent: Int?,
    isChargingOrActive: Boolean = false,
    lowThreshold: Int = 20,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
) {
    val extras = LocalLuxThemeExtras.current
    val targetFraction = (percent?.coerceIn(0, 100) ?: 0) / 100f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(stiffness = 220f),
        label = "batteryFill",
    )

    val isLow = percent != null && percent <= lowThreshold
    val activeRingColor = when {
        percent == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
        isLow -> extras.signalRed
        isChargingOrActive -> extras.signalRed
        else -> MaterialTheme.colorScheme.onBackground
    }
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val desc = if (percent != null) "$label battery $percent percent" else "$label battery unavailable"

    Column(
        modifier = modifier.semantics { contentDescription = desc },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (extras.batteryRingStyle) {
            BatteryRingStyle.ORBITAL, BatteryRingStyle.ARC -> {
                val startDeg = if (extras.batteryRingStyle == BatteryRingStyle.ORBITAL) -90f else 135f
                val maxSweep = if (extras.batteryRingStyle == BatteryRingStyle.ORBITAL) 360f else 270f
                val dotEffect = if (extras.batteryRingStyle == BatteryRingStyle.ARC) {
                    PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                } else null

                Box(
                    modifier = Modifier.size(size),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 5.5.dp.toPx()
                        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                        val topLeft = Offset(stroke / 2f, stroke / 2f)
                        drawArc(
                            color = trackColor,
                            startAngle = startDeg,
                            sweepAngle = maxSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round, pathEffect = dotEffect),
                        )
                        if (percent != null) {
                            drawArc(
                                color = activeRingColor,
                                startAngle = startDeg,
                                sweepAngle = maxSweep * animatedFraction,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round, pathEffect = dotEffect),
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = percent?.let { "$it%" } ?: "--%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = extras.dotMatrixFont,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (isLow) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            BatteryRingStyle.BAR -> {
                Column(
                    modifier = Modifier.width(size),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = percent?.let { "$it%" } ?: "--%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = extras.dotMatrixFont,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (isLow) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(trackColor),
                    ) {
                        if (percent != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedFraction)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(activeRingColor),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = when {
                isLow -> "LOW ALERT"
                percent != null && isChargingOrActive -> "ACTIVE HW"
                percent != null -> "HW DATA"
                else -> "N/A"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (isLow) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun MusicReactiveBassVisualizer(
    energy: Float,
    bassIntensity: Int,
    enabled: Boolean,
    useRedFrequencyBars: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val extras = LocalLuxThemeExtras.current
    val animEnergy by animateFloatAsState(
        targetValue = if (enabled) energy.coerceIn(0.05f, 1f) else 0.06f,
        animationSpec = spring(stiffness = 400f),
        label = "vizEnergy",
    )
    val baseBarColor = if (useRedFrequencyBars) extras.signalRed else MaterialTheme.colorScheme.onBackground
    val neutralDot = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        val bars = 30
        val barWidth = size.width / (bars * 1.9f)
        val spacing = (size.width - bars * barWidth) / (bars - 1).coerceAtLeast(1)
        val intensityFactor = (bassIntensity / 100f).coerceIn(0.15f, 1f)

        for (i in 0 until bars) {
            val subWeight = 1f - (i.toFloat() / bars) * 0.55f
            val wave = 0.40f + 0.60f * abs(sin((i * 0.65f) + animEnergy * 8f))
            val barH = (size.height * animEnergy * intensityFactor * subWeight * wave)
                .coerceIn(4.dp.toPx(), size.height)
            val x = i * (barWidth + spacing)
            val y = (size.height - barH) / 2f
            val barColor = if (enabled && (i < 12 || subWeight > 0.72f)) {
                baseBarColor.copy(alpha = 0.45f + 0.55f * subWeight)
            } else if (enabled) {
                neutralDot.copy(alpha = 0.65f)
            } else {
                neutralDot
            }
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
fun BluetoothRadarScanner(
    isScanning: Boolean,
    deviceCount: Int,
    modifier: Modifier = Modifier,
) {
    val extras = LocalLuxThemeExtras.current
    val speedFactor = extras.animationSpeed.factor
    val sweepAngle = if (isScanning && speedFactor > 0f) {
        val infinite = rememberInfiniteTransition(label = "btRadar")
        val angle by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween((2000 * speedFactor).toInt().coerceAtLeast(700), easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "radarAngle",
        )
        angle
    } else 45f

    val ringCol = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.44f
            listOf(0.35f, 0.68f, 1.0f).forEach { factor ->
                drawCircle(
                    color = ringCol.copy(alpha = 0.55f),
                    radius = maxR * factor,
                    center = c,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            if (isScanning) {
                val rad = Math.toRadians(sweepAngle.toDouble())
                val end = Offset(
                    x = c.x + (maxR * cos(rad)).toFloat(),
                    y = c.y + (maxR * sin(rad)).toFloat(),
                )
                drawLine(
                    color = extras.signalRed,
                    start = c,
                    end = end,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawArc(
                    color = extras.signalRed.copy(alpha = 0.16f),
                    startAngle = sweepAngle - 45f,
                    sweepAngle = 45f,
                    useCenter = true,
                    topLeft = Offset(c.x - maxR, c.y - maxR),
                    size = Size(maxR * 2, maxR * 2),
                )
            }
            drawCircle(
                color = if (isScanning) extras.signalRed else ringCol,
                radius = 5.dp.toPx(),
                center = c,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (isScanning) "SCANNING BLE / CLASSIC · $deviceCount FOUND" else "RADAR STANDBY · $deviceCount NEARBY",
                style = MaterialTheme.typography.labelSmall,
                color = if (isScanning) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
