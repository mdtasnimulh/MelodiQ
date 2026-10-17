package com.tasnimulhasan.playlists.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tasnimulhasan.ui.component.MelodiqSheet
import com.tasnimulhasan.ui.component.SheetHeader
import com.tasnimulhasan.ui.component.SheetPrimaryButton

/** "New playlist" as a bottom sheet (the name is kept so existing callers still work). */
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    MelodiqSheet(onDismiss = onDismiss) { dismiss ->
        SheetHeader(title = "New Playlist", icon = Icons.Filled.QueueMusic)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Playlist name") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        SheetPrimaryButton(
            text = "Create Playlist",
            enabled = name.isNotBlank(),
            onClick = {
                onCreate(name.trim(), description.trim())
                dismiss()
            },
        )
        TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
