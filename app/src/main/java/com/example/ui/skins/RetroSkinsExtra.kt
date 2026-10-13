package com.example.ui.skins

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.RetroSkin

/**
 * Eight more late-90s / Y2K hardware skins (brings the gallery to 50).
 */
object RetroSkinsExtra {

    private fun skin(
        id: String,
        name: String,
        era: String,
        description: String,
        chassis: List<Color>,
        border: Color,
        lcdBackground: Color,
        lcdText: Color,
        accent: Color,
        secondary: Color,
        buttons: List<Color>,
        buttonIcon: Color,
        eq: List<Color>,
        titleText: Color = Color.White,
        rounded: Int = 4,
        translucent: Boolean = false,
        metallic: Boolean = false
    ) = RetroSkin(
        id = id,
        name = name,
        era = era,
        description = description,
        chassisBackground = Brush.verticalGradient(chassis),
        chassisBorderColor = border,
        chassisBevelHighlight = border.copy(alpha = 0.7f),
        chassisBevelShadow = Color(0xFF05060A),
        isMetallic = metallic,
        isTranslucent = translucent,
        titleBarBackground = Brush.horizontalGradient(listOf(chassis.first(), chassis.last(), chassis.first())),
        titleBarTextColor = titleText,
        titleBarAccentColor = accent,
        lcdBackground = lcdBackground,
        lcdTextColor = lcdText,
        lcdGlowColor = lcdText,
        lcdDimColor = lcdText.copy(alpha = 0.45f),
        lcdBorderColor = border,
        eqBarGradient = eq,
        eqPeakHoldColor = Color.White,
        eqGridLineColor = lcdText.copy(alpha = 0.12f),
        buttonBackground = Brush.verticalGradient(buttons),
        buttonBorderColor = border,
        buttonHighlightColor = buttons.first(),
        buttonShadowColor = Color(0xFF000000),
        buttonIconColor = buttonIcon,
        buttonShapeRounded = rounded,
        sliderTrackColor = lcdBackground,
        sliderThumbColor = secondary,
        sliderThumbBorder = border,
        accentColor = accent,
        secondaryAccentColor = secondary
    )

