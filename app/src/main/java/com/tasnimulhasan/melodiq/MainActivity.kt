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
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
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
            val permissionState = rememberPermissionState(
                permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else Manifest.permission.READ_EXTERNAL_STORAGE
            )
            MelodiqTheme {
                if (permissionState.status.isGranted) {
                    MelodiQApp(appState = appState)
                } else {
                    LaunchedEffect(key1 = permissionState) { permissionState.launchPermissionRequest() }
                    PermissionRequestScreen()
                }
            }
        }
    }
}

@Composable
fun PermissionRequestScreen() {
    Text("Storage permission is required to access music files.")
}