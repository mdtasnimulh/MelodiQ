package com.tasnimulhasan.settings

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.entity.enums.AccentColorOption
import com.tasnimulhasan.entity.enums.CoverArtStyle
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.entity.enums.VisualizerStyle

@Composable
internal fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
    val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
    val replayGainEnabled by viewModel.replayGainEnabled.collectAsStateWithLifecycle()
    val crossfadeSettings by viewModel.crossfadeSettings.collectAsStateWithLifecycle()
    val visualizerStyle by viewModel.visualizerStyle.collectAsStateWithLifecycle()
    val coverArtStyle by viewModel.coverArtStyle.collectAsStateWithLifecycle()
    val miniPlayerPosition by viewModel.miniPlayerPosition.collectAsStateWithLifecycle()
    val appIcon by viewModel.appIcon.collectAsStateWithLifecycle()

    // Visualizer capture is gated behind RECORD_AUDIO by Android (even though it only reads
    // this app's own playback, not the microphone). Asked for only at the moment the user
    // actually picks a style - never up front, and OFF never needs it.
    val context = LocalContext.current
    var pendingStyle by remember { mutableStateOf<VisualizerStyle?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val style = pendingStyle
        pendingStyle = null
        if (granted && style != null) {
            viewModel.setVisualizerStyle(style)
        } else {
            Toast.makeText(
                context,
                "The visualizer needs audio permission to read this app's own playback. It stays off without it.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportBackup(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importBackup(uri) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    SettingsScreen(
        modifier = modifier,
        appIcon = appIcon,
        onAppIconSelected = viewModel::setAppIcon,
        onExportBackup = { exportLauncher.launch("melodiq_backup.json") },
        onImportBackup = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
        sortType = sortType,
        themeConfig = themeConfig,
        accentColor = accentColor,
        replayGainEnabled = replayGainEnabled,
        crossfadeEnabled = crossfadeSettings.enabled,
        crossfadeDurationMs = crossfadeSettings.durationMs,
        sortTypeLabel = viewModel::sortTypeToDisplayString,
        themeConfigLabel = viewModel::themeConfigToDisplayString,
        accentColorLabel = viewModel::accentColorLabel,
        onSortTypeSelected = viewModel::setSortType,
        onThemeConfigSelected = viewModel::setThemeConfig,
        onAccentColorSelected = viewModel::setAccentColor,
        onReplayGainToggled = viewModel::setReplayGainEnabled,
        onCrossfadeToggled = viewModel::setCrossfadeEnabled,
        onCrossfadeDurationChanged = viewModel::setCrossfadeDurationMs,
        visualizerStyle = visualizerStyle,
        visualizerStyleLabel = viewModel::visualizerStyleLabel,
        coverArtStyle = coverArtStyle,
        coverArtStyleLabel = viewModel::coverArtStyleLabel,
        miniPlayerPosition = miniPlayerPosition,
        miniPlayerPositionLabel = viewModel::miniPlayerPositionLabel,
        onMiniPlayerPositionSelected = viewModel::setMiniPlayerPosition,
        onCoverArtStyleSelected = { style ->
            // The circular visualizer is a ring drawn around round artwork - on any other
            // cover style it has nothing to wrap, so switching away from Circle while it's
            // active drops back to Bars (and says so) instead of leaving a broken combo.
            if (style != CoverArtStyle.CIRCLE && visualizerStyle == VisualizerStyle.CIRCULAR) {
                viewModel.setVisualizerStyle(VisualizerStyle.BARS)
                Toast.makeText(
                    context,
                    "Circular visualizer needs Circle cover art - switched visualizer to Bars.",
                    Toast.LENGTH_LONG
                ).show()
            }
            viewModel.setCoverArtStyle(style)
        },
        onVisualizerStyleSelected = { style ->
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (style == VisualizerStyle.CIRCULAR && coverArtStyle != CoverArtStyle.CIRCLE) {
                // Don't even ask for the permission yet - this combination can't render.
                Toast.makeText(
                    context,
                    "First set Cover art to Circle, then enable the circular visualizer.",
                    Toast.LENGTH_LONG
                ).show()
            } else if (style == VisualizerStyle.OFF || granted) {
                viewModel.setVisualizerStyle(style)
            } else {
                pendingStyle = style
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
    )
}

@Composable
internal fun SettingsScreen(
    modifier: Modifier = Modifier,
    sortType: SortType,
    themeConfig: DarkThemeConfig,
    accentColor: AccentColorOption,
    replayGainEnabled: Boolean,
    crossfadeEnabled: Boolean,
    crossfadeDurationMs: Long,
    sortTypeLabel: (SortType) -> String,
    themeConfigLabel: (DarkThemeConfig) -> String,
    accentColorLabel: (AccentColorOption) -> String,
    onSortTypeSelected: (SortType) -> Unit,
    onThemeConfigSelected: (DarkThemeConfig) -> Unit,
    onAccentColorSelected: (AccentColorOption) -> Unit,
    onReplayGainToggled: (Boolean) -> Unit,
    onCrossfadeToggled: (Boolean) -> Unit,
    onCrossfadeDurationChanged: (Long) -> Unit,
    visualizerStyle: VisualizerStyle,
    visualizerStyleLabel: (VisualizerStyle) -> String,
    onVisualizerStyleSelected: (VisualizerStyle) -> Unit,
    coverArtStyle: CoverArtStyle,
    coverArtStyleLabel: (CoverArtStyle) -> String,
    onCoverArtStyleSelected: (CoverArtStyle) -> Unit,
    miniPlayerPosition: com.tasnimulhasan.entity.enums.MiniPlayerPosition,
    miniPlayerPositionLabel: (com.tasnimulhasan.entity.enums.MiniPlayerPosition) -> String,
    onMiniPlayerPositionSelected: (com.tasnimulhasan.entity.enums.MiniPlayerPosition) -> Unit,
    appIcon: AppIconOption,
    onAppIconSelected: (AppIconOption) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item { SectionHeader("Sort songs by") }
        items(SortType.entries.toList()) { option ->
            SettingsRadioRow(
                label = sortTypeLabel(option),
                selected = option == sortType,
                onClick = { onSortTypeSelected(option) },
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        item { SectionHeader("Theme") }
        items(DarkThemeConfig.entries.toList()) { option ->
            SettingsRadioRow(
                label = themeConfigLabel(option),
                selected = option == themeConfig,
                onClick = { onThemeConfigSelected(option) },
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        item { SectionHeader("Accent color") }
        item {
            AccentColorPicker(
                selected = accentColor,
                label = accentColorLabel,
                onSelected = onAccentColorSelected,
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        item { SectionHeader("Player cover art") }
        items(CoverArtStyle.entries.toList()) { option ->
            SettingsRadioRow(
                label = coverArtStyleLabel(option),
                selected = option == coverArtStyle,
                onClick = { onCoverArtStyleSelected(option) },
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        item { SectionHeader("Mini player position") }
        items(com.tasnimulhasan.entity.enums.MiniPlayerPosition.entries.toList()) { option ->
            SettingsRadioRow(
                label = miniPlayerPositionLabel(option),
                selected = option == miniPlayerPosition,
                onClick = { onMiniPlayerPositionSelected(option) },
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        item { SectionHeader("Audio visualizer") }
        items(VisualizerStyle.entries.toList()) { option ->
            SettingsRadioRow(
                label = visualizerStyleLabel(option) +
                    if (option == VisualizerStyle.CIRCULAR && coverArtStyle != CoverArtStyle.CIRCLE)
                        "  (needs Circle cover art)" else "",
                selected = option == visualizerStyle,
                onClick = { onVisualizerStyleSelected(option) },
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        item { SectionHeader("App icon") }
        AppIconOption.Group.entries.forEach { group ->
            item(key = "icon_group_${group.name}") {
                AppIconGroup(
                    group = group,
                    selected = appIcon,
                    onSelected = onAppIconSelected,
                )
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        item { SectionHeader("Playback") }
        item {
            SettingsSwitchRow(
                title = "Volume normalization",
                subtitle = "Even out loudness between tracks using embedded ReplayGain tags, when present",
                checked = replayGainEnabled,
                onCheckedChange = onReplayGainToggled,
            )
        }
        item {
            SettingsSwitchRow(
                title = "Crossfade",
                subtitle = "Fade out the end of a track while the next one fades in",
                checked = crossfadeEnabled,
                onCheckedChange = onCrossfadeToggled,
            )
        }
        if (crossfadeEnabled) {
            item {
                CrossfadeDurationSlider(
                    durationMs = crossfadeDurationMs,
                    onDurationChanged = onCrossfadeDurationChanged,
                )
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        item { SectionHeader("Backup & restore") }
        item {
            Text(
                text = "Save your playlists, favourites, play history and settings to a file, and restore them later or on another device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.Button(onClick = onExportBackup, modifier = Modifier.weight(1f)) { Text("Export") }
                androidx.compose.material3.OutlinedButton(onClick = onImportBackup, modifier = Modifier.weight(1f)) { Text("Import") }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CrossfadeDurationSlider(
    durationMs: Long,
    onDurationChanged: (Long) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text = "Duration: ${durationMs / 1000}s",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = (durationMs / 1000L).toInt().coerceIn(1, 12).toFloat(),
            onValueChange = { onDurationChanged((it.toLong()) * 1000L) },
            valueRange = 1f..12f,
            steps = 10,
        )
    }
}

@Composable
private fun AccentColorPicker(
    selected: AccentColorOption,
    label: (AccentColorOption) -> String,
    onSelected: (AccentColorOption) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(AccentColorOption.entries.toList()) { option ->
            AccentColorSwatch(
                option = option,
                label = label(option),
                isSelected = option == selected,
                onClick = { onSelected(option) },
            )
        }
    }
}

@Composable
private fun AccentColorSwatch(
    option: AccentColorOption,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        val swatchColor = accentPreviewColor(option)
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(swatchColor ?: MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isSelected -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                )
                option == AccentColorOption.DYNAMIC || option == AccentColorOption.ALBUM_ART || option == AccentColorOption.APP_ICON -> Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** A representative preview swatch for the picker row. DYNAMIC/ALBUM_ART have no single
 * fixed color (they're resolved at runtime), so they get a neutral gradient-like tone with
 * a palette icon on top instead of pretending to show a specific color. */
private fun accentPreviewColor(option: AccentColorOption): Color? = when (option) {
    AccentColorOption.PURPLE -> Color(0xFF6650A4)
    AccentColorOption.BLUE -> Color(0xFF0077B6)
    AccentColorOption.GREEN -> Color(0xFF2E7D32)
    AccentColorOption.ORANGE -> Color(0xFFEF6C00)
    AccentColorOption.PINK -> Color(0xFFD81B60)
    AccentColorOption.RED -> Color(0xFFC62828)
    AccentColorOption.TEAL -> Color(0xFF00796B)
    AccentColorOption.DYNAMIC -> Color(0xFF757575)
    AccentColorOption.ALBUM_ART -> Color(0xFF757575)
    AccentColorOption.APP_ICON -> Color(0xFF757575)
}


@Composable
private fun AppIconGroup(
    group: AppIconOption.Group,
    selected: AppIconOption,
    onSelected: (AppIconOption) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIconOption.entries.filter { it.group == group }.forEach { option ->
                AppIconTile(
                    option = option,
                    isSelected = option == selected,
                    onClick = { onSelected(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AppIconTile(
    option: AppIconOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resId = remember(option) { AppIconManager.previewResId(context, option) }
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .selectable(selected = isSelected, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Outer radius = inner image radius (18) + gap (4) + border (3) so the selection ring
        // hugs the rounded logo evenly instead of looking boxy.
        val ringShape = androidx.compose.foundation.shape.RoundedCornerShape(25.dp)
        val logoShape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(3.dp, borderColor, ringShape)
                .padding(7.dp)
                .clip(logoShape),
            contentAlignment = Alignment.Center,
        ) {
            if (resId != 0) {
                // The real logo drawable (not the adaptive wrapper), clipped to a rounded square.
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.ImageView(ctx).apply {
                            scaleType = android.widget.ImageView.ScaleType.FIT_XY
                            setImageResource(resId)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Palette, contentDescription = null)
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
