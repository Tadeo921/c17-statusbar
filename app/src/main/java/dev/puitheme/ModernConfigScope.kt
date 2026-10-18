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

private fun verb(kind: String) = when (kind) { "share" -> "分享"; "import" -> "导入"; else -> "导出" }

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
        "这个文件包含 ${pages.size} 个页面、共 $total 项设置。是否自定义导入项？\n选择“全部导入”会导入文件里的所有设置；自定义可以只导入需要的页面。"
    else
        "是否自定义${action}项？\n选择“全部${action}”会带上所有页面的设置；自定义可以只${action}需要的页面。"
    C17OverlayDialog(show = true, title = "${action}配置", summary = summary, onDismissRequest = app::cancelConfigScope) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                C17TextButton(text = "自定义", onClick = app::customizeConfigScope, modifier = Modifier.weight(1f))
                C17TextButton(text = "全部$action", onClick = app::confirmConfigScopeAll, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary())
            }
            C17TextButton(text = "取消", onClick = app::cancelConfigScope, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ConfigScopeChecklist(app: ModernMainActivity, request: ConfigScopeRequest) {
    val action = verb(request.kind)
    val pages = choices(request)
    val chosen = pages.count { it.first.id in request.selected }
    val items = pages.filter { it.first.id in request.selected }.sumOf { it.second }
    C17OverlayDialog(show = true, title = "选择${action}项", summary = "已选 $chosen / ${pages.size} 个页面，共 $items 项设置",
        onDismissRequest = app::cancelConfigScope) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                C17TextButton(text = "全选", onClick = { app.selectAllConfigScope(true) }, modifier = Modifier.weight(1f))
                C17TextButton(text = "全不选", onClick = { app.selectAllConfigScope(false) }, modifier = Modifier.weight(1f))
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                pages.forEach { (section, count) ->
                    C17SwitchPreference(title = section.label(), summary = "$count 项", checked = section.id in request.selected,
                        onCheckedChange = { app.toggleConfigScope(section.id, it) })
                }
                if (request.prepared == null) Text("字体、图标库等文件不会随配置${action}，只带上它们的设置。", Modifier.padding(top = 8.dp),
                    fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                C17TextButton(text = "返回", onClick = app::backConfigScope, modifier = Modifier.weight(1f))
                C17TextButton(text = action, enabled = chosen > 0, onClick = app::confirmConfigScopeChoice, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary())
            }
        }
    }
}
