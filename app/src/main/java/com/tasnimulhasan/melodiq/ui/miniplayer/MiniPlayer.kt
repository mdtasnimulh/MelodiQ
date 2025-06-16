package com.tasnimulhasan.melodiq.ui.miniplayer

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.tasnimulhasan.designsystem.R
import com.tasnimulhasan.designsystem.theme.LightOrange
import com.tasnimulhasan.designsystem.theme.MelodiqTheme

@Composable
fun MiniPlayer(
    modifier: Modifier,
    cover: Bitmap?,
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

    Column(
        modifier = modifier
            .wrapContentSize()
            .background(color = Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                onImageClick.invoke()
            },
    ) {
        AsyncImage(
            modifier = modifier
                .size(65.dp)
                .clip(MaterialTheme.shapes.medium)
                .border(width = 3.dp, shape = MaterialTheme.shapes.medium, color = Color(darkPaletteColor)),
            model = cover,
            contentDescription = "Cover art",
            contentScale = ContentScale.FillHeight,
            placeholder = painterResource(R.drawable.default_cover),
            error = painterResource(R.drawable.default_cover),
            alignment = Alignment.Center,
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
            onImageClick = {},
        )
    }
}