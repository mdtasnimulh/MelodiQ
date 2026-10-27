package com.tasnimulhasan.eqalizer

import com.tasnimulhasan.designsystem.theme.LocalAppFontFamily
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import com.tasnimulhasan.common.constant.AppConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tasnimulhasan.common.constant.AppConstants.effectType
import com.tasnimulhasan.designsystem.theme.Blue20
import com.tasnimulhasan.designsystem.theme.Blue90
import com.tasnimulhasan.designsystem.theme.BlueMedium
import com.tasnimulhasan.designsystem.theme.CreamRed
import com.tasnimulhasan.eqalizer.components.FiveEqualizerItem
import com.tasnimulhasan.designsystem.R as Res
import com.tasnimulhasan.ui.motion.entrance

@Composable
internal fun EqualizerScreen(
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val enableEqualizer by viewModel.enableEqualizer.collectAsState()
    val enableTenBand by viewModel.enableTenBand.collectAsState()
    val equalizerError by viewModel.equalizerError.collectAsState()

    LaunchedEffect(Unit) { viewModel.onStart() }

    LazyColumn(
        modifier = Modifier.entrance(0).padding(horizontal = 16.dp)
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
                    text = stringResource(Res.string.label_enable_eq),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(
fontFamily = LocalAppFontFamily.current,
color = MaterialTheme.colorScheme.onBackground,
                    )
                )

                Switch(
                    checked = enableEqualizer,
                    onCheckedChange = { viewModel.toggleEqualizer() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = BlueMedium,
                        checkedIconColor = BlueMedium,
                        uncheckedTrackColor = Blue20.copy(alpha = 0.25f),
                        uncheckedBorderColor = Blue90
                    )
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            AnimatedVisibility(
                visible = enableEqualizer,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                // 5 / 10 band layout - available on every device; the curve is mapped onto the
                // device's own bands, so there is no hardware support check.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bands",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = !enableTenBand,
                        onClick = { viewModel.setTenBand(false) },
                        label = { Text("5 band") },
                    )
                    FilterChip(
                        selected = enableTenBand,
                        onClick = { viewModel.setTenBand(true) },
                        label = { Text("10 band") },
                    )
                }
            }
        }

        item {
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
                        text = equalizerError.orEmpty(),
                        color = CreamRed,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.retryEqualizer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Retry")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = enableEqualizer && !enableTenBand,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                EqualizerView(viewModel = viewModel)
            }
        }

        item {
            AnimatedVisibility(
                visible = enableEqualizer && enableTenBand,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                EqualizerView10Band(viewModel = viewModel)
            }
        }

        item {
            AnimatedVisibility(
                visible = enableEqualizer,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PresetsView(viewModel)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetsView(viewModel: EqualizerViewModel) {
    Column {
        val audioEffects by viewModel.audioEffects.collectAsState()

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp)),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Text(
                text = stringResource(Res.string.presets_title_text),
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
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

        Spacer(modifier = Modifier.height(8.dp))

        // A wrapping chip row instead of a fixed-height grid: the grid clipped the last
        // presets and scrolled inside the page on small screens.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            effectType.forEachIndexed { index, label ->
                FilterChip(
                    selected = index == audioEffects?.selectedEffectType,
                    onClick = { viewModel.onSelectPreset(index) },
                    label = { Text(label, maxLines = 1) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { viewModel.onSelectPreset(AppConstants.PRESET_FLAT) }) {
            Text("Reset to flat")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerView(viewModel: EqualizerViewModel) {
    val audioEffects by viewModel.audioEffects.collectAsState()

    // Fixed labels: this view stays on screen while it fades out after a switch to 10 bands, and
    // must not pick up the 10-band label list meanwhile.
    FiveEqualizerItem(
        modifier = Modifier,
        audioEffects = audioEffects,
        frequencyLabels = com.tasnimulhasan.common.audio.EqBands.labelsFor(5),
        onBandValueChange = { index, value -> viewModel.onBandLevelChanged(index, value) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerView10Band(viewModel: EqualizerViewModel) {
    val frequencyLabels = com.tasnimulhasan.common.audio.EqBands.labelsFor(10)
    val audioEffects by viewModel.audioEffects.collectAsState()

    // One full-width row per band (frequency, slider, dB) - easier to drag precisely than ten
    // narrow columns, and it fits any screen width.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        frequencyLabels.forEachIndexed { index, label ->
            val gain = (audioEffects?.gainValues?.getOrNull(index) ?: 0.0).toFloat() * 1000f
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(52.dp)
                )
                Slider(
                    modifier = Modifier.weight(1f),
                    value = gain.coerceIn(-1500f, 1500f),
                    onValueChange = { viewModel.onBandLevelChanged(index, it.toInt()) },
                    valueRange = -1500f..1500f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                )
                Text(
                    text = "%+.1f dB".format(gain / 100f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(58.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlbumsScreenPreview() {
    EqualizerScreen()
}