package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AnimationSpeedOption
import com.example.ui.components.DotMatrixLogoMark
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalLuxThemeExtras
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    val alphaAnim = remember { Animatable(0f) }
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (extras.animationSpeed == AnimationSpeedOption.REDUCED) {
            alphaAnim.snapTo(1f)
            progressAnim.snapTo(1f)
            delay(150L)
        } else {
            alphaAnim.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
            progressAnim.animateTo(1f, animationSpec = tween(750, easing = FastOutSlowInEasing))
            delay(120L)
        }
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progressAnim.value },
                    modifier = Modifier.size(118.dp),
                    color = extras.signalRed,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    strokeWidth = 2.dp,
                )
                DotMatrixLogoMark(dotSize = 6.dp, animatePulse = true)
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "HARDIK BUDS PRO",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )

            Spacer(Modifier.height(8.dp))

            StatusBadgePill(
                label = "ULTRA PREMIUM",
                isRedActive = true,
            )

            Spacer(Modifier.height(26.dp))

            LinearProgressIndicator(
                progress = { progressAnim.value },
                modifier = Modifier
                    .width(160.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50)),
                color = extras.signalRed,
                trackColor = Color.White.copy(alpha = 0.14f),
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "INITIALIZING AUDIO OS...",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA1A1AA),
            )
        }

        Text(
            text = "© 2026 Hardik Kumar. All Rights Reserved.",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF71717A),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
