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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.model.AnimationSpeedOption
import com.example.model.BatteryRingStyle
import com.example.model.ButtonStyleOption
import com.example.model.ConnectionAnimStyle
import com.example.ui.theme.LocalLuxThemeExtras
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusWarning
import kotlin.math.sin

@Composable
fun LuxGlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(extras.cardCornerRadius)

    val clickMod = if (onClick != null) {
        Modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(extras.cardGradientBrush)
            .border(width = 1.dp, color = extras.cardBorderColor, shape = shape)
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
    enabled: Boolean = true,
) {
    val extras = LocalLuxThemeExtras.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(extras.buttonCornerRadius)
    val useOutline = !filled || extras.buttonStyle == ButtonStyleOption.METALLIC

    if (useOutline) {
        OutlinedButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            enabled = enabled,
            shape = shape,
            modifier = modifier.minimumInteractiveComponentSize().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = extras.accent,
            ),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            enabled = enabled,
            shape = shape,
            modifier = modifier.minimumInteractiveComponentSize().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = extras.accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun StatusBadgePill(
    label: String,
    color: Color = LocalLuxThemeExtras.current.accent,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
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
                text = title,
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
fun Earbuds3DShowcase(
    isConnected: Boolean,
    bassEnergy: Float,
    modifier: Modifier = Modifier,
) {
    val extras = LocalLuxThemeExtras.current
    val speedFactor = extras.animationSpeed.factor

    val floatOffset: Float
    val tiltY: Float
    val pulseProgress: Float

    if (speedFactor > 0f) {
        val infinite = rememberInfiniteTransition(label = "earbuds3d")
        val f by infinite.animateFloat(
            initialValue = -7f,
            targetValue = 7f,
            animationSpec = infiniteRepeatable(
                animation = tween((2600 * speedFactor).toInt().coerceAtLeast(800), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "floatY",
        )
        val r by infinite.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween((3600 * speedFactor).toInt().coerceAtLeast(1000), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "rotY",
        )
        val p by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween((2000 * speedFactor).toInt().coerceAtLeast(600), easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "pulse",
        )
        floatOffset = f
        tiltY = r
        pulseProgress = p
    } else {
        floatOffset = 0f
        tiltY = 0f
        pulseProgress = 0.35f
    }

    val animatedBass by animateFloatAsState(
        targetValue = bassEnergy.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 320f),
        label = "bassGlow",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Connection & Bass Reactive Orbital Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.36f
            val accent = if (isConnected) extras.accent else extras.secondaryAccent.copy(alpha = 0.45f)

            when (extras.connectionAnimStyle) {
                ConnectionAnimStyle.SONIC_PULSE -> {
                    val r1 = baseRadius + (pulseProgress * 46f) + (animatedBass * 18f)
                    drawCircle(
                        color = accent.copy(alpha = (1f - pulseProgress) * if (isConnected) 0.42f else 0.14f),
                        radius = r1,
                        center = center,
                        style = Stroke(width = 2.5f),
                    )
                    drawCircle(
                        color = accent.copy(alpha = 0.18f + animatedBass * 0.25f),
                        radius = baseRadius + animatedBass * 12f,
                        center = center,
                        style = Stroke(width = 1.5f),
                    )
                }
                ConnectionAnimStyle.ORBIT_GLOW -> {
                    drawArc(
                        color = accent.copy(alpha = if (isConnected) 0.65f else 0.2f),
                        startAngle = pulseProgress * 360f,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                        size = Size(baseRadius * 2, baseRadius * 2),
                        style = Stroke(width = 3f, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = accent.copy(alpha = 0.28f),
                        startAngle = pulseProgress * -360f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(center.x - baseRadius * 1.18f, center.y - baseRadius * 1.18f),
                        size = Size(baseRadius * 2.36f, baseRadius * 2.36f),
                        style = Stroke(width = 1.8f, cap = StrokeCap.Round),
                    )
                }
                ConnectionAnimStyle.MINIMAL -> {
                    drawCircle(
                        color = accent.copy(alpha = 0.22f),
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 1.5f),
                    )
                }
            }
        }

        // Floating 3D Studio Earbuds Visual
        Box(
            modifier = Modifier
                .size(168.dp)
                .graphicsLayer {
                    translationY = floatOffset
                    rotationY = tiltY
                    rotationX = -tiltY * 0.4f
                    cameraDistance = 12f * density
                    val scale = 1f + animatedBass * 0.04f
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            extras.accent.copy(alpha = 0.7f),
                            Color.Transparent,
                            extras.secondaryAccent.copy(alpha = 0.5f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_earbuds_hero),
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

    val ringColor = when {
        percent == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        percent <= 15 -> StatusCritical
        percent <= 30 -> StatusWarning
        isChargingOrActive -> StatusConnected
        else -> extras.accent
    }

    val desc = if (percent != null) "$label battery $percent percent" else "$label battery unavailable"

    Column(
        modifier = modifier.semantics { contentDescription = desc },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (extras.batteryRingStyle) {
            BatteryRingStyle.ORBITAL, BatteryRingStyle.ARC -> {
                val startDeg = if (extras.batteryRingStyle == BatteryRingStyle.ORBITAL) -90f else 135f
                val maxSweep = if (extras.batteryRingStyle == BatteryRingStyle.ORBITAL) 360f else 270f
                Box(
                    modifier = Modifier.size(size),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 7.dp.toPx()
                        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                        val topLeft = Offset(stroke / 2f, stroke / 2f)
                        drawArc(
                            color = ringColor.copy(alpha = 0.16f),
                            startAngle = startDeg,
                            sweepAngle = maxSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                        if (percent != null) {
                            drawArc(
                                color = ringColor,
                                startAngle = startDeg,
                                sweepAngle = maxSweep * animatedFraction,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = percent?.let { "$it%" } ?: "--%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (percent != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (percent != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ringColor.copy(alpha = 0.18f)),
                    ) {
                        if (percent != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedFraction)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(ringColor),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = if (percent != null) "Live HW" else "Unavailable",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun MusicReactiveBassVisualizer(
    energy: Float,
    bassIntensity: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val extras = LocalLuxThemeExtras.current
    val animEnergy by animateFloatAsState(
        targetValue = if (enabled) energy.coerceIn(0.05f, 1f) else 0.06f,
        animationSpec = spring(stiffness = 400f),
        label = "vizEnergy",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        val bars = 28
        val barWidth = size.width / (bars * 1.7f)
        val spacing = (size.width - bars * barWidth) / (bars - 1).coerceAtLeast(1)
        val intensityFactor = (bassIntensity / 100f).coerceIn(0.15f, 1f)

        for (i in 0 until bars) {
            // Shape spectrum with stronger sub-bass energy on the left
            val subWeight = 1f - (i.toFloat() / bars) * 0.55f
            val wave = 0.45f + 0.55f * kotlin.math.abs(sin((i * 0.65f) + animEnergy * 8f))
            val barH = (size.height * animEnergy * intensityFactor * subWeight * wave)
                .coerceIn(6.dp.toPx(), size.height)
            val x = i * (barWidth + spacing)
            val y = (size.height - barH) / 2f
            drawRoundRect(
                color = extras.accent.copy(alpha = if (enabled) 0.35f + 0.6f * subWeight else 0.22f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
