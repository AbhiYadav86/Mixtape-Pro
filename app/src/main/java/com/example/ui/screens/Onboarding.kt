package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.R
import com.example.audio.RetroSoundEffects
import com.example.data.model.RetroSkin
import com.example.ui.components.RetroTactileButton

// ============================================================================ app prefs

/** Small flags for the welcome tour, the rating prompt and "What's new". */
object AppPrefs {
    private fun p(c: Context) = c.getSharedPreferences("mixtape_onboarding", Context.MODE_PRIVATE)
    fun tourDone(c: Context) = p(c).getBoolean("tour_done", false)
    fun setTourDone(c: Context) = p(c).edit().putBoolean("tour_done", true).apply()
    fun reviewAsked(c: Context) = p(c).getBoolean("review_asked", false)
    fun setReviewAsked(c: Context) = p(c).edit().putBoolean("review_asked", true).apply()
    fun lastVersion(c: Context) = p(c).getInt("last_version", 0)
    fun setLastVersion(c: Context) = p(c).edit().putInt("last_version", BuildConfig.VERSION_CODE).apply()
}

const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.mixtapepro.player"
const val PRIVACY_URL = "https://abhiyadav86.github.io/Mixtape-Pro/privacy.html"
/** Where "Send feedback" emails go. Blank: the user picks the app and types the address. */
const val SUPPORT_EMAIL = ""

const val DEFAULT_SHARE_MESSAGE =
    "Been using Mixtape Pro: a 90s-style music player with retro skins, a 10-band EQ and a DJ studio. No ads, works offline."

/** Shown once after an update, and from Settings. Keep it short; newest first. */
val WHATS_NEW = listOf(
    "A quick tour of the deck for new users (replay it any time from ⚙ Settings)",
    "New ⚙ Settings & About screen",
    "Share Mixtape Pro with friends, with your own message",
    "Rate the app on Google Play",
    "Send feedback or report a bug by email",
)

// ============================================================================ tour targets

/** Where each highlighted control is on screen, filled in by [tourTarget]. */
object TourTargets {
    val bounds = mutableStateMapOf<String, Rect>()
}

/** Marks a control the tour can point at. */
fun Modifier.tourTarget(id: String): Modifier = onGloballyPositioned { TourTargets.bounds[id] = it.boundsInWindow() }

data class TourStop(val target: String, val title: String, val text: String)

val TOUR_STOPS = listOf(
    TourStop("transport", "Play your music", "Prev, Play, Stop and Next work like a 90s deck. Open loads any song on your phone."),
    TourStop("skins", "Pick your skin", "Tap Skins to change the whole deck's look: Aqua Bubble, VCR Camcorder and more 90s themes."),
    TourStop("eq", "Shape the sound", "Eq opens the 10-band equalizer with presets. Drag the bars to tune bass, mids and treble."),
    TourStop("djstudio", "Make mixes & ringtones", "DJ Studio is where you mix two tracks, add effects and cut your own ringtones."),
    TourStop("settings", "Settings & more", "Share the app, rate it, see what's new, send feedback or replay this tour."),
)

// ============================================================================ shared bits

