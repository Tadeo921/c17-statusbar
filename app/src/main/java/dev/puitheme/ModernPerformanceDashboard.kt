// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Status first: a progress ring and one sentence, the scope it covers, one action, and the rules in one line. */
@Composable
internal fun PerformanceDashboard(app: ModernMainActivity, installation: String, result: String) {
    val module = app.performanceReport
    val present = module?.installed == true || module?.pending == true
    val reliable = module != null && module.root && !app.performanceError
    val editable = app.canEdit && !app.busy && !app.performanceBusy && reliable
    val hasRecord = module != null && module.lastState != "unknown"
    val fresh = hasRecord && reliable && module!!.currentBoot &&
        module.recordVersionCode == module.moduleVersionCode &&
        module.recordVersionCode >= RootPerformanceModule.VERSION_CODE
    val activeTask = present && fresh && module!!.lastState in listOf("waiting", "running")
    val palette = homeCardPalette(installation, LocalC17Dark.current)
    val cardLabel = if (result == installation) "附属模块" else "附属模块 · $installation"
    var expanded by rememberSaveable { mutableStateOf(false) }
    val recordCounts = hasRecord && module!!.recordVersionCode >= 3L && module.packages > 0L
    val ringProgress = if (recordCounts) (module!!.verified.toFloat() / module.packages.toFloat()).coerceIn(0f, 1f) else 0f
    val ringLabel = if (recordCounts) "${module!!.verified}/${module.packages}" else "—"
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        C17Card(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)), cornerRadius = 22.dp,
            colors = CardDefaults.defaultColors(color = palette.base)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    val track = palette.accent.copy(alpha = .22f)
                    val progress = palette.accent.copy(alpha = if (fresh || !recordCounts) 1f else .55f)
                    Canvas(Modifier.matchParentSize()) {
                        val stroke = 6.dp.toPx()
                        val inset = stroke / 2f
                        val box = Size(size.width - stroke, size.height - stroke)
                        drawArc(track, 0f, 360f, false, Offset(inset, inset), box, style = Stroke(stroke))
                        if (ringProgress > 0f) drawArc(progress, -90f, 360f * ringProgress, false, Offset(inset, inset), box,
                            style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Text(ringLabel, style = C17CardTypography.label.copy(fontSize = 18.sp, lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold), color = palette.foreground)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(cardLabel, style = C17CardTypography.label, color = palette.secondary)
                    Text(result, style = C17CardTypography.title.copy(fontSize = 22.sp, lineHeight = 28.sp), color = palette.foreground)
                    val subtitle = when {
                        app.performanceError -> "状态读取失败，请重新查询。保留的上次记录不代表当前状态。"
                        module == null || !module.root -> "授权 Root 后，即可安装并管理附属模块。"
                        module.remove -> "已请求卸载，重启时尝试恢复编译策略。"
                        module.disabled -> "附属模块已停用，启用后自动检查；已有结果由系统管理。"
                        module.pending -> "重启后加载新的附属模块，当前运行记录不代表新版本已生效。"
                        module.outdated -> "更新到 1.7，先核验并复用可用结果，需要时再编译；新版修复偶发失败后整个开机内无法再优化的问题，并开始记录收益。"
                        !present -> "按需优化系统界面与控制中心，复用有效编译结果。"
                        !fresh && hasRecord -> "以下为上次运行记录，本次执行尚未确认。"
                        module.lastState == "waiting" -> if (module.trigger == "run") "手动任务正在准备执行。" else "自动任务等待息屏或 10 分钟无操作。"
                        module.lastState == "running" -> if (module.trigger == "run") "手动任务正在检查或编译，无环境条件等待。" else "自动任务正在检查或编译。"
                        module.errorMessage().isNotEmpty() -> module.errorMessage()
                        module.lastState == "current" -> "有效结果直接沿用，减少重复编译。"
                        module.lastState == "ok" -> module.verificationMessage()
                        else -> "安装与激活状态已确认；优化结果以独立运行记录为准。"
                    }
                    Text(subtitle, style = C17CardTypography.supporting, color = palette.secondary)
                }
            }
        }
        if (hasRecord) {
            val verification = when {
                fresh -> module!!.verificationMessage()
                module!!.recordVersionCode >= 3L && module.packages > 0L ->
                    "上次核验记录：${module.verified}/${module.packages} 个组件，当前状态尚未确认"
                module.recordVersionCode < 3L -> "旧版记录没有核验结果，当前状态尚未确认"
                else -> "尚无核验结果，当前状态尚未确认"
            }
            Text(verification, Modifier.padding(horizontal = 20.dp), style = C17CardTypography.supporting,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        // Measured costs and labelled estimates only; there is no before/after run, so no speed-up percentage appears.
        val benefit = remember(module) { PerformanceBenefit.of(module) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("收益记录", style = C17CardTypography.label.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = MiuixTheme.colorScheme.onSurface)
            Text(benefit.since, style = C17CardTypography.supporting, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        UiGroupCard {
            UiRow("累计编译", benefit.compilesNote, benefit.compiles, Icons.Outlined.Memory)
            UiRow("免去重编译", benefit.savedNote, benefit.saved, Icons.Outlined.Bolt)
            UiRow("系统界面", benefit.aotNote, benefit.aot, Icons.Outlined.Smartphone)
            UiRow("运行时编译", benefit.jitNote, benefit.jit, Icons.Outlined.Speed)
        }
        Text("耗时来自 ART 每次编译的返回值；“免去”按各组件上次编译耗时估算。没有做前后对比，不给出加速百分比。",
            Modifier.padding(horizontal = 20.dp), style = C17CardTypography.supporting,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        // One contextual primary action, rather than four equally prominent operation buttons.
        val primary = when {
            !reliable -> "重新查询状态"
            module!!.remove -> "等待卸载完成"
            module.outdated -> "更新附属模块"
            !present -> "安装附属模块"
            module.disabled -> "启用附属模块"
            activeTask -> if (module.lastState == "running") "正在优化" else "自动任务等待中"
            else -> "立即优化"
        }
        C17TextButton(text = primary, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            enabled = if (!reliable) !app.performanceBusy && !app.busy else editable && module?.remove != true &&
                (!activeTask || module?.outdated == true || module?.disabled == true), onClick = {
                when {
                    !reliable -> app.refreshPerformance()
                    module!!.outdated || !present -> app.installPerformance()
                    module.disabled -> app.enablePerformance()
                    else -> app.runPerformance()
                }
            })
        if (activeTask) Text("已有任务正在等待或执行，为避免重叠，结束后可手动优化。",
            Modifier.fillMaxWidth().padding(horizontal = 20.dp), style = C17CardTypography.supporting,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary, textAlign = TextAlign.Center)
        Text("自动：息屏或 10 分钟无操作 · 电量 > 15% · 电池 < 55℃",
            Modifier.fillMaxWidth().padding(horizontal = 20.dp), style = C17CardTypography.supporting,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary, textAlign = TextAlign.Center)
        UiGroupCard {
            UiRow("运行详情", if (module == null) "管理器与执行记录" else
                "${module.manager} · ${if (module.moduleVersion.isBlank()) "版本待查询" else "v${module.moduleVersion}"}",
                if (expanded) "收起" else "展开", Icons.Outlined.Info) { expanded = !expanded }
            if (expanded) {
                if (module != null) {
                    val time = if (module.time == 0L) "尚无运行时间" else java.text.DateFormat.getDateTimeInstance().format(java.util.Date(module.time * 1000L))
                    val origin = when (module.trigger) { "boot" -> "开机自动"; "auto" -> "按需自动"; "run" -> "手动优化"; "uninstall" -> "卸载恢复"; else -> "尚未记录" }
                    UiRow("运行来源", "$origin · ${if (fresh) "本次开机" else "历史或未确认记录"}\n$time")
                    UiRow("模块版本", "${module.moduleVersion}（code ${module.moduleVersionCode}）")
                    if (module.packages > 0L) UiRow("处理记录", "共 ${module.packages} 个组件 · 编译 ${module.compiled} · 复用 ${module.reused} · 延期 ${module.deferred}")
                    UiRow("任务优先级", when (module.priority) { "background" -> "ART 系统后台优先级"; "compatibility" -> "系统兼容方式"; else -> "尚无记录" })
                }
                Text(app.performanceDetail.ifEmpty { app.performanceMessage }, Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = C17CardTypography.supporting, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text("自动任务只在需要编译时运行，有效结果直接复用，延期后有限重试；手动优化不受环境条件限制，保留操作超时、可靠取消和防重叠。",
                    Modifier.padding(horizontal = 20.dp, vertical = 10.dp), style = C17CardTypography.supporting,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text("检查 SystemUI 和已识别控制中心插件内的全部主要 DEX，覆盖通知、锁屏、状态栏等代码。本应用不在其中：LSPosed 以内存方式把模块代码交给系统界面，为本应用单独预编译对它无效。组件数量不等于功能数量，实际收益以实测为准。",
                    Modifier.padding(horizontal = 20.dp, vertical = 10.dp), style = C17CardTypography.supporting,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            UiRow("刷新状态", icon = Icons.Outlined.Refresh, enabled = !app.performanceBusy && !app.busy) { app.refreshPerformance() }
            UiRow("控制中心", "查看任务、结果和记录", icon = Icons.Outlined.Dashboard) {
                app.startActivity(android.content.Intent(app, PerformanceWebActivity::class.java))
            }
            if (present && module?.remove != true) {
                if (module?.disabled != true) UiRow("停用附属模块", "保留已有编译结果", icon = Icons.Outlined.Pause,
                    enabled = editable) { app.disablePerformance() }
                UiRow("卸载附属模块", "重启时尝试恢复策略，失败保留记录", icon = Icons.Outlined.DeleteOutline,
                    enabled = editable) { app.removePerformance() }
            }
        }
    }
}
