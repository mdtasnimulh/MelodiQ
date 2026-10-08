package com.tasnimulhasan.featureplayer.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Material 3 Expressive motion tokens. Expressive motion is spring-based rather than
 * duration/easing-based: "spatial" springs move or reshape things and are allowed a little
 * overshoot (that bounce is the expressive character), "effects" springs fade/recolor and
 * never overshoot. Values are the published expressive defaults.
 *
 * These are applied directly instead of through Material's experimental expressive theme
 * APIs, whose exact surface differs between Compose versions - behavior matches the spec
 * without tying the build to one of them.
 */
object ExpressiveMotion {
    fun <T> spatial(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
    fun <T> fastSpatial(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)
    fun <T> slowSpatial(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 200f)
    fun <T> effects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
}

/** Staggered rise-and-fade on first appearance. [index] orders the stagger (70ms apart). */
@Composable
fun Modifier.expressiveEntrance(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        progress.animateTo(1f, ExpressiveMotion.spatial())
    }
    return this.graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 40.dp.toPx()
    }
}

/**
 * Round icon button whose shape morphs from a circle toward a rounded square while pressed
 * (with a small squash), springing back on release - the expressive "shape change on press".
 */
@Composable
fun ExpressiveIconButton(
    icon: ImageVector,
    contentDescription: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    iconScale: Float = 1f,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) size * 0.28f else size / 2,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "iconButtonCorner"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "iconButtonScale"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(corner))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier
                .size(size * 0.5f)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
    }
}

@Composable
fun PlayerTopBar(
    albumName: String,
    onClose: () -> Unit,
    onQueue: () -> Unit,
    onDark: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val container = if (onDark) Color.Black.copy(alpha = 0.35f) else accent.copy(alpha = 0.10f)
    val content = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExpressiveIconButton(Icons.Default.KeyboardArrowDown, "Close player", container, content, onClose)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "NOW PLAYING",
                color = content.copy(alpha = 0.7f),
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = albumName,
                color = content,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ExpressiveIconButton(Icons.AutoMirrored.Filled.QueueMusic, "Open queue", container, content, onQueue)
    }
}

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    onDark: Boolean,
    accent: Color,
) {
    // Pops (shrinks, then springs past full size) every time it becomes a favorite.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(isFavorite) {
        if (isFavorite) {
            pop.snapTo(0.55f)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 400f))
        }
    }
    ExpressiveIconButton(
        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
        container = if (onDark) Color.White.copy(alpha = 0.16f) else accent.copy(alpha = 0.10f),
        content = if (isFavorite) Color(0xFFFF4D6D) else if (onDark) Color.White else MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
        iconScale = pop.value,
        size = 48.dp,
    )
}

/**
 * Transport row: seek back, previous, a large play/pause that morphs between a pill (playing)
 * and a squircle (paused) with a bouncy spring, next, seek forward.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExpressiveTransportControls(
    isPlaying: Boolean,
    accent: Color,
    onAccent: Color,
    onDark: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tonal = if (onDark) Color.White.copy(alpha = 0.16f) else accent.copy(alpha = 0.12f)
    val onTonal = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val playCorner by animateDpAsState(
        targetValue = if (pressed) 18.dp else if (isPlaying) 36.dp else 26.dp,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "playCorner"
    )
    val playScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "playScale"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExpressiveIconButton(Icons.Default.Replay5, "Back 5 seconds", Color.Transparent, onTonal, onSeekBack, size = 40.dp)
        ExpressiveIconButton(Icons.Default.SkipPrevious, "Previous", tonal, onTonal, onPrevious, size = 54.dp)

        Box(
            modifier = Modifier
                .width(84.dp)
                .height(72.dp)
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                }
                .clip(RoundedCornerShape(playCorner))
                .background(accent)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(ExpressiveMotion.effects()) + scaleIn(ExpressiveMotion.fastSpatial(), initialScale = 0.5f))
                        .togetherWith(fadeOut(ExpressiveMotion.effects()) + scaleOut(ExpressiveMotion.effects(), targetScale = 0.5f))
                },
                label = "playPauseIcon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = onAccent,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        ExpressiveIconButton(Icons.Default.SkipNext, "Next", tonal, onTonal, onNext, size = 54.dp)
        ExpressiveIconButton(Icons.Default.Forward5, "Forward 5 seconds", Color.Transparent, onTonal, onSeekForward, size = 40.dp)
    }
}

data class PlayerAction(
    val icon: ImageVector,
    val label: String,
    val active: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Secondary actions as one scrollable row of labelled chips - replaces two rows of seven
 * unlabelled icons pinned to the bottom. Active chips (repeat on, sleep timer running) fill
 * with the accent color; chips squash into a rounded square while pressed.
 */
@Composable
fun PlayerActionChips(
    actions: List<PlayerAction>,
    accent: Color,
    onAccent: Color,
    onDark: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.forEach { action ->
            ActionChip(action, accent, onAccent, onDark)
        }
    }
}

@Composable
private fun ActionChip(action: PlayerAction, accent: Color, onAccent: Color, onDark: Boolean) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) 12.dp else 22.dp,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "chipCorner"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "chipScale"
    )
    val idle = if (onDark) Color.White.copy(alpha = 0.16f) else accent.copy(alpha = 0.10f)
    val container by animateColorAsState(
        targetValue = if (action.active) accent else idle,
        animationSpec = ExpressiveMotion.effects(),
        label = "chipContainer"
    )
    val content = if (action.active) onAccent else if (onDark) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(corner))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = action.onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(action.icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = action.label,
            color = content,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
