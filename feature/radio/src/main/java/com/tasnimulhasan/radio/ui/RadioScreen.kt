package com.tasnimulhasan.radio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.designsystem.component.Icon
import com.tasnimulhasan.designsystem.theme.glassBorder
import com.tasnimulhasan.radio.domain.FmBand
import com.tasnimulhasan.radio.domain.RadioError
import com.tasnimulhasan.radio.domain.RadioMode
import com.tasnimulhasan.radio.domain.RadioStation
import com.tasnimulhasan.radio.domain.RadioStatus
import com.tasnimulhasan.radio.domain.StationGroup
import com.tasnimulhasan.ui.motion.entrance

/** Entry point used by navigation: wires the ViewModel to the stateless [RadioContent]. */
@Composable
internal fun RadioScreen(
    modifier: Modifier = Modifier,
    viewModel: RadioViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RadioContent(
        state = state,
        modifier = modifier,
        onModeSelected = viewModel::onModeSelected,
        onStationSelected = viewModel::onStationSelected,
        onCustomUrlChanged = viewModel::onCustomUrlChanged,
        onCustomNameChanged = viewModel::onCustomNameChanged,
        onCustomGroupChanged = viewModel::onCustomGroupChanged,
        onPlayCustomUrl = viewModel::onPlayCustomUrl,
        onSaveCustomStation = viewModel::onSaveCustomStation,
        onRemoveStation = viewModel::onRemoveStation,
        onFrequencyChanged = viewModel::onFrequencyChanged,
        onFrequencyCommitted = viewModel::onFrequencyCommitted,
        onTogglePlayPause = viewModel::onTogglePlayPause,
        onStop = viewModel::onStop,
        onDismissError = viewModel::onDismissError,
    )
}

@Composable
internal fun RadioContent(
    state: RadioUiState,
    onModeSelected: (RadioMode) -> Unit,
    onStationSelected: (RadioStation) -> Unit,
    onCustomUrlChanged: (String) -> Unit,
    onCustomNameChanged: (String) -> Unit,
    onCustomGroupChanged: (StationGroup) -> Unit,
    onPlayCustomUrl: () -> Unit,
    onSaveCustomStation: () -> Unit,
    onRemoveStation: (RadioStation) -> Unit,
    onFrequencyChanged: (Int) -> Unit,
    onFrequencyCommitted: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.entrance(0), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Radio",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                "Stream stations over the internet, or tune the phone's own FM chip with wired headphones.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ModeTabs(
            mode = state.mode,
            onModeSelected = onModeSelected,
            modifier = Modifier.entrance(1),
        )

        AnimatedVisibility(visible = state.error != null) {
            state.error?.let { error ->
                ErrorBanner(
                    error = error,
                    onDismiss = onDismissError,
                    onSwitchToInternet = { onModeSelected(RadioMode.INTERNET) },
                )
            }
        }

        when (state.mode) {
            RadioMode.INTERNET -> InternetSection(
                state = state,
                onStationSelected = onStationSelected,
                onCustomUrlChanged = onCustomUrlChanged,
                onCustomNameChanged = onCustomNameChanged,
                onCustomGroupChanged = onCustomGroupChanged,
                onPlayCustomUrl = onPlayCustomUrl,
                onSaveCustomStation = onSaveCustomStation,
                onRemoveStation = onRemoveStation,
            )
            RadioMode.HARDWARE -> FmTunerCard(
                state = state,
                onFrequencyChanged = onFrequencyChanged,
                onFrequencyCommitted = onFrequencyCommitted,
                modifier = Modifier.entrance(2),
            )
        }

        TransportControls(
            state = state,
            onTogglePlayPause = onTogglePlayPause,
            onStop = onStop,
            modifier = Modifier.entrance(3),
        )
    }
}

// ---- mode tabs --------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeTabs(
    mode: RadioMode,
    onModeSelected: (RadioMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(RadioMode.INTERNET to "Internet", RadioMode.HARDWARE to "Hardware FM")
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (option, label) ->
            SegmentedButton(
                selected = mode == option,
                onClick = { onModeSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(label)
            }
        }
    }
}

// ---- error banner -----------------------------------------------------------------------------

@Composable
private fun ErrorBanner(
    error: RadioError,
    onDismiss: () -> Unit,
    onSwitchToInternet: () -> Unit,
) {
    val title: String
    val message: String
    var offerInternet = false
    var dismissable = true
    when (error) {
        is RadioError.HardwareUnavailable -> {
            title = "FM tuner unavailable"
            message = error.reason +
                " Most phones keep their FM chip away from normal apps, so this mode only works on devices that expose it."
            offerInternet = true
        }
        RadioError.HeadphonesUnplugged -> {
            title = "Plug in wired headphones"
            message = "FM reception uses the headphone cable as its antenna. Connect wired headphones (Bluetooth will not work), then press play."
            // The warning clears itself when headphones are connected.
            dismissable = false
        }
        is RadioError.TuneFailed -> {
            title = "Could not tune"
            message = error.reason
        }
        is RadioError.StreamFailed -> {
            title = "Stream problem"
            message = error.reason
        }
        RadioError.FocusDenied -> {
            title = "Audio is busy"
            message = "Another app (for example a call) is using the audio output. Try again when it finishes."
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = glassBorder(),
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (offerInternet) {
                    FilledTonalButton(onClick = onSwitchToInternet) { Text("Use Internet mode") }
                }
                if (dismissable) {
                    TextButton(onClick = onDismiss) { Text("Dismiss") }
                }
            }
        }
    }
}

