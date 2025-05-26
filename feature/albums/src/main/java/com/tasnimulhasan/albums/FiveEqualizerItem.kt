package com.tasnimulhasan.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.tasnimulhasan.entity.eqalizer.AudioEffects
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiveEqualizerItem(
    modifier: Modifier = Modifier,
    audioEffects: AudioEffects?,
    frequencyLabels: List<String>,
    onBandValueChange: (index: Int, gainValue: Int) -> Unit,
) {
    val xAxisLabels = frequencyLabels
    val xAxisLabelName = listOf("Bass / Sub-bass", "Low Mids / Warmth", "Mids / Vocal body", "Upper Mids / Clarity", "Upper Mids / Clarity")

    var currentAngle by remember { mutableFloatStateOf(-90f) }
    val progress = ((currentAngle + 90f) / 360f * 100f).coerceIn(0f, 100f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(540.dp)
    ){
        LazyColumn(
            modifier = Modifier.height(540.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            items(xAxisLabels.size) { index ->
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(vertical = 4.dp),
                ) {
                    val (knob, bar) = createRefs()

                    val gainValue = (audioEffects?.gainValues?.getOrNull(index) ?: 0.0).toFloat()

                    val knobAngle = ((gainValue + 1.5f) / 3f * 360f - 90f).coerceIn(-90f, 270f)
                    val progress = ((knobAngle + 90f) / 360f).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .constrainAs(knob) {
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                                start.linkTo(parent.start, margin = 12.dp)
                                width = Dimension.wrapContent
                                height = Dimension.wrapContent
                            }
                    ) {
                        CustomKnob(
                            angle = knobAngle,
                            onValueChange = { value ->
                                val gain = ((value + 90f) / 360f * 3000f - 1500f).roundToInt()
                                onBandValueChange(index, gain)
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .constrainAs(bar) {
                                top.linkTo(knob.top)
                                bottom.linkTo(knob.bottom)
                                start.linkTo(knob.end)
                                end.linkTo(parent.end, margin = 16.dp)
                                width = Dimension.fillToConstraints
                                height = Dimension.wrapContent
                            },
                        verticalArrangement = Arrangement.Center
                    ) {
                        CustomKnobProgressBar(volumeLevel = progress)

                        Spacer(modifier = modifier.height(4.dp))

                        Row(
                            modifier = modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = xAxisLabelName[index],
                                fontSize = 12.sp,
                                style = TextStyle(
                                    textAlign = TextAlign.Center,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Medium,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )

                            Spacer(modifier = modifier.width(4.dp))

                            Text(
                                text = "(${xAxisLabels[index]})",
                                fontSize = 12.sp,
                                style = TextStyle(
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }

                        Spacer(modifier = modifier.height(2.dp))

                        Text(
                            modifier = modifier.wrapContentSize().padding(horizontal = 16.dp),
                            text = "${(gainValue * 1000).toInt() / 100f}dB",
                            fontSize = 12.sp,
                            style = TextStyle(
                                textAlign = TextAlign.Center,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.SemiBold,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }
        }

    }

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewEqualizerItem() {
    FiveEqualizerItem(
        modifier = Modifier,
        audioEffects = AudioEffects(
            0, listOf(0.0, 0.0, 0.0, 0.0, 0.0)
        ),
        frequencyLabels = listOf("60Hz", "230Hz", "910Hz", "3kHz", "14kHz"),
        onBandValueChange = {_, _ ->}
    )
}