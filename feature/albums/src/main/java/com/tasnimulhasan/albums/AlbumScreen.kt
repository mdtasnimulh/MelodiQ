package com.tasnimulhasan.albums

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import timber.log.Timber
import com.tasnimulhasan.designsystem.R as Res

@Composable
internal fun AlbumsScreen(
    viewModel: AlbumViewModel = hiltViewModel()
) {
    val enableEqualizer by viewModel.enableEqualizer.collectAsState()

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
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
                    checked = enableEqualizer, onCheckedChange = {
                        viewModel.toggleEqualizer()
                    }, colors = SwitchDefaults.colors(
                        checkedTrackColor = Color.Black,
                        checkedIconColor = Color.Black,
                        uncheckedTrackColor = Color.Gray,
                        uncheckedBorderColor = Color.Black,
                    )
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            AnimatedVisibility(
                visible = enableEqualizer,
                enter = fadeIn() + slideInVertically { fullHeight -> -fullHeight / 2 },
                exit = fadeOut() + slideOutVertically { fullHeight -> -fullHeight / 3 }
            ) {
                EqualizerView(viewModel = viewModel)
            }
        }

        item {
            AnimatedVisibility(
                visible = enableEqualizer,
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
fun PresetsView(viewModel: AlbumViewModel) {
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
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                val horizontalPadding =
                    if (this.maxWidth < 320.dp) 8.dp else if (this.maxWidth > 400.dp) 40.dp else 20.dp
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
                        val index by remember {
                            mutableIntStateOf(
                                effectType.indexOf(item)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .border(
                                    1.dp,
                                    if (index == audioEffects?.selectedEffectType) Color.Gray else Color.Black,
                                    RoundedCornerShape(40.dp)
                                )
                                .clip(RoundedCornerShape(40.dp))
                                .clickable {
                                    viewModel.onSelectPreset(index)
                                }
                                .background(if (index == audioEffects?.selectedEffectType) Color.Black else Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .padding(
                                        horizontal = horizontalPadding,
                                        vertical = 12.dp
                                    ),
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
fun EqualizerView(viewModel: AlbumViewModel) {

    val xAxisLabels = listOf("60Hz", "230Hz", "910Hz", "3kHz", "14kHz")
    val maxLength = xAxisLabels.maxByOrNull { it.length }?.length ?: 0
    val audioEffects by viewModel.audioEffects.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .graphicsLayer {
                rotationZ = 270f
            }
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Timber.e("CheckAudioEffects: $audioEffects")
        for (index in xAxisLabels.indices) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val paddedLabel = xAxisLabels[index].padStart(maxLength, ' ')
                Text(
                    text = paddedLabel, modifier = Modifier
                        .wrapContentWidth()
                        .rotate(90f),
                    fontSize = 8.sp,
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.width(8.dp))

                Slider(
                    modifier = Modifier,
                    value = audioEffects!!.gainValues[index].times(1000f).toFloat()
                        .coerceIn(-3000f, 3000f),
                    onValueChange = {
                        viewModel.onBandLevelChanged(index, it.toInt())
                    },
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
                                .border(
                                    1.dp,
                                    Color.Gray,
                                    CircleShape
                                )
                                .clip(CircleShape)
                                .background(Color.Black, CircleShape)
                        )
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlbumsScreenPreview() {
    AlbumsScreen()
}