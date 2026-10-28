// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/** 关于 → 界面设置: the look of this app only. Few words, one row per choice, the same rows as everywhere else. */
@Composable
fun UiSettingsPage(app: ModernMainActivity) {
    val ui = app.ui
    var choosingUi by rememberSaveable { mutableStateOf(false) }
    var typing by remember { mutableStateOf<String?>(null) }
    // The dark preview stays a moment after the finger lifts, so the result can be seen.
    val scope = rememberCoroutineScope()
    var release by remember { mutableStateOf<Job?>(null) }
    fun preview(on: Boolean) {
        release?.cancel()
        if (on) ui.previewDark = true else release = scope.launch { delay(1500); ui.previewDark = false }
    }
    LaunchedEffect(typing) { if (typing == "dark") preview(true) else if (ui.previewDark) preview(false) }
    DisposableEffect(Unit) { onDispose { release?.cancel(); ui.previewDark = false } }
    val scroll = rememberC17ScrollBehavior(rememberC17TopAppBarState(initialHeightOffsetLimit = 0f))
    val list = rememberLazyListState()
    C17Scaffold(containerColor = MiuixTheme.colorScheme.surface, topBar = {
        C17SmallTopAppBar(title = "Ajustes de interfaz", scrollBehavior = scroll, navigationIcon = {
            C17IconButton(onClick = app::closeUiSettings) { Icon(Icons.Outlined.ArrowBack, "Volver") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())
            .nestedScroll(scroll.nestedScrollConnection).edgeFade(list, ui.edgeFade), state = list,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp,
                bottom = padding.calculateBottomPadding() + 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "ui-style") { UiGroupCard {
                UiRow("Estilo de la interfaz", "Elige el estilo visual de la aplicación.", ui.uiStyleTitle) { choosingUi = true }
            } }
            item { UiSectionTitle("Tema") }
            item { UiGroupCard {
                UiChoiceRow("Modo claro/oscuro", listOf("Seguir al sistema", "Claro", "Oscuro"), ui.themeMode) { ui.updateThemeMode(it) }
            } }
            item { UiSectionTitle("Colores") }
            item { UiGroupCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Esquema de colores", Modifier.weight(1f), fontSize = 16.sp)
                        Text(ui.scheme(ui.scheme).title, fontSize = 14.sp, color = MiuixTheme.colorScheme.primary)
                    }
                    // Keep labels readable on narrow screens and with the user's larger text setting.
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
                        val columns = (maxWidth / (56.dp * fontScale)).toInt().coerceIn(2, 5)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            UiSettings.SCHEMES.chunked(columns).forEach { line ->
                                Row(Modifier.fillMaxWidth()) {
                                    line.forEach { scheme -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SchemeSwatch(ui, scheme) } }
                                    repeat(columns - line.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
                UiChoiceRow("Estilo de color", listOf("Suave", "Vivo", "Neutro"), ui.paletteStyle, enabled = ui.scheme(ui.scheme).theme == null) { ui.updatePaletteStyle(it) }
                UiTypableSlider("Fondo oscuro", ui.darkLevel.toFloat(), 0f..100f,
                    when { ui.darkLevel >= 98 -> "Negro puro"; ui.darkLevel == 50 -> "Predeterminado"; ui.darkLevel > 50 -> "Más oscuro"; else -> "Más gris" },
                    onType = { typing = "dark" }, onDragging = ::preview) { ui.updateDarkLevel(it.roundToInt()) }
            } }
            item { UiSectionTitle("Nombre") }
            item { UiGroupCard {
                UiSwitchRow("Restaurar el nombre anterior", "El icono del escritorio mostrará «${LauncherIcon.LEGACY_NAME}»; el nombre en Ajustes del sistema y en LSPosed seguirá siendo ${LauncherIcon.NAME}.",
                    app.legacyName, enabled = app.desktopIconAvailable && !app.busy && !app.maintenanceRunning,
                    onChecked = app::requestLegacyName)
            } }
            item { UiSectionTitle("Temas de código abierto") }
            item { UiGroupCard {
                UiThemes.ALL.forEach { theme -> ThemeRow(ui, theme) }
            } }
            item { UiSectionTitle("Los colores de estos temas proceden de proyectos de código abierto y conservan los originales. Al seleccionarlos, el estilo de color deja de aplicarse; el modo claro/oscuro y el fondo oscuro siguen disponibles.") }
            item { UiSectionTitle("Mostrar") }
            item { UiGroupCard {
                UiTypableSlider("Tamaño del texto de la interfaz", ui.textScale.toFloat(), 80f..130f, "${ui.textScale}%", onType = { typing = "scale" }) { ui.updateTextScale(it.roundToInt()) }
                UiSwitchRow("Desvanecer al desplazarse", checked = ui.edgeFade) { ui.updateEdgeFade(it) }
                val glass = GlassNavigationPreference.supported()
                UiSwitchRow("Barra inferior de vidrio líquido", if (glass) "" else "Requiere Android 13 o posterior.",
                    checked = app.glassNavigation, enabled = glass) { app.updateGlassNavigation(it) }
            } }
            item { UiGroupCard {
                UiRow("Restaurar valores predeterminados", icon = Icons.Outlined.RestartAlt) { app.resetUi(); app.toast("Se restauraron los ajustes predeterminados de la interfaz.") }
            } }
        }
        if (choosingUi) UiChoiceDialog(
            title = "Estilo de la interfaz",
            values = listOf(UiSettings.MIUIX, UiSettings.COUI),
            labels = listOf("Miuix", "COUI"),
            selected = ui.uiStyle,
            onDismiss = { choosingUi = false },
            onChoice = { app.updateUiStyle(it) },
        )
        // Tapping the value types it in, like every other number in the app.
        when (typing) {
            "scale" -> UiInputDialog("Tamaño del texto de la interfaz", ui.textScale.toString(), description = "80–130, en %", numeric = true, integer = true,
                allowNegative = false, label = "Tamaño del texto (%)", onDismiss = { typing = null }) { text ->
                val value = text.trim().toDoubleOrNull()
                if (value == null || !value.isFinite() || value < 80 || value > 130) "Introduce un número entre 80 y 130."
                else { ui.updateTextScale(value.roundToInt()); null }
            }
            "dark" -> UiInputDialog("Fondo oscuro", ui.darkLevel.toString(), description = "0 = grisáceo, 50 = predeterminado, 100 = negro puro", numeric = true, integer = true,
                allowNegative = false, label = "Intensidad del negro", onDismiss = { typing = null }) { text ->
                val value = text.trim().toDoubleOrNull()
                if (value == null || !value.isFinite() || value < 0 || value > 100) "Introduce un número entre 0 y 100."
                else { ui.updateDarkLevel(value.roundToInt()); null }
            }
        }
    }
}

/** A slider whose value can be tapped to type an exact number. */
@Composable
private fun UiTypableSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, valueText: String,
    onType: () -> Unit, onDragging: (Boolean) -> Unit = {}, onChange: (Float) -> Unit) {
    var current by remember(value) { mutableStateOf(value) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(onClick = onType), fontSize = 16.sp)
            Text(valueText, Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onType).padding(horizontal = 8.dp, vertical = 6.dp),
                fontSize = 14.sp, color = MiuixTheme.colorScheme.primary)
        }
        C17Slider(value = current, onValueChange = { onDragging(true); current = it; onChange(it) }, valueRange = range,
            onValueChangeFinished = { onDragging(false) })
    }
}