@Composable
private fun SkinButton(skin: RetroSkin, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    RetroTactileButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(skin.buttonShapeRounded.coerceIn(3, 10).dp),
        backgroundBrush = skin.buttonBackground,
        borderUnpressed = skin.buttonBorderColor,
        elevation = 3.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
        soundType = RetroSoundEffects.SoundType.WINAMP_CLICK,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = skin.buttonIconColor,
            textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun LinkText(skin: RetroSkin, text: String, onClick: () -> Unit) {
    Text(
        text, fontSize = 14.sp, color = skin.lcdTextColor.copy(alpha = 0.85f), textDecoration = TextDecoration.Underline,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

/** A panel in the skin's display colours (always readable: light skins get light panels with dark text). */
private fun Modifier.lcdPanel(skin: RetroSkin, radius: Int = 12) = this
    .clip(RoundedCornerShape(radius.dp))
    .background(skin.lcdBackground)
    .border(2.dp, skin.lcdTextColor.copy(alpha = 0.85f), RoundedCornerShape(radius.dp))

private val blockTouches = Modifier.pointerInput(Unit) {
    awaitPointerEventScope { while (true) { awaitPointerEvent().changes.forEach { it.consume() } } }
}

// ============================================================================ welcome + tour

/** First launch: welcome card offering the tour. */
@Composable
fun WelcomeCard(skin: RetroSkin, onStart: () -> Unit, onSkip: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xD9020A0D)).then(blockTouches).testTag("welcome_card"), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp).fillMaxWidth().lcdPanel(skin, 16).padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ra98_note_logo_1787601220471), contentDescription = null,
                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.height(12.dp))
            Text("▶ PLAY · STEREO · 320 kbps", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = skin.lcdTextColor,
                modifier = Modifier.border(1.dp, skin.lcdTextColor.copy(alpha = 0.5f), RoundedCornerShape(5.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
            Spacer(Modifier.height(12.dp))
            Text("Welcome to Mixtape Pro", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = skin.lcdTextColor, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Your MP3s, the 90s way. Take a quick tour of the deck: 5 stops, under a minute.",
                fontSize = 15.sp, lineHeight = 21.sp, color = skin.lcdTextColor.copy(alpha = 0.88f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            SkinButton(skin, "Start the tour ▸", Modifier.fillMaxWidth().testTag("tour_start"), onStart)
            Spacer(Modifier.height(6.dp))
            LinkText(skin, "Skip, I'll explore on my own", onSkip)
            Text("You can replay it any time from ⚙ Settings.", fontSize = 12.sp, color = skin.lcdTextColor.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}

/**
 * Dims the screen except the control at [stop]'s target, with a tip card above or below it.
 * Taps outside the buttons are swallowed so the tour can't be broken by accident.
 */
@Composable
fun TourOverlay(skin: RetroSkin, index: Int, onNext: () -> Unit, onSkip: () -> Unit) {
    val stop = TOUR_STOPS[index]
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val glow = skin.lcdTextColor
    BoxWithConstraints(
        Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }.then(blockTouches).testTag("tour_overlay")
    ) {
        val target = TourTargets.bounds[stop.target]?.translate(-origin.x, -origin.y)
        val pad = with(density) { 6.dp.toPx() }
        val hole = target?.let { Rect(it.left - pad, it.top - pad, it.right + pad, it.bottom + pad) }
        val heightPx = with(density) { maxHeight.toPx() }
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color(0xCC020A0D))
            if (hole != null) {
                val r = CornerRadius(10.dp.toPx())
                drawRoundRect(Color.Black, hole.topLeft, hole.size, r, blendMode = BlendMode.Clear)
                drawRoundRect(glow.copy(alpha = 0.35f), hole.topLeft - Offset(4f, 4f), Size(hole.width + 8f, hole.height + 8f), r, style = Stroke(8.dp.toPx()))
                drawRoundRect(glow, hole.topLeft, hole.size, r, style = Stroke(3.dp.toPx()))
            }
        }
        // Card goes on the side with more room.
        val below = hole == null || hole.center.y < heightPx / 2
        val gap = with(density) { 16.dp.toPx() }
        var cardHeight by remember(index) { mutableStateOf(0) }
        val y = when {
            hole == null -> (heightPx - cardHeight) / 2
            below -> hole.bottom + gap
            else -> hole.top - gap - cardHeight
        }.coerceIn(0f, (heightPx - cardHeight).coerceAtLeast(0f))
        Column(
            Modifier.offset { IntOffset(0, y.toInt()) }.padding(horizontal = 16.dp).fillMaxWidth()
                .onGloballyPositioned { cardHeight = it.size.height }
                .lcdPanel(skin).padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text("TOUR · STOP ${index + 1} OF ${TOUR_STOPS.size}", fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, color = skin.lcdTextColor.copy(alpha = 0.8f), letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(stop.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = skin.lcdTextColor)
            Spacer(Modifier.height(6.dp))
            Text(stop.text, fontSize = 15.sp, lineHeight = 21.sp, color = skin.lcdTextColor.copy(alpha = 0.9f))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TOUR_STOPS.indices.forEach { i ->
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == index) skin.lcdTextColor else skin.lcdTextColor.copy(alpha = 0.25f)))
                    }
                }
                if (index < TOUR_STOPS.lastIndex) LinkText(skin, "Skip tour", onSkip)
                Spacer(Modifier.width(8.dp))
                SkinButton(skin, if (index == TOUR_STOPS.lastIndex) "Done" else "Next ▸", Modifier.testTag("tour_next"), onNext)
            }
        }
        // Little pointer between the card and the control.
        if (hole != null && cardHeight > 0) {
            val tipX = hole.center.x
            val tipY = if (below) y else y + cardHeight
            Canvas(Modifier.fillMaxSize()) {
                val s = 10.dp.toPx()
                val path = Path().apply {
                    if (below) { moveTo(tipX - s, tipY + 1); lineTo(tipX, tipY - s); lineTo(tipX + s, tipY + 1) }
                    else { moveTo(tipX - s, tipY - 1); lineTo(tipX, tipY + s); lineTo(tipX + s, tipY - 1) }
                    close()
                }
                drawPath(path, glow)
            }
        }
    }
}

