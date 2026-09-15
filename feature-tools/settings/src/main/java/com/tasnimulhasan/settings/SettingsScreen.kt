package com.tasnimulhasan.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import com.tasnimulhasan.entity.enums.SortType

@Composable
internal fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()

    SettingsScreen(
        modifier = modifier,
        sortType = sortType,
        themeConfig = themeConfig,
        sortTypeLabel = viewModel::sortTypeToDisplayString,
        themeConfigLabel = viewModel::themeConfigToDisplayString,
        onSortTypeSelected = viewModel::setSortType,
        onThemeConfigSelected = viewModel::setThemeConfig,
    )
}

@Composable
internal fun SettingsScreen(
    modifier: Modifier = Modifier,
    sortType: SortType,
    themeConfig: DarkThemeConfig,
    sortTypeLabel: (SortType) -> String,
    themeConfigLabel: (DarkThemeConfig) -> String,
    onSortTypeSelected: (SortType) -> Unit,
    onThemeConfigSelected: (DarkThemeConfig) -> Unit,
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
