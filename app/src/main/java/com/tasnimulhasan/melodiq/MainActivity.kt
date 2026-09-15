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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
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

            val requiredPermissions = buildList {
                add(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val permissionsState = rememberMultiplePermissionsState(permissions = requiredPermissions)

            val themeConfig by mainActivityViewModel.themeConfig.collectAsStateWithLifecycle()
            val darkTheme = when (themeConfig) {
                DarkThemeConfig.LIGHT -> false
                DarkThemeConfig.DARK -> true
                DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
            }

            MelodiqTheme(darkTheme = darkTheme) {
                if (permissionsState.allPermissionsGranted) {
                    val shouldOpenPlayer by openPlayerRequested
                    MelodiQApp(
                        appState = appState,
                        openPlayerRequested = shouldOpenPlayer,
                        onOpenPlayerHandled = { openPlayerRequested.value = false },
                    )
                } else {
                    LaunchedEffect(key1 = permissionsState) { permissionsState.launchMultiplePermissionRequest() }
                    PermissionRequestScreen()
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
fun PermissionRequestScreen() {
    Text("Storage and notification permissions are required to play music and show playback controls.")
}