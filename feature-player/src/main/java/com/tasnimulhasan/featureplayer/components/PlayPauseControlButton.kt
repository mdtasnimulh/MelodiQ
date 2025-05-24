package com.tasnimulhasan.featureplayer.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.tasnimulhasan.designsystem.theme.DarkGreen
import com.tasnimulhasan.designsystem.theme.LightOrange
import com.tasnimulhasan.designsystem.theme.PeaceOrange
import com.tasnimulhasan.designsystem.theme.violet70
import com.tasnimulhasan.designsystem.theme.violet80

@Composable
fun PlayPauseControlButton(
    isPlaying: Boolean,
    playButtonColor: Color,
    buttonColor: Color,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    // Animate the corner radius based on isPlaying state
    val cornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 40.dp else 10.dp,
        animationSpec = tween(durationMillis = 300),
        label = "CornerRadiusAnimation"
    )

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val (previousButton, playPauseButton, nextButton) = createRefs()

        IconButton(
            modifier = Modifier
                .background(
                    color = playButtonColor,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .constrainAs(playPauseButton) {
                    top.linkTo(parent.top, margin = 8.dp)
                    bottom.linkTo(parent.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.value(150.dp)
                    height = Dimension.value(80.dp)
                },
            onClick = onPlayPauseClick
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300)))
                        .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300)))
                        .using(SizeTransform(clip = false))
                },
                label = "PlayPauseIconAnimation"
            ) { targetIsPlaying ->
                Icon(
                    modifier = Modifier.width(50.dp).height(50.dp),
                    imageVector = if (targetIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    tint = buttonColor,
                    contentDescription = "Play Pause Icon"
                )
            }
        }

        IconButton(
            modifier = Modifier
                .background(
                    color = playButtonColor.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(25.dp)
                )
                .constrainAs(previousButton) {
                    top.linkTo(playPauseButton.top)
                    bottom.linkTo(playPauseButton.bottom)
                    end.linkTo(playPauseButton.start, margin = 8.dp)
                    width = Dimension.value(80.dp)
                    height = Dimension.value(70.dp)
                },
            onClick = onPreviousClick
        ) {
            Icon(
                modifier = Modifier.width(40.dp).height(40.dp),
                imageVector = Icons.Default.SkipPrevious,
                tint = playButtonColor.copy(alpha = 0.75f),
                contentDescription = "Previous Icon"
            )
        }

        IconButton(
            modifier = Modifier
                .background(
                    color = playButtonColor.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(25.dp)
                )
                .constrainAs(nextButton) {
                    top.linkTo(playPauseButton.top)
                    bottom.linkTo(playPauseButton.bottom)
                    start.linkTo(playPauseButton.end, margin = 8.dp)
                    width = Dimension.value(80.dp)
                    height = Dimension.value(70.dp)
                },
            onClick = onNextClick
        ) {
            Icon(
                modifier = Modifier.width(40.dp).height(40.dp),
                imageVector = Icons.Default.SkipNext,
                tint = playButtonColor.copy(alpha = 0.75f),
                contentDescription = "Next Icon"
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPlayPauseControlButton() {
    var isPlaying by remember { mutableStateOf(false) }
    PlayPauseControlButton(
        onPlayPauseClick = { isPlaying = !isPlaying },
        onPreviousClick = { /* Handle previous click */ },
        onNextClick = { /* Handle next click */ },
        isPlaying = isPlaying,
        playButtonColor = PeaceOrange,
        buttonColor = LightOrange
    )
}