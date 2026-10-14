package com.tasnimulhasan.melodiq.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import com.tasnimulhasan.melodiq.MainActivity
import com.tasnimulhasan.melodiq.R

/** Opens the app (single top, so an already running instance is reused). */
internal fun openAppAction(context: Context): Action = actionStartActivity(
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
)

/** Square album cover with rounded corners, or a themed placeholder when there is no art. */
@Composable
internal fun WidgetCover(art: Bitmap?, size: Dp, radius: Dp = 18.dp) {
    if (art != null) {
        Image(
            provider = ImageProvider(art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.size(size).cornerRadius(radius),
        )
    } else {
        Box(
            modifier = GlanceModifier
                .size(size)
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(radius),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_music),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                modifier = GlanceModifier.size(size / 2),
            )
        }
    }
}

/** Round icon button. [primary] = filled accent (play/pause); otherwise a tonal button. */
@Composable
internal fun WidgetRoundButton(
    iconRes: Int,
    description: String,
    size: Dp,
    onClick: Action,
    primary: Boolean = false,
) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .background(if (primary) GlanceTheme.colors.primary else GlanceTheme.colors.secondaryContainer)
            .cornerRadius(size / 2)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = description,
            colorFilter = ColorFilter.tint(
                if (primary) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSecondaryContainer
            ),
            modifier = GlanceModifier.size(size * 0.5f),
        )
    }
}

/** Heart toggle with no background, sized as a comfortable tap target. */
@Composable
internal fun WidgetFavouriteButton(isFavourite: Boolean, size: Dp = 36.dp) {
    Box(
        modifier = GlanceModifier.size(size).clickable(actionRunCallback<ToggleFavouriteAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(if (isFavourite) R.drawable.ic_widget_fav_on else R.drawable.ic_widget_fav_off),
            contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
            colorFilter = ColorFilter.tint(
                if (isFavourite) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant
            ),
            modifier = GlanceModifier.size(size * 0.6f),
        )
    }
}

@Composable
internal fun titleStyle(sizeSp: Int = 16) = TextStyle(
    color = GlanceTheme.colors.onSurface,
    fontSize = sizeSp.sp,
    fontWeight = FontWeight.Bold,
)

@Composable
internal fun subtitleStyle(sizeSp: Int = 13) = TextStyle(
    color = GlanceTheme.colors.onSurfaceVariant,
    fontSize = sizeSp.sp,
)

internal fun WidgetState.titleText(): String = song?.songTitle?.takeIf { it.isNotBlank() } ?: "Nothing playing"

internal fun WidgetState.artistText(): String = when {
    song == null -> "Tap play to resume"
    song.artist.isBlank() || song.artist == "<unknown>" -> "Unknown artist"
    else -> song.artist
}

@Composable
internal fun PlayPauseButton(state: WidgetState, size: Dp) = WidgetRoundButton(
    iconRes = if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
    description = if (state.isPlaying) "Pause" else "Play",
    size = size,
    primary = true,
    onClick = actionRunCallback<PlayPauseAction>(),
)

@Composable
internal fun NextButton(size: Dp) = WidgetRoundButton(
    iconRes = R.drawable.ic_widget_next,
    description = "Next",
    size = size,
    onClick = actionRunCallback<NextAction>(),
)

@Composable
internal fun PreviousButton(size: Dp) = WidgetRoundButton(
    iconRes = R.drawable.ic_widget_previous,
    description = "Previous",
    size = size,
    onClick = actionRunCallback<PreviousAction>(),
)
