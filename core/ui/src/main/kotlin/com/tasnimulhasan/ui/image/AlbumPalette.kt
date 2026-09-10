package com.tasnimulhasan.ui.image

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A small (64x64) thumbnail decoded off the main thread, re-decoded only when [songId]
 * changes — not on every recomposition/progress tick. Use this (never a full-size Bitmap)
 * when all you need is dominant-color extraction via Palette.
 */
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun rememberPaletteThumbnail(songId: Long, contentUri: Uri): Bitmap? {
    val context = LocalContext.current
    var thumbnail by remember(songId) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(songId) {
        thumbnail = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.loadThumbnail(contentUri, Size(64, 64), null) }.getOrNull()
        }
    }
    return thumbnail
}