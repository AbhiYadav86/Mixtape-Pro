package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.RetroSoundEffects
import com.example.data.model.RetroSkin
import com.example.ui.skins.RetroSkinDefinitions

@Composable
fun SkinSelectorDialog(
    currentSkin: RetroSkin,
    onSkinSelected: (RetroSkin) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val categories = listOf("ALL", "RETRO MP3", "GAMING", "Y2K CYBER", "TRANSLUCENT", "HI-FI STUDIO")

    val filteredSkins = remember(searchQuery, selectedCategory) {
        RetroSkinDefinitions.ALL_SKINS.filter { skin ->
            val matchesCategory = when (selectedCategory) {
                "RETRO MP3" -> skin.id.contains("winamp", ignoreCase = true) ||
                        skin.id.contains("wmp", ignoreCase = true) ||
                        skin.id.contains("napster", ignoreCase = true) ||
                        skin.id.contains("kazaa", ignoreCase = true) ||
                        skin.id.contains("ipod", ignoreCase = true) ||
                        skin.id.contains("win98", ignoreCase = true)
                "GAMING" -> skin.id.contains("gameboy", ignoreCase = true) ||
                        skin.id.contains("gba", ignoreCase = true) ||
                        skin.id.contains("sega", ignoreCase = true) ||
                        skin.id.contains("snes", ignoreCase = true) ||
                        skin.id.contains("ps1", ignoreCase = true) ||
                        skin.id.contains("n64", ignoreCase = true) ||
                        skin.id.contains("gbc", ignoreCase = true) ||
                        skin.id.contains("psp", ignoreCase = true) ||
                        skin.id.contains("ps2", ignoreCase = true) ||
                        skin.id.contains("dreamcast", ignoreCase = true) ||
                        skin.id.contains("xbox", ignoreCase = true) ||
                        skin.id.contains("tamagotchi", ignoreCase = true)
                "Y2K CYBER" -> skin.era.contains("2000") || skin.era.contains("2001") ||
                        skin.era.contains("2002") || skin.era.contains("1999") ||
                        skin.name.contains("Y2K", ignoreCase = true) ||
                        skin.name.contains("Cyber", ignoreCase = true) ||
                        skin.name.contains("Vaporwave", ignoreCase = true) ||
                        skin.name.contains("Outrun", ignoreCase = true)
                "TRANSLUCENT" -> skin.isTranslucent ||
                        skin.name.contains("Bubble", ignoreCase = true) ||
                        skin.name.contains("Amethyst", ignoreCase = true) ||
                        skin.name.contains("Citrus", ignoreCase = true) ||
                        skin.name.contains("Ruby", ignoreCase = true) ||
                        skin.name.contains("Clear", ignoreCase = true) ||
                        skin.name.contains("Glacier", ignoreCase = true) ||
                        skin.name.contains("Atomic", ignoreCase = true)
                "HI-FI STUDIO" -> skin.id.contains("walkman", ignoreCase = true) ||
                        skin.id.contains("discman", ignoreCase = true) ||
                        skin.id.contains("minidisc", ignoreCase = true) ||
                        skin.id.contains("sl1200", ignoreCase = true) ||
                        skin.id.contains("tr808", ignoreCase = true) ||
                        skin.id.contains("mpc", ignoreCase = true) ||
                        skin.id.contains("hifi", ignoreCase = true) ||
                        skin.id.contains("studio", ignoreCase = true) ||
                        skin.id.contains("boombox", ignoreCase = true) ||
                        skin.id.contains("trinitron", ignoreCase = true) ||
                        skin.id.contains("vcr", ignoreCase = true)
                else -> true
            }

            val matchesSearch = searchQuery.isBlank() ||
                    skin.name.contains(searchQuery, ignoreCase = true) ||
                    skin.era.contains(searchQuery, ignoreCase = true) ||
                    skin.description.contains(searchQuery, ignoreCase = true) ||
                    skin.id.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .testTag("skin_selector_dialog")
                .shadow(16.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14171E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, currentSkin.chassisBorderColor, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                // Header styled with active skin title bar aesthetic
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(currentSkin.titleBarBackground)
                        .border(1.dp, currentSkin.titleBarAccentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Skin Theme",
                            tint = currentSkin.titleBarAccentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "SKINS",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = currentSkin.titleBarTextColor,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                            onDismiss()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = currentSkin.titleBarTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar with Active Skin Equalizer & LCD Aesthetic
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentSkin.lcdBackground)
                        .border(1.5.dp, currentSkin.lcdBorderColor, RoundedCornerShape(8.dp))
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = currentSkin.accentColor,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search ${RetroSkinDefinitions.ALL_SKINS.size} skins (cassette, CD, neon…)",
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = currentSkin.lcdTextColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                ),
                                cursorBrush = SolidColor(currentSkin.accentColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("skin_search_input")
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                    searchQuery = ""
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Search",
                                    tint = currentSkin.lcdTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category filters: a 3 x 2 grid so every label fits, even with large system text
                val categoryLabels = mapOf(
                    "ALL" to "All", "RETRO MP3" to "Retro MP3", "GAMING" to "Gaming",
                    "Y2K CYBER" to "Y2K", "TRANSLUCENT" to "Clear", "HI-FI STUDIO" to "Hi-Fi"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                    categories.chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { category ->
                                val isSelected = selectedCategory == category
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 38.dp)
                                        .clip(RoundedCornerShape(19.dp))
                                        .background(if (isSelected) currentSkin.accentColor else Color(0xFF1E222D))
                                        .border(1.dp, if (isSelected) currentSkin.accentColor else Color(0x55FFFFFF), RoundedCornerShape(19.dp))
                                        .clickable {
                                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                            selectedCategory = category
                                        }
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = categoryLabels[category] ?: category,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif,
                                        color = if (isSelected) readableOn(currentSkin.accentColor) else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Results count sub-header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredSkins.size} of ${RetroSkinDefinitions.ALL_SKINS.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Now: ${currentSkin.name}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = readableAccent(currentSkin.accentColor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Skins Grid
                if (filteredSkins.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(currentSkin.lcdBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, currentSkin.lcdBorderColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NO SKINS MATCHED \"${searchQuery.uppercase()}\"",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = currentSkin.lcdTextColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedCategory = "ALL"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = currentSkin.accentColor)
                            ) {
                                Text(
                                    text = "RESET SEARCH",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredSkins) { skin ->
                            val isSelected = skin.id == currentSkin.id
                            SkinCard(
                                skin = skin,
                                isSelected = isSelected,
                                onSelect = {
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                    onSkinSelected(skin)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinCard(
    skin: RetroSkin,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) skin.accentColor else Color(0xFF333842),
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF22252D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Miniature chassis preview box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(skin.chassisBackground)
                    .border(1.dp, skin.chassisBorderColor, RoundedCornerShape(6.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mini title bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(skin.titleBarBackground, RoundedCornerShape(2.dp))
                    )

                    // Mini LCD & equalizer preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(skin.lcdBackground, RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "03:42",
                            color = skin.lcdTextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (c in skin.eqBarGradient) {
                                Box(
                                    modifier = Modifier
                                        .width(3.5.dp)
                                        .height(16.dp)
                                        .background(c, RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Skin Name & Era Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = skin.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = if (isSelected) readableAccent(skin.accentColor) else Color.White,
                    maxLines = 2,
                    minLines = 2,
                    lineHeight = 17.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = skin.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Era: ${skin.era}",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                color = readableAccent(skin.accentColor),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = skin.description,
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFFAAB6C6),
                maxLines = 2,
                minLines = 2,
                lineHeight = 14.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** The skin's accent colour, lifted when it's too dark to read on the dark gallery cards. */
private fun readableAccent(c: Color): Color =
    if (c.luminance() < 0.18f) androidx.compose.ui.graphics.lerp(c, Color.White, 0.55f) else c

/** Black or white text, whichever reads better on [bg]. */
private fun readableOn(bg: Color): Color = if (bg.luminance() > 0.45f) Color.Black else Color.White
