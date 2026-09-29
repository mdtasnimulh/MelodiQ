package com.tasnimulhasan.designsystem.component

import com.tasnimulhasan.designsystem.R as Res
import androidx.annotation.StringRes
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tasnimulhasan.designsystem.icon.MelodiqIcons
import com.tasnimulhasan.designsystem.theme.MelodiqTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelodiqTopAppBar(
    @StringRes titleRes: Int,
    navigationIcon: ImageVector,
    navigationIconContentDescription: String,
    modifier: Modifier = Modifier,
    // Optional now: the app has no use for a right-side top bar action (Settings already
    // lives in the drawer, reached via [navigationIcon]) - passing null renders a plain bar
    // with just the title and navigation icon.
    actionIcon: ImageVector? = null,
    actionIconsContentDescription: String = "",
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    onNavigationClick: () -> Unit = {},
    onActionClick: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(text = stringResource(id = titleRes)) },
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = navigationIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            if (actionIcon != null) {
                IconButton(onClick = onActionClick) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionIconsContentDescription,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        colors = colors,
        modifier = modifier.testTag("melodicTopAppBar"),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview("Top App Bar")
@Composable
private fun MelodiqTopAppBarPreview() {
    MelodiqTheme {
        MelodiqTopAppBar(
            titleRes = Res.string.app_name,
            navigationIcon = MelodiqIcons.NavigationMenu,
            navigationIconContentDescription = "Navigation Icon",
        )
    }
}