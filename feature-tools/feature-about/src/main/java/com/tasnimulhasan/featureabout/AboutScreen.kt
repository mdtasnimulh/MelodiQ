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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
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
import com.tasnimulhasan.designsystem.component.Icon
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
import androidx.compose.ui.unit.sp
import com.tasnimulhasan.ui.component.MelodiqSheet
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.NewReleases
import androidx.activity.compose.BackHandler

private const val DEV_NAME = "Md. Tasnimul Hasan"
private const val DEV_EMAIL = "mdtasnimulh@gmail.com"

private data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    /** Things added in this version (not in the previous one). */
    val new: List<String> = emptyList(),
    /** Problems fixed in this version. */
    val fixed: List<String> = emptyList(),
)

private val features = listOf(
    FeatureItem(Icons.Filled.Folder, "Fast local library", "Scans your device once, stores it in a database and stays in sync as files change. Browse songs, albums and folders.", new = listOf("Albums use large rounded cover cards"), fixed = listOf("Permission explanation only appears after a permission is denied, not behind the first system dialog")),
    FeatureItem(Icons.Filled.Home, "Smart Home", "Recently played, most played, recently added and a \"Not played yet\" list so forgotten songs get a second chance.", new = listOf("Recently played, Most played and Favourites headers always show, with a friendly placeholder until you have something there"), fixed = listOf("The first song in your list no longer appears in Recently / Most played before you have played anything")),
    FeatureItem(Icons.Filled.MusicNote, "Expressive player", "Full-screen player with half, circle or full cover styles, a draggable mini player and a close button.", new = listOf("Visualizer now wraps around a circular cover as a ring"), fixed = listOf()),
    FeatureItem(Icons.Filled.GraphicEq, "Audio visualizer", "Bars, waveform or circular visualizer that moves with the music.", new = listOf("Player visualizer now matches the Settings previews: smooth bars, flowing waveform and spoke ring", "Bars and waveform can circle the round cover art"), fixed = listOf("Animation is eased every frame so it no longer steps")),
    FeatureItem(Icons.Filled.Subtitles, "Lyrics", "Synced lyrics that follow the song as it plays."),
    FeatureItem(Icons.Filled.Equalizer, "Equalizer & presets", "Tune the sound with a built-in equalizer, preset chips and one-tap reset to flat.", new = listOf("10-band equalizer on every device, no support check", "5 / 10 band toggle", "Built-in presets for both layouts"), fixed = listOf()),
    FeatureItem(Icons.Filled.Tune, "Crossfade & volume normalization", "Smooth transitions between tracks and even loudness using ReplayGain tags."),
    FeatureItem(Icons.AutoMirrored.Filled.QueueMusic, "Queue & playlists", "Manage the play queue, create playlists (even straight from a song's menu) and mark favourites.", new = listOf("Delete a playlist from the Playlists screen (MelodiQ and iPod)", "Press and hold a song in a playlist, then drag it up or down to reorder", "Drag to reorder the queue too", "Modern stacked-cover playlist cards"), fixed = listOf("Long-pressing a song in a playlist no longer deletes it", "The cross on queue items now removes the right song", "Queue moves and removals now act on the queue that is actually playing")),
    FeatureItem(Icons.Filled.Edit, "Tag & file editor", "Edit title, artist, album and more, change embedded cover art, rename, move or delete files."),
    FeatureItem(Icons.Filled.Notifications, "Rich notification", "Cover art, previous / play / next, plus repeat and favourite buttons right in the notification."),
    FeatureItem(Icons.Filled.AutoAwesome, "Glass UI", "In Settings, App UI type, switch from the default solid look to Glass: frosted translucent panels over a gradient that follows your theme colour, on every screen including the iPod player. Switch back any time.", new = listOf("New App UI type setting: Glass", "Glass panels follow your theme colour"), fixed = listOf("App drawer no longer shows through glass screens", "Bottom bar, mini player and music cards are readable on translucent backgrounds", "White box behind music card text is gone")),
    FeatureItem(Icons.Filled.Palette, "Themes", "Light, dark and AMOLED black, wallpaper-based dynamic colour, album-art colour, app-icon colour or a fixed accent.", new = listOf("Custom palette styles: Hue, Vibrant, Content, Rainbow, Fruit Salad and Mono, each with twelve colours to pick from"), fixed = listOf()),
    FeatureItem(Icons.Filled.AutoAwesome, "Smart playlists", "Lists that build themselves: most played this month, on repeat, recently added, long tracks, rediscover and not played yet."),
    FeatureItem(Icons.Filled.BarChart, "Listening stats", "See your listening time, a daily or monthly chart, top songs and artists, plus a shareable \"Your year in music\" summary."),
    FeatureItem(Icons.Filled.TouchApp, "Long-press song menu", "Press and hold any song on Home or Songs to play it next, add it to the queue, add it to a playlist (or create a new one) and see its info."),
    FeatureItem(Icons.Filled.Widgets, "Home screen widgets", "Five widgets: Player, Compact, Cover, Recently Played and Favourites, with play, skip and favourite controls right on your home screen."),
    FeatureItem(Icons.Filled.Speed, "Speed & pitch", "Change playback speed and pitch from the player with presets, and optionally remember the setting for each song."),
    FeatureItem(Icons.Filled.Shortcut, "App shortcuts", "Press and hold the launcher icon for Shuffle all, Favourites and Continue listening."),
    FeatureItem(Icons.Filled.ContentCopy, "Duplicate finder", "Scan your library for repeated songs (same title, artist and length) and delete the extra copies in one go."),
    FeatureItem(Icons.Filled.MusicNote, "Player B: iPod style", "In Settings, switch Player type to B for a whole-app iPod look. It opens on a Cover Flow of your songs; press MENU on the click wheel for the menu (MENU again closes it). Turn the wheel or drag the display to browse, press the centre or tap to play, and use the labels for previous, next and play / pause.", new = listOf("Delete playlists from the iPod menu"), fixed = listOf()),
    FeatureItem(Icons.Filled.Headphones, "Audio Books", "Add PDF or EPUB books from your phone (they stay where they are, nothing is copied) and keep them in an animated shelf with All and Favourites tabs. Add new books right from the shelf. Book covers come from the book itself.", new = listOf("Modern shelf look shared with playlists"), fixed = listOf()),
    FeatureItem(Icons.Filled.MenuBook, "Built-in book reader", "Read books inside the app with page-by-page scrolling, zoom for PDFs and adjustable text size for EPUBs. Press and hold to select a single word or extend the selection across words."),
    FeatureItem(Icons.Filled.RecordVoiceOver, "Read aloud", "Press play to have a book narrated, with four narrator voices (two female, two male), speed from 0.75x to 2x and playback controls in the notification. The sentence being read is highlighted and the current word is marked in orange, in both PDFs and EPUBs."),
    FeatureItem(Icons.Filled.Bookmark, "Resume where you stopped", "The page and the spot on it are remembered for every book, for reading and listening alike."),
    FeatureItem(Icons.Filled.Bedtime, "Sleep timer", "Fall asleep to music; playback fades out and stops on its own."),
    FeatureItem(Icons.Filled.Apps, "Floating navigation", "A floating bottom bar for Home, Songs, Library and Playlists, with a button that opens your Audio Books.", new = listOf("Selected tab becomes a pill with its name", "New Settings option to show or hide the tab labels"), fixed = listOf("Tabs no longer scale up and down when selected")),
    FeatureItem(Icons.Filled.Apps, "App icon switcher", "Pick your launcher icon from eight designs in Settings; the drawer logo and theme can follow it."),
    FeatureItem(Icons.Filled.AutoAwesome, "Fluid motion", "Screens turn like pages of frosted glass and lists glide in one by one on every screen except the iPod player.", new = listOf("Page-turn transition between screens","Entrance animation on all non-iPod screens"), fixed = listOf("Mixed fade-from-centre transitions replaced by a consistent crossfade")),
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

    var showFeatures by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Reveal(0) { HeroCard(versionName) }

        Reveal(1) { WhatsNewCard(versionName) { showFeatures = true } }

        Reveal(2) { FeaturesEntry(count = features.size) { showFeatures = true } }

        Reveal(3) { DeveloperCard(context) }

        Reveal(4) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = "Copyright © 2026 $DEV_NAME.",
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
    if (showFeatures) FeaturesSheet(versionName) { showFeatures = false }
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
private fun FeatureRow(item: FeatureItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f, fill = false))
                if (item.new.isNotEmpty()) Badge("NEW", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                if (item.fixed.isNotEmpty()) Badge("FIXED", MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
            }
            Text(
                item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Badge(text: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(50), color = container) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = content,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

/** Short "what's new" summary; tapping opens the full list. */
@Composable
private fun WhatsNewCard(versionName: String, onOpen: () -> Unit) {
    val newCount = features.sumOf { it.new.size }
    val fixedCount = features.sumOf { it.fixed.size }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(onClick = onOpen),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "What's new in $versionName",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                "$newCount new things and $fixedCount fixes: glass UI, page-turn motion, 10-band equalizer, theme palette styles, bottom bar labels, playlist reorder and delete, and more.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** The single row that replaces the long inline list. */
@Composable
private fun FeaturesEntry(count: Int, onOpen: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(onClick = onOpen),
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text("What MelodiQ can do", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text("$count features, with what's new and fixed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** One sheet that shows the feature list, and swaps to a feature's detail + changelog when one is tapped. */
@Composable
private fun FeaturesSheet(versionName: String, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf<FeatureItem?>(null) }
    BackHandler(enabled = selected != null) { selected = null }
    MelodiqSheet(onDismiss = onDismiss) {
        val item = selected
        if (item == null) {
            Text(
                "What MelodiQ can do",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                "Version $versionName. Tap a feature to see what's new and fixed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                itemsIndexed(features) { index, f ->
                    FeatureRow(f) { selected = f }
                    if (index != features.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 64.dp, end = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        } else {
            FeatureDetail(item, versionName) { selected = null }
        }
    }
}

@Composable
private fun FeatureDetail(item: FeatureItem, versionName: String, onBack: () -> Unit) {
    Column(modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
            }
            Text(
                item.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Text(
            item.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
        )
        if (item.new.isEmpty() && item.fixed.isEmpty()) {
            Text(
                "No changes in version $versionName.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ChangeSection("New in this version", "NEW", item.new, Icons.Filled.NewReleases, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        ChangeSection("Fixed", "FIXED", item.fixed, Icons.Filled.BugReport, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ChangeSection(
    title: String,
    badge: String,
    lines: List<String>,
    icon: ImageVector,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
) {
    if (lines.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        Badge(badge, container, content)
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(start = 8.dp))
    }
    lines.forEach { line ->
        Row(modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = container, modifier = Modifier.size(18.dp).padding(top = 2.dp))
            Text(line, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
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
