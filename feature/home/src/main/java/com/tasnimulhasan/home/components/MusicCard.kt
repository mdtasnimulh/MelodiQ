package com.tasnimulhasan.home.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tasnimulhasan.designsystem.theme.BlueDarker
import com.tasnimulhasan.designsystem.theme.CardBlueMediumTextColor
import com.tasnimulhasan.designsystem.theme.RobotoFontFamily
import com.tasnimulhasan.designsystem.theme.WhiteOrange
import java.text.SimpleDateFormat
import java.util.Locale
import com.tasnimulhasan.designsystem.R as Res

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.MusicCard(
    modifier: Modifier = Modifier,
    bitmap: Bitmap?,
    title: String,
    artist: String,
    duration: String,
    songId: Long,
    selectedId: Long,
    isPlaying: Boolean,
    isFavourite: Boolean,
    onMusicClicked: () -> Unit,
    onFavouriteIconClicked: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val isSelected = selectedId == songId && isPlaying
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CardBlueMediumTextColor else MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = { onMusicClicked.invoke() },
    ) {
        Row(
            modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = bitmap,
                contentDescription = "Cover art",
                modifier = modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "image-$songId"),
                        animatedVisibilityScope = animatedVisibilityScope ?: return@Row,
                    )
                    .width(100.dp)
                    .fillMaxHeight()
                    .weight(2f),
                contentScale = ContentScale.FillHeight,
                placeholder = painterResource(Res.drawable.default_cover),
                error = painterResource(Res.drawable.default_cover)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .weight(5.2f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    modifier = modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "title-$songId"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                    text = title,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = if (isSelected) WhiteOrange else BlueDarker,
                        fontWeight = FontWeight.Medium,
                        fontFamily = RobotoFontFamily
                    ),
                    maxLines = 2
                )
                Text(
                    modifier = modifier
                        .padding(top = 6.dp),
                    text = artist,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = if (isSelected) WhiteOrange else BlueDarker,
                        fontWeight = FontWeight.Normal,
                        fontFamily = RobotoFontFamily
                    )
                )
                Text(
                    modifier = modifier.padding(top = 4.dp),
                    text = convertLongToReadableDateTime(duration.toLong(), "mm:ss"),
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = if (isSelected) WhiteOrange else BlueDarker,
                        fontWeight = FontWeight.Normal,
                        fontFamily = RobotoFontFamily
                    )
                )
            }

            IconButton(
                onClick = { onFavouriteIconClicked.invoke() },
                modifier = Modifier
                    .weight(0.8f)
                    .padding(end = 8.dp),
                enabled = true
            ) {
                Icon(
                    modifier = Modifier,
                    imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Filled.HeartBroken,
                    contentDescription = "Favourite Icon",
                    tint = if (isFavourite) Color.Red else Color.Gray
                )
            }
        }
    }
}

fun convertLongToReadableDateTime(time: Long, format: String): String {
    val df = SimpleDateFormat(format, Locale.US)
    return df.format(time)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true)
@Composable
fun MusicCardPreview() {
    SharedTransitionLayout {
        MusicCard(
            bitmap = null,
            title = "Song df afdasdfadsf fasdf asdfasdf dasffsa Title",
            artist = "Artist Name",
            duration = "134654",
            onMusicClicked = {},
            modifier = Modifier,
            songId = 0L,
            selectedId = 0L,
            isPlaying = true,
            isFavourite = true,
            onFavouriteIconClicked = {},
        )
    }
}
