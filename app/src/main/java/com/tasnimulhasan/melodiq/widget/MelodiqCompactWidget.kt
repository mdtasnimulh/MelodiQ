package com.tasnimulhasan.melodiq.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
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
import androidx.glance.layout.padding
import androidx.glance.layout.width

/** Slim one-row widget (about 4x1): cover, title / artist, play-pause and next. */
class MelodiqCompactWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // A widget was just added/updated: make sure live updates are running this process.
        context.widgetEntryPoint().widgetUpdater().start()
        val state = loadWidgetState(context)
        provideContent {
            GlanceTheme { CompactWidgetContent(state) }
        }
    }
}

class MelodiqCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MelodiqCompactWidget()
}

@Composable
private fun CompactWidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val padding = 8.dp
    val cover = (size.height - padding * 2).coerceIn(40.dp, 72.dp)
    // Very narrow widgets drop the next button instead of squeezing the title to nothing.
    val showNext = size.width >= 200.dp

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(padding)
            .clickable(openAppAction(context)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetCover(art = state.art, size = cover, radius = 16.dp)
        Spacer(GlanceModifier.width(10.dp))
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.glance.text.Text(text = state.titleText(), maxLines = 1, style = titleStyle(15))
            androidx.glance.text.Text(text = state.artistText(), maxLines = 1, style = subtitleStyle(12))
        }
        Spacer(GlanceModifier.width(8.dp))
        PlayPauseButton(state, 44.dp)
        if (showNext) {
            Spacer(GlanceModifier.width(6.dp))
            NextButton(36.dp)
        }
    }
}
