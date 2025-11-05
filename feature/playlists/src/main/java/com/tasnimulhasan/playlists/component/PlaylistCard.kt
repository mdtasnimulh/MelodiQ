package com.tasnimulhasan.playlists.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import com.tasnimulhasan.entity.room.playlist.PlaylistEntity
import com.tasnimulhasan.designsystem.R as Res

@Composable
fun PlaylistCard(
    playlist: PlaylistEntity,
    onPlaylistClicked: (Int) -> Unit,
) {
    val cover = remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 10.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        onClick = {
            onPlaylistClicked.invoke(playlist.id)
        }
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(12.dp)
        ) {
            val (coverImage, playlistName, totalSong) = createRefs()

            AsyncImage(
                modifier = Modifier
                    .constrainAs(coverImage) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        bottom.linkTo(parent.bottom)
                    }
                    .size(75.dp)
                    .clip(MaterialTheme.shapes.medium),
                model = cover,
                contentDescription = "Album cover",
                contentScale = ContentScale.Fit,
                placeholder = painterResource(Res.drawable.playlist_img_2),
                error = painterResource(Res.drawable.playlist_img_1)
            )

            Text(
                modifier = Modifier
                    .constrainAs(playlistName){
                        start.linkTo(coverImage.end, margin = 10.dp)
                        top.linkTo(coverImage.top)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.wrapContent
                    },
                text = playlist.playlistName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
            )

            Text(
                modifier = Modifier
                    .constrainAs(totalSong){
                        top.linkTo(playlistName.bottom, margin = 8.dp)
                        start.linkTo(playlistName.start)
                        width = Dimension.wrapContent
                        height = Dimension.wrapContent
                    },
                text = "Total Song: 5",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}