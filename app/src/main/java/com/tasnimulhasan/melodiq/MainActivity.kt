package com.tasnimulhasan.melodiq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tasnimulhasan.designsystem.theme.MelodiqTheme
import com.tasnimulhasan.melodiq.ui.MelodiqApp
import com.tasnimulhasan.melodiq.ui.rememberMelodiqAppState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appState = rememberMelodiqAppState()
            MelodiqTheme {
                MelodiqApp(appState = appState)
            }
        }
    }
}