// ============================================================================ settings & about

private data class SettingsItem(val icon: String, val title: String, val sub: String, val tag: String, val onClick: () -> Unit)

/** Full-screen Settings & About, in the current skin's colours. */
@Composable
fun SettingsAboutScreen(
    skin: RetroSkin,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onRate: () -> Unit,
    onReplayTour: () -> Unit,
    onWhatsNew: () -> Unit,
    onFeedback: () -> Unit,
    onPrivacy: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        SettingsAboutContent(skin, onDismiss, onShare, onRate, onReplayTour, onWhatsNew, onFeedback, onPrivacy)
    }
}

@Composable
fun SettingsAboutContent(
    skin: RetroSkin,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onRate: () -> Unit,
    onReplayTour: () -> Unit,
    onWhatsNew: () -> Unit,
    onFeedback: () -> Unit,
    onPrivacy: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(skin.chassisBackground).windowInsetsPadding(WindowInsets.safeDrawing).testTag("settings_screen")) {
        // Title bar, like the player's
        Row(
            Modifier.fillMaxWidth().background(skin.titleBarBackground).border(1.dp, skin.chassisBorderColor).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RetroTactileButton(
                onClick = onBack, shape = RoundedCornerShape(3.dp), backgroundBrush = skin.buttonBackground,
                borderUnpressed = skin.buttonBorderColor, elevation = 3.dp, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("settings_back"),
            ) { Text("◂", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = skin.buttonIconColor) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("SETTINGS & ABOUT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = skin.titleBarTextColor, letterSpacing = 1.sp)
                Text("MIXTAPE PRO ${BuildConfig.VERSION_NAME} · ${skin.name}", fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    color = skin.titleBarTextColor.copy(alpha = 0.8f), maxLines = 1)
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            Row(Modifier.fillMaxWidth().lcdPanel(skin).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(painterResource(R.drawable.ra98_note_logo_1787601220471), null,
                    Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Mixtape Pro", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = skin.lcdTextColor)
                    Text("No ads. No tracking.\nYour music stays on your phone.", fontSize = 13.sp, lineHeight = 18.sp,
                        color = skin.lcdTextColor.copy(alpha = 0.8f))
                }
            }
            SettingsSection(skin, "SPREAD THE WORD", listOf(
                SettingsItem("⇪", "Share Mixtape Pro", "Send the Play Store link to a friend", "settings_share", onShare),
                SettingsItem("★", "Rate on Google Play", "Takes 10 seconds, helps a lot", "settings_rate", onRate),
            ))
            SettingsSection(skin, "HELP", listOf(
                SettingsItem("▶", "Replay the tour", "5 stops, under a minute", "settings_tour", onReplayTour),
                SettingsItem("✦", "What's new", "Changes in version ${BuildConfig.VERSION_NAME}", "settings_whatsnew", onWhatsNew),
                SettingsItem("✉", "Send feedback / report a bug", "Opens your email app", "settings_feedback", onFeedback),
            ))
            SettingsSection(skin, "LEGAL", listOf(
                SettingsItem("§", "Privacy policy", "Opens in your browser", "settings_privacy", onPrivacy),
            ))
        }
    }
}

@Composable
private fun SettingsSection(skin: RetroSkin, title: String, items: List<SettingsItem>) {
    Spacer(Modifier.height(16.dp))
    Text(title, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
        color = skin.titleBarTextColor,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(skin.titleBarBackground).padding(horizontal = 8.dp, vertical = 3.dp))
    Spacer(Modifier.height(6.dp))
    Column(Modifier.fillMaxWidth().lcdPanel(skin)) {
        items.forEachIndexed { i, item ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(skin.lcdTextColor.copy(alpha = 0.2f)))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 58.dp).clickable {
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK); item.onClick()
                }.padding(horizontal = 12.dp, vertical = 10.dp).testTag(item.tag),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(skin.buttonBackground).border(1.dp, skin.buttonBorderColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center) {
                    Text(item.icon, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = skin.buttonIconColor)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = skin.lcdTextColor)
                    Text(item.sub, fontSize = 12.sp, color = skin.lcdTextColor.copy(alpha = 0.75f))
                }
                Text("›", fontSize = 20.sp, color = skin.lcdTextColor)
            }
        }
    }
}

