package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AudioSource
import com.example.data.model.RetroSkin
import com.example.data.model.Song
import java.io.File

/*
 * Deleting tracks. Two levels, always confirmed first:
 *   - Remove from Library: the file stays on the phone (and is not re-added by a scan)
 *   - Delete from device: the file itself is deleted. Android shows its own permission prompt
 *     (Android 11+: one "Allow Mixtape Pro to delete these files?" sheet; Android 10: one per
 *     file; Android 9 and older: the storage permission).
 */

private fun isMediaStoreUri(uri: Uri) = uri.scheme == "content" && uri.authority == MediaStore.AUTHORITY

/** Whether the song is a file on this device that can be deleted (not a stream). */
fun canDeleteFromDevice(song: Song): Boolean {
    if (song.source == AudioSource.RADIO_STREAM || song.source == AudioSource.DEMO_JUKEBOX) return false
    val uri = Uri.parse(song.uri)
    return uri.scheme == "content" || uri.scheme == "file"
}

private class StepHolder { var run: () -> Unit = {} }

private class DeleteSession {
    val queue = ArrayDeque<Song>()           // Android 10 and older: one at a time
    var batch: List<Song> = emptyList()      // Android 11+: one system request for all
    var current: Song? = null                // Android 10: waiting on the per-file prompt
    val deleted = mutableListOf<Song>()
    val failed = mutableListOf<Song>()
    fun reset() { queue.clear(); batch = emptyList(); current = null; deleted.clear(); failed.clear() }
}

private fun deleteDirectly(context: Context, song: Song): Boolean {
    val uri = Uri.parse(song.uri)
    return try {
        when {
            uri.scheme == "file" -> uri.path?.let { File(it).delete() } ?: false
            isMediaStoreUri(uri) -> context.contentResolver.delete(uri, null, null) > 0
            DocumentsContract.isDocumentUri(context, uri) -> DocumentsContract.deleteDocument(context.contentResolver, uri)
            else -> context.contentResolver.delete(uri, null, null) > 0
        }
    } catch (e: SecurityException) {
        throw e
    } catch (e: Exception) {
        false
    }
}

/**
 * Returns a function that deletes songs' files from the device, asking Android for permission
 * as needed. [onFinished] gets the songs that were deleted and the ones that couldn't be.
 */
@Composable
fun rememberDeviceDeleter(onFinished: (deleted: List<Song>, failed: List<Song>) -> Unit): (List<Song>) -> Unit {
    val context = LocalContext.current
    val session = remember { DeleteSession() }
    val callback by rememberUpdatedState(onFinished)
    val step = remember { StepHolder() }

    fun finish() {
        val d = session.deleted.toList()
        val f = session.failed.toList()
        session.reset()
        callback(d, f)
    }

    val systemPrompt = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        val ok = res.resultCode == Activity.RESULT_OK
        if (session.batch.isNotEmpty()) {
            // Android 11+: the system deleted the files itself if the user allowed it
            if (ok) session.deleted += session.batch else session.failed += session.batch
            session.batch = emptyList()
            step.run()
        } else {
            val s = session.current
            session.current = null
            if (s != null) {
                val done = ok && try { deleteDirectly(context, s) } catch (e: Exception) { false }
                if (done) session.deleted += s else session.failed += s
            }
            step.run()
        }
    }

    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            step.run()
        } else {
            session.failed += session.queue
            session.queue.clear()
            finish()
        }
    }

    step.run = step@{
        while (session.queue.isNotEmpty()) {
            val s = session.queue.first()
            val uri = Uri.parse(s.uri)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && isMediaStoreUri(uri) &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return@step
            }
            session.queue.removeFirst()
            try {
                if (deleteDirectly(context, s)) session.deleted += s else session.failed += s
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    session.current = s
                    systemPrompt.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                    return@step
                }
                session.failed += s
            }
        }
        finish()
    }

    return remember {
        { songs: List<Song> ->
            session.reset()
            val deletable = songs.filter { canDeleteFromDevice(it) }
            session.failed += songs.filter { !canDeleteFromDevice(it) }
            val media = deletable.filter { isMediaStoreUri(Uri.parse(it.uri)) }
            val others = deletable - media.toSet()
            session.queue.addAll(others)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && media.isNotEmpty()) {
                try {
                    val pi = MediaStore.createDeleteRequest(context.contentResolver, media.map { Uri.parse(it.uri) })
                    session.batch = media
                    systemPrompt.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                } catch (e: Exception) {
                    session.failed += media
                    step.run()
                }
            } else {
                session.queue.addAll(media)
                step.run()
            }
        }
    }
}

/** Confirmation dialog: remove from the Library only, or delete the files from the device. */
@Composable
fun DeleteTracksDialog(
    songs: List<Song>,
    skin: RetroSkin,
    onRemoveFromLibrary: () -> Unit,
    onDeleteFromDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    val deletableCount = songs.count { canDeleteFromDevice(it) }
    var deleteFiles by remember { mutableStateOf(false) }
    val danger = Color(0xFFEF4444)
    val what = if (songs.size == 1) "\"${songs[0].title}\"" else "${songs.size} tracks"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16181E),
        icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = danger) },
        title = { Text("Delete $what?", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DeleteChoice(
                    selected = !deleteFiles,
                    icon = Icons.Default.PlaylistRemove,
                    title = "Remove from Library",
                    sub = "The file stays on your phone. It's also taken out of your mixtapes.",
                    accent = skin.accentColor
                ) { deleteFiles = false }
                DeleteChoice(
                    selected = deleteFiles,
                    enabled = deletableCount > 0,
                    icon = Icons.Default.DeleteForever,
                    title = "Delete from this device",
                    sub = when {
                        deletableCount == 0 -> "Streams and online tracks have no file on this device to delete."
                        deletableCount < songs.size -> "Permanently deletes $deletableCount file(s); the ${songs.size - deletableCount} stream(s) are only removed. Android will ask your permission."
                        else -> "Permanently deletes the file${if (songs.size > 1) "s" else ""}. Android will ask your permission first."
                    },
                    accent = danger
                ) { deleteFiles = true }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (deleteFiles) onDeleteFromDevice() else onRemoveFromLibrary() }) {
                Text(if (deleteFiles) "Delete" else "Remove", color = if (deleteFiles) danger else skin.accentColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) } }
    )
}

@Composable
private fun DeleteChoice(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    sub: String,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.14f) else Color(0xFF1E222D))
            .border(if (selected) 2.dp else 1.dp, if (selected) accent else Color(0x30FFFFFF), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) accent else Color(0xFF475569), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (enabled) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(sub, color = Color(0xFFB4BFCE), fontSize = 12.sp)
        }
        RadioButton(selected = selected, onClick = if (enabled) onClick else null, enabled = enabled, colors = RadioButtonDefaults.colors(selectedColor = accent))
    }
}
