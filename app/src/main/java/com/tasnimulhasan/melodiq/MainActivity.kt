package com.tasnimulhasan.melodiq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tasnimulhasan.designsystem.theme.MelodiqTheme
import com.tasnimulhasan.melodiq.ui.MelodiQApp
import com.tasnimulhasan.melodiq.ui.rememberMelodiQAppState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appState = rememberMelodiQAppState()
            MelodiqTheme {
                MelodiQApp(appState = appState)
            }
        }
    }
}