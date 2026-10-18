// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors

/** The roles an open-source theme defines itself. Roles it leaves out (null) keep the colour derived from its primary. */
class UiPalette(
    val primary: Color, val onPrimary: Color,
    val background: Color, val onBackground: Color, val onSurfaceVariant: Color,
    val container: Color, val containerHigh: Color, val containerHighest: Color,
    val outline: Color, val divider: Color,
    val error: Color, val onError: Color,
    val primaryContainer: Color? = null, val onPrimaryContainer: Color? = null,
    val secondaryContainer: Color? = null, val onSecondaryContainer: Color? = null,
    val tertiaryContainer: Color? = null, val onTertiaryContainer: Color? = null,
    val errorContainer: Color? = null, val onErrorContainer: Color? = null,
) {
    /** Lays this palette over a Miuix palette. Page and card surfaces, text, outlines and the roles above come from the
     * theme; everything that is a state of the primary (disabled, variant) stays derived from the primary. */
    fun overlay(base: Colors): Colors = base.copy(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer ?: base.primaryContainer, onPrimaryContainer = onPrimaryContainer ?: base.onPrimaryContainer,
        secondaryContainer = secondaryContainer ?: base.secondaryContainer, onSecondaryContainer = onSecondaryContainer ?: base.onSecondaryContainer,
        tertiaryContainer = tertiaryContainer ?: base.tertiaryContainer, onTertiaryContainer = onTertiaryContainer ?: base.onTertiaryContainer,
        error = error, onError = onError,
        errorContainer = errorContainer ?: base.errorContainer, onErrorContainer = onErrorContainer ?: base.onErrorContainer,
        background = background, onBackground = onBackground, onBackgroundVariant = onSurfaceVariant,
        surface = background, onSurface = onBackground, surfaceVariant = container,
        onSurfaceSecondary = onSurfaceVariant, onSurfaceVariantSummary = onSurfaceVariant, onSurfaceVariantActions = onSurfaceVariant,
        surfaceContainer = container, onSurfaceContainer = onBackground, onSurfaceContainerVariant = onSurfaceVariant,
        surfaceContainerHigh = containerHigh, onSurfaceContainerHigh = onBackground,
        surfaceContainerHighest = containerHighest, onSurfaceContainerHighest = onBackground,
        outline = outline, dividerLine = divider,
    )
}

/** A published colour theme with a light and a dark palette. The colours are the theme's own, not derived. */
class UiTheme(
    val id: String, val title: String, val summary: String, val light: UiPalette, val dark: UiPalette,
) {
    fun palette(dark: Boolean): UiPalette = if (dark) this.dark else light
    /** The colours shown in the picker, for the mode the app is in. */
    fun swatch(dark: Boolean): List<Color> = palette(dark).let { listOf(it.background, it.container, it.primary, it.onBackground) }
}

object UiThemes {
    /**
     * Catppuccin, https://github.com/catppuccin/catppuccin (MIT, Copyright (c) 2021 Catppuccin).
     * Latte for the light mode and Mocha for the dark mode; the accent is Mauve, text is Text, secondary text Subtext.
     */
    val CATPPUCCIN = UiTheme(
        "catppuccin", "Catppuccin", "浅色 Latte，深色 Mocha",
        light = UiPalette(
            primary = Color(0xFF8839EF), onPrimary = Color(0xFFEFF1F5),
            background = Color(0xFFEFF1F5), onBackground = Color(0xFF4C4F69), onSurfaceVariant = Color(0xFF6C6F85),
            container = Color(0xFFDCE0E8), containerHigh = Color(0xFFCCD0DA), containerHighest = Color(0xFFBCC0CC),
            outline = Color(0xFF9CA0B0), divider = Color(0xFFCCD0DA),
            error = Color(0xFFD20F39), onError = Color(0xFFEFF1F5),
        ),
        dark = UiPalette(
            primary = Color(0xFFCBA6F7), onPrimary = Color(0xFF1E1E2E),
            background = Color(0xFF1E1E2E), onBackground = Color(0xFFCDD6F4), onSurfaceVariant = Color(0xFFA6ADC8),
            container = Color(0xFF313244), containerHigh = Color(0xFF45475A), containerHighest = Color(0xFF585B70),
            outline = Color(0xFF6C7086), divider = Color(0xFF45475A),
            error = Color(0xFFF38BA8), onError = Color(0xFF1E1E2E),
        ),
    )

    /**
     * Material Design 3 baseline colour scheme (Google, https://m3.material.io/styles/color/static/baseline, Apache-2.0),
     * the published tokens for the purple baseline, used unchanged for both modes.
     */
    val MATERIAL = UiTheme(
        "material3", "Material 3", "Google 基线配色，浅色与深色",
        light = UiPalette(
            primary = Color(0xFF6750A4), onPrimary = Color(0xFFFFFFFF),
            background = Color(0xFFFEF7FF), onBackground = Color(0xFF1D1B20), onSurfaceVariant = Color(0xFF49454F),
            container = Color(0xFFF3EDF7), containerHigh = Color(0xFFECE6F0), containerHighest = Color(0xFFE6E0E9),
            outline = Color(0xFF79747E), divider = Color(0xFFCAC4D0),
            error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF4F378B),
            secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF4A4458),
            tertiaryContainer = Color(0xFFFFD8E4), onTertiaryContainer = Color(0xFF633B48),
            errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF8C1D18),
        ),
        dark = UiPalette(
            primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72),
            background = Color(0xFF141218), onBackground = Color(0xFFE6E0E9), onSurfaceVariant = Color(0xFFCAC4D0),
            container = Color(0xFF211F26), containerHigh = Color(0xFF2B2930), containerHighest = Color(0xFF36343B),
            outline = Color(0xFF938F99), divider = Color(0xFF49454F),
            error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
            primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
            secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
            tertiaryContainer = Color(0xFF633B48), onTertiaryContainer = Color(0xFFFFD8E4),
            errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
        ),
    )

    val ALL: List<UiTheme> = listOf(MATERIAL, CATPPUCCIN)
    fun byId(id: String): UiTheme? = ALL.firstOrNull { it.id == id }
}
