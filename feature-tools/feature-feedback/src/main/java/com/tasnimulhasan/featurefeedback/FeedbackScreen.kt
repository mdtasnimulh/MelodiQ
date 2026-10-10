package com.tasnimulhasan.featurefeedback

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val DEV_EMAIL = "mdtasnimulh@gmail.com"
private val topics = listOf("Bug report", "Feature request", "Compliment", "Question", "Other")
private val ratingLabels = listOf("", "Needs a lot of work", "Could be better", "It's okay", "Really good", "Love it!")

@Composable
internal fun FeedbackRoute(modifier: Modifier = Modifier) {
    FeedbackScreen(modifier)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FeedbackScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var rating by rememberSaveable { mutableIntStateOf(0) }
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val ratingMissing = rating == 0
    val titleMissing = title.isBlank()
    val messageMissing = message.isBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "We'd love to hear from you",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            "Rate MelodiQ, report a problem or suggest an idea. Your message opens in Gmail, addressed to the developer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("How would you rate the app?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                RatingBar(rating = rating, onRatingChange = { rating = it })
                Text(
                    text = if (rating == 0) (if (showErrors) "Please choose a rating" else "Tap a star") else ratingLabels[rating],
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (rating == 0 && showErrors) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("What is it about?", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    topics.forEach { topic ->
                        FilterChip(
                            selected = title == topic,
                            onClick = { title = topic },
                            label = { Text(topic) },
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("Title") },
                    singleLine = true,
                    isError = showErrors && titleMissing,
                    supportingText = if (showErrors && titleMissing) ({ Text("Please add a title") }) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(2000) },
                    label = { Text("Message") },
                    minLines = 5,
                    isError = showErrors && messageMissing,
                    supportingText = {
                        Text(if (showErrors && messageMissing) "Please write a message" else "${message.length}/2000")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Button(
            onClick = {
                if (ratingMissing || titleMissing || messageMissing) {
                    showErrors = true
                } else {
                    sendFeedback(context, rating, title.trim(), message.trim())
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Send", modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            "Sends to $DEV_EMAIL",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun RatingBar(rating: Int, onRatingChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (star in 1..5) {
            val filled = star <= rating
            val scale by animateFloatAsState(
                targetValue = if (filled) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "star$star",
            )
            val tint by animateColorAsState(
                targetValue = if (filled) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                label = "starTint$star",
            )
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "$star star${if (star > 1) "s" else ""}",
                tint = tint,
                modifier = Modifier
                    .size(48.dp)
                    .scale(scale)
                    .clickable { onRatingChange(if (rating == star) 0 else star) },
            )
        }
    }
}

/** Opens Gmail's compose screen to the developer with the feedback filled in; falls back to
 * any mail app, then to a toast with the address. */
private fun sendFeedback(context: Context, rating: Int, title: String, message: String) {
    val appVersion = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    val subject = "[MelodiQ] $title (${rating}/5)"
    val body = buildString {
        append(message)
        append("\n\n--\n")
        append("Rating: $rating/5\n")
        append("App version: $appVersion\n")
        append("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}\n")
    }
    val uri = Uri.parse("mailto:$DEV_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
    try {
        context.startActivity(Intent(Intent.ACTION_SENDTO, uri).setPackage("com.google.android.gm"))
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SENDTO, uri), "Send feedback"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No email app found. Write to $DEV_EMAIL", Toast.LENGTH_LONG).show()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeedbackScreenPreview() {
    FeedbackScreen(modifier = Modifier)
}
