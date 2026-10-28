// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Good = Color(0xFF28AE83)
private val Caution = Color(0xFFCE9846)
private val Info = Color(0xFF5487E6)

private fun tone(severity: HookHealth.Severity, error: Color): Color = when (severity) {
    HookHealth.Severity.ERROR -> error
    HookHealth.Severity.WARN -> Caution
    HookHealth.Severity.INFO -> Info
    HookHealth.Severity.OK -> Good
}

private fun bytes(value: Long): String = when {
    value < 1024L -> "$value B"
    value < 1024L * 1024L -> String.format(Locale.ROOT, "%.1f KB", value / 1024.0)
    else -> String.format(Locale.ROOT, "%.2f MB", value / 1024.0 / 1024.0)
}

private fun span(first: Long, last: Long): String {
    val minutes = ((last - first) / 60000L).coerceAtLeast(0L)
    return when {
        minutes < 1L -> "Menos de 1 minuto"
        minutes < 60L -> "$minutes minutos en total"
        minutes < 24L * 60L -> "${minutes / 60} h y ${minutes % 60} min en total"
        else -> "${minutes / (24 * 60)} días y ${minutes % (24 * 60) / 60} h en total"
    }
}

/** 关于 → 问题诊断. Two sections the reader chooses between: what the log holds and what to do with it, and the log itself. */
@Composable
fun DiagnosticsPage(app: ModernMainActivity) {
    val result = app.diagnostics
    val stats = app.diagnosticsStats
    val hooks = app.diagnosticsHooks
    val errorColor = MiuixTheme.colorScheme.error
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var level by rememberSaveable { mutableIntStateOf(0) }
    var hooksOpen by rememberSaveable { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<Int?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val scroll = rememberC17ScrollBehavior(rememberC17TopAppBarState(initialHeightOffsetLimit = 0f))
    val list = rememberLazyListState()
    val clock = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.ROOT) }
    val short = remember { SimpleDateFormat("MM-dd HH:mm", Locale.ROOT) }
    val filtered = remember(result, level, query) {
        (result?.entries ?: emptyList()).asReversed().filter { entry ->
            (level == 0 || (level == 1 && entry.level != "info") || (level == 2 && entry.level == "error")) &&
                (query.isBlank() || entry.message.contains(query, true) || entry.sourceLabel.contains(query, true) || entry.detail.contains(query, true))
        }.take(150)
    }
    C17Scaffold(containerColor = MiuixTheme.colorScheme.surface, topBar = {
        C17SmallTopAppBar(title = "Diagnóstico de problemas", scrollBehavior = scroll, navigationIcon = {
            C17IconButton(onClick = app::closeDiagnostics) { Icon(Icons.Outlined.ArrowBack, "Volver") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).nestedScroll(scroll.nestedScrollConnection),
            state = list, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = padding.calculateBottomPadding() + 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "summary") { Summary(result, app.diagnosticsLoading, errorColor) }
            item(key = "tabs") { UiContentTabs(listOf("Exportación de registros", "Registro de eventos"), tab, { tab = it }) }
            if (tab == 0) {
                item(key = "overview") {
                    UiGroupCard {
                        UiRow("Intervalo de tiempo", if (stats == null || stats.first == 0L) "Todavía no hay registros." else span(stats.first, stats.last),
                            if (stats == null || stats.first == 0L) "—" else "${short.format(Date(stats.first))} → ${short.format(Date(stats.last))}")
                        UiRow("Contenido", "Los eventos se cuentan por elemento y los archivos de registro por línea.", if (stats == null) "—" else "${stats.events} eventos · ${stats.lines} líneas")
                        UiRow("Tamaño del archivo", if (stats == null) "" else "El informe exportado ocupa aproximadamente ${bytes(stats.exportBytes)}.", if (stats == null) "—" else bytes(stats.fileBytes))
                        val worry = result?.findings?.filter { it.severity == HookHealth.Severity.ERROR || it.severity == HookHealth.Severity.WARN } ?: emptyList()
                        UiRow("Posibles problemas", if (result == null) "Analizando…" else if (worry.isEmpty()) "No se encontraron problemas evidentes." else worry.take(3).joinToString("、") { it.title },
                            if (result == null) "—" else "${worry.size} elementos")
                    }
                }
                if (result != null) {
                    item { SectionLabel("Conclusiones del diagnóstico") }
                    items(result.findings, key = { it.id }) { finding -> Finding(finding, errorColor) }
                }
                item { SectionLabel("Estado de los hooks") }
                item(key = "hooks") {
                    UiGroupCard {
                        val rows = HookHealth.table(hooks)
                        val bad = rows.count { it[2] == "No preparado" || it[2] == "Error" }
                        UiRow("Hooks de la interfaz del sistema", if (hooks == null) "Aún no se ha recibido respuesta de la interfaz del sistema. Pulsa Actualizar abajo." else "${rows.size} grupos en total; $bad no están preparados.",
                            if (hooksOpen) "Contraer" else "Expandir") { hooksOpen = !hooksOpen }
                        if (hooksOpen) rows.forEach { row ->
                            val color = when (row[2]) { "No preparado", "Error" -> errorColor; "Bajo demanda", "No activado" -> MiuixTheme.colorScheme.onSurfaceVariantSummary; else -> Good }
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                                Text(row[1], Modifier.weight(1f).padding(start = 10.dp), fontSize = 14.sp)
                                Text(row[3], fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                Text(row[2], Modifier.padding(start = 10.dp), fontSize = 13.sp, color = color)
                            }
                        }
                    }
                }
                item { SectionLabel("Acciones") }
                item(key = "actions") {
                    UiGroupCard {
                        UiSwitchRow("Registro de diagnóstico", "Actívalo antes de reproducir el problema; el estado de los hooks no depende del registro.", app.bool(StatusBarSettings.DIAGNOSTICS_ENABLED), app.canEdit) {
                            app.save(StatusBarSettings.DIAGNOSTICS_ENABLED, it)
                        }
                        UiRow("Actualizar", "Volver a leer los registros y consultar a la interfaz del sistema.", icon = Icons.Outlined.Refresh) { app.openDiagnostics() }
                        UiRow("Exportar informe", "Guardar como texto legible para revisarlo antes.", icon = Icons.Outlined.FileUpload) { app.chooseLog() }
                        UiRow("Compartir informe", icon = Icons.Outlined.Share) { app.shareLog() }
                        UiRow("Borrar registros", "Solo elimina los registros; no afecta a la configuración.", icon = Icons.Outlined.DeleteOutline, enabled = !app.busy) { app.clearLogHistory() }
                    }
                }
            } else {
                item(key = "log-controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        UiContentTabs(listOf("Todo", "Advertencias y niveles superiores", "Solo errores"), level, { level = it })
                        C17TextField(value = query, onValueChange = { query = it }, label = "Buscar mensaje, origen o excepción", singleLine = true,
                            useLabelAsPlaceholder = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (result == null || filtered.isEmpty()) item {
                    Text(if (result == null) "Leyendo…" else if (query.isNotBlank() || level != 0) "No hay registros que coincidan." else "Todavía no hay registros. Activa el registro de diagnóstico, reproduce el problema y vuelve aquí.",
                        Modifier.padding(horizontal = 4.dp, vertical = 8.dp), fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
                items(filtered.size, key = { index -> "e${filtered[index].time}-$index" }) { index ->
                    val entry = filtered[index]
                    val color = when (entry.level) { "error" -> errorColor; "warn" -> Caution; else -> MiuixTheme.colorScheme.onSurfaceVariantSummary }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MiuixTheme.colorScheme.onSurface.copy(alpha = .045f))
                        .clickable(enabled = entry.detail.isNotEmpty()) { expanded = if (expanded == index) null else index }
                        .padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(entry.levelLabel(), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
                            Text(entry.sourceLabel, fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            Text(clock.format(Date(entry.time)), Modifier.weight(1f), fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                        Text(entry.message.ifEmpty { "(sin mensaje)" }, fontSize = 14.sp, maxLines = if (expanded == index) 12 else 2, overflow = TextOverflow.Ellipsis)
                        if (expanded == index && entry.detail.isNotEmpty())
                            Text(entry.detail, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
                if (result != null && result.entries.size >= DiagnosticCenter.MAX_ENTRIES) item {
                    Text("Solo se muestran los ${filtered.size} registros más recientes; el informe exportado contiene el historial completo.", Modifier.padding(horizontal = 4.dp), fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(title, Modifier.padding(start = 4.dp, top = 6.dp), fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
}

@Composable
private fun Summary(result: DiagnosticCenter.Result?, loading: Boolean, errorColor: Color) {
    val worst = result?.findings?.firstOrNull { it.severity != HookHealth.Severity.OK && it.severity != HookHealth.Severity.INFO }
    val color = when {
        result == null -> Info
        worst != null -> tone(worst.severity, errorColor)
        else -> Good
    }
    val title = when {
        result == null -> if (loading) "Comprobando…" else "Aún no comprobado"
        worst == null -> "No se encontraron problemas."
        else -> worst.title
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = .14f)).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(if (result == null) "Leer los registros y consultar a la interfaz del sistema" else "${result.errors} errores · ${result.warnings} advertencias · ${result.entries.size} registros",
                fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

@Composable
private fun Finding(finding: HookHealth.Finding, errorColor: Color) {
    val color = tone(finding.severity, errorColor)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MiuixTheme.colorScheme.onSurface.copy(alpha = .045f)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.padding(top = 5.dp).size(9.dp).clip(CircleShape).background(color))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(finding.title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (finding.detail.isNotEmpty()) Text(finding.detail, fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}
