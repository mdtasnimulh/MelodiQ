package com.tasnimulhasan.featureabout

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.designsystem.component.Icon

private const val POLICY_DEVELOPER = "Md. Tasnimul Hasan"
private const val POLICY_EMAIL = "mdtasnimulh@gmail.com"
private const val POLICY_UPDATED = "10 November 2026"

private data class PolicyBullet(val label: String?, val text: String)
private data class PolicySection(
    val icon: ImageVector,
    val title: String,
    val intro: String? = null,
    val bullets: List<PolicyBullet> = emptyList(),
)

private val sections = listOf(
    PolicySection(
        icon = Icons.Filled.Storage,
        title = "What stays on your phone",
        intro = "Your music library information, playlists, favourites, play counts, listening statistics, " +
            "audiobook and book list, reading positions and app settings are stored only on your device. " +
            "We cannot see them. Uninstalling the app removes them.",
    ),
    PolicySection(
        icon = Icons.Filled.SettingsSuggest,
        title = "Permissions",
        bullets = listOf(
            PolicyBullet("Audio / media access", "to find and play the music on your device."),
            PolicyBullet("Notifications", "to show playback controls."),
            PolicyBullet("Foreground service", "to keep music, radio or read-aloud playing with the screen off."),
            PolicyBullet(
                "Microphone (optional)",
                "only for the live audio visualizer, which reads the sound the app itself is playing. " +
                    "No audio is recorded, saved or sent anywhere. You can leave this off.",
            ),
            PolicyBullet("Internet", "for the features listed below."),
        ),
    ),
    PolicySection(
        icon = Icons.Filled.Public,
        title = "Information sent over the internet",
        bullets = listOf(
            PolicyBullet(
                "Lyrics",
                "when you open lyrics, the song's title, artist, album and duration are sent to lrclib.net to " +
                    "look them up. No personal identifier is sent.",
            ),
            PolicyBullet(
                "Internet radio",
                "when you play a station, your device connects to that station's server, which can see your " +
                    "IP address, as with any web audio stream.",
            ),
            PolicyBullet(
                "Lyrics translation and text recognition",
                "Google ML Kit downloads language models to your device; translation then runs on the device.",
            ),
            PolicyBullet(
                "Feedback",
                "if you choose to send feedback, your email app opens with a draft; nothing is sent unless you send it.",
            ),
        ),
    ),
    PolicySection(
        icon = Icons.Filled.Security,
        title = "No ads, no tracking",
        intro = "We do not sell data, do not use advertising or analytics services, and do not share your " +
            "information with anyone other than the services listed above.",
    ),
    PolicySection(
        icon = Icons.Filled.LibraryMusic,
        title = "Children",
        intro = "MelodiQ is not directed to children under 13 and does not knowingly collect their information.",
    ),
    PolicySection(
        icon = Icons.Filled.Notifications,
        title = "Changes to this policy",
        intro = "If this policy changes, the new version will be published in the app and at the address shown " +
            "on the store listing, with a new date.",
    ),
)

@Composable
internal fun PrivacyPolicyScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PolicyHeader()
        Text(
            "MelodiQ is a music player, internet radio and book reader. It does not need an account and does not show ads.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        sections.forEach { PolicyCard(it) }
        ContactCard(context)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PolicyHeader() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                "Privacy Policy",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "Last updated: $POLICY_UPDATED",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Text(
                "Developer: $POLICY_DEVELOPER",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PolicyCard(section: PolicySection) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Text(
                    section.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            section.intro?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            section.bullets.forEach { bullet ->
                Row {
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    val body = if (bullet.label != null) "${bullet.label} – ${bullet.text}" else bullet.text
                    Text(
                        body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactCard(context: Context) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable { sendPolicyEmail(context) },
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Questions about privacy?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(POLICY_EMAIL, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun sendPolicyEmail(context: Context) {
    val uri = Uri.parse("mailto:$POLICY_EMAIL?subject=" + Uri.encode("MelodiQ privacy"))
    try {
        context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
    } catch (_: Exception) {
        Toast.makeText(context, "No email app found. Write to $POLICY_EMAIL", Toast.LENGTH_LONG).show()
    }
}
