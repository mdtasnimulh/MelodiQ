package com.tasnimulhasan.melodiq.ui.miniplayer

import android.graphics.Bitmap
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.tasnimulhasan.designsystem.theme.BlueDarker
import com.tasnimulhasan.designsystem.theme.LightOrange
import com.tasnimulhasan.designsystem.theme.MelodiqTheme
import com.tasnimulhasan.melodiq.ui.components.MiniPlayerWaveProgressBar
import kotlin.random.Random
import com.tasnimulhasan.designsystem.R as Res

@Composable
fun PopUpPlayer(
    modifier: Modifier = Modifier,
    cover: Bitmap?,
    songTitle: String,
    progress: Float,
    onProgress: (Float) -> Unit,
    isPlaying: Boolean,
    progressString: String,
    onMiniPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekPreviousClick: () -> Unit,
    onSeekNextClick: () -> Unit,
    onImageClick: () -> Unit,
) {
    val darkPaletteColor = remember(cover) {
        cover?.let {
            val palette = Palette.from(it).generate()
            palette.vibrantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: LightOrange.toArgb()
        } ?: LightOrange.toArgb()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onMiniPlayerClick() },
    ) {
        ConstraintLayout (
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
        ) {
            val (coverArt, title, slider, playPauseBtn, nextBtn, previousBtn, seek5SecForward, seek5SecBackward, progressStringTv) = createRefs()

            AsyncImage(
                modifier = Modifier
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(width = 3.dp, shape = MaterialTheme.shapes.medium, color = Color(darkPaletteColor))
                    .constrainAs(coverArt) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
                    .clickable(onClick = onImageClick),
                model = cover,
                contentDescription = "Album cover",
                contentScale = ContentScale.Crop,
                placeholder = painterResource(Res.drawable.default_cover),
                error = painterResource(Res.drawable.default_cover)
            )

            Text(
                modifier = Modifier
                    .constrainAs(title) {
                        top.linkTo(coverArt.top, margin = 8.dp)
                        start.linkTo(coverArt.end, margin = 8.dp)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.wrapContent
                    }
                    .basicMarquee(),
                text = songTitle,
                style = TextStyle(
                    color = BlueDarker,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                modifier = Modifier
                    .constrainAs(progressStringTv) {
                        top.linkTo(title.bottom, margin = 6.dp)
                        start.linkTo(title.start)
                        width = Dimension.wrapContent
                        height = Dimension.wrapContent
                    },
                text = progressString,
                style = TextStyle(
                    color = BlueDarker,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Box(
                modifier = Modifier
                    .constrainAs(slider) {
                        top.linkTo(coverArt.bottom, margin = 8.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.value(32.dp)
                    },
                contentAlignment = Alignment.Center
            ) {
                val amplitudes = remember { List(60) { Random.nextFloat() } }
                val normalizedProgress = progress / 100f

                MiniPlayerWaveProgressBar(
                    amplitudes = amplitudes,
                    currentProgress = normalizedProgress.coerceIn(0f, 1f),
                    isPlaying = isPlaying,
                    barColor = Color(darkPaletteColor).copy(alpha = 0.25f),
                    playedColor = Color(darkPaletteColor),
                    onSeek = { normalized ->
                        val seekPosition = normalized * 100f
                        onProgress.invoke(seekPosition)
                    }
                )
            }

            IconButton(
                modifier = Modifier
                    .constrainAs(playPauseBtn) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(slider.bottom, margin = 6.dp)
                        width = Dimension.wrapContent
                        height = Dimension.wrapContent
                    },
                onClick = onPlayPauseClick
            ) {
                Icon(
                    painter = if (isPlaying) painterResource(Res.drawable.ic_pause_circle) else painterResource(Res.drawable.ic_play_circle),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }

            IconButton(
                modifier = Modifier
                    .constrainAs(nextBtn) {
                        start.linkTo(playPauseBtn.end, margin = 8.dp)
                        top.linkTo(playPauseBtn.top)
                        bottom.linkTo(playPauseBtn.bottom)
                        width = Dimension.wrapContent
                        height = Dimension.wrapContent
                    },
                onClick = onNextClick
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_next),
                    contentDescription = "Next",
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                modifier = Modifier
                    .constrainAs(previousBtn) {
                        end.linkTo(playPauseBtn.start, margin = 8.dp)
                        top.linkTo(playPauseBtn.top)
                        bottom.linkTo(playPauseBtn.bottom)
                        width = Dimension.wrapContent
                        height = Dimension.wrapContent
                    },
                onClick = onPreviousClick
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_backward),
                    contentDescription = "Previous",
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                modifier = Modifier
                    .constrainAs(seek5SecBackward) {
                        end.linkTo(previousBtn.start, margin = 8.dp)
                        top.linkTo(playPauseBtn.top)
                        bottom.linkTo(playPauseBtn.bottom)
                        width = Dimension.wrapContent
                        height = Dimension.wrapContent
                    },
                onClick = onSeekPreviousClick
            ) {
                Icon(
                    imageVector = Icons.Default.Replay5,
                    contentDescription = "Replay 5 Sec",
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                modifier = Modifier
                    .constrainAs(seek5SecForward) {
                        start.linkTo(nextBtn.end, margin = 8.dp)
                        top.linkTo(playPauseBtn.top)
                        bottom.linkTo(playPauseBtn.bottom)
                        width = Dimension.wrapContent
                        height = Dimension.wrapContent
                    },
                onClick = onSeekNextClick
            ) {
                Icon(
                    imageVector = Icons.Default.Forward5,
                    contentDescription = "Forward 5 Sec",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewMiniPlayer2() {
    MelodiqTheme {
        PopUpPlayer(
            modifier = Modifier,
            cover = null,
            songTitle = "Song Title Song Title Song Title Song Title Song Title",
            progress = 50f,
            onProgress = {},
            isPlaying = true,
            progressString = "05:00",
            onMiniPlayerClick = {},
            onPlayPauseClick = {},
            onNextClick = {},
            onPreviousClick = {},
            onSeekNextClick = {},
            onSeekPreviousClick = {},
            onImageClick = {}
        )
    }
}