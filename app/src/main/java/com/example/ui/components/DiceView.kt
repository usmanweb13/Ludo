package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LudoAccent
import com.example.ui.theme.LudoAccentDim
import com.example.ui.theme.LudoPanel
import com.example.ui.theme.LudoPanelLight
import kotlinx.coroutines.delay

@Composable
fun DiceView(
    diceValue: Long,
    isRolling: Boolean,
    enabled: Boolean,
    onRollClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val rotationZ = remember { Animatable(0f) }
    val rotationX = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    var displayedValue by remember { mutableIntStateOf(diceValue.toInt().coerceIn(1, 6)) }

    // Continuous glowing pulse for enabled turn
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    // Rotating ring around the dice tray
    val ringAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAngle"
    )

    // Fast tumbling numbers during roll
    LaunchedEffect(isRolling) {
        if (isRolling) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // Animate spin & 3D tilt
            rotationZ.animateTo(
                targetValue = rotationZ.value + 720f,
                animationSpec = tween(400)
            )
        }
    }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            while (isRolling) {
                displayedValue = (1..6).random()
                delay(60)
            }
        } else if (diceValue in 1..6) {
            displayedValue = diceValue.toInt()
        }
    }

    LaunchedEffect(diceValue) {
        if (diceValue > 0) {
            displayedValue = diceValue.toInt()
            // Bouncy landing
            scale.snapTo(1.35f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 350
                    1.35f at 0
                    0.90f at 180
                    1.08f at 260
                    1.0f at 350
                }
            )
        }
    }

    // Outer Dice Tray / Slot
    Box(
        modifier = modifier
            .size(76.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2C2216),
                        Color(0xFF151009)
                    )
                )
            )
            .border(
                width = if (enabled) 2.5.dp else 1.dp,
                brush = if (enabled) {
                    Brush.sweepGradient(
                        colors = listOf(
                            LudoAccent.copy(alpha = pulseGlow),
                            Color(0xFFFFDF9E),
                            LudoAccentDim,
                            LudoAccent.copy(alpha = pulseGlow)
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(LudoAccentDim.copy(alpha = 0.4f), Color(0xFF1E160E)))
                },
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // 3D Dice Cube
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(scale.value)
                .rotate(rotationZ.value)
                .graphicsLayer {
                    this.rotationX = rotationX.value
                    cameraDistance = 12f * density
                }
                .shadow(
                    elevation = if (enabled) 10.dp else 2.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = if (diceValue == 6L) Color(0xFFFFD700) else Color.Black,
                    spotColor = if (diceValue == 6L) Color(0xFFFFD700) else Color.Black
                )
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = if (enabled) {
                            if (diceValue == 6L && !isRolling) {
                                listOf(Color(0xFF4A3816), Color(0xFF2B1F0A))
                            } else {
                                listOf(Color(0xFF382C1E), Color(0xFF21180E))
                            }
                        } else {
                            listOf(Color(0xFF261D14), Color(0xFF19120B))
                        }
                    )
                )
                .border(
                    width = if (diceValue == 6L && !isRolling) 2.dp else if (enabled) 1.5.dp else 1.dp,
                    brush = if (diceValue == 6L && !isRolling) {
                        Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300)))
                    } else if (enabled) {
                        Brush.linearGradient(listOf(Color(0xFFFFDF9E), LudoAccentDim))
                    } else {
                        Brush.linearGradient(listOf(LudoAccentDim.copy(alpha = 0.3f), Color.Transparent))
                    },
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable(
                    enabled = enabled,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRollClicked()
                    }
                )
                .testTag("dice_button"),
            contentAlignment = Alignment.Center
        ) {
            if (isRolling) {
                AdvancedDicePips(value = displayedValue, isGolden = false)
            } else if (diceValue in 1..6) {
                AdvancedDicePips(value = diceValue.toInt(), isGolden = diceValue == 6L)
            } else {
                Text(
                    text = "ROLL",
                    color = if (enabled) LudoAccent else LudoAccentDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif
                )
            }
        }
    }
}

@Composable
private fun AdvancedDicePips(value: Int, isGolden: Boolean) {
    val pipColor = if (isGolden) Color(0xFFFFE082) else LudoAccent
    val pipShadow = if (isGolden) Color(0xFFFF8F00) else Color(0xFF100B06)
    val dotSize = 8.5.dp

    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        when (value) {
            1 -> {
                PipDot(dotSize * 1.35f, pipColor, pipShadow, Alignment.Center)
            }
            2 -> {
                PipDot(dotSize, pipColor, pipShadow, Alignment.TopStart)
                PipDot(dotSize, pipColor, pipShadow, Alignment.BottomEnd)
            }
            3 -> {
                PipDot(dotSize, pipColor, pipShadow, Alignment.TopStart)
                PipDot(dotSize, pipColor, pipShadow, Alignment.Center)
                PipDot(dotSize, pipColor, pipShadow, Alignment.BottomEnd)
            }
            4 -> {
                PipDot(dotSize, pipColor, pipShadow, Alignment.TopStart)
                PipDot(dotSize, pipColor, pipShadow, Alignment.TopEnd)
                PipDot(dotSize, pipColor, pipShadow, Alignment.BottomStart)
                PipDot(dotSize, pipColor, pipShadow, Alignment.BottomEnd)
            }
            5 -> {
                PipDot(dotSize, pipColor, pipShadow, Alignment.TopStart)
                PipDot(dotSize, pipColor, pipShadow, Alignment.TopEnd)
                PipDot(dotSize, pipColor, pipShadow, Alignment.Center)
                PipDot(dotSize, pipColor, pipShadow, Alignment.BottomStart)
                PipDot(dotSize, pipColor, pipShadow, Alignment.BottomEnd)
            }
            6 -> {
                PipDot(dotSize, pipColor, pipShadow, Alignment.TopStart)
                PipDot(dotSize, pipColor, pipShadow, Alignment.TopEnd)
                PipDot(dotSize, pipColor, pipShadow, Alignment.CenterStart)
                PipDot(dotSize, pipColor, pipShadow, Alignment.CenterEnd)
                PipDot(dotSize, pipColor, pipShadow, Alignment.BottomStart)
                PipDot(dotSize, pipColor, pipShadow, Alignment.BottomEnd)
            }
        }
    }
}

@Composable
private fun BoxScope_PipDot(size: androidx.compose.ui.unit.Dp, color: Color, shadowColor: Color, alignment: Alignment) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.8f), color, shadowColor)
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
    )
}

@Composable
private fun BoxScopePipDot(size: androidx.compose.ui.unit.Dp, color: Color, shadowColor: Color): Modifier {
    return Modifier
        .size(size)
        .clip(CircleShape)
        .background(
            Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.8f), color, shadowColor)
            )
        )
        .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.PipDot(
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    shadowColor: Color,
    alignment: Alignment
) {
    Box(
        modifier = Modifier
            .size(size)
            .align(alignment)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.7f), color, shadowColor)
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
    )
}
