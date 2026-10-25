package com.example.ui.skins

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.RetroSkin

object RetroSkinDefinitions {

    // 1. Classic MP3 2.0 (1999)
    val WINAMP_CLASSIC = RetroSkin(
        id = "winamp_classic",
        name = "Titanium Hi-Fi '99",
        era = "1999",
        description = "Sleek brushed titanium chassis, high-contrast neon green LED spectrum & golden accents.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF383A3F), Color(0xFF23252A), Color(0xFF1B1D22))
        ),
        chassisBorderColor = Color(0xFF5A5E66),
        chassisBevelHighlight = Color(0xFF7A7E88),
        chassisBevelShadow = Color(0xFF101114),
        isMetallic = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF26292F), Color(0xFF424650), Color(0xFF26292F))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFF00FF66),
        lcdBackground = Color(0xFF000000),
        lcdTextColor = Color(0xFF00FF00),
        lcdGlowColor = Color(0xFF00FF66),
        lcdDimColor = Color(0xFF003300),
        lcdBorderColor = Color(0xFF4A4E57),
        eqBarGradient = listOf(Color(0xFFFF0055), Color(0xFFFFCC00), Color(0xFF00FF66)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF152A15),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF4A4E58), Color(0xFF2C2F36), Color(0xFF202227))
        ),
        buttonBorderColor = Color(0xFF6B7280),
        buttonHighlightColor = Color(0xFF8B92A0),
        buttonShadowColor = Color(0xFF141619),
        buttonIconColor = Color(0xFFE2E8F0),
        buttonShapeRounded = 2,
        sliderTrackColor = Color(0xFF15171C),
        sliderThumbColor = Color(0xFFFFCC00),
        sliderThumbBorder = Color(0xFFB45309),
        accentColor = Color(0xFF00FF66),
        secondaryAccentColor = Color(0xFFFFCC00)
    )

    // 2. Translucent Bondi Aqua
    val IMAC_BONDI = RetroSkin(
        id = "imac_bondi",
        name = "Aqua Bubble '98",
        era = "1998",
        description = "Translucent aqua casing with frosted curves and luminous aqua glow.",
        chassisBackground = Brush.radialGradient(
            listOf(Color(0xFF00A896), Color(0xFF028090), Color(0xFF05668D))
        ),
        chassisBorderColor = Color(0xFF02C39A),
        chassisBevelHighlight = Color(0x80E0FBFC),
        chassisBevelShadow = Color(0x60003049),
        isTranslucent = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF028090), Color(0xFF00A896), Color(0xFF028090))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFFE0FBFC),
        lcdBackground = Color(0xFF002830),
        lcdTextColor = Color(0xFF00F5D4),
        lcdGlowColor = Color(0xFF70E000),
        lcdDimColor = Color(0xFF00444A),
        lcdBorderColor = Color(0xFF00A896),
        eqBarGradient = listOf(Color(0xFF9B5DE5), Color(0xFF00BBF9), Color(0xFF00F5D4)),
        eqPeakHoldColor = Color(0xFFF15BB5),
        eqGridLineColor = Color(0xFF004455),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFFCBD5E1))
        ),
        buttonBorderColor = Color(0xFF94A3B8),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF64748B),
        buttonIconColor = Color(0xFF05668D),
        buttonShapeRounded = 12,
        sliderTrackColor = Color(0xFF024654),
        sliderThumbColor = Color(0xFF00F5D4),
        sliderThumbBorder = Color(0xFF028090),
        accentColor = Color(0xFF00F5D4),
        secondaryAccentColor = Color(0xFFF15BB5)
    )

    // 3. Vintage Cassette Portable TPS
    val WALKMAN_TPS = RetroSkin(
        id = "walkman_tps",
        name = "Tape Deck '79",
        era = "1979/90s",
        description = "Iconic navy and silver brushed aluminum chassis with bright orange hotline button.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF1E3A8A), Color(0xFF172554), Color(0xFF0F172A))
        ),
        chassisBorderColor = Color(0xFF94A3B8),
        chassisBevelHighlight = Color(0xFFCBD5E1),
        chassisBevelShadow = Color(0xFF020617),
        isMetallic = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0), Color(0xFF64748B))
        ),
        titleBarTextColor = Color(0xFF0F172A),
        titleBarAccentColor = Color(0xFFF97316),
        lcdBackground = Color(0xFF334155),
        lcdTextColor = Color(0xFFFEF08A),
        lcdGlowColor = Color(0xFFFACC15),
        lcdDimColor = Color(0xFF475569),
        lcdBorderColor = Color(0xFF64748B),
        eqBarGradient = listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFEAB308)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF1E293B),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFF97316), Color(0xFFEA580C), Color(0xFFC2410C))
        ),
        buttonBorderColor = Color(0xFFFB923C),
        buttonHighlightColor = Color(0xFFFED7AA),
        buttonShadowColor = Color(0xFF7C2D12),
        buttonIconColor = Color(0xFFFFFFFF),
        buttonShapeRounded = 4,
        sliderTrackColor = Color(0xFF0F172A),
        sliderThumbColor = Color(0xFFF97316),
        sliderThumbBorder = Color(0xFF9A3412),
        accentColor = Color(0xFFF97316),
        secondaryAccentColor = Color(0xFF38BDF8)
    )

    // 4. Anti-Shock Disc Player
    val DISCMAN_ESP = RetroSkin(
        id = "discman_esp",
        name = "Skip-Free CD '96",
        era = "1997",
        description = "Circular brushed metallic silver finish, holographic disc shimmer, and crisp LCD segments.",
        chassisBackground = Brush.linearGradient(
            listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFFE2E8F0))
        ),
        chassisBorderColor = Color(0xFFE2E8F0),
        chassisBevelHighlight = Color(0xFFFFFFFF),
        chassisBevelShadow = Color(0xFF475569),
        isMetallic = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF475569), Color(0xFF64748B), Color(0xFF334155))
        ),
        titleBarTextColor = Color(0xFFF8FAFC),
        titleBarAccentColor = Color(0xFF38BDF8),
        lcdBackground = Color(0xFFB5C99A),
        lcdTextColor = Color(0xFF1B2A18),
        lcdGlowColor = Color(0xFF87986A),
        lcdDimColor = Color(0xFFA5B888),
        lcdBorderColor = Color(0xFF475569),
        eqBarGradient = listOf(Color(0xFF2563EB), Color(0xFF38BDF8), Color(0xFF06B6D4)),
        eqPeakHoldColor = Color(0xFF0284C7),
        eqGridLineColor = Color(0xFF8E9E74),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFF94A3B8))
        ),
        buttonBorderColor = Color(0xFF64748B),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF334155),
        buttonIconColor = Color(0xFF0F172A),
        buttonShapeRounded = 8,
        sliderTrackColor = Color(0xFF64748B),
        sliderThumbColor = Color(0xFF0284C7),
        sliderThumbBorder = Color(0xFF0369A1),
        accentColor = Color(0xFF0284C7),
        secondaryAccentColor = Color(0xFFE11D48)
    )

    // 5. Classic Desktop OS 98
    val WIN98_PLAYER = RetroSkin(
        id = "win98_player",
        name = "OS '98 Player",
        era = "1998",
        description = "Pure nostalgic gray beveled 3D borders, classic navy title bar, and system buttons.",
        chassisBackground = Brush.linearGradient(
            listOf(Color(0xFFC0C0C0), Color(0xFFC0C0C0))
        ),
        chassisBorderColor = Color(0xFF000000),
        chassisBevelHighlight = Color(0xFFFFFFFF),
        chassisBevelShadow = Color(0xFF808080),
        isWin98Style = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF000080), Color(0xFF1084D0))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFFFFFF00),
        lcdBackground = Color(0xFF000000),
        lcdTextColor = Color(0xFF00FF00),
        lcdGlowColor = Color(0xFF00FF00),
        lcdDimColor = Color(0xFF002200),
        lcdBorderColor = Color(0xFF808080),
        eqBarGradient = listOf(Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF003300),
        buttonBackground = Brush.linearGradient(
            listOf(Color(0xFFC0C0C0), Color(0xFFC0C0C0))
        ),
        buttonBorderColor = Color(0xFF000000),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF808080),
        buttonIconColor = Color(0xFF000000),
        buttonShapeRounded = 0,
        sliderTrackColor = Color(0xFF808080),
        sliderThumbColor = Color(0xFFC0C0C0),
        sliderThumbBorder = Color(0xFF000000),
        accentColor = Color(0xFF000080),
        secondaryAccentColor = Color(0xFF008080)
    )

    // 6. Cyber Matrix Neon 1999
    val CYBER_MATRIX = RetroSkin(
        id = "cyber_matrix",
        name = "Cyber Matrix 1999",
        era = "1999",
        description = "Falling green rain code phosphor on absolute black with digital scanline grid.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF0A0F0D), Color(0xFF050806), Color(0xFF000000))
        ),
        chassisBorderColor = Color(0xFF00FF41),
        chassisBevelHighlight = Color(0xFF00FF66),
        chassisBevelShadow = Color(0xFF002200),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF002200), Color(0xFF004400), Color(0xFF001100))
        ),
        titleBarTextColor = Color(0xFF00FF41),
        titleBarAccentColor = Color(0xFF33FF77),
        lcdBackground = Color(0xFF020B04),
        lcdTextColor = Color(0xFF00FF41),
        lcdGlowColor = Color(0xFF00FF66),
        lcdDimColor = Color(0xFF003311),
        lcdBorderColor = Color(0xFF00AA33),
        eqBarGradient = listOf(Color(0xFF008F11), Color(0xFF00FF41), Color(0xFF66FFAA)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF003311),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF0D1B11), Color(0xFF050D07), Color(0xFF000000))
        ),
        buttonBorderColor = Color(0xFF00FF41),
        buttonHighlightColor = Color(0xFF33FF77),
        buttonShadowColor = Color(0xFF002200),
        buttonIconColor = Color(0xFF00FF41),
        buttonShapeRounded = 2,
        sliderTrackColor = Color(0xFF002200),
        sliderThumbColor = Color(0xFF00FF41),
        sliderThumbBorder = Color(0xFF008F11),
        accentColor = Color(0xFF00FF41),
        secondaryAccentColor = Color(0xFF008F11)
    )

    // 7. Y2K Millennium Bubblegum Pink
    val Y2K_BUBBLEGUM = RetroSkin(
        id = "y2k_bubblegum",
        name = "Y2K Millennium Pink",
        era = "2000",
        description = "Glossy translucent hot magenta jelly with chrome trim and shimmering sparkle highlights.",
        chassisBackground = Brush.radialGradient(
            listOf(Color(0xFFFF70A6), Color(0xFFFF1654), Color(0xFF700353))
        ),
        chassisBorderColor = Color(0xFFFF99C8),
        chassisBevelHighlight = Color(0xCCFFFFFF),
        chassisBevelShadow = Color(0x884A0033),
        isTranslucent = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFFFF1654), Color(0xFFFF70A6), Color(0xFFFF1654))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFFFFE3E3),
        lcdBackground = Color(0xFF330022),
        lcdTextColor = Color(0xFFFF70A6),
        lcdGlowColor = Color(0xFFFFB3D1),
        lcdDimColor = Color(0xFF55003A),
        lcdBorderColor = Color(0xFFFF70A6),
        eqBarGradient = listOf(Color(0xFF700353), Color(0xFFFF1654), Color(0xFFFF99C8)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF4A0030),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFFFD6E7), Color(0xFFFF99C8))
        ),
        buttonBorderColor = Color(0xFFFF1654),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFFB3004B),
        buttonIconColor = Color(0xFF700353),
        buttonShapeRounded = 16,
        sliderTrackColor = Color(0xFF4A0033),
        sliderThumbColor = Color(0xFFFF1654),
        sliderThumbBorder = Color(0xFFFFFFFF),
        accentColor = Color(0xFFFF70A6),
        secondaryAccentColor = Color(0xFF70D6FF)
    )

    // 8. Vaporwave Sunset '95
    val VAPORWAVE_95 = RetroSkin(
        id = "vaporwave_95",
        name = "Vaporwave Sunset '95",
        era = "1995",
        description = "Synthwave magenta and cyan horizon, sunset neon grid, and dreamy nostalgic pastels.",
        chassisBackground = Brush.linearGradient(
            listOf(Color(0xFF240046), Color(0xFF3C096C), Color(0xFF5A189A), Color(0xFF7B2CBF))
        ),
        chassisBorderColor = Color(0xFF00F5D4),
        chassisBevelHighlight = Color(0xFFFF9E00),
        chassisBevelShadow = Color(0xFF10002B),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFFFF007F), Color(0xFF7B2CBF), Color(0xFF00F5D4))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFFFFE600),
        lcdBackground = Color(0xFF10002B),
        lcdTextColor = Color(0xFF00F5D4),
        lcdGlowColor = Color(0xFFFF007F),
        lcdDimColor = Color(0xFF240046),
        lcdBorderColor = Color(0xFF7B2CBF),
        eqBarGradient = listOf(Color(0xFFFF007F), Color(0xFFFF9E00), Color(0xFF00F5D4)),
        eqPeakHoldColor = Color(0xFFFFFF00),
        eqGridLineColor = Color(0xFF2D0A4E),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF9D4EDD), Color(0xFF5A189A), Color(0xFF240046))
        ),
        buttonBorderColor = Color(0xFF00F5D4),
        buttonHighlightColor = Color(0xFFFF70A6),
        buttonShadowColor = Color(0xFF10002B),
        buttonIconColor = Color(0xFF00F5D4),
        buttonShapeRounded = 6,
        sliderTrackColor = Color(0xFF10002B),
        sliderThumbColor = Color(0xFFFF007F),
        sliderThumbBorder = Color(0xFF00F5D4),
        accentColor = Color(0xFF00F5D4),
        secondaryAccentColor = Color(0xFFFF007F)
    )

    // 9. 8-Bit Dot Matrix Portable (1989/90s)
    val GAMEBOY_DMG = RetroSkin(
        id = "gameboy_dmg",
        name = "8-Bit Handheld '89",
        era = "1989/90s",
        description = "Dot-matrix olive LCD screen, pebble-gray chassis, and iconic dark magenta A/B buttons.",
        chassisBackground = Brush.linearGradient(
            listOf(Color(0xFFD6D6C2), Color(0xFFBDBDA8), Color(0xFFA5A591))
        ),
        chassisBorderColor = Color(0xFF8C8C7A),
        chassisBevelHighlight = Color(0xFFEBEBE0),
        chassisBevelShadow = Color(0xFF6B6B5E),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF5C5C70), Color(0xFF707085), Color(0xFF4A4A5A))
        ),
        titleBarTextColor = Color(0xFFBDBDA8),
        titleBarAccentColor = Color(0xFF9C2A5B),
        lcdBackground = Color(0xFF8B956D),
        lcdTextColor = Color(0xFF0F380F),
        lcdGlowColor = Color(0xFF306230),
        lcdDimColor = Color(0xFF8BAC0F),
        lcdBorderColor = Color(0xFF6B7552),
        eqBarGradient = listOf(Color(0xFF0F380F), Color(0xFF306230), Color(0xFF8BAC0F)),
        eqPeakHoldColor = Color(0xFF0F380F),
        eqGridLineColor = Color(0xFF7A835E),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFB82865), Color(0xFF8A1E4B), Color(0xFF661536))
        ),
        buttonBorderColor = Color(0xFF4A0F27),
        buttonHighlightColor = Color(0xFFD44C82),
        buttonShadowColor = Color(0xFF3B0C1F),
        buttonIconColor = Color(0xFFFFFFFF),
        buttonShapeRounded = 10,
        sliderTrackColor = Color(0xFF6B6B5E),
        sliderThumbColor = Color(0xFF8A1E4B),
        sliderThumbBorder = Color(0xFF4A0F27),
        accentColor = Color(0xFF8A1E4B),
        secondaryAccentColor = Color(0xFF0F380F)
    )

    // 10. VCR Camcorder OSD
    val VCR_CAMCORDER = RetroSkin(
        id = "vcr_camcorder",
        name = "VCR Camcorder OSD",
        era = "1994",
        description = "Analog tape CRT display, bright lime OSD typography 'PLAY ▶ SP 1999', and VHS tracking.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF141414), Color(0xFF080808), Color(0xFF000000))
        ),
        chassisBorderColor = Color(0xFF2A2A2A),
        chassisBevelHighlight = Color(0xFF3A3A3A),
        chassisBevelShadow = Color(0xFF000000),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF1A1A1A), Color(0xFF2E2E2E), Color(0xFF121212))
        ),
        titleBarTextColor = Color(0xFF00FF66),
        titleBarAccentColor = Color(0xFFFF3333),
        lcdBackground = Color(0xFF000000),
        lcdTextColor = Color(0xFFFFFFFF),
        lcdGlowColor = Color(0xFF00FF66),
        lcdDimColor = Color(0xFF333333),
        lcdBorderColor = Color(0xFF222222),
        eqBarGradient = listOf(Color(0xFFFF3333), Color(0xFFFFFF33), Color(0xFF33FF33)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF111111),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A), Color(0xFF0F0F0F))
        ),
        buttonBorderColor = Color(0xFF444444),
        buttonHighlightColor = Color(0xFF555555),
        buttonShadowColor = Color(0xFF000000),
        buttonIconColor = Color(0xFF00FF66),
        buttonShapeRounded = 2,
        sliderTrackColor = Color(0xFF1A1A1A),
        sliderThumbColor = Color(0xFF00FF66),
        sliderThumbBorder = Color(0xFF00AA44),
        accentColor = Color(0xFF00FF66),
        secondaryAccentColor = Color(0xFFFF3333)
    )

    // 11. Magneto-Optical Disc Sonic Blue
    val MINIDISC_SONIC = RetroSkin(
        id = "minidisc_sonic",
        name = "Sonic Disc '99",
        era = "1998",
        description = "Cobalt blue magnesium alloy shell, amber optical sub-display, and sleek jog dial.",
        chassisBackground = Brush.linearGradient(
            listOf(Color(0xFF1D4ED8), Color(0xFF1E40AF), Color(0xFF172554))
        ),
        chassisBorderColor = Color(0xFF60A5FA),
        chassisBevelHighlight = Color(0xFF93C5FD),
        chassisBevelShadow = Color(0xFF0F172A),
        isMetallic = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF1E3A8A))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFFF59E0B),
        lcdBackground = Color(0xFF291B00),
        lcdTextColor = Color(0xFFFBBF24),
        lcdGlowColor = Color(0xFFF59E0B),
        lcdDimColor = Color(0xFF4A3405),
        lcdBorderColor = Color(0xFF78350F),
        eqBarGradient = listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF10B981)),
        eqPeakHoldColor = Color(0xFFFDE68A),
        eqGridLineColor = Color(0xFF3B2706),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF64748B))
        ),
        buttonBorderColor = Color(0xFFCBD5E1),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF334155),
        buttonIconColor = Color(0xFF1E3A8A),
        buttonShapeRounded = 8,
        sliderTrackColor = Color(0xFF0F172A),
        sliderThumbColor = Color(0xFFF59E0B),
        sliderThumbBorder = Color(0xFFB45309),
        accentColor = Color(0xFFF59E0B),
        secondaryAccentColor = Color(0xFF60A5FA)
    )

    // 12. Peer-to-Peer 1999 Green
    val NAPSTER_GREEN = RetroSkin(
        id = "napster_green",
        name = "PeerNet 1999 Green",
        era = "1999",
        description = "Dark gunmetal gray with the iconic glowing green headphone logo aesthetic.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF262626), Color(0xFF171717), Color(0xFF0A0A0A))
        ),
        chassisBorderColor = Color(0xFF22C55E),
        chassisBevelHighlight = Color(0xFF404040),
        chassisBevelShadow = Color(0xFF000000),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF14532D), Color(0xFF16A34A), Color(0xFF14532D))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFF86EFAC),
        lcdBackground = Color(0xFF052E16),
        lcdTextColor = Color(0xFF4ADE80),
        lcdGlowColor = Color(0xFF22C55E),
        lcdDimColor = Color(0xFF14532D),
        lcdBorderColor = Color(0xFF166534),
        eqBarGradient = listOf(Color(0xFF15803D), Color(0xFF22C55E), Color(0xFF86EFAC)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF0A3F20),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF404040), Color(0xFF262626), Color(0xFF171717))
        ),
        buttonBorderColor = Color(0xFF22C55E),
        buttonHighlightColor = Color(0xFF525252),
        buttonShadowColor = Color(0xFF0A0A0A),
        buttonIconColor = Color(0xFF4ADE80),
        buttonShapeRounded = 4,
        sliderTrackColor = Color(0xFF0A0A0A),
        sliderThumbColor = Color(0xFF22C55E),
        sliderThumbBorder = Color(0xFF166534),
        accentColor = Color(0xFF22C55E),
        secondaryAccentColor = Color(0xFF86EFAC)
    )

    // 13. Ghettoblaster Boombox
    val BOOMBOX_GHETTO = RetroSkin(
        id = "boombox_ghetto",
        name = "Street Studio Boombox",
        era = "1988/90s",
        description = "Twin analog needle VU meters, heavy metallic speaker grills, and chrome toggle knobs.",
        chassisBackground = Brush.linearGradient(
            listOf(Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B))
        ),
        chassisBorderColor = Color(0xFF94A3B8),
        chassisBevelHighlight = Color(0xFFCBD5E1),
        chassisBevelShadow = Color(0xFF0F172A),
        isMetallic = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF1E293B))
        ),
        titleBarTextColor = Color(0xFFFEF08A),
        titleBarAccentColor = Color(0xFFEF4444),
        lcdBackground = Color(0xFF422006),
        lcdTextColor = Color(0xFFFDE047),
        lcdGlowColor = Color(0xFFEAB308),
        lcdDimColor = Color(0xFF713F12),
        lcdBorderColor = Color(0xFF854D0E),
        eqBarGradient = listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFEAB308)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF542C08),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFF8FAFC), Color(0xFFCBD5E1), Color(0xFF64748B))
        ),
        buttonBorderColor = Color(0xFF94A3B8),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF1E293B),
        buttonIconColor = Color(0xFF0F172A),
        buttonShapeRounded = 2,
        sliderTrackColor = Color(0xFF0F172A),
        sliderThumbColor = Color(0xFFEF4444),
        sliderThumbBorder = Color(0xFF991B1B),
        accentColor = Color(0xFFEAB308),
        secondaryAccentColor = Color(0xFFEF4444)
    )

    // 14. Glossy Piano Obsidian Crystal
    val PSP_PIANO_BLACK = RetroSkin(
        id = "psp_piano_black",
        name = "Obsidian Glass Menu",
        era = "2004",
        description = "Glossy piano black obsidian finish with smooth wave ribbons and crystal glass buttons.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF18181B), Color(0xFF09090B), Color(0xFF000000))
        ),
        chassisBorderColor = Color(0xFF3F3F46),
        chassisBevelHighlight = Color(0xFF52525B),
        chassisBevelShadow = Color(0xFF000000),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF09090B), Color(0xFF27272A), Color(0xFF09090B))
        ),
        titleBarTextColor = Color(0xFFF4F4F5),
        titleBarAccentColor = Color(0xFF38BDF8),
        lcdBackground = Color(0xFF09090B),
        lcdTextColor = Color(0xFFFFFFFF),
        lcdGlowColor = Color(0xFF38BDF8),
        lcdDimColor = Color(0xFF27272A),
        lcdBorderColor = Color(0xFF27272A),
        eqBarGradient = listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFBAE6FD)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF18181B),
        buttonBackground = Brush.radialGradient(
            listOf(Color(0xFF27272A), Color(0xFF18181B), Color(0xFF09090B))
        ),
        buttonBorderColor = Color(0xFF52525B),
        buttonHighlightColor = Color(0xFF71717A),
        buttonShadowColor = Color(0xFF000000),
        buttonIconColor = Color(0xFFF4F4F5),
        buttonShapeRounded = 24,
        sliderTrackColor = Color(0xFF27272A),
        sliderThumbColor = Color(0xFF38BDF8),
        sliderThumbBorder = Color(0xFF0284C7),
        accentColor = Color(0xFF38BDF8),
        secondaryAccentColor = Color(0xFFA855F7)
    )

    // 15. Scroll Wheel White Acrylic (2001)
    val IPOD_CLASSIC_01 = RetroSkin(
        id = "ipod_classic_01",
        name = "Scroll Wheel 2001",
        era = "2001",
        description = "Pure white acrylic casing, mechanical scroll wheel graphic, and monochrome LCD backlight.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFFE2E8F0))
        ),
        chassisBorderColor = Color(0xFFCBD5E1),
        chassisBevelHighlight = Color(0xFFFFFFFF),
        chassisBevelShadow = Color(0xFF94A3B8),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFFE2E8F0), Color(0xFFF8FAFC), Color(0xFFCBD5E1))
        ),
        titleBarTextColor = Color(0xFF0F172A),
        titleBarAccentColor = Color(0xFF64748B),
        lcdBackground = Color(0xFFD6E2E6),
        lcdTextColor = Color(0xFF1E293B),
        lcdGlowColor = Color(0xFFB8D0D6),
        lcdDimColor = Color(0xFFCAD8DC),
        lcdBorderColor = Color(0xFF94A3B8),
        eqBarGradient = listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF64748B)),
        eqPeakHoldColor = Color(0xFF0F172A),
        eqGridLineColor = Color(0xFFB8CBD0),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFFCBD5E1))
        ),
        buttonBorderColor = Color(0xFF94A3B8),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF64748B),
        buttonIconColor = Color(0xFF334155),
        buttonShapeRounded = 24,
        sliderTrackColor = Color(0xFF94A3B8),
        sliderThumbColor = Color(0xFF334155),
        sliderThumbBorder = Color(0xFF0F172A),
        accentColor = Color(0xFF0284C7),
        secondaryAccentColor = Color(0xFF64748B)
    )

    // 16. Hot Rod Flame Red
    val HOT_ROD_FLAME = RetroSkin(
        id = "hot_rod_flame",
        name = "Hot Rod Flame 99",
        era = "1999",
        description = "High-octane candy apple metallic red with chrome flame accents and fiery amber meters.",
        chassisBackground = Brush.radialGradient(
            listOf(Color(0xFFDC2626), Color(0xFF991B1B), Color(0xFF450A0A))
        ),
        chassisBorderColor = Color(0xFFFCA5A5),
        chassisBevelHighlight = Color(0xFFFEF08A),
        chassisBevelShadow = Color(0xFF200000),
        isMetallic = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF7F1D1D), Color(0xFFB91C1C), Color(0xFF7F1D1D))
        ),
        titleBarTextColor = Color(0xFFFEF08A),
        titleBarAccentColor = Color(0xFFF97316),
        lcdBackground = Color(0xFF280505),
        lcdTextColor = Color(0xFFFDE047),
        lcdGlowColor = Color(0xFFF97316),
        lcdDimColor = Color(0xFF4A0E0E),
        lcdBorderColor = Color(0xFF991B1B),
        eqBarGradient = listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFFDE047)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF3D0909),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFD97706))
        ),
        buttonBorderColor = Color(0xFFFEF08A),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF78350F),
        buttonIconColor = Color(0xFF450A0A),
        buttonShapeRounded = 6,
        sliderTrackColor = Color(0xFF450A0A),
        sliderThumbColor = Color(0xFFFDE047),
        sliderThumbBorder = Color(0xFFF97316),
        accentColor = Color(0xFFFDE047),
        secondaryAccentColor = Color(0xFFEF4444)
    )

    // 17. Acid Techno 303
    val ACID_TECHNO = RetroSkin(
        id = "acid_techno",
        name = "Acid Techno 303",
        era = "1996",
        description = "Day-glo toxic yellow, caution stripes, and radioactive green rave oscilloscope.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF1C1917), Color(0xFF0C0A09), Color(0xFF000000))
        ),
        chassisBorderColor = Color(0xFFEAB308),
        chassisBevelHighlight = Color(0xFFFDE047),
        chassisBevelShadow = Color(0xFF000000),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFFCA8A04), Color(0xFFEAB308), Color(0xFFA16207))
        ),
        titleBarTextColor = Color(0xFF000000),
        titleBarAccentColor = Color(0xFF22C55E),
        lcdBackground = Color(0xFF0C0A09),
        lcdTextColor = Color(0xFFFACC15),
        lcdGlowColor = Color(0xFF4ADE80),
        lcdDimColor = Color(0xFF292524),
        lcdBorderColor = Color(0xFFCA8A04),
        eqBarGradient = listOf(Color(0xFF22C55E), Color(0xFFEAB308), Color(0xFFEF4444)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF1C1917),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFFDE047), Color(0xFFEAB308), Color(0xFFCA8A04))
        ),
        buttonBorderColor = Color(0xFF000000),
        buttonHighlightColor = Color(0xFFFEF08A),
        buttonShadowColor = Color(0xFF713F12),
        buttonIconColor = Color(0xFF000000),
        buttonShapeRounded = 2,
        sliderTrackColor = Color(0xFF292524),
        sliderThumbColor = Color(0xFF22C55E),
        sliderThumbBorder = Color(0xFF15803D),
        accentColor = Color(0xFFFACC15),
        secondaryAccentColor = Color(0xFF22C55E)
    )

    // 18. Millennium Media Console 2000
    val WMP_MILLENNIUM = RetroSkin(
        id = "wmp_millennium",
        name = "Millennium Media 2000",
        era = "2000",
        description = "Sleek curves, cosmic cobalt blue console, glowing media orb, and millennium silver trim.",
        chassisBackground = Brush.linearGradient(
            listOf(Color(0xFF1E3A8A), Color(0xFF0284C7), Color(0xFF0C4A6E))
        ),
        chassisBorderColor = Color(0xFF38BDF8),
        chassisBevelHighlight = Color(0xFFE0F2FE),
        chassisBevelShadow = Color(0xFF082F49),
        isTranslucent = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF075985), Color(0xFF0284C7), Color(0xFF0369A1))
        ),
        titleBarTextColor = Color(0xFFF0F9FF),
        titleBarAccentColor = Color(0xFF38BDF8),
        lcdBackground = Color(0xFF031E2E),
        lcdTextColor = Color(0xFF38BDF8),
        lcdGlowColor = Color(0xFF7DD3FC),
        lcdDimColor = Color(0xFF075985),
        lcdBorderColor = Color(0xFF0284C7),
        eqBarGradient = listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFBAE6FD)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF083344),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFF7DD3FC))
        ),
        buttonBorderColor = Color(0xFF0284C7),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF0369A1),
        buttonIconColor = Color(0xFF075985),
        buttonShapeRounded = 14,
        sliderTrackColor = Color(0xFF082F49),
        sliderThumbColor = Color(0xFF38BDF8),
        sliderThumbBorder = Color(0xFF0284C7),
        accentColor = Color(0xFF38BDF8),
        secondaryAccentColor = Color(0xFF0284C7)
    )

    // 19. Hacker Amber CRT Terminal
    val HACKER_TERMINAL = RetroSkin(
        id = "hacker_terminal",
        name = "Hacker Amber CRT",
        era = "1991",
        description = "Amber monochrome glowing CRT phosphor with slow decay persistence and raster scanlines.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF1C1004), Color(0xFF0F0802), Color(0xFF000000))
        ),
        chassisBorderColor = Color(0xFFF59E0B),
        chassisBevelHighlight = Color(0xFFFBBF24),
        chassisBevelShadow = Color(0xFF451A03),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF451A03), Color(0xFF78350F), Color(0xFF451A03))
        ),
        titleBarTextColor = Color(0xFFFBBF24),
        titleBarAccentColor = Color(0xFFFDE68A),
        lcdBackground = Color(0xFF0B0500),
        lcdTextColor = Color(0xFFF59E0B),
        lcdGlowColor = Color(0xFFFBBF24),
        lcdDimColor = Color(0xFF451A03),
        lcdBorderColor = Color(0xFF92400E),
        eqBarGradient = listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFFFBBF24)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF291001),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF2E1504), Color(0xFF1A0B02), Color(0xFF0B0500))
        ),
        buttonBorderColor = Color(0xFFD97706),
        buttonHighlightColor = Color(0xFFF59E0B),
        buttonShadowColor = Color(0xFF000000),
        buttonIconColor = Color(0xFFFBBF24),
        buttonShapeRounded = 0,
        sliderTrackColor = Color(0xFF291001),
        sliderThumbColor = Color(0xFFF59E0B),
        sliderThumbBorder = Color(0xFFB45309),
        accentColor = Color(0xFFF59E0B),
        secondaryAccentColor = Color(0xFFFBBF24)
    )

    // 20. Glacier Translucent 32-Bit
    val GLACIER_GBA = RetroSkin(
        id = "glacier_gba",
        name = "Glacier Frost 32-Bit",
        era = "2001",
        description = "Frosty translucent clear violet chassis revealing motherboard circuit traces.",
        chassisBackground = Brush.radialGradient(
            listOf(Color(0xFFDDD6FE), Color(0xFFA78BFA), Color(0xFF6D28D9))
        ),
        chassisBorderColor = Color(0xFFC4B5FD),
        chassisBevelHighlight = Color(0xCCFFFFFF),
        chassisBevelShadow = Color(0x604C1D95),
        isTranslucent = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF6D28D9), Color(0xFF8B5CF6), Color(0xFF6D28D9))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFF2DD4BF),
        lcdBackground = Color(0xFF2E1065),
        lcdTextColor = Color(0xFF2DD4BF),
        lcdGlowColor = Color(0xFFA78BFA),
        lcdDimColor = Color(0xFF4C1D95),
        lcdBorderColor = Color(0xFF7C3AED),
        eqBarGradient = listOf(Color(0xFF8B5CF6), Color(0xFF2DD4BF), Color(0xFFF472B6)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF3B0764),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF7C3AED), Color(0xFF5B21B6), Color(0xFF4C1D95))
        ),
        buttonBorderColor = Color(0xFFA78BFA),
        buttonHighlightColor = Color(0xFFC4B5FD),
        buttonShadowColor = Color(0xFF2E1065),
        buttonIconColor = Color(0xFFFFFFFF),
        buttonShapeRounded = 12,
        sliderTrackColor = Color(0xFF3B0764),
        sliderThumbColor = Color(0xFF2DD4BF),
        sliderThumbBorder = Color(0xFF0F766E),
        accentColor = Color(0xFF2DD4BF),
        secondaryAccentColor = Color(0xFFF472B6)
    )

    // 21. Grape Translucent Purple G3
    val GRAPE_IMAC = RetroSkin(
        id = "grape_imac",
        name = "Grape Amethyst '99",
        era = "1999",
        description = "Juicy translucent deep amethyst purple casing with glowing white optical controls.",
        chassisBackground = Brush.radialGradient(
            listOf(Color(0xFF86198F), Color(0xFF701A75), Color(0xFF4A044E))
        ),
        chassisBorderColor = Color(0xFFD946EF),
        chassisBevelHighlight = Color(0xAAE879F9),
        chassisBevelShadow = Color(0x882A022E),
        isTranslucent = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF701A75), Color(0xFFA21CAF), Color(0xFF701A75))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFFFDF4FF),
        lcdBackground = Color(0xFF2A022E),
        lcdTextColor = Color(0xFFF5D0FE),
        lcdGlowColor = Color(0xFFE879F9),
        lcdDimColor = Color(0xFF581C87),
        lcdBorderColor = Color(0xFF86198F),
        eqBarGradient = listOf(Color(0xFFC026D3), Color(0xFFE879F9), Color(0xFFF5D0FE)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF3B0744),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFF5D0FE), Color(0xFFE879F9))
        ),
        buttonBorderColor = Color(0xFFA21CAF),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF701A75),
        buttonIconColor = Color(0xFF4A044E),
        buttonShapeRounded = 14,
        sliderTrackColor = Color(0xFF3B0744),
        sliderThumbColor = Color(0xFFE879F9),
        sliderThumbBorder = Color(0xFFA21CAF),
        accentColor = Color(0xFFE879F9),
        secondaryAccentColor = Color(0xFF38BDF8)
    )

    // 22. Vintage Hi-Fi Champagne Gold
    val HIFI_GOLD = RetroSkin(
        id = "hifi_gold",
        name = "Hi-Fi Champagne Gold",
        era = "1992",
        description = "Brushed champagne gold alloy, heavy knurled metal dials, and warm analog needles.",
        chassisBackground = Brush.linearGradient(
            listOf(Color(0xFFE4D5B7), Color(0xFFC9B78C), Color(0xFFA89462), Color(0xFFD6C6A0))
        ),
        chassisBorderColor = Color(0xFFF3E8CE),
        chassisBevelHighlight = Color(0xFFFFF9EC),
        chassisBevelShadow = Color(0xFF74643E),
        isMetallic = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF6B5931), Color(0xFF8E7947), Color(0xFF534421))
        ),
        titleBarTextColor = Color(0xFFFFF9EC),
        titleBarAccentColor = Color(0xFFFFD700),
        lcdBackground = Color(0xFF262013),
        lcdTextColor = Color(0xFFFFE082),
        lcdGlowColor = Color(0xFFFFD54F),
        lcdDimColor = Color(0xFF4A3E26),
        lcdBorderColor = Color(0xFF8E7947),
        eqBarGradient = listOf(Color(0xFFD32F2F), Color(0xFFF57C00), Color(0xFFFFD54F)),
        eqPeakHoldColor = Color(0xFFFFF9EC),
        eqGridLineColor = Color(0xFF3A311F),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFF3E8CE), Color(0xFFC9B78C), Color(0xFFA89462))
        ),
        buttonBorderColor = Color(0xFF74643E),
        buttonHighlightColor = Color(0xFFFFF9EC),
        buttonShadowColor = Color(0xFF42371E),
        buttonIconColor = Color(0xFF262013),
        buttonShapeRounded = 4,
        sliderTrackColor = Color(0xFF262013),
        sliderThumbColor = Color(0xFFFFD54F),
        sliderThumbBorder = Color(0xFF8E7947),
        accentColor = Color(0xFFFFD54F),
        secondaryAccentColor = Color(0xFFD32F2F)
    )

    // 23. Bento Modern Slate Deck (2007)
    val WINAMP_BENTO = RetroSkin(
        id = "winamp_bento",
        name = "Bento Modern Slate",
        era = "2007",
        description = "Sleek all-in-one dark obsidian chassis with electric cyan indicators and polished glass.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF252930), Color(0xFF191C22), Color(0xFF101216))
        ),
        chassisBorderColor = Color(0xFF38BDF8),
        chassisBevelHighlight = Color(0xFF475569),
        chassisBevelShadow = Color(0xFF0A0C0E),
        isMetallic = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF0F172A), Color(0xFF0284C7), Color(0xFF0F172A))
        ),
        titleBarTextColor = Color(0xFFE2E8F0),
        titleBarAccentColor = Color(0xFF38BDF8),
        lcdBackground = Color(0xFF0B0F19),
        lcdTextColor = Color(0xFF38BDF8),
        lcdGlowColor = Color(0xFF0EA5E9),
        lcdDimColor = Color(0xFF0C243C),
        lcdBorderColor = Color(0xFF1E293B),
        eqBarGradient = listOf(Color(0xFF06B6D4), Color(0xFF38BDF8), Color(0xFF60A5FA)),
        eqPeakHoldColor = Color(0xFFE0F2FE),
        eqGridLineColor = Color(0xFF132038),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A))
        ),
        buttonBorderColor = Color(0xFF475569),
        buttonHighlightColor = Color(0xFF64748B),
        buttonShadowColor = Color(0xFF05070A),
        buttonIconColor = Color(0xFF38BDF8),
        buttonShapeRounded = 6,
        sliderTrackColor = Color(0xFF0B0F19),
        sliderThumbColor = Color(0xFF38BDF8),
        sliderThumbBorder = Color(0xFF0284C7),
        accentColor = Color(0xFF38BDF8),
        secondaryAccentColor = Color(0xFFF59E0B)
    )

    // 24. Mega 16-Bit Blast Console (1989)
    val SEGA_GENESIS = RetroSkin(
        id = "sega_genesis",
        name = "Mega 16-Bit Blast",
        era = "1989",
        description = "Matte charcoal console chassis with iconic High Definition Audio gold ring & bright red power switch.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF282828), Color(0xFF1A1A1A), Color(0xFF111111))
        ),
        chassisBorderColor = Color(0xFFFFD700),
        chassisBevelHighlight = Color(0xFF444444),
        chassisBevelShadow = Color(0xFF050505),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF111111), Color(0xFF990000), Color(0xFF111111))
        ),
        titleBarTextColor = Color(0xFFFFD700),
        titleBarAccentColor = Color(0xFFFF3333),
        lcdBackground = Color(0xFF0A0A0A),
        lcdTextColor = Color(0xFFFF3333),
        lcdGlowColor = Color(0xFFFF5555),
        lcdDimColor = Color(0xFF330000),
        lcdBorderColor = Color(0xFFFFD700),
        eqBarGradient = listOf(Color(0xFFFF0033), Color(0xFFFFCC00), Color(0xFF0099FF)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF2B1010),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF3A3A3A), Color(0xFF222222), Color(0xFF141414))
        ),
        buttonBorderColor = Color(0xFF555555),
        buttonHighlightColor = Color(0xFF666666),
        buttonShadowColor = Color(0xFF000000),
        buttonIconColor = Color(0xFFFFD700),
        buttonShapeRounded = 8,
        sliderTrackColor = Color(0xFF151515),
        sliderThumbColor = Color(0xFFFF0033),
        sliderThumbBorder = Color(0xFF990000),
        accentColor = Color(0xFFFFD700),
        secondaryAccentColor = Color(0xFFFF0033)
    )

    // 25. Cyberpunk Neon Tokyo 2077
    val CYBER_TOKYO_2077 = RetroSkin(
        id = "cyber_tokyo_2077",
        name = "Cyberpunk Neo Tokyo",
        era = "2077/90s",
        description = "Midnight high-tech carbon chassis with neon magenta, caution yellow stripes, and holo cyan glow.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF150D24), Color(0xFF0D0817), Color(0xFF05030A))
        ),
        chassisBorderColor = Color(0xFFFF007F),
        chassisBevelHighlight = Color(0xFF00F5D4),
        chassisBevelShadow = Color(0xFF000000),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFFFF007F), Color(0xFF7928CA), Color(0xFF00F5D4))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFFFFE600),
        lcdBackground = Color(0xFF080214),
        lcdTextColor = Color(0xFF00F5D4),
        lcdGlowColor = Color(0xFFFF007F),
        lcdDimColor = Color(0xFF003833),
        lcdBorderColor = Color(0xFFFF007F),
        eqBarGradient = listOf(Color(0xFFFF007F), Color(0xFFFFE600), Color(0xFF00F5D4)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF220A38),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF2B1347), Color(0xFF190B2E), Color(0xFF0F061D))
        ),
        buttonBorderColor = Color(0xFFFF007F),
        buttonHighlightColor = Color(0xFFFF70A6),
        buttonShadowColor = Color(0xFF000000),
        buttonIconColor = Color(0xFF00F5D4),
        buttonShapeRounded = 2,
        sliderTrackColor = Color(0xFF0D041A),
        sliderThumbColor = Color(0xFFFFE600),
        sliderThumbBorder = Color(0xFFFF007F),
        accentColor = Color(0xFFFF007F),
        secondaryAccentColor = Color(0xFF00F5D4)
    )

    // 26. Super 16-Bit Jewel Pad (1990)
    val SNES_FAMICOM = RetroSkin(
        id = "snes_famicom",
        name = "Super 16-Bit Jewel",
        era = "1990",
        description = "Warm two-tone console grey with iconic Red, Yellow, Blue, and Green controller jewel buttons.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFFD6D3D1), Color(0xFFBCB8B5), Color(0xFFA8A29E))
        ),
        chassisBorderColor = Color(0xFF57534E),
        chassisBevelHighlight = Color(0xFFF5F5F4),
        chassisBevelShadow = Color(0xFF44403C),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF334155), Color(0xFF4F46E5), Color(0xFF334155))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFFEF4444),
        lcdBackground = Color(0xFF1E1B2E),
        lcdTextColor = Color(0xFFA5B4FC),
        lcdGlowColor = Color(0xFF6366F1),
        lcdDimColor = Color(0xFF312E81),
        lcdBorderColor = Color(0xFF4F46E5),
        eqBarGradient = listOf(Color(0xFFEF4444), Color(0xFFEAB308), Color(0xFF3B82F6), Color(0xFF22C55E)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF3730A3),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF94A3B8))
        ),
        buttonBorderColor = Color(0xFF475569),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF334155),
        buttonIconColor = Color(0xFF0F172A),
        buttonShapeRounded = 10,
        sliderTrackColor = Color(0xFF1E1B2E),
        sliderThumbColor = Color(0xFF4F46E5),
        sliderThumbBorder = Color(0xFF312E81),
        accentColor = Color(0xFF4338CA),
        secondaryAccentColor = Color(0xFFEF4444)
    )

    // 27. 32-Bit CD Console 1994
    val PS1_CLASSIC = RetroSkin(
        id = "ps1_classic",
        name = "32-Bit Disc Slate 94",
        era = "1994",
        description = "Authentic matte industrial grey CD chassis with bold green, red and blue button hues.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFFB5B5B5), Color(0xFF9E9E9E), Color(0xFF888888))
        ),
        chassisBorderColor = Color(0xFFD1D1D1),
        chassisBevelHighlight = Color(0xFFEDEDED),
        chassisBevelShadow = Color(0xFF5A5A5A),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF6B7280), Color(0xFF9CA3AF), Color(0xFF6B7280))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFF00F5D4),
        lcdBackground = Color(0xFF212121),
        lcdTextColor = Color(0xFF00F5D4),
        lcdGlowColor = Color(0xFF00C853),
        lcdDimColor = Color(0xFF14291E),
        lcdBorderColor = Color(0xFF616161),
        eqBarGradient = listOf(Color(0xFFFF3366), Color(0xFF00E676), Color(0xFF2979FF)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF374151),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFE0E0E0), Color(0xFFBDBDBD), Color(0xFF9E9E9E))
        ),
        buttonBorderColor = Color(0xFF757575),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF424242),
        buttonIconColor = Color(0xFF003087),
        buttonShapeRounded = 8,
        sliderTrackColor = Color(0xFF212121),
        sliderThumbColor = Color(0xFF0070D1),
        sliderThumbBorder = Color(0xFF003087),
        accentColor = Color(0xFF0070D1),
        secondaryAccentColor = Color(0xFFFF3366)
    )

    // 28. 64-Bit Emerald Funtastic (1999)
    val N64_JUNGLE = RetroSkin(
        id = "n64_jungle",
        name = "Emerald 64 Jungle",
        era = "1999",
        description = "Translucent emerald casing with glowing circuit textures and bright yellow jewel controls.",
        chassisBackground = Brush.radialGradient(
            listOf(Color(0xFF15803D), Color(0xFF166534), Color(0xFF052E16))
        ),
        chassisBorderColor = Color(0xFF22C55E),
        chassisBevelHighlight = Color(0x8086EFAC),
        chassisBevelShadow = Color(0x60022C11),
        isTranslucent = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF052E16), Color(0xFF15803D), Color(0xFF052E16))
        ),
        titleBarTextColor = Color(0xFFF0FDF4),
        titleBarAccentColor = Color(0xFFFBBF24),
        lcdBackground = Color(0xFF031C0D),
        lcdTextColor = Color(0xFF4ADE80),
        lcdGlowColor = Color(0xFF22C55E),
        lcdDimColor = Color(0xFF053818),
        lcdBorderColor = Color(0xFF166534),
        eqBarGradient = listOf(Color(0xFFEF4444), Color(0xFFFBBF24), Color(0xFF22C55E)),
        eqPeakHoldColor = Color(0xFFDCFCE7),
        eqGridLineColor = Color(0xFF0B421F),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF22C55E), Color(0xFF15803D), Color(0xFF166534))
        ),
        buttonBorderColor = Color(0xFF4ADE80),
        buttonHighlightColor = Color(0xFF86EFAC),
        buttonShadowColor = Color(0xFF052E16),
        buttonIconColor = Color(0xFFFEF08A),
        buttonShapeRounded = 12,
        sliderTrackColor = Color(0xFF031C0D),
        sliderThumbColor = Color(0xFFFBBF24),
        sliderThumbBorder = Color(0xFFB45309),
        accentColor = Color(0xFF4ADE80),
        secondaryAccentColor = Color(0xFFFBBF24)
    )

    // 29. 64-Bit Atomic Amethyst (1998)
    val N64_ATOMIC_PURPLE = RetroSkin(
        id = "n64_atomic_purple",
        name = "Atomic Purple 64",
        era = "1998",
        description = "Legendary see-through purple smoky acrylic chassis revealing hardware circuitry underneath.",
        chassisBackground = Brush.radialGradient(
            listOf(Color(0xFF6B21A8), Color(0xFF581C87), Color(0xFF2E1065))
        ),
        chassisBorderColor = Color(0xFFA855F7),
        chassisBevelHighlight = Color(0x80D8B4FE),
        chassisBevelShadow = Color(0x601E0638),
        isTranslucent = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF3B0764), Color(0xFF7E22CE), Color(0xFF3B0764))
        ),
        titleBarTextColor = Color(0xFFFAF5FF),
        titleBarAccentColor = Color(0xFF38BDF8),
        lcdBackground = Color(0xFF1A052E),
        lcdTextColor = Color(0xFFC084FC),
        lcdGlowColor = Color(0xFFA855F7),
        lcdDimColor = Color(0xFF380C5E),
        lcdBorderColor = Color(0xFF7E22CE),
        eqBarGradient = listOf(Color(0xFFF43F5E), Color(0xFFA855F7), Color(0xFF38BDF8)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF340D59),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF9333EA), Color(0xFF7E22CE), Color(0xFF581C87))
        ),
        buttonBorderColor = Color(0xFFC084FC),
        buttonHighlightColor = Color(0xFFE9D5FF),
        buttonShadowColor = Color(0xFF2E1065),
        buttonIconColor = Color(0xFFF3E8FF),
        buttonShapeRounded = 12,
        sliderTrackColor = Color(0xFF1A052E),
        sliderThumbColor = Color(0xFF38BDF8),
        sliderThumbBorder = Color(0xFF0284C7),
        accentColor = Color(0xFFC084FC),
        secondaryAccentColor = Color(0xFF38BDF8)
    )

    // 30. Broadcast Studio Trinitron CRT (1995)
    val TRINITRON_CRT = RetroSkin(
        id = "trinitron_crt",
        name = "Aperture Grille CRT",
        era = "1995",
        description = "Dark aperture grille studio monitor bezel with phosphorescent green raster glow & CRT tube depth.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF2B2D31), Color(0xFF1E2024), Color(0xFF121417))
        ),
        chassisBorderColor = Color(0xFF4A4E57),
        chassisBevelHighlight = Color(0xFF6B7280),
        chassisBevelShadow = Color(0xFF090A0C),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF121417), Color(0xFF1E293B), Color(0xFF121417))
        ),
        titleBarTextColor = Color(0xFF00FF66),
        titleBarAccentColor = Color(0xFF00FF66),
        lcdBackground = Color(0xFF041208),
        lcdTextColor = Color(0xFF00FF66),
        lcdGlowColor = Color(0xFF39FF14),
        lcdDimColor = Color(0xFF0B2912),
        lcdBorderColor = Color(0xFF1E3A24),
        eqBarGradient = listOf(Color(0xFFFF0033), Color(0xFFFFCC00), Color(0xFF00FF66)),
        eqPeakHoldColor = Color(0xFFE0FFE8),
        eqGridLineColor = Color(0xFF0B2B12),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF374151), Color(0xFF1F2937), Color(0xFF111827))
        ),
        buttonBorderColor = Color(0xFF4B5563),
        buttonHighlightColor = Color(0xFF6B7280),
        buttonShadowColor = Color(0xFF030712),
        buttonIconColor = Color(0xFF00FF66),
        buttonShapeRounded = 3,
        sliderTrackColor = Color(0xFF080B0F),
        sliderThumbColor = Color(0xFF00FF66),
        sliderThumbBorder = Color(0xFF059669),
        accentColor = Color(0xFF00FF66),
        secondaryAccentColor = Color(0xFFFFCC00)
    )

    // 31. Direct Drive Vinyl Deck SL-12 (1991)
    val SL1200_TURNTABLE = RetroSkin(
        id = "sl1200_turntable",
        name = "Direct Drive Deck",
        era = "1991",
        description = "Die-cast brushed aluminium alloy turntable platter, strobe red LED target lamp & pitch fader.",
        chassisBackground = Brush.linearGradient(
            listOf(Color(0xFFC0C5CE), Color(0xFFA7ADBA), Color(0xFF8A939E), Color(0xFFB4BAC4))
        ),
        chassisBorderColor = Color(0xFFDFE2E8),
        chassisBevelHighlight = Color(0xFFF3F4F6),
        chassisBevelShadow = Color(0xFF4F5660),
        isMetallic = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF1F2937), Color(0xFF374151), Color(0xFF1F2937))
        ),
        titleBarTextColor = Color(0xFFF9FAFB),
        titleBarAccentColor = Color(0xFFFF0000),
        lcdBackground = Color(0xFF111827),
        lcdTextColor = Color(0xFFFF3333),
        lcdGlowColor = Color(0xFFFF0000),
        lcdDimColor = Color(0xFF330000),
        lcdBorderColor = Color(0xFF4B5563),
        eqBarGradient = listOf(Color(0xFFFF0000), Color(0xFFFF9900), Color(0xFF00E676)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF263238),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB), Color(0xFF9CA3AF))
        ),
        buttonBorderColor = Color(0xFF6B7280),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF374151),
        buttonIconColor = Color(0xFF111827),
        buttonShapeRounded = 4,
        sliderTrackColor = Color(0xFF111827),
        sliderThumbColor = Color(0xFFFF0000),
        sliderThumbBorder = Color(0xFF990000),
        accentColor = Color(0xFFFF3333),
        secondaryAccentColor = Color(0xFF1F2937)
    )

    // 32. Vintage 808 Analog Drum Machine (1980/90s)
    val TR808_RHYTHM = RetroSkin(
        id = "tr808_rhythm",
        name = "Analog Beatbox 808",
        era = "1980/90s",
        description = "Iconic studio drum machine with beige chassis, warm cream panels, and red/orange/yellow step keys.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF363432), Color(0xFF242220), Color(0xFF171615))
        ),
        chassisBorderColor = Color(0xFFF97316),
        chassisBevelHighlight = Color(0xFF57534E),
        chassisBevelShadow = Color(0xFF0C0A09),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFFDC2626), Color(0xFFEA580C), Color(0xFFD97706))
        ),
        titleBarTextColor = Color(0xFFFFFBEB),
        titleBarAccentColor = Color(0xFFFEF08A),
        lcdBackground = Color(0xFF1C1917),
        lcdTextColor = Color(0xFFF97316),
        lcdGlowColor = Color(0xFFFB923C),
        lcdDimColor = Color(0xFF431407),
        lcdBorderColor = Color(0xFF78350F),
        eqBarGradient = listOf(Color(0xFFDC2626), Color(0xFFEA580C), Color(0xFFFACC15), Color(0xFF84CC16)),
        eqPeakHoldColor = Color(0xFFFFFBEB),
        eqGridLineColor = Color(0xFF292524),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFEA580C), Color(0xFFC2410C), Color(0xFF9A3412))
        ),
        buttonBorderColor = Color(0xFFFDBA74),
        buttonHighlightColor = Color(0xFFFED7AA),
        buttonShadowColor = Color(0xFF431407),
        buttonIconColor = Color(0xFFFFF7ED),
        buttonShapeRounded = 4,
        sliderTrackColor = Color(0xFF1C1917),
        sliderThumbColor = Color(0xFFFACC15),
        sliderThumbBorder = Color(0xFFCA8A04),
        accentColor = Color(0xFFF97316),
        secondaryAccentColor = Color(0xFFFACC15)
    )

    // 33. Pad Sampler 2000 Workstation (1997)
    val MPC_2000XL = RetroSkin(
        id = "mpc_2000xl",
        name = "Groove Sampler 2000",
        era = "1997",
        description = "Legendary hip-hop boom-bap workstation beige metal casing, blue jog wheel, and green LCD screen.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFFDDD8CE), Color(0xFFC8C2B4), Color(0xFFB0AA9B))
        ),
        chassisBorderColor = Color(0xFFE5E0D6),
        chassisBevelHighlight = Color(0xFFF5F2EB),
        chassisBevelShadow = Color(0xFF787265),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF1E3A5F), Color(0xFF2563EB), Color(0xFF1E3A5F))
        ),
        titleBarTextColor = Color(0xFFF8FAFC),
        titleBarAccentColor = Color(0xFF60A5FA),
        lcdBackground = Color(0xFF203B26),
        lcdTextColor = Color(0xFF4ADE80),
        lcdGlowColor = Color(0xFF22C55E),
        lcdDimColor = Color(0xFF132819),
        lcdBorderColor = Color(0xFF42634B),
        eqBarGradient = listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF22C55E)),
        eqPeakHoldColor = Color(0xFFDCFCE7),
        eqGridLineColor = Color(0xFF2E4D36),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF64748B), Color(0xFF475569), Color(0xFF334155))
        ),
        buttonBorderColor = Color(0xFF94A3B8),
        buttonHighlightColor = Color(0xFFCBD5E1),
        buttonShadowColor = Color(0xFF1E293B),
        buttonIconColor = Color(0xFFF8FAFC),
        buttonShapeRounded = 4,
        sliderTrackColor = Color(0xFF203B26),
        sliderThumbColor = Color(0xFF2563EB),
        sliderThumbBorder = Color(0xFF1D4ED8),
        accentColor = Color(0xFF2563EB),
        secondaryAccentColor = Color(0xFF4ADE80)
    )

    // 34. Cyber Rig Predator Xenon (2002)
    val ALIENWARE_PREDATOR = RetroSkin(
        id = "alienware_predator",
        name = "Cyber Rig '01",
        era = "2002",
        description = "Futuristic liquid mercury titanium chassis with glowing alien cyan radiator vents and neon visor.",
        chassisBackground = Brush.linearGradient(
            listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF1E293B))
        ),
        chassisBorderColor = Color(0xFF00F5D4),
        chassisBevelHighlight = Color(0xFF67E8F9),
        chassisBevelShadow = Color(0xFF020617),
        isMetallic = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF042F2E), Color(0xFF0D9488), Color(0xFF042F2E))
        ),
        titleBarTextColor = Color(0xFFCCFBF1),
        titleBarAccentColor = Color(0xFF00F5D4),
        lcdBackground = Color(0xFF021C1A),
        lcdTextColor = Color(0xFF00F5D4),
        lcdGlowColor = Color(0xFF14B8A6),
        lcdDimColor = Color(0xFF064E3B),
        lcdBorderColor = Color(0xFF0D9488),
        eqBarGradient = listOf(Color(0xFFF43F5E), Color(0xFF06B6D4), Color(0xFF00F5D4)),
        eqPeakHoldColor = Color(0xFFE0F2FE),
        eqGridLineColor = Color(0xFF08332F),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
        ),
        buttonBorderColor = Color(0xFF00F5D4),
        buttonHighlightColor = Color(0xFF2DD4BF),
        buttonShadowColor = Color(0xFF000000),
        buttonIconColor = Color(0xFF00F5D4),
        buttonShapeRounded = 6,
        sliderTrackColor = Color(0xFF021C1A),
        sliderThumbColor = Color(0xFF00F5D4),
        sliderThumbBorder = Color(0xFF0D9488),
        accentColor = Color(0xFF00F5D4),
        secondaryAccentColor = Color(0xFFF43F5E)
    )

    // 35. Rugged Tactical Illuminator (1996)
    val GSHOCK_TACTICAL = RetroSkin(
        id = "gshock_tactical",
        name = "Tactical Shock Watch",
        era = "1996",
        description = "Shock-resistant matte black resin armor with teal electro-luminescent backlight display.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF262626), Color(0xFF171717), Color(0xFF0A0A0A))
        ),
        chassisBorderColor = Color(0xFF525252),
        chassisBevelHighlight = Color(0xFF737373),
        chassisBevelShadow = Color(0xFF000000),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF171717), Color(0xFF065F46), Color(0xFF171717))
        ),
        titleBarTextColor = Color(0xFFD1FAE5),
        titleBarAccentColor = Color(0xFF34D399),
        lcdBackground = Color(0xFF064E3B),
        lcdTextColor = Color(0xFFA7F3D0),
        lcdGlowColor = Color(0xFF34D399),
        lcdDimColor = Color(0xFF065F46),
        lcdBorderColor = Color(0xFF047857),
        eqBarGradient = listOf(Color(0xFFF87171), Color(0xFFFBBF24), Color(0xFF34D399)),
        eqPeakHoldColor = Color(0xFFECFDF5),
        eqGridLineColor = Color(0xFF065F46),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF404040), Color(0xFF262626), Color(0xFF171717))
        ),
        buttonBorderColor = Color(0xFF737373),
        buttonHighlightColor = Color(0xFFA3A3A3),
        buttonShadowColor = Color(0xFF000000),
        buttonIconColor = Color(0xFF34D399),
        buttonShapeRounded = 8,
        sliderTrackColor = Color(0xFF0A0A0A),
        sliderThumbColor = Color(0xFF34D399),
        sliderThumbBorder = Color(0xFF059669),
        accentColor = Color(0xFF34D399),
        secondaryAccentColor = Color(0xFFFBBF24)
    )

    // 36. Executive PDA Stylus (2001)
    val PALM_CLIE = RetroSkin(
        id = "palm_clie",
        name = "Executive Pocket PDA",
        era = "2001",
        description = "High-end brushed magnesium PDA casing with amber monochrome high-res display and jog dial.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFFC7CBD1), Color(0xFFAEB4BC), Color(0xFF9399A3))
        ),
        chassisBorderColor = Color(0xFFE2E5E9),
        chassisBevelHighlight = Color(0xFFF1F3F5),
        chassisBevelShadow = Color(0xFF646B76),
        isMetallic = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF334155))
        ),
        titleBarTextColor = Color(0xFFF8FAFC),
        titleBarAccentColor = Color(0xFFF59E0B),
        lcdBackground = Color(0xFF332005),
        lcdTextColor = Color(0xFFFBBF24),
        lcdGlowColor = Color(0xFFF59E0B),
        lcdDimColor = Color(0xFF5C3B0E),
        lcdBorderColor = Color(0xFF92400E),
        eqBarGradient = listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFFBBF24)),
        eqPeakHoldColor = Color(0xFFFEF3C7),
        eqGridLineColor = Color(0xFF4A300A),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF94A3B8))
        ),
        buttonBorderColor = Color(0xFF64748B),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF475569),
        buttonIconColor = Color(0xFF1E293B),
        buttonShapeRounded = 8,
        sliderTrackColor = Color(0xFF332005),
        sliderThumbColor = Color(0xFFF59E0B),
        sliderThumbBorder = Color(0xFFB45309),
        accentColor = Color(0xFFFBBF24),
        secondaryAccentColor = Color(0xFF3B82F6)
    )

    // 37. Key Lime Citrus G3 (1999)
    val LIME_IMAC = RetroSkin(
        id = "lime_imac",
        name = "Citrus Lime '99",
        era = "1999",
        description = "Translucent key lime candy housing with vivid neon citrus curves and frosted glow.",
        chassisBackground = Brush.radialGradient(
            listOf(Color(0xFF65A30D), Color(0xFF4D7C0F), Color(0xFF365314))
        ),
        chassisBorderColor = Color(0xFF84CC16),
        chassisBevelHighlight = Color(0x80D9F99D),
        chassisBevelShadow = Color(0x601A2E05),
        isTranslucent = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF365314), Color(0xFF65A30D), Color(0xFF365314))
        ),
        titleBarTextColor = Color(0xFFF7FEE7),
        titleBarAccentColor = Color(0xFFBEF264),
        lcdBackground = Color(0xFF142404),
        lcdTextColor = Color(0xFFA3E635),
        lcdGlowColor = Color(0xFF84CC16),
        lcdDimColor = Color(0xFF274708),
        lcdBorderColor = Color(0xFF4D7C0F),
        eqBarGradient = listOf(Color(0xFFE11D48), Color(0xFFFACC15), Color(0xFFA3E635)),
        eqPeakHoldColor = Color(0xFFECFCCB),
        eqGridLineColor = Color(0xFF203A07),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFECFCCB), Color(0xFFD9F99D))
        ),
        buttonBorderColor = Color(0xFF84CC16),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF4D7C0F),
        buttonIconColor = Color(0xFF365314),
        buttonShapeRounded = 12,
        sliderTrackColor = Color(0xFF142404),
        sliderThumbColor = Color(0xFFA3E635),
        sliderThumbBorder = Color(0xFF65A30D),
        accentColor = Color(0xFFA3E635),
        secondaryAccentColor = Color(0xFFFACC15)
    )

    // 38. Translucent Ruby Candy G3 (1999)
    val RUBY_IMAC = RetroSkin(
        id = "ruby_imac",
        name = "Ruby Candy '99",
        era = "1999",
        description = "Translucent deep ruby jewel casing with crimson candy curves and luminous white highlights.",
        chassisBackground = Brush.radialGradient(
            listOf(Color(0xFFBE123C), Color(0xFF9F1239), Color(0xFF4C0519))
        ),
        chassisBorderColor = Color(0xFFF43F5E),
        chassisBevelHighlight = Color(0x80FECDD3),
        chassisBevelShadow = Color(0x6025020B),
        isTranslucent = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF4C0519), Color(0xFFBE123C), Color(0xFF4C0519))
        ),
        titleBarTextColor = Color(0xFFFFF1F2),
        titleBarAccentColor = Color(0xFFFDA4AF),
        lcdBackground = Color(0xFF28020D),
        lcdTextColor = Color(0xFFFB7185),
        lcdGlowColor = Color(0xFFF43F5E),
        lcdDimColor = Color(0xFF4E071B),
        lcdBorderColor = Color(0xFF9F1239),
        eqBarGradient = listOf(Color(0xFFF43F5E), Color(0xFFFB923C), Color(0xFFFDE047)),
        eqPeakHoldColor = Color(0xFFFFF1F2),
        eqGridLineColor = Color(0xFF450617),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFFFE4E6), Color(0xFFFECDD3))
        ),
        buttonBorderColor = Color(0xFFFB7185),
        buttonHighlightColor = Color(0xFFFFFFFF),
        buttonShadowColor = Color(0xFF9F1239),
        buttonIconColor = Color(0xFF881337),
        buttonShapeRounded = 12,
        sliderTrackColor = Color(0xFF28020D),
        sliderThumbColor = Color(0xFFFB7185),
        sliderThumbBorder = Color(0xFFE11D48),
        accentColor = Color(0xFFFB7185),
        secondaryAccentColor = Color(0xFFFDE047)
    )

    // 39. Peer-to-Peer Orbit Blue (2001)
    val KAZAA_P2P = RetroSkin(
        id = "kazaa_p2p",
        name = "Orbit P2P Blue",
        era = "2001",
        description = "Early 2000s peer-to-peer file sharing blue gradient with gold bandwidth meters & peer node lights.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF1E3A8A), Color(0xFF172554), Color(0xFF0F172A))
        ),
        chassisBorderColor = Color(0xFF38BDF8),
        chassisBevelHighlight = Color(0xFF60A5FA),
        chassisBevelShadow = Color(0xFF050B14),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6), Color(0xFF1D4ED8))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFFFACC15),
        lcdBackground = Color(0xFF0C192E),
        lcdTextColor = Color(0xFF38BDF8),
        lcdGlowColor = Color(0xFF60A5FA),
        lcdDimColor = Color(0xFF0F2D54),
        lcdBorderColor = Color(0xFF1E40AF),
        eqBarGradient = listOf(Color(0xFFEF4444), Color(0xFFFACC15), Color(0xFF38BDF8)),
        eqPeakHoldColor = Color(0xFFE0F2FE),
        eqGridLineColor = Color(0xFF142947),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF1E40AF))
        ),
        buttonBorderColor = Color(0xFF60A5FA),
        buttonHighlightColor = Color(0xFF93C5FD),
        buttonShadowColor = Color(0xFF0F172A),
        buttonIconColor = Color(0xFFF8FAFC),
        buttonShapeRounded = 6,
        sliderTrackColor = Color(0xFF0C192E),
        sliderThumbColor = Color(0xFFFACC15),
        sliderThumbBorder = Color(0xFFCA8A04),
        accentColor = Color(0xFF38BDF8),
        secondaryAccentColor = Color(0xFFFACC15)
    )

    // 40. Pro Audio Studio Monitor (1993)
    val STUDIO_MDR = RetroSkin(
        id = "studio_mdr",
        name = "Studio Reference Monitor",
        era = "1993",
        description = "Professional broadcast audio monitor styling in studio textured black, red/gold stereo decals.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF27272A), Color(0xFF18181B), Color(0xFF09090B))
        ),
        chassisBorderColor = Color(0xFFDC2626),
        chassisBevelHighlight = Color(0xFF3F3F46),
        chassisBevelShadow = Color(0xFF000000),
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF991B1B), Color(0xFFDC2626), Color(0xFF991B1B))
        ),
        titleBarTextColor = Color(0xFFFEF2F2),
        titleBarAccentColor = Color(0xFFFACC15),
        lcdBackground = Color(0xFF111113),
        lcdTextColor = Color(0xFFF87171),
        lcdGlowColor = Color(0xFFEF4444),
        lcdDimColor = Color(0xFF450A0A),
        lcdBorderColor = Color(0xFF7F1D1D),
        eqBarGradient = listOf(Color(0xFFEF4444), Color(0xFFFACC15), Color(0xFF4ADE80)),
        eqPeakHoldColor = Color(0xFFFEF2F2),
        eqGridLineColor = Color(0xFF291B1B),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF3F3F46), Color(0xFF27272A), Color(0xFF18181B))
        ),
        buttonBorderColor = Color(0xFF71717A),
        buttonHighlightColor = Color(0xFFA1A1AA),
        buttonShadowColor = Color(0xFF09090B),
        buttonIconColor = Color(0xFFFACC15),
        buttonShapeRounded = 4,
        sliderTrackColor = Color(0xFF111113),
        sliderThumbColor = Color(0xFFDC2626),
        sliderThumbBorder = Color(0xFF991B1B),
        accentColor = Color(0xFFF87171),
        secondaryAccentColor = Color(0xFFFACC15)
    )

    // 41. Color Handheld Atomic Clear (1998)
    val GBC_ATOMIC_CLEAR = RetroSkin(
        id = "gbc_atomic_clear",
        name = "Atomic Clear Handheld",
        era = "1998",
        description = "100% transparent clear shell showcasing pristine circuit traces, capacitors, and speaker coils.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0x99475569), Color(0x77334155), Color(0x551E293B))
        ),
        chassisBorderColor = Color(0xFF94A3B8),
        chassisBevelHighlight = Color(0xCCF1F5F9),
        chassisBevelShadow = Color(0x880F172A),
        isTranslucent = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFF334155), Color(0xFF64748B), Color(0xFF334155))
        ),
        titleBarTextColor = Color(0xFFF8FAFC),
        titleBarAccentColor = Color(0xFF38BDF8),
        lcdBackground = Color(0xFF1B2430),
        lcdTextColor = Color(0xFF86EFAC),
        lcdGlowColor = Color(0xFF4ADE80),
        lcdDimColor = Color(0xFF133224),
        lcdBorderColor = Color(0xFF475569),
        eqBarGradient = listOf(Color(0xFFF43F5E), Color(0xFFFACC15), Color(0xFF4ADE80), Color(0xFF38BDF8)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF243447),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF64748B), Color(0xFF475569), Color(0xFF334155))
        ),
        buttonBorderColor = Color(0xFF94A3B8),
        buttonHighlightColor = Color(0xFFCBD5E1),
        buttonShadowColor = Color(0xFF1E293B),
        buttonIconColor = Color(0xFFF1F5F9),
        buttonShapeRounded = 10,
        sliderTrackColor = Color(0xFF1B2430),
        sliderThumbColor = Color(0xFF38BDF8),
        sliderThumbBorder = Color(0xFF0284C7),
        accentColor = Color(0xFF86EFAC),
        secondaryAccentColor = Color(0xFF38BDF8)
    )

    // 42. Synthwave Outrun Neon 1984
    val OUTRUN_SYNTH_84 = RetroSkin(
        id = "outrun_synth_84",
        name = "Synthwave Outrun '84",
        era = "1984/90s",
        description = "High-octane chrome bevels, glowing magenta gridlines, and neon sunset horizon gradients.",
        chassisBackground = Brush.verticalGradient(
            listOf(Color(0xFF2E0854), Color(0xFF18032E), Color(0xFF0A0014))
        ),
        chassisBorderColor = Color(0xFFFF007F),
        chassisBevelHighlight = Color(0xFFFF70A6),
        chassisBevelShadow = Color(0xFF000000),
        isMetallic = true,
        titleBarBackground = Brush.horizontalGradient(
            listOf(Color(0xFFFF007F), Color(0xFF8A2BE2), Color(0xFF00FFFF))
        ),
        titleBarTextColor = Color(0xFFFFFFFF),
        titleBarAccentColor = Color(0xFFFFE600),
        lcdBackground = Color(0xFF120024),
        lcdTextColor = Color(0xFFFF70A6),
        lcdGlowColor = Color(0xFFFF007F),
        lcdDimColor = Color(0xFF4A0033),
        lcdBorderColor = Color(0xFFFF007F),
        eqBarGradient = listOf(Color(0xFFFF007F), Color(0xFFFF9900), Color(0xFF00FFFF)),
        eqPeakHoldColor = Color(0xFFFFFFFF),
        eqGridLineColor = Color(0xFF380054),
        buttonBackground = Brush.verticalGradient(
            listOf(Color(0xFF4A0E6B), Color(0xFF2E0854), Color(0xFF18032E))
        ),
        buttonBorderColor = Color(0xFFFF007F),
        buttonHighlightColor = Color(0xFFFF70A6),
        buttonShadowColor = Color(0xFF000000),
        buttonIconColor = Color(0xFF00FFFF),
        buttonShapeRounded = 6,
        sliderTrackColor = Color(0xFF120024),
        sliderThumbColor = Color(0xFFFFE600),
        sliderThumbBorder = Color(0xFFFF007F),
        accentColor = Color(0xFFFF007F),
        secondaryAccentColor = Color(0xFF00FFFF)
    )

    val ALL_SKINS: List<RetroSkin> = listOf(
        WINAMP_CLASSIC,
        IMAC_BONDI,
        WALKMAN_TPS,
        DISCMAN_ESP,
        WIN98_PLAYER,
        CYBER_MATRIX,
        Y2K_BUBBLEGUM,
        VAPORWAVE_95,
        GAMEBOY_DMG,
        VCR_CAMCORDER,
        MINIDISC_SONIC,
        NAPSTER_GREEN,
        BOOMBOX_GHETTO,
        PSP_PIANO_BLACK,
        IPOD_CLASSIC_01,
        HOT_ROD_FLAME,
        ACID_TECHNO,
        WMP_MILLENNIUM,
        HACKER_TERMINAL,
        GLACIER_GBA,
        GRAPE_IMAC,
        HIFI_GOLD,
        WINAMP_BENTO,
        SEGA_GENESIS,
        CYBER_TOKYO_2077,
        SNES_FAMICOM,
        PS1_CLASSIC,
        N64_JUNGLE,
        N64_ATOMIC_PURPLE,
        TRINITRON_CRT,
        SL1200_TURNTABLE,
        TR808_RHYTHM,
        MPC_2000XL,
        ALIENWARE_PREDATOR,
        GSHOCK_TACTICAL,
        PALM_CLIE,
        LIME_IMAC,
        RUBY_IMAC,
        KAZAA_P2P,
        STUDIO_MDR,
        GBC_ATOMIC_CLEAR,
        OUTRUN_SYNTH_84
    ) + RetroSkinsExtra.ALL

    fun getSkinById(id: String): RetroSkin {
        return ALL_SKINS.find { it.id == id } ?: WINAMP_CLASSIC
    }
}