    val PS2_MONOLITH = skin(
        id = "ps2_monolith",
        name = "Monolith Black '00",
        era = "2000",
        description = "Matte black tower console with blue power LED and ridged vents.",
        chassis = listOf(Color(0xFF1A1C22), Color(0xFF0E0F13), Color(0xFF07080B)),
        border = Color(0xFF2E3A5C),
        lcdBackground = Color(0xFF02040A),
        lcdText = Color(0xFF3B82F6),
        accent = Color(0xFF3B82F6),
        secondary = Color(0xFFE5E7EB),
        buttons = listOf(Color(0xFF2A2D35), Color(0xFF15171C)),
        buttonIcon = Color(0xFFBFDBFE),
        eq = listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6), Color(0xFF93C5FD)),
        rounded = 2
    )

    val DREAMCAST_SWIRL = skin(
        id = "dreamcast_swirl",
        name = "Swirl White '99",
        era = "1999",
        description = "Clean white shell with the iconic orange swirl and VMU-style LCD.",
        chassis = listOf(Color(0xFFF5F5F4), Color(0xFFE7E5E4), Color(0xFFD6D3D1)),
        border = Color(0xFFF97316),
        lcdBackground = Color(0xFF1C2A1C),
        lcdText = Color(0xFF9AE6B4),
        accent = Color(0xFFF97316),
        secondary = Color(0xFF2563EB),
        buttons = listOf(Color(0xFFFFFFFF), Color(0xFFE7E5E4)),
        buttonIcon = Color(0xFF1F2937),
        eq = listOf(Color(0xFFF97316), Color(0xFFFB923C), Color(0xFFFDBA74)),
        titleText = Color(0xFF1F2937),
        rounded = 10
    )

    val NOKIA_BRICK = skin(
        id = "nokia_brick",
        name = "Snake Phone 3310",
        era = "2000",
        description = "Indestructible navy brick phone with a backlit green monochrome screen.",
        chassis = listOf(Color(0xFF1E3A5F), Color(0xFF172E4D), Color(0xFF0F1F36)),
        border = Color(0xFF60A5FA),
        lcdBackground = Color(0xFF9BBB59),
        lcdText = Color(0xFF1B2B0E),
        accent = Color(0xFFA3E635),
        secondary = Color(0xFFE5E7EB),
        buttons = listOf(Color(0xFF9CA3AF), Color(0xFF6B7280)),
        buttonIcon = Color(0xFF0F172A),
        eq = listOf(Color(0xFF365314), Color(0xFF4D7C0F), Color(0xFF84CC16)),
        rounded = 12
    )

    val TAMAGOTCHI_EGG = skin(
        id = "tamagotchi_egg",
        name = "Virtual Pet Egg '97",
        era = "1997",
        description = "Candy-pink pocket pet shell with a pixel LCD and three chunky buttons.",
        chassis = listOf(Color(0xFFF472B6), Color(0xFFEC4899), Color(0xFFDB2777)),
        border = Color(0xFFFDE68A),
        lcdBackground = Color(0xFFC7D2A4),
        lcdText = Color(0xFF2D3319),
        accent = Color(0xFFFDE047),
        secondary = Color(0xFF7DD3FC),
        buttons = listOf(Color(0xFFFEF08A), Color(0xFFFACC15)),
        buttonIcon = Color(0xFF831843),
        eq = listOf(Color(0xFF831843), Color(0xFFDB2777), Color(0xFFFDE047)),
        rounded = 16,
        translucent = true
    )

    val XBOX_DUKE = skin(
        id = "xbox_duke",
        name = "Green Jewel Duke '01",
        era = "2001",
        description = "Hulking black console with a glowing green jewel and chunky controller vibes.",
        chassis = listOf(Color(0xFF111312), Color(0xFF0A0C0B), Color(0xFF050605)),
        border = Color(0xFF16A34A),
        lcdBackground = Color(0xFF020803),
        lcdText = Color(0xFF4ADE80),
        accent = Color(0xFF22C55E),
        secondary = Color(0xFFFACC15),
        buttons = listOf(Color(0xFF1F2420), Color(0xFF0F1210)),
        buttonIcon = Color(0xFF86EFAC),
        eq = listOf(Color(0xFF14532D), Color(0xFF16A34A), Color(0xFFBBF7D0))
    )

    val XP_LUNA = skin(
        id = "xp_luna",
        name = "Luna Blue Desktop '01",
        era = "2001",
        description = "Glossy blue taskbar, green Start button and rolling-hills optimism.",
        chassis = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF1E40AF)),
        border = Color(0xFF60A5FA),
        lcdBackground = Color(0xFF0B1B3F),
        lcdText = Color(0xFFE0F2FE),
        accent = Color(0xFF4ADE80),
        secondary = Color(0xFFFDE68A),
        buttons = listOf(Color(0xFF4ADE80), Color(0xFF16A34A)),
        buttonIcon = Color(0xFFFFFFFF),
        eq = listOf(Color(0xFF16A34A), Color(0xFF4ADE80), Color(0xFFBBF7D0)),
        rounded = 8,
        metallic = true
    )

    val ZIP_DRIVE = skin(
        id = "zip_drive_blue",
        name = "Zip Disk 100MB",
        era = "1995",
        description = "Smurf-blue removable drive with translucent purple disk window.",
        chassis = listOf(Color(0xFF1E3A8A), Color(0xFF1E40AF), Color(0xFF312E81)),
        border = Color(0xFFA78BFA),
        lcdBackground = Color(0xFF0E0B2A),
        lcdText = Color(0xFFC4B5FD),
        accent = Color(0xFFA78BFA),
        secondary = Color(0xFF38BDF8),
        buttons = listOf(Color(0xFF4338CA), Color(0xFF312E81)),
        buttonIcon = Color(0xFFE0E7FF),
        eq = listOf(Color(0xFF6D28D9), Color(0xFFA78BFA), Color(0xFF38BDF8)),
        translucent = true
    )

    val PAGER_BEEPER = skin(
        id = "pager_beeper",
        name = "Beeper Clip-On '96",
        era = "1996",
        description = "Translucent teal belt pager with a green backlit two-line display.",
        chassis = listOf(Color(0xFF0F766E), Color(0xFF115E59), Color(0xFF134E4A)),
        border = Color(0xFF5EEAD4),
        lcdBackground = Color(0xFF0B2A1E),
        lcdText = Color(0xFF6EE7B7),
        accent = Color(0xFF2DD4BF),
        secondary = Color(0xFFF0ABFC),
        buttons = listOf(Color(0xFF334155), Color(0xFF1E293B)),
        buttonIcon = Color(0xFFCCFBF1),
        eq = listOf(Color(0xFF0D9488), Color(0xFF2DD4BF), Color(0xFFF0ABFC)),
        rounded = 8,
        translucent = true
    )

    val ALL: List<RetroSkin> = listOf(
        PS2_MONOLITH, DREAMCAST_SWIRL, NOKIA_BRICK, TAMAGOTCHI_EGG,
        XBOX_DUKE, XP_LUNA, ZIP_DRIVE, PAGER_BEEPER
    )
}
