package com.tasnimulhasan.melodiq.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.melodiq.navigation.CustomNavigationItem
import com.tasnimulhasan.designsystem.R as Res

@Composable
fun CustomDrawer(
    onDrawerCloseClick: () -> Unit,
    onAboutClick: () -> Unit,
    onFeedBackClick: () -> Unit,
    onFavouriteClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(fraction = 0.6f)
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
        ) {
            IconButton(onClick = onDrawerCloseClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back Arrow Icon",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Image(
            modifier = Modifier.size(100.dp),
            painter = painterResource(id = Res.drawable.ic_launcher_foreground),
            contentDescription = "App Logo"
        )
        Spacer(modifier = Modifier.height(40.dp))
        CustomNavigationItem.entries.toTypedArray().forEach { navigationItem ->
            CustomNavigationItemView(
                navigationItem = navigationItem,
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
                        CustomNavigationItem.SETTINGS -> {
                            onSettingsClick.invoke()
                            onDrawerCloseClick.invoke()
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}