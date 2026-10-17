package com.tasnimulhasan.melodiq.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.designsystem.component.Icon
import com.tasnimulhasan.ui.component.MelodiqSheet
import com.tasnimulhasan.ui.component.SheetHeader
import com.tasnimulhasan.ui.component.SheetInfoRow
import com.tasnimulhasan.ui.component.SheetPrimaryButton

/** Where the person is in the permission conversation. */
internal enum class PermissionStage {
    /** Never asked: explain first, then ask. */
    EXPLAIN,

    /** Asked and refused, but Android will still show its dialog: explain again why it matters. */
    RATIONALE,

    /** Refused with "don't ask again" (or twice): the only way is the system settings. */
    SETTINGS,
}

/**
 * The screen shown until the audio permission is granted. Nothing is requested on its own: a
 * bottom sheet first tells the person WHY, and only its button opens Android's permission
 * dialog. After a refusal the sheet explains again (rationale), and after a permanent refusal
 * it sends them to the system settings.
 */
@Composable
internal fun PermissionScreen(
    stage: PermissionStage,
    /** True while Android's own permission dialog is on screen, so the sheet stays out of the way. */
    requesting: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    // Dismissing the sheet leaves the screen's own button, which reopens it. A new stage reopens it too.
    var sheetShown by rememberSaveable(stage) { mutableStateOf(true) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(com.tasnimulhasan.designsystem.R.drawable.ic_stat_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(52.dp),
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Let MelodiQ find your music",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = when (stage) {
                    PermissionStage.EXPLAIN -> "MelodiQ plays the songs already on your phone. It needs your permission to see them."
                    PermissionStage.RATIONALE -> "Without access to your audio files there is nothing for MelodiQ to play."
                    PermissionStage.SETTINGS -> "Access is turned off. You can switch it on in your phone's settings."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { sheetShown = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    when (stage) {
                        PermissionStage.EXPLAIN -> "Allow access"
                        PermissionStage.RATIONALE -> "Try again"
                        PermissionStage.SETTINGS -> "Open settings"
                    }
                )
            }
        }
    }

    if (sheetShown && !requesting) {
        MelodiqSheet(onDismiss = { sheetShown = false }) { dismiss ->
            when (stage) {
                PermissionStage.EXPLAIN -> {
                    SheetHeader(
                        title = "Allow MelodiQ to access your music",
                        message = "Here is what each permission is for. Your music never leaves your phone.",
                        icon = Icons.Filled.Lock,
                    )
                    Spacer(Modifier.height(10.dp))
                    SheetInfoRow(
                        icon = Icons.Filled.LibraryMusic,
                        title = "Music and audio (required)",
                        text = "Lets MelodiQ find the songs on your phone and build your library, albums and playlists.",
                    )
                    SheetInfoRow(
                        icon = Icons.Filled.Notifications,
                        title = "Notifications (optional)",
                        text = "Shows play, pause and skip controls on your lock screen and in the notification shade.",
                    )
                    Spacer(Modifier.height(16.dp))
                    SheetPrimaryButton(text = "Continue", onClick = {
                        sheetShown = false
                        onRequest()
                    })
                    TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text("Not now") }
                }

                PermissionStage.RATIONALE -> {
                    SheetHeader(
                        title = "MelodiQ needs access to play your music",
                        message = "You chose not to allow it. Without access to your audio files MelodiQ can't see any " +
                            "songs, so there is nothing to play. Please allow \"Music and audio\" on the next screen.",
                        icon = Icons.Filled.LibraryMusic,
                    )
                    Spacer(Modifier.height(22.dp))
                    SheetPrimaryButton(text = "Try again", onClick = {
                        sheetShown = false
                        onRequest()
                    })
                    TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text("Not now") }
                }

                PermissionStage.SETTINGS -> {
                    SheetHeader(
                        title = "Turn on access in Settings",
                        message = "Android won't ask again. Open Settings, choose Permissions, then \"Music and audio\", " +
                            "and select Allow. Then come back to MelodiQ.",
                        icon = Icons.Filled.Settings,
                    )
                    Spacer(Modifier.height(22.dp))
                    SheetPrimaryButton(text = "Open settings", onClick = {
                        sheetShown = false
                        onOpenSettings()
                    })
                    TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text("Not now") }
                }
            }
        }
    }
}