// ---- internet mode ----------------------------------------------------------------------------

@Composable
private fun InternetSection(
    state: RadioUiState,
    onStationSelected: (RadioStation) -> Unit,
    onCustomUrlChanged: (String) -> Unit,
    onCustomNameChanged: (String) -> Unit,
    onCustomGroupChanged: (StationGroup) -> Unit,
    onPlayCustomUrl: () -> Unit,
    onSaveCustomStation: () -> Unit,
    onRemoveStation: (RadioStation) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = glassBorder(),
        modifier = Modifier.fillMaxWidth().entrance(2),
    ) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Text(
                state.stationName ?: "No station",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                state.nowPlaying ?: statusLabel(state.status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = glassBorder(),
        modifier = Modifier.fillMaxWidth().entrance(3),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            state.groups.forEach { (group, stations) ->
                Text(
                    group.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
                )
                if (stations.isEmpty()) {
                    Text(
                        text = if (group == StationGroup.BANGLADESHI) {
                            "No Bangladeshi stations yet. Paste a stream address below, choose Bangladeshi and tap Save."
                        } else {
                            "No stations in this group yet. Add new stations to your saved list for this group."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                stations.forEach { station ->
                    StationRow(
                        station = station,
                        selected = station.url == state.selectedStationUrl,
                        active = state.isActive,
                        onClick = { onStationSelected(station) },
                        onRemove = if (station in state.savedStations) ({ onRemoveStation(station) }) else null,
                    )
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = glassBorder(),
        modifier = Modifier.fillMaxWidth().entrance(4),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your own stream", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.customName,
                onValueChange = onCustomNameChanged,
                label = { Text("Name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.customUrl,
                onValueChange = onCustomUrlChanged,
                label = { Text("Stream address (https://...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onPlayCustomUrl() }),
            )
            Text(
                "Save it under",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(StationGroup.entries.size) { index ->
                    val group = StationGroup.entries[index]

                    FilterChip(
                        selected = state.customGroup == group,
                        onClick = { onCustomGroupChanged(group) },
                        label = {
                            Text(
                                text = group.label,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onSaveCustomStation, enabled = state.customUrl.isNotBlank()) {
                    Text("Save")
                }
                Button(onClick = onPlayCustomUrl, enabled = state.customUrl.isNotBlank()) {
                    Text("Play stream")
                }
            }
        }
    }
}

@Composable
private fun StationRow(
    station: RadioStation,
    selected: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (selected && active) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                station.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                station.genre,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected && active) {
            Text("On air", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove ${station.name}")
            }
        }
    }
}

// ---- hardware FM mode -------------------------------------------------------------------------

@Composable
private fun FmTunerCard(
    state: RadioUiState,
    onFrequencyChanged: (Int) -> Unit,
    onFrequencyCommitted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        border = glassBorder(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeadphoneChip(plugged = state.headphonesPlugged)

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    FmBand.mhzLabel(state.frequencyKhz),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "MHz",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Text(
                "${state.frequencyKhz} kHz · ${statusLabel(state.status)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FmDial(
                frequencyKhz = state.frequencyKhz,
                onFrequencyChange = onFrequencyChanged,
                onFrequencyCommitted = onFrequencyCommitted,
                enabled = true,
                fadeColor = surface,
                tickColor = MaterialTheme.colorScheme.onSurfaceVariant,
                needleColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FilledTonalIconButton(onClick = {
                    onFrequencyChanged(state.frequencyKhz - FmBand.STEP_KHZ)
                    onFrequencyCommitted()
                }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Tune down 0.1 MHz")
                }
                Text(
                    "${FmBand.mhzLabel(FmBand.MIN_KHZ)} - ${FmBand.mhzLabel(FmBand.MAX_KHZ)} MHz",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalIconButton(onClick = {
                    onFrequencyChanged(state.frequencyKhz + FmBand.STEP_KHZ)
                    onFrequencyCommitted()
                }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Tune up 0.1 MHz")
                }
            }
        }
    }
}

@Composable
private fun HeadphoneChip(plugged: Boolean) {
    val container = if (plugged) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (plugged) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.Headphones, contentDescription = null, modifier = Modifier.size(18.dp), tint = content)
            Text(
                if (plugged) "Headphones connected" else "No wired headphones",
                style = MaterialTheme.typography.labelLarge,
                color = content,
            )
        }
    }
}

// ---- shared transport controls ----------------------------------------------------------------

@Composable
private fun TransportControls(
    state: RadioUiState,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            FilledTonalIconButton(
                onClick = onStop,
                enabled = state.canStop,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = "Stop", modifier = Modifier.size(28.dp))
            }

            Box(contentAlignment = Alignment.Center) {
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    enabled = state.isActive || state.canPlay,
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(),
                ) {
                    Icon(
                        imageVector = if (state.isActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isActive) "Pause" else "Play",
                        modifier = Modifier.size(40.dp),
                    )
                }
                if (state.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(88.dp),
                        strokeWidth = 3.dp,
                    )
                }
            }

            // Keeps Play centred between Stop and an empty slot of the same width.
            Spacer(Modifier.width(56.dp))
        }
        Text(
            text = statusLabel(state.status),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun statusLabel(status: RadioStatus): String = when (status) {
    RadioStatus.IDLE -> "Stopped"
    RadioStatus.BUFFERING -> "Connecting…"
    RadioStatus.PLAYING -> "Playing live"
    RadioStatus.PAUSED -> "Paused"
}
