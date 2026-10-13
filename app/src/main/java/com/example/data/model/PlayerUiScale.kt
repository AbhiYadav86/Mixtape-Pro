package com.example.data.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PlayerUiScale(
    val id: String,
    val label: String,
    val title: String,
    val screenDescription: String,
    val factor: Float,
    val heroButtonSize: Dp,
    val normalButtonSize: Dp,
    val heroIconSize: Dp,
    val normalIconSize: Dp,
    val transportButtonPadding: Dp,
    val buttonLabelSize: Float,
    val secondaryButtonHeight: Dp,
    val secondaryButtonTextSize: Float,
    val deckHeight: Dp,
    val visualizerHeight: Dp,
    val timerFontSize: Float,
    val timerPadding: Dp,
    val seekBarHeight: Dp,
    val timeLabelFontSize: Float,
    val chipTextSize: Float
) {
    COMPACT(
        id = "compact",
        label = "S",
        title = "Compact (6.0\")",
        screenDescription = "Fit 6.0\" - 6.2\" screens with zero scrolling needed",
        factor = 0.82f,
        heroButtonSize = 48.dp,
        normalButtonSize = 39.dp,
        heroIconSize = 25.dp,
        normalIconSize = 19.dp,
        transportButtonPadding = 2.dp,
        buttonLabelSize = 9.5f,
        secondaryButtonHeight = 31.dp,
        secondaryButtonTextSize = 10f,
        deckHeight = 180.dp,
        visualizerHeight = 85.dp,
        timerFontSize = 35f,
        timerPadding = 4.dp,
        seekBarHeight = 24.dp,
        timeLabelFontSize = 14f,
        chipTextSize = 8.5f
    ),
    STANDARD(
        id = "standard",
        label = "M",
        title = "Standard (6.5\")",
        screenDescription = "Balanced fit for standard 6.3\" - 6.6\" phones",
        factor = 1.0f,
        heroButtonSize = 56.dp,
        normalButtonSize = 46.dp,
        heroIconSize = 30.dp,
        normalIconSize = 22.dp,
        transportButtonPadding = 3.dp,
        buttonLabelSize = 11f,
        secondaryButtonHeight = 36.dp,
        secondaryButtonTextSize = 11.5f,
        deckHeight = 230.dp,
        visualizerHeight = 110.dp,
        timerFontSize = 42f,
        timerPadding = 6.dp,
        seekBarHeight = 30.dp,
        timeLabelFontSize = 17f,
        chipTextSize = 9f
    ),
    EXPANDED(
        id = "expanded",
        label = "L",
        title = "Expanded (7.0\"+)",
        screenDescription = "Large buttons for 6.7\" - 7.0\"+ phablets and tablets",
        factor = 1.18f,
        heroButtonSize = 64.dp,
        normalButtonSize = 52.dp,
        heroIconSize = 34.dp,
        normalIconSize = 26.dp,
        transportButtonPadding = 4.dp,
        buttonLabelSize = 12f,
        secondaryButtonHeight = 42.dp,
        secondaryButtonTextSize = 12.5f,
        deckHeight = 280.dp,
        visualizerHeight = 138.dp,
        timerFontSize = 48f,
        timerPadding = 8.dp,
        seekBarHeight = 34.dp,
        timeLabelFontSize = 19.5f,
        chipTextSize = 9.5f
    );

    companion object {
        fun fromId(id: String?): PlayerUiScale {
            return entries.firstOrNull { it.id == id } ?: STANDARD
        }
    }
}
