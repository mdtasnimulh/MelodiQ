package com.tasnimulhasan.home

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasnimulhasan.common.service.MelodiqPlayerService
import com.tasnimulhasan.designsystem.theme.BlueDarker
import com.tasnimulhasan.designsystem.theme.RobotoFontFamily
import com.tasnimulhasan.entity.enums.SortType
import com.tasnimulhasan.home.components.MusicCard
import timber.log.Timber

@Composable
internal fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    navigateToPlayer: (String) -> Unit,
) {
    HomeScreen(
        context = LocalContext.current,
        viewModel,
        Modifier,
        navigateToPlayer,
    )
}

@Composable
internal fun HomeScreen(
    context: Context,
    viewModel: HomeViewModel,
    modifier: Modifier,
    navigateToPlayer: (String) -> Unit,
) {
    val audioList by viewModel.audioList.collectAsStateWithLifecycle()
    val currentSelectedAudio by viewModel.currentSelectedAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    var isFavourite by remember { mutableStateOf(false) }
    val selectedSortOption = remember { mutableStateOf(viewModel.sortTypeToDisplayString(viewModel.sortType.value)) }
    val showSortDialog = remember { mutableStateOf(false) }
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    LaunchedEffect(sortType) {
        selectedSortOption.value = viewModel.sortTypeToDisplayString(sortType)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = modifier
                            .padding(top = 6.dp),
                        text = "Sort Type",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = BlueDarker,
                            fontWeight = FontWeight.Normal,
                            fontFamily = RobotoFontFamily
                        )
                    )

                    IconButton(
                        onClick = {
                            showSortDialog.value = !showSortDialog.value
                        },
                        modifier = Modifier,
                        enabled = true
                    ) {
                        Icon(
                            modifier = Modifier,
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort Icon",
                            tint = Color.Red
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
            itemsIndexed(audioList) { index, item ->
                /*val shouldLoadBitmap = remember(item.songId) { true }
                if (shouldLoadBitmap) {
                    LaunchedEffect(item.songId) {
                        viewModel.loadBitmapIfNeeded(context, index)
                    }
                }*/
                viewModel.loadBitmapIfNeeded(context, index)
                MusicCard(
                    modifier = modifier,
                    bitmap = item.cover,
                    title = item.songTitle,
                    artist = item.artist,
                    duration = item.duration,
                    songId = item.songId,
                    selectedId = currentSelectedAudio.songId,
                    isPlaying = context.isServiceRunning(MelodiqPlayerService::class.java),
                    isFavourite = isFavourite,
                    onMusicClicked = {
                        if (!context.isServiceRunning(MelodiqPlayerService::class.java)) {
                            val intent = Intent(context, MelodiqPlayerService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                        }
                        if (currentSelectedAudio.songId != item.songId) {
                            viewModel.onUiEvents(UIEvents.SelectedAudioChange(index))
                        }
                        navigateToPlayer(item.songId.toString())
                    },
                    onFavouriteIconClicked = {
                        isFavourite = !isFavourite
                    }
                )
            }
        }

        SortTypeDialog(
            showDialog = showSortDialog,
            selectedSortOption = selectedSortOption,
            onSortOptionSelected = { selected ->
                Timber.e("SortOption, Selected: $selected")

                val sortedBy = when (selected) {
                    "Date Modified (ASC)" -> SortType.DATE_MODIFIED_ASC
                    "Date Modified (DESC)" -> SortType.DATE_MODIFIED_DESC
                    "Name (ASC)" -> SortType.NAME_ASC
                    "Name (DESC)" -> SortType.NAME_DESC
                    "Artist (ASC)" -> SortType.ARTIST_ASC
                    "Artist (DESC)" -> SortType.ARTIST_DESC
                    "Duration (ASC)" -> SortType.DURATION_ASC
                    "Duration (DESC)" -> SortType.DURATION_DESC
                    else -> SortType.DATE_MODIFIED_DESC
                }

                viewModel.setSortType(sortedBy)
            }
        )
    }
}

@Composable
fun SortTypeDialog(
    showDialog: MutableState<Boolean>,
    selectedSortOption: MutableState<String>,
    onSortOptionSelected: (String) -> Unit
) {
    val options = listOf(
        "Date Modified (ASC)",
        "Date Modified (DESC)",
        "Name (ASC)",
        "Name (DESC)",
        "Artist (ASC)",
        "Artist (DESC)",
        "Duration (ASC)",
        "Duration (DESC)"
    )

    if (showDialog.value) {
        Dialog(onDismissRequest = {
            showDialog.value = false
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(15.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Sort By",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedSortOption.value = option
                                    onSortOptionSelected(selectedSortOption.value)
                                    showDialog.value = false
                                }
                        ) {
                            Checkbox(
                                checked = selectedSortOption.value == option,
                                onCheckedChange = {
                                    selectedSortOption.value = option
                                    onSortOptionSelected(selectedSortOption.value)
                                    showDialog.value = false
                                }
                            )
                            Text(text = option)
                        }
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
fun <T> Context.isServiceRunning(service: Class<T>): Boolean {
    return (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any { it -> it.service.className == service.name }
}