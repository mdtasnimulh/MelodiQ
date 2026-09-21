package com.tasnimulhasan.library.components

import android.net.Uri
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tasnimulhasan.designsystem.theme.CardBlueMediumTextColor
import com.tasnimulhasan.designsystem.theme.RobotoFontFamily
import com.tasnimulhasan.designsystem.theme.WhiteOrange
import com.tasnimulhasan.ui.image.AlbumArt
import com.tasnimulhasan.designsystem.R as Res

/** The one song-row look used across every Library sub-screen, so Artists/Genres/Folders/
 * Search/Recently Played all feel like the same app instead of five different lists. */
@Composable
fun LibrarySongRow(
    modifier: Modifier = Modifier,
    contentUri: Uri,
    title: String,
    subtitle: String,
    songId: Long,
    albumId: Long,
    isSelected: Boolean,
    isFavourite: Boolean,
    trailingText: String? = null,
    onClick: () -> Unit,
    onFavouriteClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CardBlueMediumTextColor else MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(vertical = 4.dp, horizontal = 16.dp)
            .combinedClickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = AlbumArt(songId = songId, contentUri = contentUri, albumId = albumId),
                contentDescription = null,
                modifier = Modifier.width(64.dp).fillMaxHeight(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(Res.drawable.default_cover),
                error = painterResource(Res.drawable.default_cover),
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = if (isSelected) WhiteOrange else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontFamily = RobotoFontFamily
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = if (isSelected) WhiteOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = RobotoFontFamily
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            IconButton(onClick = onFavouriteClick) {
                Icon(
                    imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Filled.HeartBroken,
                    contentDescription = "Favourite",
                    tint = if (isFavourite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LibrarySectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
