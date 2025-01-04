package com.tasnimulhasan.home

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tasnimulhasan.designsystem.R
import com.tasnimulhasan.designsystem.theme.MelodiqTheme

@Composable
fun MiniPlayer(
    modifier: Modifier,
    cover: Bitmap?,
    songTitle: String,
    progress: Float,
    onProgress: (Float) -> Unit,
    isPlaying: Boolean,
    onMiniPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable {
                onMiniPlayerClick.invoke()
            }
            .fillMaxWidth()
            .height(150.dp)
            .background(color = Color.White.copy(alpha = 1f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 12.dp)
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = cover,
                contentDescription = "Cover art",
                modifier = modifier
                    .width(50.dp)
                    .height(50.dp)
                    .clip(shape = RoundedCornerShape(100.dp))
                    .weight(1f),
                contentScale = ContentScale.FillHeight,
                placeholder = painterResource(R.drawable.ic_launcher_background),
                error = painterResource(R.drawable.ic_launcher_background),
                alignment = Alignment.Center,
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .weight(4f)
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 2.dp),
                    text = songTitle,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = modifier.height(6.dp))

                Slider(
                    modifier = modifier.fillMaxWidth(),
                    value = progress,
                    onValueChange = { onProgress(it) },
                    valueRange = 0f..100f
                )
            }
        }

        Row (
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    onPlayPauseClick.invoke()
                }
            ) {
                Icon(
                    modifier = Modifier
                        .size(32.dp)
                        .weight(1f),
                    painter = if (isPlaying) painterResource(R.drawable.ic_pause_circle) else painterResource(
                        R.drawable.ic_play_circle
                    ),
                    contentDescription = null
                )
            }

            Spacer(modifier = modifier.width(16.dp))

            IconButton(
                onClick = {
                    onNextClick.invoke()
                }
            ) {
                Icon(
                    modifier = Modifier
                        .size(32.dp)
                        .weight(1f),
                    painter = painterResource(R.drawable.ic_next),
                    contentDescription = null
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Gray,
            thickness = 1.dp
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewMiniPlayer() {
    MelodiqTheme {
        MiniPlayer(
            modifier = Modifier,
            cover = null,
            songTitle = "Song Title",
            progress = 50f,
            onProgress = {},
            isPlaying = true,
            onMiniPlayerClick = {},
            onPlayPauseClick = {},
            onNextClick = {}
        )
    }
}