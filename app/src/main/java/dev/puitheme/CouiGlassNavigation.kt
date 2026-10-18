// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import dev.puitheme.liquid.lens
import dev.puitheme.liquid.vibrancy
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop

/** COUI typography and motion over the existing app-local refraction renderer. No idle animation. */
@Composable
internal fun CouiGlassNavigationBar(selectedIndex: Int, onItemClick: (Int) -> Unit,
    backdrop: LayerBackdrop, isBlurActive: Boolean, modifier: Modifier = Modifier) {
    val colors = COUITheme.colorScheme
    val dark = LocalC17Dark.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val selected = selectedIndex.coerceIn(0, 2)
    val labels = remember { listOf("主页", "配置", "关于") }
    val icons = remember { listOf(Icons.Outlined.Home, Icons.Outlined.Settings, Icons.Outlined.Info) }
    val measurer = rememberTextMeasurer()
    val labelStyle = COUITheme.textStyles.footnote1.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val labelHeight = with(density) { measurer.measure("主页", style = labelStyle).size.height.toDp() }
    val barHeight = maxOf(64.dp, 24.dp + labelHeight + 20.dp)
    val shape = remember { RoundedCornerShape(28.dp) }
    val indicatorShape = remember { RoundedCornerShape(23.dp) }
    val motion = remember { CubicBezierEasing(.25f, .46f, .45f, .94f) }
    val position = animateFloatAsState(selected.toFloat(), tween(260, easing = motion), label = "coui-glass-selection")
    val glassTint = if (dark) Color(0xFF22262D).copy(alpha = .88f) else Color.White.copy(alpha = .80f)
    val opaque = lerp(colors.background, colors.primary, if (dark) .09f else .025f).copy(alpha = 1f)
    val inkSurfaces = remember(glassTint, opaque, colors.primary, dark, isBlurActive) {
        c17GlassSurfaces(if (isBlurActive) glassTint else opaque, colors.primary, if (dark) .25f else .15f)
    }
    val activeInk = remember(colors.primary, inkSurfaces) { c17ReadableInk(colors.primary, inkSurfaces) }
    val idleInk = remember(colors.onSurfaceVariantSummary, inkSurfaces) {
        c17ReadableInk(colors.onSurfaceVariantSummary, inkSurfaces)
    }
    val rim = remember(dark) { Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) .26f else .90f),
        Color.White.copy(alpha = .08f), Color.White.copy(alpha = if (dark) .18f else .65f))) }
    val selectedFill = remember(colors.primary, dark) { Brush.verticalGradient(listOf(
        colors.primary.copy(alpha = if (dark) .25f else .15f), colors.primary.copy(alpha = if (dark) .13f else .07f))) }
    Column(modifier.fillMaxWidth()
        .pointerInput(Unit) { detectTapGestures { /* The glass surround must not click through to a page row. */ } }
        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
            val cellWidth = (maxWidth - 8.dp) / 3
            val cellWidthPx = with(density) { cellWidth.toPx() }
            Box(Modifier.fillMaxWidth().height(barHeight)
                .then(if (isBlurActive) Modifier.drawBackdrop(backdrop = backdrop, shape = { shape },
                    effects = {
                        padding = maxOf(padding, 14.dp.toPx())
                        vibrancy()
                        blur(12.dp.toPx())
                        lens(refractionHeight = 12.dp.toPx(), refractionAmount = 8.dp.toPx())
                    }, onDrawSurface = { drawRect(glassTint) }) else Modifier.background(opaque, shape))
                .border(1.dp, rim, shape).clip(shape)) {
                Box(Modifier.padding(start = 4.dp, top = 4.dp)
                    .width(cellWidth).height(barHeight - 8.dp)
                    .graphicsLayer { translationX = cellWidthPx * position.value * (if (rtl) -1f else 1f) }
                    .background(selectedFill, indicatorShape)
                    .border(.5.dp, colors.primary.copy(alpha = if (dark) .16f else .10f), indicatorShape))
                Row(Modifier.fillMaxSize().padding(4.dp).selectableGroup()) {
                    labels.forEachIndexed { index, label ->
                        val tint by animateColorAsState(if (selected == index) activeInk else idleInk,
                            tween(180), label = "coui-glass-content-$index")
                        Column(Modifier.weight(1f).fillMaxHeight().clip(indicatorShape)
                            .selectable(selected == index, role = Role.Tab,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { if (selected != index) onItemClick(index) })
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
                            Icon(icons[index], contentDescription = null, modifier = Modifier.size(24.dp), tint = tint)
                            Text(label, style = labelStyle, color = tint, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}
