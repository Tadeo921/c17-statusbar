// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ApplicationChoice(val packageName: String, val label: String, val system: Boolean = false)

/** Launcher icons of the app pickers, drawn once at their shown size and kept for the session (bounded). */
private val appIcons = java.util.Collections.synchronizedMap(object : LinkedHashMap<String, androidx.compose.ui.graphics.ImageBitmap?>(64, .75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, androidx.compose.ui.graphics.ImageBitmap?>?) = size > 200
})

/** One application's icon, loaded off the main thread; a soft square stands in while it loads or when it has none. */
@Composable
internal fun AppIcon(app: ModernMainActivity, packageName: String, size: androidx.compose.ui.unit.Dp = 32.dp) {
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val key = "$packageName@$px"
    val bitmap by produceState(appIcons[key], key) {
        if (value == null && !appIcons.containsKey(key)) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val source = app.packageManager.getApplicationIcon(packageName)
                    val drawable = source.constantState?.newDrawable()?.mutate() ?: source
                    val bitmap = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
                    drawable.setBounds(0, 0, px, px); drawable.draw(android.graphics.Canvas(bitmap))
                    bitmap.asImageBitmap()
                }.getOrNull()
            }
            appIcons[key] = loaded
            value = loaded
        }
    }
    val shown = bitmap
    if (shown != null) Image(shown, null, Modifier.size(size))
    else Box(Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(MiuixTheme.colorScheme.primary.copy(alpha = .12f)))
}

