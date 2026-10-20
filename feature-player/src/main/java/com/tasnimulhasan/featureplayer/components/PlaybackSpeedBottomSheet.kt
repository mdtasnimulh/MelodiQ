package com.tasnimulhasan.featureplayer.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.tasnimulhasan.designsystem.theme.glassSheetColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.domain.player.PlaybackTuning
import java.util.Locale
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

private val SPEED_PRESETS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

fun formatSpeed(speed: Float): String {
    val text = String.format(Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')
    return "${text}x"
}

private fun semitonesOf(pitch: Float): Int = (12f * log2(pitch)).roundToInt()
private fun pitchOf(semitones: Int): Float = 2f.pow(semitones / 12f)

private fun pitchLabel(semitones: Int): String = when {
    semitones == 0 -> "Original key"
    semitones > 0 -> "+$semitones semitones"
    else -> "$semitones semitones"
}

/**
 * Speed and pitch controls. Speed changes tempo without changing the key; pitch shifts the key
 * in semitones without changing the tempo. "Remember for this song" keeps the values for the
 * current song only (handy for podcasts, audiobooks and practising); otherwise they are the
 * default for every song.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedBottomSheet(
    tuning: PlaybackTuning,
    onDismiss: () -> Unit,
    onChange: (speed: Float, pitch: Float) -> Unit,
    onRememberChange: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    val semitones = semitonesOf(tuning.pitch)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = glassSheetColor(androidx.compose.material3.BottomSheetDefaults.ContainerColor),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Speed & pitch", style = MaterialTheme.typography.titleLarge)

            Text(
                formatSpeed(tuning.speed),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SPEED_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = tuning.speed == preset,
                        onClick = { onChange(preset, tuning.pitch) },
                        label = { Text(formatSpeed(preset)) },
                    )
                }
            }

            Slider(
                value = tuning.speed,
                onValueChange = { onChange(((it * 20f).roundToInt() / 20f), tuning.pitch) },
                valueRange = 0.5f..2f,
                steps = 29, // 0.05 increments
            )

            Text("Pitch", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Text(
                pitchLabel(semitones),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = semitones.toFloat(),
                onValueChange = { onChange(tuning.speed, pitchOf(it.roundToInt())) },
                valueRange = -12f..12f,
                steps = 23, // one semitone per step
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Remember for this song", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (tuning.rememberedForSong) "Applied whenever this song plays."
                        else "Off: changes apply to all songs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = tuning.rememberedForSong, onCheckedChange = onRememberChange)
            }

            OutlinedButton(
                onClick = onReset,
                enabled = !tuning.isDefault,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Reset to 1.0x, original key") }
        }
    }
}