// ============================================================================ share / what's new

/** Edit the share message, then hand it to the phone's share menu with the Play Store link. */
@Composable
fun ShareAppDialog(skin: RetroSkin, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf(DEFAULT_SHARE_MESSAGE) }
    Dialog(onDismissRequest = onDismiss) {
        ShareAppContent(skin, message, { message = it }, onDismiss) {
            shareApp(context, message)
            onDismiss()
        }
    }
}

@Composable
fun ShareAppContent(skin: RetroSkin, message: String, onMessage: (String) -> Unit, onCancel: () -> Unit, onShare: () -> Unit) {
    Column(Modifier.fillMaxWidth().lcdPanel(skin, 16).padding(18.dp).testTag("share_dialog")) {
        Text("Share Mixtape Pro", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = skin.lcdTextColor)
        Spacer(Modifier.height(4.dp))
        Text("Edit the message if you like. The link is added at the end.", fontSize = 13.sp, color = skin.lcdTextColor.copy(alpha = 0.75f))
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).border(1.5.dp, skin.lcdTextColor, RoundedCornerShape(8.dp)).padding(12.dp)) {
            BasicTextField(
                value = message, onValueChange = { onMessage(it.take(400)) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 21.sp, color = skin.lcdTextColor),
                cursorBrush = SolidColor(skin.lcdTextColor),
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).testTag("share_message"),
            )
            Spacer(Modifier.height(8.dp))
            Text(PLAY_STORE_URL.removePrefix("https://"), fontSize = 12.sp, color = skin.lcdTextColor.copy(alpha = 0.7f))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LinkText(skin, "Reset message") { onMessage(DEFAULT_SHARE_MESSAGE) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            SkinButton(skin, "Cancel", onClick = onCancel)
            SkinButton(skin, "Share ▸", Modifier.testTag("share_send"), onShare)
        }
    }
}

@Composable
fun WhatsNewDialog(skin: RetroSkin, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { WhatsNewContent(skin, onDismiss) }
}

@Composable
fun WhatsNewContent(skin: RetroSkin, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth().lcdPanel(skin, 16).padding(18.dp).testTag("whats_new")) {
        Text("What's new", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = skin.lcdTextColor)
        Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = skin.lcdTextColor.copy(alpha = 0.7f))
        Spacer(Modifier.height(10.dp))
        WHATS_NEW.forEach { line ->
            Row(Modifier.padding(vertical = 3.dp)) {
                Text("✦ ", fontSize = 14.sp, color = skin.lcdTextColor)
                Text(line, fontSize = 14.sp, lineHeight = 19.sp, color = skin.lcdTextColor.copy(alpha = 0.9f))
            }
        }
        Spacer(Modifier.height(12.dp))
        SkinButton(skin, "Got it", Modifier.align(Alignment.End), onDismiss)
    }
}

// ============================================================================ actions

fun shareApp(context: Context, message: String) {
    val text = message.trim().ifBlank { DEFAULT_SHARE_MESSAGE } + "\n" + PLAY_STORE_URL
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Mixtape Pro")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share Mixtape Pro"))
}

/** The Play Store page (the app if installed, else the browser). */
fun openPlayStore(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.mixtapepro.player"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { context.startActivity(market) } catch (_: Exception) { openUrl(context, PLAY_STORE_URL) }
}

fun openUrl(context: Context, url: String) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
}

fun sendFeedback(context: Context) {
    val body = "\n\n---\nMixtape Pro ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
        "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}"
    val intent = if (SUPPORT_EMAIL.isNotBlank()) {
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply { putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL)) }
    } else {
        Intent(Intent.ACTION_SEND).apply { type = "message/rfc822" }
    }.apply {
        putExtra(Intent.EXTRA_SUBJECT, "Mixtape Pro feedback")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try { context.startActivity(Intent.createChooser(intent, "Send feedback")) } catch (_: Exception) {}
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

/**
 * Google Play's own rating sheet. Google decides whether it actually appears (it limits how often,
 * and it never shows for sideloaded builds), so nothing in the app depends on it.
 */
fun requestInAppReview(activity: Activity) {
    try {
        val manager = com.google.android.play.core.review.ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
        }
    } catch (_: Throwable) {}
}
