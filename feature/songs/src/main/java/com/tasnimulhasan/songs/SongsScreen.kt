package com.tasnimulhasan.songs

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
internal fun SongsRoute(
    modifier: Modifier = Modifier,
    viewModel: SongsViewModel = hiltViewModel()
) {
    SongsScreen(
        modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SongsScreen(modifier: Modifier) {

    var sliderValue by remember { mutableFloatStateOf(0f) }
    var currentAngle by remember { mutableFloatStateOf(0f) }
    val volume = ((currentAngle + 180f) / 360f).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Spacer(modifier.height(25.dp))
            LineSlider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                modifier = Modifier
                    .padding(vertical = 15.dp, horizontal = 30.dp)
                    .fillMaxWidth(),
                valueRange = 0f..100f, // Example range
                steps = 20, // Example steps for graduation marks
                thumbDisplay = { it.roundToInt().toString() }
            )
        }

        item {
            Spacer(modifier.height(25.dp))

            val amplitudes = remember { List(60) { Random.nextFloat() } }
            var progress by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                CustomWaveProgressBar(
                    amplitudes = amplitudes,
                    currentProgress = progress,
                    onSeek = { progress = it }
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomKnob(
                        onValueChange = { angle ->
                            currentAngle = angle
                        }
                    )

                    CustomKnobProgressBar(volumeLevel = volume)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Angle: ${currentAngle.roundToInt()}°")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SongsScreenPreview() {
    SongsScreen(modifier = Modifier)
}