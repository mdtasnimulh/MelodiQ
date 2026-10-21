// app/src/main/java/com/tasnimulhasan/melodiq/MainActivity.kt
package com.tasnimulhasan.melodiq

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import com.tasnimulhasan.entity.enums.PlayerType
import com.tasnimulhasan.melodiq.ipod.IpodApp
import com.tasnimulhasan.melodiq.onboarding.OnboardingPrefs
import com.tasnimulhasan.melodiq.onboarding.OnboardingScreen
import com.tasnimulhasan.melodiq.permission.PermissionScreen
import com.tasnimulhasan.melodiq.permission.PermissionStage
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.tasnimulhasan.melodiq.shortcut.AppShortcuts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Button
import com.tasnimulhasan.designsystem.component.Icon
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
import androidx.compose.runtime.remember
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

        // Long-press launcher shortcuts. Published once per launch off the main thread; a
        // shortcut tap that cold-starts the app arrives here, one that hits a running
        // activity arrives in onNewIntent.
        lifecycleScope.launch(Dispatchers.Default) { AppShortcuts.publish(applicationContext) }
        if (savedInstanceState == null) handleShortcut(intent)

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
            // True while Android's own permission dialog is showing (the explanation sheet stays hidden).
            var requesting by remember { mutableStateOf(false) }
            // The rationale / settings views are for AFTER a refusal only. `denied` flips when Android's
            // dialog actually comes back with "no"; until then (first launch, and while the dialog is on
            // screen) the person only ever sees the explanation.
            var denied by rememberSaveable { mutableStateOf(false) }
            var canAskAgain by rememberSaveable { mutableStateOf(true) }
            val permissionsState = rememberMultiplePermissionsState(permissions = requested) { result ->
                requesting = false
                if (result[audioPermission] == false) {
                    denied = true
                    // Read it from the system right now: the Compose permission state can lag a frame behind.
                    canAskAgain = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                        this@MainActivity, audioPermission,
                    )
                }
            }
            val audioGranted = permissionsState.permissions.firstOrNull { it.permission == audioPermission }?.status?.isGranted == true

            // First launch shows the intro before anything is asked. People who already granted access
            // (installed before the intro existed) never see it.
            var onboardingDone by rememberSaveable {
                mutableStateOf(audioGranted || OnboardingPrefs.isDone(applicationContext))
            }
            LaunchedEffect(Unit) {
                if (audioGranted) OnboardingPrefs.markDone(applicationContext)
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
            val uiStyle by mainActivityViewModel.uiStyle.collectAsStateWithLifecycle()

            MelodiqTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor,
                amoledBlack = amoledBlack,
                seedColor = accentSeedColor,
                uiStyle = uiStyle,
            ) {
                val playerType by mainActivityViewModel.playerType.collectAsStateWithLifecycle()

                // Player A uses the default system-bar handling; the iPod UI (Player B) sets its own
                // bar icon colours from the chosen body colour (white / black / AMOLED).
                DisposableEffect(playerType, uiStyle) {
                    if (playerType != PlayerType.IPOD) {
                        if (uiStyle == com.tasnimulhasan.entity.enums.UiStyle.GLASS) {
                            // The glass gradient is always dark/colourful, so the bar icons stay light.
                            enableEdgeToEdge(
                                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                            )
                        } else {
                            enableEdgeToEdge()
                        }
                    }
                    onDispose { }
                }

                if (!onboardingDone) {
                    OnboardingScreen(
                        onFinish = {
                            OnboardingPrefs.markDone(applicationContext)
                            onboardingDone = true
                        },
                    )
                } else if (audioGranted) {
                    val shouldOpenPlayer by openPlayerRequested
                    when (playerType) {
                        // Saved choice not read yet (a few ms): show nothing rather than the wrong UI.
                        null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                        PlayerType.CLASSIC -> MelodiQApp(
                            appState = appState,
                            openPlayerRequested = shouldOpenPlayer,
                            onOpenPlayerHandled = { openPlayerRequested.value = false },
                        )
                        PlayerType.IPOD -> IpodApp(
                            openNowPlayingRequested = shouldOpenPlayer,
                            onOpenNowPlayingHandled = { openPlayerRequested.value = false },
                            onSwitchToClassic = { mainActivityViewModel.setPlayerType(PlayerType.CLASSIC) },
                        )
                    }
                } else {
                    // Never ask unprompted: explain first (EXPLAIN), explain again after a refusal
                    // (RATIONALE), and point to Settings once Android won't show its dialog any more.
                    val stage = when {
                        !denied -> PermissionStage.EXPLAIN
                        canAskAgain -> PermissionStage.RATIONALE
                        else -> PermissionStage.SETTINGS
                    }
                    PermissionScreen(
                        stage = stage,
                        requesting = requesting,
                        onRequest = {
                            requesting = true
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

    private fun handleShortcut(intent: Intent?) {
        val action = intent?.action
        if (!AppShortcuts.isShortcutAction(action)) return
        // Consume it so a later recreate (rotation, theme change) doesn't run it again.
        intent?.action = null
        lifecycleScope.launch {
            if (mainActivityViewModel.runShortcut(action!!)) openPlayerRequested.value = true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcut(intent)
        if (intent.getBooleanExtra(AppConstants.EXTRA_OPEN_PLAYER, false)) {
            openPlayerRequested.value = true
        }
    }
}
