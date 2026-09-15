package com.tasnimulhasan.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tasnimulhasan.ui.image.AlbumArt
import com.tasnimulhasan.designsystem.R as Res

@Composable
internal fun AlbumsScreen(
    modifier: Modifier = Modifier,
    onAlbumClicked: (Long) -> Unit = {},
    viewModel: AlbumViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()

    if (albums.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items = albums, key = { it.albumId }) { album ->
            AlbumGridCard(album = album, onClick = { onAlbumClicked(album.albumId) })
        }
    }
}

@Composable
private fun AlbumGridCard(
    album: AlbumUiModel,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = AlbumArt(
                songId = album.representativeSongId,
                contentUri = album.representativeContentUri,
                albumId = album.albumId,
            ),
            contentDescription = "Album art for ${album.albumName}",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(Res.drawable.default_cover),
            error = painterResource(Res.drawable.default_cover),
        )
        Text(
            text = album.albumName,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "${album.artist} • ${album.songCount} songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
