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
    val cardLabel = if (result == installation) "Módulo complementario" else "Módulo complementario · $installation"
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
                        app.performanceError -> "No se pudo leer el estado. Vuelve a consultarlo. El último registro conservado no representa el estado actual."
                        module == null || !module.root -> "Concede permisos Root para instalar y administrar el módulo complementario."
                        module.remove -> "Se solicitó la desinstalación; al reiniciar, se intentará restaurar la estrategia de compilación."
                        module.disabled -> "El módulo complementario está desactivado. Al activarlo, comprobará automáticamente el estado; el sistema administra los resultados existentes."
                        module.pending -> "La nueva versión del módulo complementario se cargará tras reiniciar. Los registros actuales no confirman que la nueva versión esté activa."
                        module.outdated -> "Actualiza a 1.7: primero verifica y reutiliza los resultados disponibles, y compila solo cuando sea necesario. Esta versión corrige un fallo ocasional que impedía volver a optimizar hasta el siguiente reinicio y empieza a registrar los beneficios."
                        !present -> "Optimiza bajo demanda la interfaz del sistema y el centro de control, reutilizando los resultados de compilación válidos."
                        !fresh && hasRecord -> "A continuación se muestran los registros de la ejecución anterior; la ejecución actual aún no está confirmada."
                        module.lastState == "waiting" -> if (module.trigger == "run") "La tarea manual se está preparando." else "La tarea automática espera a que la pantalla se apague o a 10 minutos de inactividad."
                        module.lastState == "running" -> if (module.trigger == "run") "La tarea manual está comprobando o compilando; no espera a que se cumplan condiciones del entorno." else "La tarea automática está comprobando o compilando."
                        module.errorMessage().isNotEmpty() -> module.errorMessage()
                        module.lastState == "current" -> "Se reutilizan directamente los resultados válidos para reducir compilaciones repetidas."
                        module.lastState == "ok" -> module.verificationMessage()
                        else -> "Se ha confirmado el estado de instalación y activación; consulta el registro de ejecución independiente para conocer el resultado de la optimización."
                    }
                    Text(subtitle, style = C17CardTypography.supporting, color = palette.secondary)
                }
            }
        }
        if (hasRecord) {
            val verification = when {
                fresh -> module!!.verificationMessage()
                module!!.recordVersionCode >= 3L && module.packages > 0L ->
                    "Última verificación: ${module.verified}/${module.packages} componentes; el estado actual aún no está confirmado."
                module.recordVersionCode < 3L -> "El registro anterior no contiene resultados verificados; el estado actual aún no está confirmado."
                else -> "Todavía no hay resultados verificados; el estado actual aún no está confirmado."
            }
            Text(verification, Modifier.padding(horizontal = 20.dp), style = C17CardTypography.supporting,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        // Measured costs and labelled estimates only; there is no before/after run, so no speed-up percentage appears.
        val benefit = remember(module) { PerformanceBenefit.of(module) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Registro de beneficios", style = C17CardTypography.label.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = MiuixTheme.colorScheme.onSurface)
            Text(benefit.since, style = C17CardTypography.supporting, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        UiGroupCard {
            UiRow("Compilaciones acumuladas", benefit.compilesNote, benefit.compiles, Icons.Outlined.Memory)
            UiRow("Compilaciones repetidas evitadas", benefit.savedNote, benefit.saved, Icons.Outlined.Bolt)
            UiRow("Interfaz del sistema", benefit.aotNote, benefit.aot, Icons.Outlined.Smartphone)
            UiRow("Compilación en tiempo de ejecución", benefit.jitNote, benefit.jit, Icons.Outlined.Speed)
        }
        Text("Los tiempos proceden de los valores devueltos por ART en cada compilación; las compilaciones «evitadas» se estiman con la duración de la compilación anterior de cada componente. No se ha realizado una comparación antes/después, por lo que no se ofrece un porcentaje de aceleración.",
            Modifier.padding(horizontal = 20.dp), style = C17CardTypography.supporting,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        // One contextual primary action, rather than four equally prominent operation buttons.
        val primary = when {
            !reliable -> "Volver a consultar el estado"
            module!!.remove -> "Esperando a que termine la desinstalación"
            module.outdated -> "Actualizar módulo complementario"
            !present -> "Instalar módulo complementario"
            module.disabled -> "Activar módulo complementario"
            activeTask -> if (module.lastState == "running") "Optimizando…" else "Tarea automática en espera"
            else -> "Optimizar ahora"
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
        if (activeTask) Text("Ya hay una tarea en espera o en ejecución. Para evitar que se superpongan, podrás optimizar manualmente cuando termine.",
            Modifier.fillMaxWidth().padding(horizontal = 20.dp), style = C17CardTypography.supporting,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary, textAlign = TextAlign.Center)
        Text("Automático: pantalla apagada o 10 min de inactividad · batería > 15 % · temperatura < 55 °C",
            Modifier.fillMaxWidth().padding(horizontal = 20.dp), style = C17CardTypography.supporting,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary, textAlign = TextAlign.Center)
        UiGroupCard {
            UiRow("Detalles de ejecución", if (module == null) "Administrador y registros de ejecución" else
                "${module.manager} · ${if (module.moduleVersion.isBlank()) "Versión pendiente de consulta" else "v${module.moduleVersion}"}",
                if (expanded) "Contraer" else "Expandir", Icons.Outlined.Info) { expanded = !expanded }
            if (expanded) {
                if (module != null) {
                    val time = if (module.time == 0L) "Todavía no hay datos de duración." else java.text.DateFormat.getDateTimeInstance().format(java.util.Date(module.time * 1000L))
                    val origin = when (module.trigger) { "boot" -> "Automático al iniciar"; "auto" -> "Automático bajo demanda"; "run" -> "Optimización manual"; "uninstall" -> "Restauración al desinstalar"; else -> "Aún no hay registros." }
                    UiRow("Origen de ejecución", "$origin · ${if (fresh) "Este arranque" else "Registro histórico o no confirmado"}\n$time")
                    UiRow("Versión del módulo", "${module.moduleVersion}（code ${module.moduleVersionCode}）")
                    if (module.packages > 0L) UiRow("Registro de procesamiento", "${module.packages} componentes · compilados ${module.compiled} · reutilizados ${module.reused} · pospuestos ${module.deferred}")
                    UiRow("Prioridad de la tarea", when (module.priority) { "background" -> "Prioridad de ART en segundo plano del sistema"; "compatibility" -> "Método de compatibilidad del sistema"; else -> "Todavía no hay registros." })
                }
                Text(app.performanceDetail.ifEmpty { app.performanceMessage }, Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = C17CardTypography.supporting, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text("La tarea automática solo se ejecuta cuando se necesita compilar, reutiliza los resultados válidos y realiza un número limitado de reintentos tras un aplazamiento. La optimización manual no depende de condiciones del entorno y conserva el límite de tiempo, la cancelación fiable y la prevención de solapamientos.",
                    Modifier.padding(horizontal = 20.dp, vertical = 10.dp), style = C17CardTypography.supporting,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text("Comprueba los principales archivos DEX de SystemUI y de los complementos de centro de control reconocidos, incluidos los códigos de notificaciones, pantalla de bloqueo y barra de estado. Esta aplicación no se incluye: LSPosed entrega el código del módulo a la interfaz del sistema en memoria, por lo que precompilar esta aplicación por separado no ayuda. El número de componentes no equivale al número de funciones; los beneficios reales dependen de las mediciones.",
                    Modifier.padding(horizontal = 20.dp, vertical = 10.dp), style = C17CardTypography.supporting,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            UiRow("Actualizar estado", icon = Icons.Outlined.Refresh, enabled = !app.performanceBusy && !app.busy) { app.refreshPerformance() }
            UiRow("Centro de control", "Ver tareas, resultados y registros", icon = Icons.Outlined.Dashboard) {
                app.startActivity(android.content.Intent(app, PerformanceWebActivity::class.java))
            }
            if (present && module?.remove != true) {
                if (module?.disabled != true) UiRow("Desactivar módulo complementario", "Conservar los resultados de compilación existentes", icon = Icons.Outlined.Pause,
                    enabled = editable) { app.disablePerformance() }
                UiRow("Desinstalar módulo complementario", "Intentará restaurar la estrategia al reiniciar; si falla, conservará el registro.", icon = Icons.Outlined.DeleteOutline,
                    enabled = editable) { app.removePerformance() }
            }
        }
    }
}
