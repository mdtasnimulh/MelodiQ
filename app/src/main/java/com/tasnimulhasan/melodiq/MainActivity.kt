package com.tasnimulhasan.melodiq

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.tasnimulhasan.designsystem.theme.MelodiqTheme
import com.tasnimulhasan.melodiq.ui.MelodiQApp
import com.tasnimulhasan.melodiq.ui.rememberMelodiQAppState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                // POST_NOTIFICATIONS is a runtime permission only from API 33+; below that
                // the notification just shows once POST_NOTIFICATIONS is manifest-declared.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val permissionsState = rememberMultiplePermissionsState(permissions = requiredPermissions)

            MelodiqTheme {
                if (permissionsState.allPermissionsGranted) {
                    MelodiQApp(appState = appState)
                } else {
                    LaunchedEffect(key1 = permissionsState) { permissionsState.launchMultiplePermissionRequest() }
                    PermissionRequestScreen()
                }
            }
        }
    }
}

@Composable
fun PermissionRequestScreen() {
    Text("Storage and notification permissions are required to play music and show playback controls.")
}