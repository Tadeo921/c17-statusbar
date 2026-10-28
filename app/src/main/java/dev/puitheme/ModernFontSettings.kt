// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** A font choice without any persisted effect. Scope checkboxes live only in the dialog draft. */
data class FontScopeSelection(val source: String, val title: String)

@Composable
internal fun FontSettingsPanel(app: ModernMainActivity) {
    val assignments = remember(app.values) {
        runCatching { FontScopes.parse(app.value(FontScopes.KEY)?.toString() ?: "{}") }.getOrDefault(emptyMap())
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UiGroupCard {
            UiRow("Seleccionar fuente", "Primero elige una fuente y después el ámbito de aplicación. Se aplicará al confirmar.", app.currentFontTitle(), Icons.Outlined.TextFields,
                enabled = !app.busy) { app.chooseOpenFont() }
        }
        if (assignments.isNotEmpty()) UiGroupCard {
            FontScopes.SCOPES.filter { assignments.containsKey(it) }.forEach { scope ->
                val source = assignments.getValue(scope)
                UiRow(FontScopes.title(scope), app.fontSourceTitle(source), "Cambiar", enabled = app.canEdit && !app.busy) {
                    app.chooseOpenFont()
                }
            }
        }
        Text("Se recomienda usar una sola fuente; mezclar fuentes puede causar diferencias de ancho y alineación. El ámbito solo cubre los textos compatibles con el módulo; si la función correspondiente está desactivada, no se modifica nada.",
            fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (assignments.size < FontScopes.SCOPES.size) Text("Los ámbitos no asignados seguirán usando la selección de fuente anterior. Se conservan los ajustes individuales de cada función.",
            fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        FontLicenseFooter(app)
    }
}

@Composable
internal fun FontCatalogDialog(app: ModernMainActivity) {
    val ready = app.fontLibraryEntries.associateBy { it.revision }
    val assignments = remember(app.values) {
        runCatching { FontScopes.parse(app.value(FontScopes.KEY)?.toString() ?: "{}") }.getOrDefault(emptyMap())
    }
    val allowed = app.canEdit && !app.busy
    C17OverlayDialog(show = true, title = "Seleccionar fuente",
        summary = "Después de seleccionarla, elige el ámbito de aplicación. La fuente solo cambiará al pulsar Aplicar. El tamaño y el grosor se siguen ajustando en cada función.",
        onDismissRequest = { if (!app.busy) app.fontCatalogOpen = false }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { UiGroupCard {
                    FontSourceRow(app, "Seguir al sistema", "Conservar la familia tipográfica del sistema\n${app.fontUsage("system")}",
                        assignments.containsValue("system")) { app.selectFontScopes("system", "Seguir al sistema") }
                    FontSourceRow(app, "PingFang", "Fuente variable integrada, grosor 100–900; el sistema sustituye los caracteres que falten\n${app.fontUsage("pingfang")}",
                        assignments.containsValue("pingfang")) { app.selectFontScopes("pingfang", "PingFang") }
                    UiRow("Importar fuente", "TTF / OTF / TTC; después de añadirla a la biblioteca, elige el ámbito de aplicación.", "Importar", Icons.Outlined.FileUpload,
                        enabled = allowed) { app.chooseFont() }
                } }
                items(app.fontLibraryEntries.filter { it.catalog == null }, key = { "import-${it.revision}" }) { entry ->
                    val source = FontScopes.source("custom", entry.revision)
                    UiGroupCard {
                        FontSourceRow(app, entry.name, "Importada · los grosores disponibles dependen del archivo\n${app.fontUsage(source)}",
                            assignments.containsValue(source), entry, canRename = true) { app.selectLibraryFont(entry) }
                    }
                }
                items(FontCatalog.entries(), key = { it.id }) { entry ->
                    val stored = ready[entry.sha256]
                    val source = FontScopes.source("custom", entry.sha256)
                    UiGroupCard {
                        FontSourceRow(app, stored?.name ?: entry.displayName,
                            "${entry.coverage}\nGrosor ${entry.minWeight}–${entry.maxWeight} · %.1f MB · ${if (stored == null) "Pendiente de descarga" else "Descargada"}\n${app.fontUsage(source)}"
                                .format(Locale.ROOT, entry.bytes / 1048576.0),
                            assignments.containsValue(source), stored) {
                            if (stored == null) app.downloadFont(entry) else app.selectLibraryFont(stored)
                        }
                    }
                }
                item { FontLicenseFooter(app) }
            }
            C17TextButton(text = "Cerrar", enabled = !app.busy,
                onClick = { app.fontCatalogOpen = false }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FontSourceRow(app: ModernMainActivity, title: String, summary: String, selected: Boolean,
    stored: FontLibrary.Entry? = null, canRename: Boolean = false, onClick: () -> Unit) {
    C17ArrowPreference(title = title, summary = summary, enabled = app.canEdit && !app.busy,
        onClick = onClick, endActions = {
            if (selected) Icon(Icons.Outlined.Check, "Ámbitos de aplicación asignados", tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            if (stored != null) {
                if (canRename) C17IconButton(enabled = app.canEdit && !app.busy, onClick = { app.fontCatalogOpen = false; app.fontRenameTarget = stored }) {
                    Icon(Icons.Outlined.Edit, "Cambiar nombre: $title", modifier = Modifier.size(20.dp))
                }
                C17IconButton(enabled = app.canEdit && !app.busy, onClick = { app.fontCatalogOpen = false; app.deleteLibraryFont(stored) }) {
                    Icon(Icons.Outlined.DeleteOutline, "Eliminar $title", modifier = Modifier.size(20.dp))
                }
            }
        })
}

@Composable
internal fun FontScopeDialog(app: ModernMainActivity, selection: FontScopeSelection) {
    val scopes = FontScopes.SCOPES.toList()
    var selected by remember(selection) { mutableStateOf(scopes.toSet()) }
    val allowed = app.canEdit && !app.busy
    C17OverlayDialog(show = true, title = "Seleccionar ámbito de aplicación", summary = selection.title,
        onDismissRequest = app::cancelFontScopeSelection) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { UiGroupCard {
                    UiSwitchRow("Aplicar de forma uniforme", "Por defecto, selecciona todos los ámbitos compatibles; no cambia los interruptores de cada función.", selected.size == scopes.size, allowed) {
                        selected = if (it) scopes.toSet() else emptySet()
                    }
                } }
                item { UiGroupCard {
                    scopes.forEach { scope ->
                        UiSwitchRow(FontScopes.title(scope), fontScopeDescription(scope), scope in selected, allowed) { checked ->
                            selected = if (checked) selected + scope else selected - scope
                        }
                    }
                } }
                item {
                    Text(if (selected.size < scopes.size) "Se recomienda usar una sola fuente. Mezclarlas puede causar diferencias de ancho y alineación; los demás ámbitos conservan la fuente nativa o la selección previa de cada función. Los ámbitos seleccionados se aplican cuando la función correspondiente está activada."
                        else "Una única fuente reduce las diferencias de ancho y alineación. Solo afecta a textos compatibles con el módulo y se aplica cuando la función correspondiente está activada.",
                        fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
                if (!app.bool(FeatureOptions.masterKey("font"))) item {
                    Text("Si la función de fuentes está desactivada, al aplicar solo se activarán los ámbitos seleccionados en esta ocasión.", fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
                if (selected.isEmpty()) item { Text("Selecciona al menos un ámbito de aplicación.", fontSize = 13.sp, color = MiuixTheme.colorScheme.primary) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                C17TextButton(text = "Cancelar", enabled = !app.busy, modifier = Modifier.weight(1f), onClick = app::cancelFontScopeSelection)
                C17TextButton(text = if (app.busy) "Aplicando…" else "Aplicar a ${selected.size} ámbitos", enabled = allowed && selected.isNotEmpty(),
                    modifier = Modifier.weight(1f), onClick = { app.applyFontScopes(selection, selected) })
            }
        }
    }
}

private fun fontScopeDescription(scope: String): String = when (scope) {
    "statusbar_clock" -> "Texto de hora de la barra de estado; conserva el formato, tamaño y posición existentes."
    "shade_clock" -> "Reloj desplegable del centro de notificaciones, centro de control y modo clásico."
    "network_type" -> "Texto del tipo de red del módulo e indicadores de red nativos compatibles."
    "network_speed" -> "Valor y unidad de la velocidad de red del módulo."
    "shade_text" -> "Texto del operador o texto personalizado que el módulo sustituye en el panel desplegable."
    "lockscreen_text" -> "Texto sustituido por el módulo en la pantalla de bloqueo; no incluye el reloj principal del sistema."
    "notification_clock" -> "Reloj grande vertical y horizontal de la página de notificaciones, con fecha, texto adicional y componentes laterales; no incluye el contenido normal de las notificaciones."
    else -> ""
}

@Composable
internal fun FontLicenseFooter(app: ModernMainActivity) {
    var expanded by remember { mutableStateOf(false) }
    UiGroupCard {
        UiRow("Origen y licencias de fuentes", "Origen, licencias e instrucciones de importación de fuentes integradas y de código abierto.", if (expanded) "Contraer" else "Ver", Icons.Outlined.Article) {
            expanded = !expanded
        }
        if (expanded) {
            UiRow("PingFang integrada", "Diseño original de Apple · reconstrucción variable ACT-02; la aplicación conserva la atribución de origen.", "Origen", Icons.Outlined.Code) {
                app.openUrl("https://github.com/ACT-02/PingFangUI-VF")
            }
            FontCatalog.entries().forEach { entry ->
                C17ArrowPreference(title = entry.displayName, summary = entry.licenseName,
                    onClick = { app.openUrl(entry.sourceUrl) }, endActions = {
                        C17IconButton(onClick = { app.openUrl(entry.licenseUrl) }) { Icon(Icons.Outlined.Article, "Ver licencia de ${entry.displayName}", modifier = Modifier.size(20.dp)) }
                    })
            }
            UiRow("Importar fuente", "Los archivos de fuentes se guardan localmente; asegúrate de tener permiso para utilizarlos. Los grosores variables dependen de lo que admita el archivo.")
        }
    }
}