@Composable
internal fun InstalledApplicationChoices(app: ModernMainActivity, allowed: Boolean,
    entries: List<ApplicationChoice> = app.applicationChoices, launchableOnly: Boolean = false,
    loading: Boolean = false, onChoose: (ApplicationChoice) -> Unit) {
    var query by remember { mutableStateOf("") }
    var systemOnly by remember { mutableStateOf(false) }
    val choices = remember(entries, query, systemOnly) {
        entries.filter { it.system == systemOnly &&
            (query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true)) }
    }
    var page by remember(entries, query, systemOnly) { mutableIntStateOf(0) }
    val lastPage = ((choices.size - 1).coerceAtLeast(0)) / 16
    C17TextField(value = query, onValueChange = { query = it }, label = "Buscar en todas las aplicaciones instaladas", singleLine = true,
        useLabelAsPlaceholder = true, modifier = Modifier.fillMaxWidth(), insideMargin = DpSize(12.dp, 10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        C17TextButton(text = if (!systemOnly) "✓ Aplicaciones de terceros" else "Aplicaciones de terceros", modifier = Modifier.weight(1f), onClick = { systemOnly = false })
        C17TextButton(text = if (systemOnly) "✓ Aplicaciones del sistema" else "Aplicaciones del sistema", modifier = Modifier.weight(1f), onClick = { systemOnly = true })
    }
    choices.drop(page * 16).take(16).forEach { choice -> key(choice.packageName) {
        C17ArrowPreference(title = choice.label, summary = choice.packageName, enabled = allowed,
            startAction = { Box(Modifier.padding(end = 12.dp)) { AppIcon(app, choice.packageName) } },
            onClick = { onChoose(choice) })
    } }
    if (choices.isEmpty()) Text(if (loading) "Leyendo aplicaciones que se pueden abrir…" else if (!launchableOnly && entries.isEmpty()) "Leyendo la lista de aplicaciones; puedes introducir primero el nombre del paquete." else "No hay aplicaciones que cumplan los criterios.",
        fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    else {
        Text("${choices.size} aplicaciones · página ${page + 1} de ${lastPage + 1}" +
            if (launchableOnly) "; solo se muestran aplicaciones que se pueden abrir." else "; incluye aplicaciones sin acceso directo en el escritorio.", fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (lastPage > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            C17TextButton(text = "Anterior", enabled = page > 0, modifier = Modifier.weight(1f), onClick = { page-- })
            C17TextButton(text = "Siguiente", enabled = page < lastPage, modifier = Modifier.weight(1f), onClick = { page++ })
        }
    }
}

@Composable
fun NotificationOverridesDialog(app: ModernMainActivity) {
    val json = app.value(NotificationIconOverrides.RULES)?.toString() ?: "[]"
    val rules = remember(json) { runCatching { NotificationIconOverrides.rules(json) }.getOrDefault(emptyList()) }
    var editingPackage by remember { mutableStateOf<String?>(null) }
    var selectedPackage by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var replacementType by remember { mutableStateOf(NotificationIconOverrides.TYPE_TEXT) }
    var sourcePackage by remember { mutableStateOf("") }
    var selectingTarget by remember { mutableStateOf(false) }
    var selectingSource by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val allowed = app.canEdit && !app.busy
    fun edit(rule: NotificationIconOverrides.Rule?) {
        editingPackage = rule?.packageName ?: ""
        selectedPackage = rule?.packageName ?: ""
        replacement = rule?.text ?: ""
        replacementType = rule?.type ?: NotificationIconOverrides.TYPE_TEXT
        sourcePackage = rule?.sourcePackage ?: ""
        selectingTarget = rule == null; selectingSource = false; error = ""
    }
    C17OverlayDialog(show = true, title = if (editingPackage == null) "Icono de notificación por aplicación" else "Configurar sustitución de icono",
        onDismissRequest = { app.notificationOverridesOpen = false }) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (editingPackage == null) {
                Text("Elige texto, emoji o el icono a color de otra aplicación. Solo sustituye el icono pequeño de la barra de estado; el contenido y los grupos de notificaciones no cambian.", fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    if (rules.isEmpty()) UiRow("Todavía no se ha añadido ninguna aplicación.", "Pulsa Añadir abajo; las demás aplicaciones conservarán sus iconos nativos.")
                    rules.forEach { rule -> key(rule.packageName) {
                        UiGroupCard {
                            UiSwitchRow(app.applicationLabel(rule.packageName), if (rule.isAppIcon()) "Icono de ${app.applicationLabel(rule.sourcePackage)}" else "Sustituir por ${rule.text}", rule.enabled, allowed) {
                                if (rule.isAppIcon()) {
                                    val message = app.saveNotificationAppIconOverride(rule.packageName, rule.sourcePackage, it, rule.packageName)
                                    if (message.isNotEmpty()) app.toast(message)
                                }
                                else app.saveNotificationOverride(rule.packageName, rule.text, it, rule.packageName)
                            }
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                C17TextButton(text = "Editar", enabled = allowed, modifier = Modifier.weight(1f), onClick = { edit(rule) })
                                C17TextButton(text = "Eliminar", enabled = allowed, modifier = Modifier.weight(1f), onClick = { app.removeNotificationOverride(rule.packageName) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    } }
                }
                C17TextButton(text = "Añadir aplicación", enabled = allowed && rules.size < 128,
                    modifier = Modifier.fillMaxWidth(), onClick = { edit(null) })
                C17TextButton(text = "Listo", modifier = Modifier.fillMaxWidth(), onClick = { app.notificationOverridesOpen = false })
            } else {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    C17TextField(value = selectedPackage, onValueChange = { selectedPackage = it; error = "" }, label = "Nombre de paquete de la aplicación",
                        singleLine = true, useLabelAsPlaceholder = true, modifier = Modifier.fillMaxWidth(), insideMargin = DpSize(12.dp, 10.dp))
                    C17TextButton(text = if (selectingTarget) "Contraer lista de aplicaciones de notificación" else "Seleccionar la aplicación propietaria de la notificación", enabled = allowed,
                        modifier = Modifier.fillMaxWidth(), onClick = { selectingTarget = !selectingTarget })
                    if (selectingTarget) {
                        InstalledApplicationChoices(app, allowed) { selectedPackage = it.packageName; selectingTarget = false; error = "" }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        C17TextButton(text = if (replacementType == NotificationIconOverrides.TYPE_TEXT) "✓ Texto y emoji" else "Texto y emoji",
                            enabled = allowed, modifier = Modifier.weight(1f), onClick = { replacementType = NotificationIconOverrides.TYPE_TEXT; error = "" })
                        C17TextButton(text = if (replacementType == NotificationIconOverrides.TYPE_APP_ICON) "✓ Icono de aplicación" else "Icono de aplicación",
                            enabled = allowed, modifier = Modifier.weight(1f), onClick = { replacementType = NotificationIconOverrides.TYPE_APP_ICON; selectingSource = sourcePackage.isBlank(); error = "" })
                    }
                    if (replacementType == NotificationIconOverrides.TYPE_APP_ICON) {
                        C17TextField(value = sourcePackage, onValueChange = { sourcePackage = it; error = "" }, label = "Nombre de paquete de la aplicación de origen del icono",
                            singleLine = true, useLabelAsPlaceholder = true, modifier = Modifier.fillMaxWidth(), insideMargin = DpSize(12.dp, 10.dp))
                        C17TextButton(text = if (selectingSource) "Contraer lista de aplicaciones de origen del icono" else "Seleccionar la aplicación que proporciona el icono", enabled = allowed,
                            modifier = Modifier.fillMaxWidth(), onClick = { selectingSource = !selectingSource })
                        if (selectingSource) InstalledApplicationChoices(app, allowed) {
                            sourcePackage = it.packageName; selectingSource = false; error = ""
                        }
                        Text("Conserva los colores del icono de origen. Si la aplicación de origen se desinstala o no se puede leer su icono, se restaurará el icono de notificación nativo.", fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    } else {
                        C17TextField(value = replacement, onValueChange = { replacement = it; error = "" }, label = "Texto o emoji de sustitución",
                            singleLine = true, useLabelAsPlaceholder = true, modifier = Modifier.fillMaxWidth(), insideMargin = DpSize(12.dp, 10.dp))
                        Text("Hasta 8 caracteres; puedes usar emojis como ❤️ o ✨. El tamaño sigue el del icono nativo o los ajustes del área de iconos de notificación.", fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    if (error.isNotEmpty()) Text(error, fontSize = 13.sp, color = MiuixTheme.colorScheme.primary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C17TextButton(text = "Volver", modifier = Modifier.weight(1f), onClick = { editingPackage = null })
                    C17TextButton(text = "Aceptar", enabled = allowed, modifier = Modifier.weight(1f), onClick = {
                        val old = rules.firstOrNull { it.packageName == editingPackage }
                        error = if (replacementType == NotificationIconOverrides.TYPE_APP_ICON)
                            app.saveNotificationAppIconOverride(selectedPackage.trim(), sourcePackage.trim(), old?.enabled ?: true, editingPackage)
                        else app.saveNotificationOverride(selectedPackage.trim(), replacement, old?.enabled ?: true, editingPackage)
                        if (error.isEmpty()) editingPackage = null
                    })
                }
            }
        }
    }
}
