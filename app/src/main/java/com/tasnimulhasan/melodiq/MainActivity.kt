// app/src/main/java/com/tasnimulhasan/melodiq/MainActivity.kt
package com.tasnimulhasan.melodiq

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.tasnimulhasan.common.constant.AppConstants
import com.tasnimulhasan.designsystem.theme.MelodiqTheme
import com.tasnimulhasan.entity.enums.DarkThemeConfig
import com.tasnimulhasan.melodiq.ui.MelodiQApp
import com.tasnimulhasan.melodiq.ui.rememberMelodiQAppState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val openPlayerRequested = mutableStateOf(false)
    private val mainActivityViewModel: MainActivityViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openPlayerRequested.value = intent?.getBooleanExtra(AppConstants.EXTRA_OPEN_PLAYER, false) == true

        setContent {
            val appState = rememberMelodiQAppState()

            val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            // Notifications are requested together with audio but are NOT required to enter
            // the app - gating on them left users stuck on the permission screen.
            val requested = buildList {
                add(audioPermission)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            val permissionsState = rememberMultiplePermissionsState(permissions = requested)
            val audioGranted = permissionsState.permissions.firstOrNull { it.permission == audioPermission }?.status?.isGranted == true
            var askedOnce by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (!audioGranted && !askedOnce) {
                    askedOnce = true
                    permissionsState.launchMultiplePermissionRequest()
                }
            }
            LaunchedEffect(audioGranted) {
                if (audioGranted) mainActivityViewModel.onStoragePermissionGranted()
            }

            val themeConfig by mainActivityViewModel.themeConfig.collectAsStateWithLifecycle()
            val darkTheme = when (themeConfig) {
                DarkThemeConfig.LIGHT -> false
                DarkThemeConfig.DARK, DarkThemeConfig.AMOLED -> true
                DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
            }
            val amoledBlack = themeConfig == DarkThemeConfig.AMOLED
            val useDynamicColor by mainActivityViewModel.useDynamicColor.collectAsStateWithLifecycle()
            val accentSeedColor by mainActivityViewModel.accentSeedColor.collectAsStateWithLifecycle()

            MelodiqTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor,
                amoledBlack = amoledBlack,
                seedColor = accentSeedColor,
            ) {
                if (audioGranted) {
                    val shouldOpenPlayer by openPlayerRequested
                    MelodiQApp(
                        appState = appState,
                        openPlayerRequested = shouldOpenPlayer,
                        onOpenPlayerHandled = { openPlayerRequested.value = false },
                    )
                } else {
                    val audioStatus = permissionsState.permissions.first { it.permission == audioPermission }.status
                    val permanentlyDenied = askedOnce && !audioStatus.shouldShowRationale
                    PermissionRequestScreen(
                        permanentlyDenied = permanentlyDenied,
                        onGrant = {
                            askedOnce = true
                            permissionsState.launchMultiplePermissionRequest()
                        },
                        onOpenSettings = {
                            startActivity(
                                Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.fromParts("package", packageName, null)
                                )
                            )
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(AppConstants.EXTRA_OPEN_PLAYER, false)) {
            openPlayerRequested.value = true
        }
    }
}

@Composable
fun PermissionRequestScreen(
    permanentlyDenied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
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
                text = if (permanentlyDenied)
                    "Access to audio was turned off. Open settings and allow \"Music and audio\" so MelodiQ can load your songs."
                else
                    "MelodiQ needs access to the audio files on your device to build your library, and notifications to show playback controls.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = if (permanentlyDenied) onOpenSettings else onGrant,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(if (permanentlyDenied) "Open settings" else "Grant access")
            }
        }
    }
}
