package com.tasnimulhasan.melodiq.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tasnimulhasan.melodiq.R

/**
 * Two iPod-styled widgets (graphite and silver). The click wheel is left out on purpose: the cover
 * sits on the "screen" with the song and artist, a favourite heart is on top, and previous / play /
 * next are the round buttons underneath. Both read the same [WidgetState] as the other widgets.
 */
class MelodiqIpodDarkWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        context.widgetEntryPoint().widgetUpdater().start()
        val state = loadWidgetState(context)
        provideContent { IpodWidgetContent(state, silver = false) }
    }
}

class MelodiqIpodDarkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MelodiqIpodDarkWidget()
}

class MelodiqIpodSilverWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        context.widgetEntryPoint().widgetUpdater().start()
        val state = loadWidgetState(context)
        provideContent { IpodWidgetContent(state, silver = true) }
    }
}

class MelodiqIpodSilverWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MelodiqIpodSilverWidget()
}

private val ScreenText = ColorProvider(Color.White)
private val ScreenTextSoft = ColorProvider(Color(0xDDFFFFFF))
private val ScreenLabel = ColorProvider(Color(0xAAFFFFFF))

@Composable
private fun IpodWidgetContent(state: WidgetState, silver: Boolean) {
    val context = LocalContext.current
    val size = LocalSize.current
    val pad = 8.dp
    val inner = 7.dp
    val controlsWidth = if (size.width >= 240.dp) 98.dp else 86.dp
    val screenWidth = size.width - pad * 2 - controlsWidth - 6.dp
    val screenHeight = size.height - pad * 2
    // Square cover: limited by the screen's height and about half its width, so the text keeps room.
    val cover = minOf(screenHeight - inner * 2, (screenWidth - inner * 2) * 0.5f).coerceAtLeast(36.dp)
    val labelColor = ColorProvider(if (silver) Color(0xFF6C6C70) else Color(0xCCFFFFFF))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(if (silver) R.drawable.widget_ipod_body_silver else R.drawable.widget_ipod_body_dark))
            .padding(pad)
            .clickable(openAppAction(context)),
    ) {
        Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            // The screen: cover on the left, what is playing on the right.
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .background(ImageProvider(R.drawable.widget_ipod_screen))
                    .padding(inner),
            ) {
                Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    IpodCover(state.art, cover)
                    Spacer(GlanceModifier.width(8.dp))
                    Column(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                state.song == null -> "MELODIQ"
                                state.isPlaying -> "NOW PLAYING"
                                else -> "PAUSED"
                            },
                            maxLines = 1,
                            style = TextStyle(color = ScreenLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = state.titleText(),
                            maxLines = 2,
                            style = TextStyle(color = ScreenText, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = state.artistText(),
                            maxLines = 1,
                            style = TextStyle(color = ScreenTextSoft, fontSize = 11.sp),
                        )
                    }
                }
            }
            Spacer(GlanceModifier.width(6.dp))
            // Where the wheel would be: favourite on top, previous / play / next at the bottom.
            Column(
                modifier = GlanceModifier.width(controlsWidth).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    IpodHeart(state.isFavourite, silver)
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = "MUSIC",
                    maxLines = 1,
                    style = TextStyle(color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(GlanceModifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IpodButton(R.drawable.ic_widget_previous, "Previous", 27.dp, silver, actionRunCallback<PreviousAction>())
                    Spacer(GlanceModifier.width(4.dp))
                    IpodButton(
                        if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                        if (state.isPlaying) "Pause" else "Play",
                        33.dp, silver, actionRunCallback<PlayPauseAction>(),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    IpodButton(R.drawable.ic_widget_next, "Next", 27.dp, silver, actionRunCallback<NextAction>())
                }
            }
        }
    }
}

@Composable
private fun IpodCover(art: Bitmap?, size: Dp) {
    if (art != null) {
        Image(
            provider = ImageProvider(art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.size(size).cornerRadius(6.dp),
        )
    } else {
        Box(
            modifier = GlanceModifier
                .size(size)
                .background(ColorProvider(Color(0xFF6B5855)))
                .cornerRadius(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_music),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ColorProvider(Color(0xCCFFFFFF))),
                modifier = GlanceModifier.size(size / 2),
            )
        }
    }
}

/** A round iPod-style button: dark disc with a white icon, or silver disc with a dark icon. */
@Composable
private fun IpodButton(iconRes: Int, description: String, size: Dp, silver: Boolean, onClick: Action) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .background(ImageProvider(if (silver) R.drawable.widget_ipod_btn_silver else R.drawable.widget_ipod_btn_dark))
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = description,
            colorFilter = ColorFilter.tint(ColorProvider(if (silver) Color(0xFF2B2B2E) else Color.White)),
            modifier = GlanceModifier.size(size * 0.56f),
        )
    }
}

@Composable
private fun IpodHeart(isFavourite: Boolean, silver: Boolean) {
    Box(
        modifier = GlanceModifier.size(32.dp).clickable(actionRunCallback<ToggleFavouriteAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(if (isFavourite) R.drawable.ic_widget_fav_on else R.drawable.ic_widget_fav_off),
            contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
            colorFilter = ColorFilter.tint(
                ColorProvider(
                    when {
                        isFavourite -> Color(0xFFFF4D6D)
                        silver -> Color(0xFF6C6C70)
                        else -> Color(0xCCFFFFFF)
                    }
                )
            ),
            modifier = GlanceModifier.size(20.dp),
        )
    }
}
