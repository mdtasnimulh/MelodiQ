package com.tasnimulhasan.melodiq.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import androidx.compose.ui.unit.sp
import androidx.glance.unit.ColorProvider

/**
 * Square "cover art" widget (about 2x2). The album art fills the whole widget and a floating
 * translucent pill at the bottom carries the title, artist and the play/pause button.
 */
class MelodiqCoverWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        context.widgetEntryPoint().widgetUpdater().start()
        val state = loadWidgetState(context)
        provideContent {
            GlanceTheme { CoverWidgetContent(state) }
        }
    }
}

class MelodiqCoverWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MelodiqCoverWidget()
}

@Composable
private fun CoverWidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val pill = ColorProvider(Color(0xB3000000))
    val onPill = ColorProvider(Color.White)
    val onPillVariant = ColorProvider(Color(0xCCFFFFFF))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(28.dp)
            .clickable(openAppAction(context)),
    ) {
        if (state.art != null) {
            Image(
                provider = ImageProvider(state.art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize().cornerRadius(28.dp),
            )
        } else {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    provider = ImageProvider(com.tasnimulhasan.melodiq.R.drawable.ic_widget_music),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                    modifier = GlanceModifier.size(56.dp),
                )
            }
        }
        Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
            Spacer(GlanceModifier.defaultWeight())
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(pill)
                    .cornerRadius(20.dp)
                    .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = state.titleText(),
                        maxLines = 1,
                        style = TextStyle(color = onPill, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = state.artistText(),
                        maxLines = 1,
                        style = TextStyle(color = onPillVariant, fontSize = 11.sp),
                    )
                }
                Spacer(GlanceModifier.width(6.dp))
                PlayPauseButton(state, 38.dp)
            }
        }
    }
}
