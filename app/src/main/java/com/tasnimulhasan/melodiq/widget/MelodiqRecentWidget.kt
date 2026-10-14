package com.tasnimulhasan.melodiq.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.action.actionParametersOf
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

private const val MAX_RECENT = 6

/**
 * "Recently played" list widget (about 4x3). Each row is a one-tap play; the number of rows
 * shown follows the widget's height.
 */
class MelodiqRecentWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        context.widgetEntryPoint().widgetUpdater().start()
        val songs = loadRecentSongs(context, MAX_RECENT)
        provideContent {
            GlanceTheme { RecentWidgetContent(songs) }
        }
    }
}

class MelodiqRecentWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MelodiqRecentWidget()
}

@Composable
private fun RecentWidgetContent(songs: List<WidgetSong>) {
    val context = LocalContext.current
    val size = LocalSize.current
    val rowHeight = 52.dp
    // Height budget: 12dp padding top/bottom + 32dp header.
    val rows = ((size.height - 56.dp).value / rowHeight.value).toInt().coerceIn(1, MAX_RECENT)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(32.dp).clickable(openAppAction(context)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "RECENTLY PLAYED",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        if (songs.isEmpty()) {
            Column(
                modifier = GlanceModifier.fillMaxSize().clickable(openAppAction(context)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "Nothing played yet", style = titleStyle(14))
                Text(text = "Songs you play will show up here", style = subtitleStyle(12))
            }
        } else {
            songs.take(rows).forEach { item ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .padding(vertical = 4.dp)
                        .clickable(actionRunCallback<PlaySongAction>(actionParametersOf(SongIdKey to item.song.songId))),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetCover(art = item.art, size = 44.dp, radius = 12.dp)
                    Spacer(GlanceModifier.width(12.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(text = item.song.songTitle, maxLines = 1, style = titleStyle(14))
                        Text(
                            text = item.song.artist.ifBlank { "Unknown artist" },
                            maxLines = 1,
                            style = subtitleStyle(12),
                        )
                    }
                }
            }
        }
    }
}
