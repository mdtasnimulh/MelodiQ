package com.tasnimulhasan.featureplayer.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
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
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val (previousButton, playPauseButton, nextButton) = createRefs()

        IconButton(
            modifier = Modifier
                .background(
                    color = LightOrange,
                    shape = RoundedCornerShape(15.dp)
                )
                .constrainAs(playPauseButton) {
                    top.linkTo(parent.top, margin = 8.dp)
                    bottom.linkTo(parent.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.value(120.dp)
                    height = Dimension.value(70.dp)
                },
            onClick = onPlayPauseClick
        ) {
            Icon(
                modifier = Modifier.width(50.dp).height(50.dp),
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                tint = PeaceOrange,
                contentDescription = "Play Pause Icon"
            )
        }

        IconButton(
            modifier = Modifier
                .background(
                    color = PeaceOrange,
                    shape = RoundedCornerShape(50.dp)
                )
                .constrainAs(previousButton) {
                    top.linkTo(playPauseButton.top)
                    bottom.linkTo(playPauseButton.bottom)
                    end.linkTo(playPauseButton.start, margin = 8.dp)
                    width = Dimension.value(70.dp)
                    height = Dimension.value(70.dp)
                },
            onClick = onPreviousClick
        ) {
            Icon(
                modifier = Modifier.width(50.dp).height(50.dp),
                imageVector = Icons.Default.SkipPrevious,
                tint = LightOrange.copy(alpha = 0.75f),
                contentDescription = "Previous Icon"
            )
        }

        IconButton(
            modifier = Modifier
                .background(
                    color = PeaceOrange,
                    shape = RoundedCornerShape(50.dp)
                )
                .constrainAs(nextButton) {
                    top.linkTo(playPauseButton.top)
                    bottom.linkTo(playPauseButton.bottom)
                    start.linkTo(playPauseButton.end, margin = 8.dp)
                    width = Dimension.value(70.dp)
                    height = Dimension.value(70.dp)
                },
            onClick = onNextClick
        ) {
            Icon(
                modifier = Modifier.width(50.dp).height(50.dp),
                imageVector = Icons.Default.SkipNext,
                tint = LightOrange.copy(alpha = 0.75f),
                contentDescription = "Next Icon"
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPlayPauseControlButton() {
    PlayPauseControlButton(
        onPlayPauseClick = { /* Handle play/pause click */ },
        onPreviousClick = { /* Handle previous click */ },
        onNextClick = { /* Handle next click */ },
        isPlaying = false
    )
}