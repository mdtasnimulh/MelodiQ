package com.tasnimulhasan.featureplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlin.math.absoluteValue
import com.tasnimulhasan.designsystem.R as Res

@Composable
internal fun PlayerScreen(
    musicId: String,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState { viewModel.musics.size }
    val context = LocalContext.current

    // Find the index of the selected music
    val initialPageIndex = viewModel.musics.indexOfFirst { it.songId.toString() == musicId }
    LaunchedEffect(initialPageIndex) {
        pagerState.animateScrollToPage(initialPageIndex)
    }

    val currentPage = pagerState.currentPage
    val currentMusic = viewModel.musics.getOrNull(currentPage)

    Column(modifier = modifier.fillMaxSize()) {
        currentMusic?.let {
            Text(
                text = it.songTitle,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) { page ->
            val selectedMusic = viewModel.musics[page]
            val pageOffset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)

            LaunchedEffect(page) {
                viewModel.loadBitmapIfNeeded(context, page)
            }

            Card(
                modifier = Modifier
                    .graphicsLayer {
                        val scale = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.absoluteValue)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(start = 0.4f, stop = 1f, fraction = 1f - pageOffset.absoluteValue)
                        translationX = lerp(start = 0f, stop = 0f, fraction = 1f - pageOffset.absoluteValue)
                    }
            ) {
                AsyncImage(
                    model = selectedMusic.cover,
                    contentDescription = "Cover art",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape = MaterialTheme.shapes.medium),
                    contentScale = ContentScale.FillHeight,
                    placeholder = painterResource(Res.drawable.ic_launcher_foreground),
                    error = painterResource(Res.drawable.ic_launcher_foreground)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerScreenPreview() {
    PlayerScreen("12345", modifier = Modifier)
}