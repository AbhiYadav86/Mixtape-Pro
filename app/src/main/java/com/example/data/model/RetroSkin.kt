package com.example.data.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class RetroSkin(
    val id: String,
    val name: String,
    val era: String, // "1995", "1998", "2001", etc.
    val description: String,
    
    // Chassis & Shell
    val chassisBackground: Brush,
    val chassisBorderColor: Color,
    val chassisBevelHighlight: Color,
    val chassisBevelShadow: Color,
    val isMetallic: Boolean = false,
    val isTranslucent: Boolean = false,
    
    // Header / Title Bar
    val titleBarBackground: Brush,
    val titleBarTextColor: Color,
    val titleBarAccentColor: Color,
    
    // LCD Display
    val lcdBackground: Color,
    val lcdTextColor: Color,
    val lcdGlowColor: Color,
    val lcdDimColor: Color,
    val lcdBorderColor: Color,
    
    // Equalizer & Visualizer
    val eqBarGradient: List<Color>,
    val eqPeakHoldColor: Color,
    val eqGridLineColor: Color,
    
    // Transport Buttons
    val buttonBackground: Brush,
    val buttonBorderColor: Color,
    val buttonHighlightColor: Color,
    val buttonShadowColor: Color,
    val buttonIconColor: Color,
    val buttonShapeRounded: Int = 4, // corner radius in dp
    val isWin98Style: Boolean = false,
    
    // Sliders & Accents
    val sliderTrackColor: Color,
    val sliderThumbColor: Color,
    val sliderThumbBorder: Color,
    val accentColor: Color,
    val secondaryAccentColor: Color
)
