package com.tasnimulhasan.melodiq.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tasnimulhasan.melodiq.R

private const val MAX_FAVOURITES = 6

/**
 * Favourites shelf (about 4x2): a row of favourite covers with their titles. Tap one to play
 * it. The number of tiles follows the widget width.
 */
class MelodiqFavouritesWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        context.widgetEntryPoint().widgetUpdater().start()
        val songs = loadFavouriteSongs(context, MAX_FAVOURITES)
        provideContent {
            GlanceTheme { FavouritesWidgetContent(songs) }
        }
    }
}

class MelodiqFavouritesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MelodiqFavouritesWidget()
}

@Composable
private fun FavouritesWidgetContent(songs: List<WidgetSong>) {
    val context = LocalContext.current
    val size = LocalSize.current
    val padding = 12.dp
    val gap = 10.dp
    // Tile = cover size; height budget: padding, 28dp header, ~18dp title line.
    val tile: Dp = (size.height - padding * 2 - 28.dp - 18.dp).coerceIn(44.dp, 90.dp)
    val columns = (((size.width - padding * 2 + gap) / (tile + gap)).toInt()).coerceIn(1, MAX_FAVOURITES)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(padding),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(openAppAction(context)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_fav_on),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(14.dp),
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = "FAVOURITES",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(GlanceModifier.size(8.dp))
        if (songs.isEmpty()) {
            Column(
                modifier = GlanceModifier.fillMaxSize().clickable(openAppAction(context)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "No favourites yet", style = titleStyle(14))
                Text(text = "Tap the heart on a song to add it", style = subtitleStyle(12))
            }
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                songs.take(columns).forEachIndexed { index, item ->
                    if (index > 0) Spacer(GlanceModifier.width(gap))
                    Column(
                        modifier = GlanceModifier
                            .width(tile)
                            .clickable(actionRunCallback<PlaySongAction>(actionParametersOf(SongIdKey to item.song.songId))),
                    ) {
                        WidgetCover(art = item.art, size = tile, radius = 16.dp)
                        Text(
                            text = item.song.songTitle,
                            maxLines = 1,
                            modifier = GlanceModifier.padding(top = 4.dp),
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 11.sp),
                        )
                    }
                }
            }
        }
    }
}
