package com.tasnimulhasan.melodiq.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * The large "player card" widget: cover on the left, track info, a favourite heart and a
 * previous / play-pause / next row on the right. Resizable; the cover grows with the widget.
 */
class MelodiqPlayerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // A widget was just added/updated: make sure live updates are running this process.
        context.widgetEntryPoint().widgetUpdater().start()
        val state = loadWidgetState(context)
        provideContent {
            GlanceTheme { PlayerWidgetContent(state) }
        }
    }
}

class MelodiqPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MelodiqPlayerWidget()
}

@Composable
private fun PlayerWidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val padding = 14.dp
    // Cover is a square: limited by the height, and never more than ~45% of the width so the
    // controls always have room beside it.
    val cover = minOf(size.height - padding * 2, size.width * 0.45f).coerceAtLeast(64.dp)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(padding)
            .clickable(openAppAction(context)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetCover(art = state.art, size = cover, radius = 20.dp)
        Spacer(GlanceModifier.width(14.dp))
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NOW PLAYING",
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                if (state.song != null) WidgetFavouriteButton(state.isFavourite)
            }
            Text(text = state.titleText(), maxLines = 2, style = titleStyle(17))
            Text(text = state.artistText(), maxLines = 1, style = subtitleStyle(13))
            Spacer(GlanceModifier.defaultWeight())
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PreviousButton(40.dp)
                Spacer(GlanceModifier.defaultWeight())
                PlayPauseButton(state, 52.dp)
                Spacer(GlanceModifier.defaultWeight())
                NextButton(40.dp)
            }
        }
    }
}
