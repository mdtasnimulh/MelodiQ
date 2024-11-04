package com.tasnimulhasan.home.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale
import com.tasnimulhasan.designsystem.R as Res

@Composable
fun MusicCard(
    modifier: Modifier = Modifier,
    bitmap: Bitmap?,
    title: String,
    artist: String,
    duration: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = {},
    ) {
        Row(
            modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bitmap != null) {
                Image(
                    modifier = modifier
                        .width(100.dp)
                        .height(100.dp)
                        .clip(shape = MaterialTheme.shapes.medium)
                        .weight(2f),
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Cover art",
                    contentScale = ContentScale.FillHeight
                )
            } else {
                Image(
                    modifier = modifier
                        .width(100.dp)
                        .height(120.dp)
                        .clip(shape = MaterialTheme.shapes.medium)
                        .weight(2f),
                    painter = painterResource(
                        id = Res.drawable.ic_launcher_foreground
                    ),
                    contentDescription = "Null Cover Art",
                    contentScale = ContentScale.FillHeight
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 10.dp)
                    .weight(5.2f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    modifier = modifier,
                    text = title,
                    style = TextStyle(
                        fontSize = 12.sp
                    ),
                    maxLines = 2
                )
                Text(
                    modifier = modifier
                        .padding(top = 8.dp),
                    text = artist,
                    style = TextStyle(
                        fontSize = 11.sp
                    )
                )
                Text(
                    modifier = modifier.padding(top = 8.dp),
                    text = convertLongToReadableDateTime(duration.toLong(), "mm:ss"),
                    style = TextStyle(
                        fontSize = 11.sp
                    )
                )
            }

            IconButton(
                onClick = { /*TODO*/ },
                modifier = Modifier.weight(0.8f),
                enabled = true
            ) {
                Icon(
                    modifier = Modifier,
                    imageVector = Icons.Filled.HeartBroken,
                    contentDescription = "Favourite Icon",
                    tint = Color.Red
                )
            }
        }
    }
}

fun convertLongToReadableDateTime(time: Long, format: String): String {
    val df = SimpleDateFormat(format, Locale.US)
    return df.format(time)
}

@Preview(showBackground = true)
@Composable
fun MusicCardPreview() {
    MusicCard(bitmap = null, title = "Song df afdasdfadsf fasdf asdfasdf dasffsa Title", artist = "Artist Name", duration = "134654")
}