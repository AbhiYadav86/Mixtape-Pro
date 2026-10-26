package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * Saves the details of a crash on the phone, so the next launch can show them and the user can
 * send them on (there's no crash-reporting service in the app).
 */
object CrashReport {
    private fun file(context: Context) = File(context.filesDir, "last_crash.txt")

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val info = buildString {
                    append("Mixtape Pro ").append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
                    append("Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append("), ")
                    append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
                    append("Thread: ").append(thread.name).append("\n\n")
                    append(android.util.Log.getStackTraceString(error))
                }
                file(app).writeText(info)
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** The saved report from the last crash, if any (removed once read). */
    fun takeLast(context: Context): String? {
        val f = file(context)
        if (!f.exists()) return null
        return try { f.readText().also { f.delete() } } catch (_: Exception) { null }
    }
}

/** Shown once after a crash: what went wrong, with a button to share the details. */
@Composable
fun CrashReportDialog() {
    val context = LocalContext.current
    var report by remember { mutableStateOf(CrashReport.takeLast(context)) }
    val text = report ?: return
    AlertDialog(
        onDismissRequest = { report = null },
        containerColor = Color(0xFF16181E),
        title = { Text("Mixtape Pro closed unexpectedly", color = Color.White) },
        text = {
            Text(
                "Sorry about that. Sharing these details helps get it fixed:\n\n" + text.take(1800),
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Mixtape Pro crash report")
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, "Share crash report"))
                report = null
            }) { Text("Share details", color = Color(0xFF38BDF8)) }
        },
        dismissButton = { TextButton(onClick = { report = null }) { Text("Close", color = Color.White) } }
    )
}
