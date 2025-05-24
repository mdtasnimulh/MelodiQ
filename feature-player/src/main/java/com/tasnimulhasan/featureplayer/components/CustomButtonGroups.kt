package com.tasnimulhasan.featureplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.designsystem.theme.LightOrange
import com.tasnimulhasan.designsystem.theme.WhiteOrange

@Composable
fun CustomButtonGroups(
    buttonColor: Color,
    repeatModeOne: Boolean,
    repeatModeAll: Boolean,
    onRepeatButtonClicked: () -> Unit,
    onEQButtonClicked: () -> Unit,
    onSleepButtonClicked: () -> Unit,
    onShareButtonClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)
                .weight(1f)
                .clip(RoundedCornerShape(25.dp))
                .background(
                    color = buttonColor,
                    shape = RoundedCornerShape(25.dp)
                )
                .clickable(
                    onClick = { onRepeatButtonClicked.invoke() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp),
                imageVector = if (repeatModeAll) Icons.Default.AllInclusive
                else if (repeatModeOne) Icons.Default.RepeatOne
                else Icons.Default.Repeat,
                tint = buttonColor.copy(alpha = 0.75f),
                contentDescription = "Repeat Button"
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)
                .padding(start = 8.dp)
                .weight(1f)
                .clip(RoundedCornerShape(25.dp))
                .background(
                    color = buttonColor,
                    shape = RoundedCornerShape(25.dp)
                )
                .clickable(
                    onClick = { onEQButtonClicked.invoke() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp),
                imageVector = Icons.Default.GraphicEq,
                tint = buttonColor.copy(alpha = 0.75f),
                contentDescription = "Equalizer Button"
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)
                .padding(start = 8.dp)
                .weight(1f)
                .clip(RoundedCornerShape(25.dp))
                .background(
                    color = buttonColor,
                    shape = RoundedCornerShape(25.dp)
                )
                .clickable(
                    onClick = { onSleepButtonClicked.invoke() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp),
                imageVector = Icons.Default.Timer,
                tint = buttonColor.copy(alpha = 0.75f),
                contentDescription = "Sleep Button"
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)
                .padding(start = 8.dp)
                .weight(1f)
                .clip(RoundedCornerShape(25.dp))
                .background(
                    color = buttonColor,
                    shape = RoundedCornerShape(25.dp)
                )
                .clickable(
                    onClick = { onShareButtonClicked.invoke() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp),
                imageVector = Icons.Default.Share,
                tint = buttonColor.copy(alpha = 0.75f),
                contentDescription = "Share Button"
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewCustomButtonGroups() {
    CustomButtonGroups(
        buttonColor = LightOrange.copy(alpha = 0.05f),
        repeatModeOne = true,
        repeatModeAll = false,
        onRepeatButtonClicked = {},
        onEQButtonClicked = {},
        onSleepButtonClicked = {},
        onShareButtonClicked = {}
    )
}