@Composable
private fun UiSectionTitle(title: String) {
    Text(title, Modifier.padding(start = 12.dp, top = 4.dp, bottom = 0.dp), fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
}

/** The choices grow with the labels and font scale, sharing the content-sized tab control. */
@Composable
private fun UiChoiceRow(title: String, labels: List<String>, selected: Int, enabled: Boolean = true, onSelected: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).alpha(if (enabled) 1f else .4f),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 16.sp)
        UiSegmented(labels, selected.coerceIn(0, labels.lastIndex), enabled, onSelected)
    }
}

/** One rounded track with a filled thumb on the chosen segment: a single, uniform control. */
@Composable
private fun UiSegmented(labels: List<String>, selected: Int, enabled: Boolean, onSelected: (Int) -> Unit) {
    UiContentTabs(labels, selected, onSelected, contour = true, enabled = enabled)
}

/** An open-source theme: the colours it uses in the current mode, its name and where its palette comes from. */
@Composable
private fun ThemeRow(ui: UiSettings, theme: UiTheme) {
    val selected = ui.scheme == theme.id
    val ink = MiuixTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth()
        .selectable(selected = selected, role = Role.RadioButton,
            interactionSource = remember { MutableInteractionSource() }, indication = null) { ui.updateScheme(theme.id) }
        .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        val shape = RoundedCornerShape(10.dp)
        Row(Modifier.clip(shape).border(1.dp, ink.copy(alpha = .12f), shape)) {
            theme.swatch(LocalC17Dark.current).forEach { Box(Modifier.size(width = 14.dp, height = 36.dp).background(it)) }
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(theme.title, fontSize = 16.sp)
            Text(theme.summary, fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        Box(Modifier.size(22.dp).clip(CircleShape)
            .background(if (selected) MiuixTheme.colorScheme.primary else Color.Transparent)
            .border(if (selected) 0.dp else 1.5.dp, ink.copy(alpha = .3f), CircleShape),
            contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(MiuixTheme.colorScheme.onPrimary))
        }
    }
}

@Composable
private fun SchemeSwatch(ui: UiSettings, scheme: UiScheme) {
    val selected = ui.scheme == scheme.id
    val enabled = !scheme.dynamic || ui.dynamicSupported
    val ring = MiuixTheme.colorScheme.onSurface
    val fill: Brush = when {
        scheme.dynamic -> Brush.sweepGradient(listOf(Color(0xFFFF5F57), Color(0xFFFFBD2E), Color(0xFF28C840), Color(0xFF30B0C7), Color(0xFF3482FF), Color(0xFFAF52DE), Color(0xFFFF5F57)))
        scheme.key == null -> {
            val blue = if (ui.coui) Color(0xFF0066FF) else UiSettings.STOCK_BLUE
            Brush.linearGradient(listOf(blue, blue))
        }
        else -> Brush.linearGradient(listOf(scheme.key, scheme.key))
    }
    Column(Modifier.alpha(if (enabled) 1f else .4f).selectable(
        selected = selected, enabled = enabled, role = Role.RadioButton,
        interactionSource = remember { MutableInteractionSource() }, indication = null) { ui.updateScheme(scheme.id) },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(fill)
            .border(if (selected) 3.dp else 1.dp, if (selected) ring.copy(alpha = .85f) else ring.copy(alpha = .12f), CircleShape))
        Text(scheme.title, Modifier.padding(top = 4.dp), fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}
