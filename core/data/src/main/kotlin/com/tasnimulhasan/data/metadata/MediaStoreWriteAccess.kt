package com.tasnimulhasan.data.metadata

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import java.io.OutputStream

/**
 * Wraps the "try directly, fall back to a user-consent request" pattern that scoped storage
 * requires for modifying or deleting a media file the app doesn't own. This is the standard
 * Android 11+ (API 30, this project's minSdk) approach: MediaStore.createWriteRequest /
 * createDeleteRequest return a PendingIntent the UI must launch; once the user approves it,
 * the same operation that failed a moment ago succeeds.
 */
class MediaStoreWriteAccess @Inject constructor(@ApplicationContext private val context: Context) {

    sealed interface Access {
        data object Granted : Access
        data class NeedsPermission(val intentSender: IntentSender) : Access
        data object Failed : Access
    }

    /** Opens [uri] for a full overwrite (truncate mode) if already permitted, or returns the
     * consent request to launch if not. */
    fun openForOverwrite(uri: Uri): Pair<Access, OutputStream?> {
        return try {
            val stream = context.contentResolver.openOutputStream(uri, "rwt")
            if (stream != null) Access.Granted to stream else Access.Failed to null
        } catch (e: SecurityException) {
            requestWriteAccess(uri, e) to null
        }
    }

    private fun requestWriteAccess(uri: Uri, original: SecurityException): Access {
        // Pre-API-29 RecoverableSecurityException doesn't apply (minSdk is 30), but some
        // OEM ContentProviders still throw it - handle both paths defensively.
        val recoverable = original as? RecoverableSecurityException
        if (recoverable != null) return Access.NeedsPermission(recoverable.userAction.actionIntent.intentSender)

        return try {
            val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
            Access.NeedsPermission(pendingIntent.intentSender)
        } catch (_: Exception) {
            Access.Failed
        }
    }

    fun requestDeleteAccess(uris: List<Uri>): Access = try {
        val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
        Access.NeedsPermission(pendingIntent.intentSender)
    } catch (_: Exception) {
        Access.Failed
    }

    /** Direct delete attempt - succeeds outright for files the app already has access to,
     * otherwise the caller should fall back to [requestDeleteAccess]. */
    fun tryDeleteDirect(uri: Uri): Boolean = try {
        context.contentResolver.delete(uri, null, null) > 0
    } catch (_: SecurityException) {
        false
    }

    /** Renames a song's display name. Returns the same Access states as writes, since
     * renaming is also a MediaStore ContentValues update gated the same way. */
    fun tryRenameDirect(uri: Uri, newDisplayName: String): Access {
        return try {
            val values = ContentValues().apply { put(MediaStore.Audio.Media.DISPLAY_NAME, newDisplayName) }
            val updated = context.contentResolver.update(uri, values, null, null)
            if (updated > 0) Access.Granted else Access.Failed
        } catch (e: SecurityException) {
            requestWriteAccess(uri, e)
        }
    }

    fun tryMoveDirect(uri: Uri, newRelativePath: String): Access {
        return try {
            val values = ContentValues().apply { put(MediaStore.Audio.Media.RELATIVE_PATH, newRelativePath) }
            val updated = context.contentResolver.update(uri, values, null, null)
            if (updated > 0) Access.Granted else Access.Failed
        } catch (e: SecurityException) {
            requestWriteAccess(uri, e)
        }
    }
}
