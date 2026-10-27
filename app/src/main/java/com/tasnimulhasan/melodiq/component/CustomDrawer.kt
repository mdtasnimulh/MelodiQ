package com.tasnimulhasan.melodiq.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.tasnimulhasan.settings.AppIconManager
import com.tasnimulhasan.settings.AppIconOption
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.melodiq.navigation.CustomNavigationItem
import com.tasnimulhasan.designsystem.R as Res

@Composable
fun CustomDrawer(
    selectedItem: CustomNavigationItem? = null,
    progress: Float = 1f,
    onDrawerCloseClick: () -> Unit,
    onAboutClick: () -> Unit,
    onFeedBackClick: () -> Unit,
    onFavouriteClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onRadioClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    // The logo follows the launcher icon the user picked in Settings > App icon.
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedIcon by AppIconManager.selected.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { AppIconManager.refresh(context) }
    val logoRes = remember(selectedIcon) {
        AppIconManager.previewResId(context, selectedIcon ?: AppIconOption.DEFAULT)
            .takeIf { it != 0 } ?: Res.drawable.ic_logo_main
    }

    val appName = remember {
        runCatching { context.applicationInfo.loadLabel(context.packageManager).toString() }
            .getOrDefault("MelodiQ")
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(fraction = 0.66f)
            .padding(start = 28.dp, end = 8.dp)
            .graphicsLayer { alpha = progress; translationX = -40f * (1f - progress) },
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        // The launcher icon in a squircle-style rounded square.
        Image(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(26.dp)),
            painter = painterResource(id = logoRes),
            contentDescription = "App Logo"
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Hi!",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 18.sp,
        )
        Text(
            text = appName,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(36.dp))
        CustomNavigationItem.entries.toTypedArray().forEach { navigationItem ->
            CustomNavigationItemView(
                navigationItem = navigationItem,
                selected = navigationItem == selectedItem,
                onClick = {
                    when(navigationItem) {
                        CustomNavigationItem.ABOUT -> {
                            onAboutClick.invoke()
                            onDrawerCloseClick.invoke()
                        }
                        CustomNavigationItem.FEEDBACK -> {
                            onFeedBackClick.invoke()
                            onDrawerCloseClick.invoke()
                        }
                        CustomNavigationItem.FAVOURITE -> {
                            onFavouriteClick.invoke()
                            onDrawerCloseClick.invoke()
                        }
                        CustomNavigationItem.LIBRARY -> {
                            onLibraryClick.invoke()
                            onDrawerCloseClick.invoke()
                        }
                        CustomNavigationItem.RADIO -> {
                            onRadioClick.invoke()
                            onDrawerCloseClick.invoke()
                        }
                        CustomNavigationItem.SETTINGS -> {
                            onSettingsClick.invoke()
                            onDrawerCloseClick.invoke()
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}