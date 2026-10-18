// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/** Whether the app is currently drawn dark, after the user's mode choice and the system's automatic dark mode. */
val LocalC17Dark = staticCompositionLocalOf { false }

/** The settings app's look: mode, colour scheme, how dark the dark mode is and the text size. */
@Composable
fun C17Theme(ui: UiSettings, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    // While the dark background is being adjusted the whole app turns dark, so the change is seen as it is made.
    val dark = ui.previewDark || when (ui.themeMode) { 1 -> false; 2 -> true; else -> systemDark }
    if (ui.coui) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalC17Dark provides dark,
            LocalDensity provides Density(density.density, density.fontScale * ui.textScale / 100f),
        ) { C17CouiTheme(ui, dark, content) }
        return
    }
    val preset = ui.scheme(ui.scheme)
    val theme = preset.theme
    val context = LocalContext.current
    // Classic blue keeps the stock Miuix surfaces and text in every style; only its accent colours follow the style.
    val stockStyled = preset.key == null && !preset.dynamic && ui.paletteStyle != 0
    // "System" is the wallpaper palette as it is; vivid and muted derive a palette from the wallpaper's accent colour,
    // because the system palette itself has no styles.
    val systemKey = if (preset.dynamic && ui.dynamicSupported && ui.paletteStyle != 0)
        runCatching { Color(context.getColor(android.R.color.system_accent1_500)) }.getOrNull() else null
    // A key colour always derives a full palette; "system" needs Android 12's wallpaper colours.
    val monet = preset.key != null || preset.dynamic && ui.dynamicSupported
    val mode = when {
        monet -> if (dark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
        else -> if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light
    }
    // A published theme brings its own colours; the soft derivation only fills the primary's disabled and variant states.
    val style = if (theme != null) ThemePaletteStyle.TonalSpot else ui.style()
    val key = if (theme != null) theme.palette(dark).primary else if (preset.dynamic) systemKey else preset.key
    val controller = remember(mode, preset.id, style, key) {
        ThemeController(colorSchemeMode = mode, keyColor = key, paletteStyle = style)
    }
    val accentController = remember(dark, style, stockStyled) {
        if (!stockStyled) null else ThemeController(
            colorSchemeMode = if (dark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight,
            keyColor = UiSettings.STOCK_BLUE, paletteStyle = style)
    }
    val stock = controller.currentColors()
    val accent = accentController?.currentColors()
    val base = if (accent == null) stock else stock.copy(
        primary = accent.primary, onPrimary = accent.onPrimary, primaryVariant = accent.primaryVariant,
        onPrimaryVariant = accent.onPrimaryVariant, primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer, disabledPrimary = accent.disabledPrimary,
        disabledOnPrimary = accent.disabledOnPrimary, disabledPrimaryButton = accent.disabledPrimaryButton,
        disabledOnPrimaryButton = accent.disabledOnPrimaryButton, disabledPrimarySlider = accent.disabledPrimarySlider,
    )
    val themed = if (theme == null) base else theme.palette(dark).overlay(base)
    val colors = if (dark) tuneDark(themed, ui.darkLevel) else themed
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalC17Dark provides dark,
        LocalDensity provides Density(density.density, density.fontScale * ui.textScale / 100f),
    ) {
        MiuixTheme(colors = colors, content = content)
    }
    ApplySystemBars(dark, colors.surface)
}

/** 50 keeps the scheme's own background; above it pure black grows in, below it a lighter grey. Cards stay readable. */
private fun tuneDark(base: Colors, level: Int): Colors {
    if (level == 50) return base
    val deeper = level > 50
    val t = if (deeper) (level - 50) / 50f else (50 - level) / 50f
    fun background(color: Color) = if (deeper) lerp(color, Color.Black, t) else lerp(color, Color(0xFF3A3A3C), t * .55f)
    fun container(color: Color) = if (deeper) lerp(color, Color(0xFF1C1C1E), t * .8f) else lerp(color, Color(0xFF505053), t * .55f)
    return base.copy(
        background = background(base.background), surface = background(base.surface),
        surfaceVariant = container(base.surfaceVariant), surfaceContainer = container(base.surfaceContainer),
        surfaceContainerHigh = container(base.surfaceContainerHigh), surfaceContainerHighest = container(base.surfaceContainerHighest),
    )
}

/** Edge-to-edge system bars follow the app's own dark state, not only the system's. */
@Composable
private fun ApplySystemBars(dark: Boolean, surface: Color) {
    val activity = LocalContext.current as? ComponentActivity ?: return
    SideEffect {
        val clear = android.graphics.Color.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(clear) else SystemBarStyle.light(clear, clear)
        runCatching { activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style) }
        runCatching { activity.window.setBackgroundDrawable(ColorDrawable(surface.toArgb())) }
    }
}

/**
 * Content that has scrolled up past the list's top edge fades out. The fade is as long as the list has
 * scrolled, up to a fixed 33 dp, so a list at rest is untouched and nothing needs to be tuned.
 * The offscreen layer exists only while the fade is actually drawn.
 */
fun Modifier.edgeFade(state: LazyListState, enabled: Boolean): Modifier = if (!enabled) this else
    graphicsLayer {
        compositingStrategy = if (state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0)
            CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }.drawWithContent {
        drawContent()
        val longest = 33.dp.toPx()
        val length = if (state.firstVisibleItemIndex > 0) longest else minOf(longest, state.firstVisibleItemScrollOffset.toFloat())
        if (length > 0f) drawRect(
            brush = Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, startY = 0f, endY = length),
            size = Size(size.width, length), blendMode = BlendMode.DstIn)
    }
