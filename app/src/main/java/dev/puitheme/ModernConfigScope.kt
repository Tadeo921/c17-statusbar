// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 分享、导出、导入配置之前先问一句：是否自定义项。第一步选“全部”或“自定义”，自定义则逐个页面勾选。 */

private fun verb(kind: String) = when (kind) { "share" -> "Compartir"; "import" -> "Importar"; else -> "Exportar" }

/** The pages the choice is about: all of them for share and export, only those the file carries for an import. */
private fun choices(request: ConfigScopeRequest): List<Pair<ConfigScope.Section, Int>> =
    if (request.prepared != null) ConfigScope.present(request.prepared.keys()).map { it.key to it.value }
    else ConfigScope.sections().map { it to it.count() }

@Composable
fun ConfigScopeDialog(app: ModernMainActivity, request: ConfigScopeRequest) {
    if (request.customizing) ConfigScopeChecklist(app, request) else ConfigScopeAsk(app, request)
}

@Composable
private fun ConfigScopeAsk(app: ModernMainActivity, request: ConfigScopeRequest) {
    val action = verb(request.kind)
    val pages = choices(request)
    val total = pages.sumOf { it.second }
    val summary = if (request.prepared != null)
        "Este archivo contiene ${pages.size} páginas y $total ajustes. ¿Quieres personalizar qué importar?\n«Importar todo» importa todos los ajustes del archivo; la opción personalizada permite elegir solo las páginas necesarias."
    else
        "¿Personalizar los elementos para ${action}?\n«${action} todo» incluirá los ajustes de todas las páginas; la opción personalizada permite elegir solo lo necesario."
    C17OverlayDialog(show = true, title = "${action} configuración", summary = summary, onDismissRequest = app::cancelConfigScope) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                C17TextButton(text = "Personalizado", onClick = app::customizeConfigScope, modifier = Modifier.weight(1f))
                C17TextButton(text = "${action} todo", onClick = app::confirmConfigScopeAll, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary())
            }
            C17TextButton(text = "Cancelar", onClick = app::cancelConfigScope, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ConfigScopeChecklist(app: ModernMainActivity, request: ConfigScopeRequest) {
    val action = verb(request.kind)
    val pages = choices(request)
    val chosen = pages.count { it.first.id in request.selected }
    val items = pages.filter { it.first.id in request.selected }.sumOf { it.second }
    C17OverlayDialog(show = true, title = "Seleccionar elementos para ${action}", summary = "Seleccionadas $chosen de ${pages.size} páginas, con $items ajustes en total",
        onDismissRequest = app::cancelConfigScope) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                C17TextButton(text = "Seleccionar todo", onClick = { app.selectAllConfigScope(true) }, modifier = Modifier.weight(1f))
                C17TextButton(text = "Deseleccionar todo", onClick = { app.selectAllConfigScope(false) }, modifier = Modifier.weight(1f))
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                pages.forEach { (section, count) ->
                    C17SwitchPreference(title = section.label(), summary = "$count elementos", checked = section.id in request.selected,
                        onCheckedChange = { app.toggleConfigScope(section.id, it) })
                }
                if (request.prepared == null) Text("Los archivos de fuentes y bibliotecas de iconos no se incluyen al ${action} la configuración; solo se incluyen sus ajustes.", Modifier.padding(top = 8.dp),
                    fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                C17TextButton(text = "Volver", onClick = app::backConfigScope, modifier = Modifier.weight(1f))
                C17TextButton(text = action, enabled = chosen > 0, onClick = app::confirmConfigScopeChoice, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary())
            }
        }
    }
}
