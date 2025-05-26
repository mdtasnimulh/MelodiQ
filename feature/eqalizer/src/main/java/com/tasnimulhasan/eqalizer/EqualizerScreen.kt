package com.tasnimulhasan.eqalizer

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasnimulhasan.common.constant.AppConstants.effectType
import com.tasnimulhasan.eqalizer.components.FiveEqualizerItem
import com.tasnimulhasan.designsystem.R as Res

@Composable
internal fun EqualizerScreen(
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val enableEqualizer by viewModel.enableEqualizer.collectAsState()
    val enableTenBand by viewModel.enableTenBand.collectAsState()
    val isTenBandSupported by viewModel.isTenBandSupported.collectAsState()
    val equalizerError by viewModel.equalizerError.collectAsState()
    val context = LocalContext.current

    // Show Toast when an error occurs
    LaunchedEffect(equalizerError) {
        equalizerError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp)
    ) {
        viewModel.onStart()

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.equalizer_title_text),
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )

                    Switch(
                        checked = enableEqualizer,
                        onCheckedChange = { viewModel.toggleEqualizer() },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color.Black,
                            checkedIconColor = Color.Black,
                            uncheckedTrackColor = Color.Gray,
                            uncheckedBorderColor = Color.Black
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "10-Band Equalizer",
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )

                    Switch(
                        checked = enableTenBand,
                        onCheckedChange = { viewModel.toggleTenBand() },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color.Black,
                            checkedIconColor = Color.Black,
                            uncheckedTrackColor = Color.Gray,
                            uncheckedBorderColor = Color.Black
                        )
                    )
                }

                // Show error message and retry button if there's an equalizer error
                AnimatedVisibility(
                    visible = equalizerError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = equalizerError ?: "",
                            color = Color.Red,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.retryEqualizer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.Gray
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            AnimatedVisibility(
                visible = enableEqualizer && !enableTenBand,
                enter = fadeIn() + slideInVertically { fullHeight -> -fullHeight / 2 },
                exit = fadeOut() + slideOutVertically { fullHeight -> -fullHeight / 3 }
            ) {
                EqualizerView(viewModel = viewModel, isTenBand = false)
            }
        }

        item {
            AnimatedVisibility(
                visible = enableTenBand,
                enter = fadeIn() + slideInVertically { fullHeight -> -fullHeight / 2 },
                exit = fadeOut() + slideOutVertically { fullHeight -> -fullHeight / 3 }
            ) {
                EqualizerView(viewModel = viewModel, isTenBand = true)
            }
        }

        item {
            AnimatedVisibility(
                visible = enableEqualizer || enableTenBand,
                enter = fadeIn() + slideInVertically { fullHeight -> -fullHeight / 2 },
                exit = fadeOut() + slideOutVertically { fullHeight -> -fullHeight / 2 }
            ) {
                PresetsView(viewModel)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PresetsView(viewModel: EqualizerViewModel) {
    Column {
        val audioEffects by viewModel.audioEffects.collectAsState()
        val groupedList = effectType.chunked(4)

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp)),
                thickness = 1.dp,
                color = Color.Gray
            )

            Text(
                text = stringResource(Res.string.presets_title_text),
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier
                    .wrapContentWidth()
                    .weight(0.75f)
                    .padding(4.dp)
                    .zIndex(1f),
                textAlign = TextAlign.Center
            )

            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp)),
                thickness = 1.dp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        for (itemList in groupedList) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val horizontalPadding = when {
                    this.maxWidth < 320.dp -> 8.dp
                    this.maxWidth > 400.dp -> 40.dp
                    else -> 20.dp
                }
                val horizontalSpacing = if (this.maxWidth > 400.dp) 24.dp else 16.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = horizontalSpacing,
                        alignment = Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (item in itemList) {
                        val index by remember { mutableIntStateOf(effectType.indexOf(item)) }
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .border(
                                    1.dp,
                                    if (index == audioEffects?.selectedEffectType) Color.Gray else Color.Black,
                                    RoundedCornerShape(40.dp)
                                )
                                .clip(RoundedCornerShape(40.dp))
                                .clickable { viewModel.onSelectPreset(index) }
                                .background(if (index == audioEffects?.selectedEffectType) Color.Black else Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 12.dp),
                                fontSize = 14.sp,
                                color = if (index == audioEffects?.selectedEffectType) Color.Gray else Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerView(viewModel: EqualizerViewModel, isTenBand: Boolean) {
    val frequencyLabels by viewModel.frequencyLabels.collectAsState()
    val audioEffects by viewModel.audioEffects.collectAsState()

    if (isTenBand) {
        val xAxisLabels = if (frequencyLabels.isNotEmpty()) {
            frequencyLabels
        } else {
            listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items(xAxisLabels.size) { index ->
                Column(
                    modifier = Modifier
                        .width(80.dp)
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = xAxisLabels[index],
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(audioEffects?.gainValues?.getOrNull(index)?.times(1000) ?: 0.0) / 100}dB",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        modifier = Modifier.height(120.dp),
                        value = audioEffects?.gainValues?.getOrNull(index)?.times(1000f)?.toFloat() ?: 0f,
                        onValueChange = { viewModel.onBandLevelChanged(index, it.toInt()) },
                        valueRange = -3000f..3000f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Black,
                            activeTrackColor = Color.Black,
                            inactiveTrackColor = Color.Gray
                        ),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.Black, CircleShape)
                            )
                        }
                    )
                }
            }
        }
    } else {
        FiveEqualizerItem(
            modifier = Modifier,
            audioEffects = audioEffects,
            frequencyLabels = if (frequencyLabels.isNotEmpty()) frequencyLabels else listOf("60Hz", "230Hz", "910Hz", "3kHz", "14kHz"),
            onBandValueChange = { index, value -> viewModel.onBandLevelChanged(index, value) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AlbumsScreenPreview() {
    EqualizerScreen()
}