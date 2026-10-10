package com.tasnimulhasan.featureabout

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val DEV_NAME = "Md. Tasnimul Hasan"
private const val DEV_EMAIL = "mdtasnimulh@gmail.com"

private data class FeatureItem(val icon: ImageVector, val title: String, val description: String)

private val features = listOf(
    FeatureItem(Icons.Filled.Folder, "Fast local library", "Scans your device once, stores it in a database and stays in sync as files change. Browse songs, albums and folders."),
    FeatureItem(Icons.Filled.Home, "Smart Home", "Recently played, most played, recently added and a \"Not played yet\" list so forgotten songs get a second chance."),
    FeatureItem(Icons.Filled.MusicNote, "Expressive player", "Full-screen player with half, circle or full cover styles, a draggable mini player and a close button."),
    FeatureItem(Icons.Filled.GraphicEq, "Audio visualizer", "Bars, waveform or circular visualizer that moves with the music."),
    FeatureItem(Icons.Filled.Subtitles, "Lyrics", "Synced lyrics that follow the song as it plays."),
    FeatureItem(Icons.Filled.Equalizer, "Equalizer & presets", "Tune the sound with a built-in equalizer, preset chips and one-tap reset to flat."),
    FeatureItem(Icons.Filled.Tune, "Crossfade & volume normalization", "Smooth transitions between tracks and even loudness using ReplayGain tags."),
    FeatureItem(Icons.AutoMirrored.Filled.QueueMusic, "Queue & playlists", "Manage the play queue, create playlists and mark favourites."),
    FeatureItem(Icons.Filled.Edit, "Tag & file editor", "Edit title, artist, album and more, change embedded cover art, rename, move or delete files."),
    FeatureItem(Icons.Filled.Notifications, "Rich notification", "Cover art, previous / play / next, plus repeat and favourite buttons right in the notification."),
    FeatureItem(Icons.Filled.Palette, "Themes", "Light, dark and AMOLED black, wallpaper-based dynamic colour, album-art colour or a fixed accent."),
    FeatureItem(Icons.Filled.Backup, "Backup & restore", "Export playlists, favourites, play history and settings to a file and restore them anytime."),
)

@Composable
internal fun AboutRoute(modifier: Modifier = Modifier) {
    AboutScreen(modifier)
}

@Composable
internal fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Reveal(0) { HeroCard(versionName) }

        Reveal(1) {
            Column {
                Text(
                    "What MelodiQ can do",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        features.forEachIndexed { index, item ->
                            FeatureRow(item)
                            if (index != features.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }

        Reveal(2) { DeveloperCard(context) }

        Reveal(3) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = "Copyright © 2027 $DEV_NAME.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "All rights reserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Fade + slide in, staggered by [index], so the page builds up instead of popping in. */
@Composable
private fun Reveal(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 70L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 8 },
    ) { content() }
}

@Composable
private fun HeroCard(versionName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "MelodiQ",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "A beautiful, fast and private music player for the songs already on your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 14.dp),
            ) {
                Text(
                    "Version $versionName",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(item: FeatureItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeveloperCard(context: Context) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "About the developer",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text("TH", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
            }
            Text(DEV_NAME, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 12.dp))
            Text("Mobile App Developer", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(16.dp))
            InfoRow(Icons.Filled.LocationOn, "Dhaka, Bangladesh")
            InfoRow(Icons.Filled.PhoneAndroid, "Available for any kind of mobile app development")
            InfoRow(Icons.Filled.Email, DEV_EMAIL, onClick = { sendEmail(context) })

            Spacer(Modifier.height(16.dp))
            Button(onClick = { sendEmail(context) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Get in touch", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Opens Gmail's compose screen addressed to the developer; falls back to any mail app. */
private fun sendEmail(context: Context) {
    val uri = Uri.parse("mailto:$DEV_EMAIL?subject=" + Uri.encode("MelodiQ"))
    val gmail = Intent(Intent.ACTION_SENDTO, uri).setPackage("com.google.android.gm")
    try {
        context.startActivity(gmail)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SENDTO, uri), "Send email"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No email app found. Write to $DEV_EMAIL", Toast.LENGTH_LONG).show()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    AboutScreen(modifier = Modifier)
}
