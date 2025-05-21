package com.tasnimulhasan.designsystem.othercomponent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomFLipText(digit: String) {
    var oldDigit by remember { mutableStateOf(digit) }
    var xRotationValue = remember { Animatable(0f) }

    LaunchedEffect(digit) {
        if (digit != oldDigit) {
            xRotationValue.snapTo(0f)
            xRotationValue.animateTo(
                targetValue = 180f,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing)
            )
            oldDigit = digit
            xRotationValue.snapTo(0f)
        }
    }

    val displayedDigit = if (xRotationValue.value < 90f) oldDigit else digit

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp, 48.dp)
            .graphicsLayer {
                rotationX = xRotationValue.value
                cameraDistance = 8 * density
            }
            .background(Color.Black, RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
    ) {
        Text(
            text = displayedDigit.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}