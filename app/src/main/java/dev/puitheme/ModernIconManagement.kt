// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class IconManagementDestination(val section: String = "home", val scope: String? = null,
    val packId: String? = null, val targetId: String? = null)

private val iconManagementPreviews = object : android.util.LruCache<String, android.graphics.Bitmap>(1024 * 1024) {
    override fun sizeOf(key: String, value: android.graphics.Bitmap) = value.width * value.height
}

private fun sourceRoleInPack(pack: IconManagementRepository.Pack, role: String): String? {
    val target = IconRoleRegistry.role(role)?.targetId
    return when {
        PuiIconAliases.sourceRole(pack.manifest, role) != null -> PuiIconAliases.sourceRole(pack.manifest, role)
        role.endsWith(".default") && pack.manifest.icons.containsKey(role.removeSuffix(".default")) -> role.removeSuffix(".default")
        role.startsWith("hint.") && target != null && pack.manifest.icons.containsKey(target) -> target
        else -> null
    }
}
private fun availablePackRoles(pack: IconManagementRepository.Pack): List<String> =
    (pack.manifest.icons.keys.filter { IconRoleRegistry.knownResourceRole(it) } +
        IconRoleRegistry.targets(null).flatMap { it.roles }.filter { sourceRoleInPack(pack, it) != null }).distinct()

private fun roleTitle(role: String): String = IconRoleRegistry.role(role)?.title ?: IconRoleRegistry.target(role)?.title ?: role
private fun iconPackName(pack: IconManagementRepository.Pack): String = if (pack.name.startsWith("C17 Rounded 27"))
    "Biblioteca de ejemplo C17" + pack.name.removePrefix("C17 Rounded 27") else pack.name
private fun packPreviewRoles(pack: IconManagementRepository.Pack, limit: Int): List<String> {
    val representative = listOf("wifi.4", "cellular.4", "battery.100", "tile.wifi.on", "tile.bluetooth.on", "tile.flashlight.on",
        "slider.brightness.default", "media.play_pause.play", "device.headset.default", "footer.settings.default")
        .mapNotNull { sourceRoleInPack(pack, it) }
    val groups = availablePackRoles(pack)
        .groupBy { IconRoleRegistry.role(it)?.targetId ?: it }.values.map { it.first() }
    return (representative + groups).distinct().take(limit)
}
private fun unsupportedArtworkCount(pack: IconManagementRepository.Pack): Int = pack.manifest.icons.keys.count {
    IconRoleRegistry.role(it)?.supported != true && IconRoleRegistry.targetForRole(it)?.supported != true
}
private fun packFormats(pack: IconManagementRepository.Pack): String =
    "v3 · PNG ${pack.pngCount} · vectoriales ${pack.vectorCount} · animados ${pack.animationCount}"

/** Thumbnail loading shares the production PNG/vector decoder; native/default artwork is never invented. */
@Composable
private fun IconArtwork(app: ModernMainActivity, pack: IconManagementRepository.Pack?, role: String?,
    label: String = "Iconos del sistema", tint: Int? = null, opacity: Float = 100f, previewDark: Boolean? = null, geometryRole: String? = null,
    sizePercent: Float = 100f, largePreview: Boolean = false) {
    val asset = role?.let { current -> pack?.let { source -> sourceRoleInPack(source, current)?.let { source.manifest.icons[it] } } }
    val prepared = app.iconPendingImport?.takeIf { it.pack.id == pack?.id }
    val previewKey = if (pack != null && role != null) "${pack.id}/$role/${geometryRole ?: role}/96" else ""
    val loaded by produceState<Pair<String, android.graphics.Bitmap?>?>(null, previewKey) {
        value = null
        if (pack != null && role != null) {
            val decoded = withContext(Dispatchers.IO) {
                iconManagementPreviews.get(previewKey) ?: runCatching {
                    (if (prepared != null) IconManagementRepository.previewPrepared(prepared, role, 96, geometryRole ?: role)
                        else IconManagementRepository.preview(app.applicationContext, pack.id, role, 96, geometryRole ?: role))
                        ?.also { iconManagementPreviews.put(previewKey, it) }
                }.getOrNull()
            }
            value = previewKey to decoded
        }
    }
    val image = loaded?.takeIf { it.first == previewKey && pack != null && role != null }?.second
    val background = previewDark?.let { if (it) Color(0xff232426) else Color(0xfff1f2f3) } ?: MiuixTheme.colorScheme.primary.copy(alpha = .06f)
    Box(Modifier.size(if (largePreview) 80.dp else 48.dp).clip(RoundedCornerShape(12.dp)).background(background),
        contentAlignment = Alignment.Center) {
        if (image != null) Image(image.asImageBitmap(), label, Modifier.size((34f * sizePercent / 100f).dp),
            alpha = (opacity / 100f).coerceIn(0f, 1f), colorFilter = when {
                tint != null -> ColorFilter.tint(Color(tint))
                asset?.render == "mask" -> ColorFilter.tint(MiuixTheme.colorScheme.onSurface)
                else -> null
            })
        else Text(if (pack == null) "Sistema" else if (loaded?.first == previewKey) "No se puede previsualizar" else "…", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

private data class IconUiCategory(val title: String, val scopes: Set<String>)
private val iconUiCategories = listOf(
    IconUiCategory("Barra de estado", setOf("hint", "wifi", "cellular", "battery")),
    IconUiCategory("Centro de control", setOf("tile", "slider", "footer")),
    IconUiCategory("Otros", setOf("media", "device"))
)
private fun isOriginalCandidate(role: String): Boolean = !IconRoleRegistry.knownResourceRole(role)
private fun iconSectionTitle(scope: String): String = when (scope) {
    "hint" -> "Indicadores del sistema"; "tile" -> "Ajustes rápidos"; "slider" -> "Brillo y volumen"
    "media" -> "Controles multimedia"; "device" -> "Dispositivos conectados"; "footer" -> "Accesos del pie de página"
    else -> IconRoleRegistry.scopeTitle(scope)
}
private fun targetGroup(target: IconRoleRegistry.Target): String = if (target.scope != "tile") iconSectionTitle(target.scope) else when (target.id.removePrefix("tile.")) {
    "wifi", "bluetooth", "mobile_data", "hotspot", "airplane", "nfc", "data_saver", "vpn", "otg", "cast" -> "Red y conexiones"
    "rotation", "dark", "auto_brightness", "auto_color_temperature", "osie", "iris", "small_screen" -> "Pantalla y visualización"
    "dolby", "dnd", "no_disturb", "vibration", "ringer_mode" -> "Sonido y No molestar"
    "camera", "microphone", "location", "phone_anti_theft", "lock_screen" -> "Privacidad y seguridad"
    "alipay", "ali_scan", "wechat_pay", "wechat_scan", "qr_scanner", "wallet" -> "Escaneo de códigos y pagos"
    else -> "Herramientas habituales"
}

@Composable
private fun IconSectionLabel(title: String) {
    Text(title, Modifier.padding(horizontal = 12.dp), fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
}

@Composable
internal fun IconManagementPage(app: ModernMainActivity) {
    val destination = app.iconManagementDestination
    val bundledKinds by produceState<Set<String>>(emptySet(), app) {
        value = withContext(Dispatchers.IO) { IconManagementRepository.bundledKinds(app.applicationContext) }
    }
    var rename by remember { mutableStateOf<IconManagementRepository.Pack?>(null) }
    val childBack = remember { arrayOfNulls<() -> Boolean>(1) }
    val targetDrafts = remember { mutableMapOf<String, IconTargetDraft>() }
    val records = app.iconManagementRecords()
    val state = remember(records) { runCatching { IconManagementSettings.parse(records) }.getOrElse { IconManagementSettings.parse(IconManagementSettings.DEFAULT) } }
    val pending = app.iconPendingImport
    val pack = pending?.pack ?: app.iconPacks.firstOrNull { it.id == destination.packId }
    val scope = destination.scope?.takeIf { IconRoleRegistry.validScope(it) }
    var switchError by remember { mutableStateOf("") }
    fun navigate(section: String, nextScope: String? = scope, packId: String? = null, target: String? = null) {
        if (section in listOf("home", "custom", "scope", "appearance")) {
            app.iconLibraryTarget = null
            targetDrafts.clear()
        }
        app.iconManagementDestination = IconManagementDestination(section, nextScope, packId, target)
    }
    fun back() {
        if (app.busy) return
        if (rename != null) { rename = null; return }
        if (childBack[0]?.invoke() == true) return
        val libraryTarget = IconRoleRegistry.target(app.iconLibraryTarget)
        if (libraryTarget != null && (pending != null || destination.section in listOf("pack", "apply"))) {
            if (pending != null) app.cancelIconImport()
            app.iconLibraryTarget = null
            navigate("target", libraryTarget.scope, target = libraryTarget.id)
            return
        }
        if (pending != null) { app.cancelIconImport(); return }
        when (destination.section) {
            "home" -> { app.iconLibraryTarget = null; targetDrafts.clear(); app.closeIconManagement() }
            "target", "target_pick", "appearance", "scope" -> navigate("custom")
            "pack", "apply", "github" -> navigate("library", null)
            "library" -> {
                val target = IconRoleRegistry.target(app.iconLibraryTarget)
                app.iconLibraryTarget = null
                if (target != null) navigate("target", target.scope, target = target.id) else navigate("home", null)
            }
            else -> navigate("home", null)
        }
    }
    fun cancelPackPreview() {
        if (app.busy) return
        val target = IconRoleRegistry.target(app.iconLibraryTarget)
        if (pending != null) app.cancelIconImport()
        if (target != null) {
            app.iconLibraryTarget = null
            navigate("target", target.scope, target = target.id)
        } else navigate("library", null)
    }
    SideEffect { app.iconManagementBackRequest = ::back }
    DisposableEffect(app) { onDispose { app.iconManagementBackRequest = null } }
    val title = when {
        pending != null || destination.section in listOf("pack", "apply") -> if (app.iconLibraryTarget != null && destination.section != "apply") "Previsualizar biblioteca" else "Previsualizar y aplicar"
        destination.section == "library" -> if (app.iconLibraryTarget != null) "Seleccionar biblioteca" else "Elegir paquete"
        destination.section == "github" -> "Importar desde GitHub"
        destination.section == "custom" || destination.section == "scope" -> "Iconos personalizados"
        destination.section == "appearance" -> "Apariencia de ${scope?.let { iconSectionTitle(it) }.orEmpty()}"
        destination.section in listOf("target", "target_pick") -> IconRoleRegistry.target(destination.targetId)?.title ?: "Seleccionar icono"
        else -> "Gestión de iconos"
    }
    val scroll = rememberC17ScrollBehavior(rememberC17TopAppBarState(initialHeightOffsetLimit = 0f))
    C17Scaffold(containerColor = MiuixTheme.colorScheme.surface, topBar = {
        C17SmallTopAppBar(title = title, scrollBehavior = scroll, navigationIcon = {
            C17IconButton(onClick = ::back, enabled = !app.busy) { Icon(Icons.Outlined.ArrowBack, "Volver") }
        })
    }) { padding ->
        key(destination, pending?.pack?.id) {
            LazyColumn(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).nestedScroll(scroll.nestedScrollConnection),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = padding.calculateBottomPadding() + 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                app.iconMigrationNotice?.let { notice -> item { UiGroupCard {
                    UiRow(if (app.iconMigrationCompleted) "Administración de iconos actualizada" else "Hay que volver a intentar la actualización de iconos.", notice)
                    UiRow(if (app.iconMigrationCompleted) "Entendido" else "Reintentar actualización", enabled = !app.busy) {
                        if (app.iconMigrationCompleted) app.acknowledgeIconMigration() else app.refreshIconLibraries()
                    }
                } } }
                if (app.busy || app.iconLibrariesLoading) item { UiGroupCard {
                    UiRow(if (app.iconLibrariesLoading) "Leyendo la biblioteca de iconos…" else "Verificando los recursos de iconos…")
                    if (app.iconPackDownloading) UiRow("Cancelar descarga", enabled = true, onClick = app::cancelIconPackDownload)
                } }
                if (!app.iconManagementEnabled() && destination.section != "home") item { UiGroupCard {
                    UiRow("Administración de iconos desactivada", "Puedes seguir seleccionando y guardando; los cambios se aplicarán al activar el interruptor principal.")
                } }
                when {
                    pending != null -> item {
                        IconPackApplication(app, pending.pack, records, pending = true, onCancel = ::cancelPackPreview, onApplied = app::finishIconPackApplication)
                    }
                    destination.section == "library" -> {
                        app.iconLibraryTarget?.let { targetId -> item { UiGroupCard {
                            UiRow("Seleccionar recursos para ${IconRoleRegistry.target(targetId)?.title ?: "el icono actual"} ", "Selecciona una biblioteca y elige directamente los iconos; guarda todos los cambios en la página de destino.")
                        } } }
                        if (bundledKinds.isNotEmpty()) {
                            item { IconSectionLabel("Paquetes integrados") }
                            item { UiGroupCard {
                                if ("pui" in bundledKinds) UiRow("PUI ColorOS 17", "Tensankura & PanL · iconos del tema original", "Vista previa", Icons.Outlined.Collections,
                                    enabled = app.canEdit && !app.busy) { app.prepareBundledIcons("pui") }
                                if ("original" in bundledKinds) UiRow("Biblioteca de ejemplo C17", "Recursos de ejemplo de iconos de estado y animaciones", "Vista previa", Icons.Outlined.AutoAwesome,
                                    enabled = app.canEdit && !app.busy) { app.prepareBundledIcons("original") }
                            } }
                        }
                        if (app.iconPacks.isNotEmpty()) item { IconSectionLabel("Mis paquetes") }
                        items(app.iconPacks, key = { it.id }) { listed -> UiGroupCard {
                            UiRow(iconPackName(listed), "${listed.author.ifBlank { "Sin autor" }} · ${listed.iconCount} iconos", "Vista previa", Icons.Outlined.Collections,
                                enabled = !app.busy) { navigate("pack", null, listed.id) }
                            Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                packPreviewRoles(listed, 5).forEach { role -> IconArtwork(app, listed, role, roleTitle(role)) }
                            }
                            Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                C17TextButton(text = "Cambiar nombre", modifier = Modifier.weight(1f), enabled = app.canEdit && !app.busy, onClick = { rename = listed })
                                C17TextButton(text = "Eliminar", modifier = Modifier.weight(1f), enabled = app.canEdit && !app.busy, onClick = { app.deleteIconPack(listed.id, iconPackName(listed)) })
                            }
                        } }
                        item { UiGroupCard {
                            UiRow("Importar paquete de iconos", if (app.iconLibraryTarget != null) "Selecciona tu propio ZIP/RAR v3 (incluida una biblioteca PUI), previsualiza y elige los iconos." else "Selecciona tu propio ZIP/RAR v3 (incluida una biblioteca PUI), previsualiza y aplica los iconos.", "Archivo", Icons.Outlined.FileUpload,
                                enabled = app.canEdit && !app.busy, onClick = app::chooseIconPack)
                            UiRow("Obtener desde GitHub", "Repositorio o enlace de descarga del paquete de iconos", "Abrir", Icons.Outlined.CloudDownload, enabled = !app.busy) { navigate("github", null) }
                        } }
                    }
                    destination.section == "github" -> item { IconGithubImport(app) }
                    destination.section in listOf("pack", "apply") && pack != null -> item {
                        IconPackApplication(app, pack, records, pending = false, onCancel = ::cancelPackPreview, onRename = { rename = pack }, onApplied = app::finishIconPackApplication)
                    }
                    destination.section in listOf("custom", "scope") -> item {
                        IconTargetBrowser(app, state, scope, onTarget = { target -> navigate("target", target.scope, target = target.id) },
                            onAppearance = { next -> navigate("appearance", next) })
                    }
                    destination.section in listOf("target", "target_pick") -> item {
                        IconRoleRegistry.target(destination.targetId)?.let { target ->
                            IconTargetEditor(app, target, state, childBack,
                                initialDraft = targetDrafts[target.id], initiallyPick = destination.section == "target_pick", preferredPackId = destination.packId,
                                onDraft = { targetDrafts[target.id] = it }) { navigate("custom", target.scope) }
                        } ?: UiGroupCard { UiRow("El destino ha cambiado.", "Vuelve atrás y selecciona el icono otra vez.") }
                    }
                    destination.section == "appearance" && scope != null -> item {
                        IconScopeAppearance(app, scope, state, childBack) { navigate("custom") }
                    }
                    destination.section in listOf("pack", "apply") -> item { UiGroupCard { UiRow("El paquete ya no existe.", "Vuelve atrás y selecciónalo otra vez.") } }
                    else -> {
                        item { UiGroupCard {
                            UiSwitchRow("Activar Administración de iconos", "Al desactivarla, se restaurarán los iconos predeterminados del sistema o de cada función, pero se conservarán las selecciones guardadas.", app.iconManagementEnabled(), app.canEdit && !app.busy) {
                                switchError = app.setIconManagementEnabled(it)
                            }
                        } }
                        if (switchError.isNotEmpty()) item { Text(switchError, color = MiuixTheme.colorScheme.error, fontSize = 13.sp) }
                        item { UiGroupCard {
                            UiRow("Elegir paquete", "Previsualiza todos los iconos del paquete y aplícalos de una vez.", "Abrir", Icons.Outlined.Collections, enabled = !app.busy) {
                                app.iconLibraryTarget = null; targetDrafts.clear(); navigate("library", null)
                            }
                            UiRow("Personalizado", "Primero selecciona el icono que quieres cambiar y después el diseño.", "Abrir", Icons.Outlined.Tune, enabled = !app.busy) { navigate("custom", null) }
                        } }
                        item { UiGroupCard {
                            UiRow("Selección actual", "${state.scopePacks.size} ámbitos con paquetes seleccionados · ${state.overrides.keys.mapNotNull { IconRoleRegistry.role(it)?.targetId }.distinct().size} destinos personalizados")
                            UiRow("Restaurar los ajustes de iconos predeterminados", "Eliminar las selecciones de paquetes y las personalizadas; conservar las bibliotecas de iconos.", "Restaurar", enabled = app.canEdit && !app.busy, onClick = app::resetIconManagement)
                            UiRow("Guía para crear paquetes de iconos", "Formato v3 y ámbitos compatibles", "Ver") { app.openUrl("https://github.com/SANWU5/c17-icon-packs") }
                        } }
                    }
                }
            }
        }
    }
    rename?.let { selected -> UiInputDialog("Cambiar el nombre de la biblioteca de iconos", iconPackName(selected), "Usa un nombre que no esté ocupado por otra biblioteca de iconos.", onDismiss = { rename = null }) { value ->
        app.iconLibraryNameError(value, selected.id).takeIf { it.isNotEmpty() }
            ?: run { app.renameIconPack(selected.id, value); rename = null; null }
    } }
}

@Composable
private fun IconTargetBrowser(app: ModernMainActivity, state: IconManagementSettings.State, initialScope: String?,
    onTarget: (IconRoleRegistry.Target) -> Unit, onAppearance: (String) -> Unit) {
    var category by remember { mutableIntStateOf(iconUiCategories.indexOfFirst { initialScope in it.scopes }.coerceAtLeast(0)) }
    var query by remember { mutableStateOf("") }
    var showUnsupported by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }
    val allTargets = IconRoleRegistry.targets(null)
    val visible = allTargets.filter { target ->
        (if (query.isBlank()) target.scope in iconUiCategories[category].scopes else true) &&
            (query.isBlank() || target.title.contains(query.trim(), true) || target.id.contains(query.trim(), true))
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UiContentTabs(tabs = iconUiCategories.map { it.title }, selectedTabIndex = category, onTabSelected = { category = it; showUnsupported = false }, modifier = Modifier.fillMaxWidth())
        C17TextField(value = query, onValueChange = { query = it }, label = "Buscar por nombre de icono", modifier = Modifier.fillMaxWidth(), singleLine = true,
            useLabelAsPlaceholder = true, insideMargin = DpSize(12.dp, 10.dp))
        visible.filter { it.supported }.groupBy(::targetGroup).forEach { (title, targets) ->
            IconSectionLabel(title)
            UiGroupCard { targets.forEach { target ->
                val count = target.roles.count { state.overrides.containsKey(it) }
                val packName = state.scopePacks[target.scope]?.let { id -> app.iconPacks.firstOrNull { it.id == id }?.let(::iconPackName) }
                UiRow(target.title, if (count > 0) "Personalizados · $count estados" else packName ?: "Icono predeterminado del sistema o de la función", "Seleccionar", enabled = !app.busy) { onTarget(target) }
            } }
        }
        if (visible.none { it.supported }) UiGroupCard { UiRow("No se encontraron iconos que se puedan configurar.", "Prueba otro término de búsqueda o selecciona otra categoría.") }
        val unsupported = visible.filterNot { it.supported }
        if (unsupported.isNotEmpty()) UiGroupCard {
            UiRow("Aún no compatible", "${unsupported.size} destinos conservan los iconos del sistema.", if (showUnsupported) "Contraer" else "Expandir") { showUnsupported = !showUnsupported }
            if (showUnsupported) unsupported.forEach { UiRow(it.title, it.reason, "Conservar el aspecto nativo", enabled = false) }
        }
        UiGroupCard {
            UiRow("Tamaño y color por categoría", "Configura todos los iconos de una categoría; cada destino puede seguir ajustándose por separado.", if (advanced) "Contraer" else "Expandir") { advanced = !advanced }
            if (advanced) iconUiCategories[category].scopes.forEach { scope -> UiRow(iconSectionTitle(scope), "Los ajustes de cada icono individual siguen teniendo prioridad.", "Apariencia", enabled = !app.busy) { onAppearance(scope) } }
        }
    }
}

@Composable
private fun IconGithubImport(app: ModernMainActivity) {
    var source by remember { mutableStateOf("SANWU5/c17-icon-packs") }
    var name by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UiGroupCard { UiRow("Paquetes públicos de iconos v3", "Introduce owner/repo o un enlace oficial de descarga de GitHub. El repositorio debe publicar icon-pack-v3.zip; si no está publicado, no se importará todo el código fuente del repositorio.") }
        UiGroupCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            C17TextField(value = source, onValueChange = { source = it }, label = "Repositorio de GitHub o enlace de descarga", modifier = Modifier.fillMaxWidth(), singleLine = true, useLabelAsPlaceholder = true, insideMargin = DpSize(12.dp, 10.dp))
            C17TextField(value = name, onValueChange = { name = it }, label = "Nombre para mostrar (opcional)", modifier = Modifier.fillMaxWidth(), singleLine = true, useLabelAsPlaceholder = true, insideMargin = DpSize(12.dp, 10.dp))
        } }
        C17TextButton(text = "Descargar y verificar", modifier = Modifier.fillMaxWidth(), enabled = app.canEdit && !app.busy && source.isNotBlank(), onClick = { app.downloadIconPack(source.trim(), name.trim()) })
    }
}

@Composable
private fun IconPackApplication(app: ModernMainActivity, pack: IconManagementRepository.Pack, records: String,
    pending: Boolean, onCancel: () -> Unit, onRename: (() -> Unit)? = null, onApplied: () -> Unit) {
    val targetSelection = app.iconLibraryTarget != null && app.iconManagementDestination.section != "apply"
    val target = IconRoleRegistry.target(app.iconLibraryTarget)
    val availableScopes = remember(pack.id) { IconRoleRegistry.SCOPES.filter { scope ->
        IconRoleRegistry.targets(scope).any { target -> target.supported && target.roles.any { sourceRoleInPack(pack, it) != null } }
    }.toSet() }
    var selected by remember(pack.id) { mutableStateOf(runCatching {
        IconManagementSettings.parse(records).scopePacks.filterValues { it == pack.id }.keys.intersect(availableScopes)
            .ifEmpty { availableScopes }
    }.getOrDefault(availableScopes)) }
    var clearOverrides by remember(pack.id) { mutableStateOf(false) }
    var presetId by remember(pack.id) { mutableStateOf<String?>(null) }
    var presetDark by remember(pack.id) { mutableStateOf(false) }
    var editScopes by remember(pack.id) { mutableStateOf(false) }
    var advanced by remember(pack.id) { mutableStateOf(false) }
    var browse by remember(pack.id) { mutableStateOf(false) }
    var error by remember(pack.id) { mutableStateOf("") }
    val existing = if (pending) app.iconImportExisting else null
    val nameError = if (pending && existing == null) app.iconLibraryNameError(app.iconImportName, pack.id) else ""
    val palettes = remember(pack.id) { IconColorPresets.options(pack.manifest) }
    val preset = presetId?.let { palettes[it] }
    val previewRecords = remember(records, presetId, selected) {
        runCatching { if (preset == null || selected.isEmpty()) records else IconManagementSettings.applyColorPreset(records, preset, selected) }.getOrDefault(records)
    }
    val presetState = remember(previewRecords) { IconManagementSettings.parse(previewRecords) }
    val preview = remember(previewRecords, pack.id, selected, clearOverrides, targetSelection) {
        if (targetSelection || selected.isEmpty()) null else runCatching { IconManagementSelection.previewApplication(previewRecords, pack, selected, clearOverrides) }.getOrNull()
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (target != null) UiGroupCard {
            if (targetSelection) {
                UiRow("Seleccionar iconos solo para ${target.title}", "Después de seleccionar, volverás a la vista previa del destino. Los iconos y los ajustes avanzados deben guardarse en la página de destino.")
                UiRow("Cambiar a aplicar el paquete completo", "Selecciona ámbitos como la barra de estado o el centro de control; se conservará el borrador del destino actual.", "Seleccionar ámbitos", enabled = !app.busy) {
                    app.iconManagementDestination = IconManagementDestination("apply", packId = pack.id)
                }
            } else {
                UiRow("Aplicar paquete completo", "Solo se guardarán los ámbitos seleccionados abajo; se conservará el borrador sin guardar de ${target.title}.")
                UiRow("Volver a elegir el icono de un destino individual", "Seguir seleccionando iconos para ${target.title}", "Volver", enabled = !app.busy) {
                    app.iconManagementDestination = IconManagementDestination("pack", packId = pack.id)
                }
            }
        }
        if (pending) UiGroupCard {
            if (existing != null) {
                UiRow("El mismo contenido ya está en la biblioteca.", "Se usará «${iconPackName(existing)}». No se volverá a importar ni se cambiará el nombre existente. Para cambiarlo, usa la lista de bibliotecas.")
            } else {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    C17TextField(value = app.iconImportName, onValueChange = { app.iconImportName = it; app.iconImportError = "" },
                        label = "Nombre de la biblioteca de iconos", modifier = Modifier.fillMaxWidth(), singleLine = true,
                        enabled = app.canEdit && !app.busy, isError = nameError.isNotEmpty(), errorMessage = nameError.takeIf { it.isNotEmpty() },
                        useLabelAsPlaceholder = true, insideMargin = DpSize(12.dp, 10.dp))
                    Text("El contenido diferente no puede usar un nombre existente. Cambia el nombre y vuelve a guardar; la biblioteca original no se modificará.", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
        }
        UiGroupCard {
            UiRow(existing?.let(::iconPackName) ?: iconPackName(pack), "${pack.author.ifBlank { "Sin autor" }} · ${pack.iconCount} iconos" + if (pack.animationCount > 0) " · ${pack.animationCount} animaciones" else "")
            val samples = packPreviewRoles(pack, 12)
            samples.chunked(4).forEach { row -> Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { role -> Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val targetRole = IconRoleRegistry.role(role)?.id ?: IconRoleRegistry.roles(null).firstOrNull { sourceRoleInPack(pack, it) == role } ?: role
                    val color = IconManagementSelection.appearance(presetState, targetRole, presetDark, pack.manifest.icons[role]?.render ?: "mask")
                    IconArtwork(app, pack, role, roleTitle(role), tint = when {
                        color.mode == "custom" -> color.color
                        color.artwork -> null
                        presetDark -> 0xffffffff.toInt()
                        else -> 0xff000000.toInt()
                    }, opacity = color.opacity, previewDark = presetDark)
                    Text(roleTitle(role), fontSize = 11.sp, maxLines = 2, modifier = Modifier.padding(top = 4.dp), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                } }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            } }
            UiRow("Explorar todos los iconos", "Ver recursos y opciones originales por categoría", if (browse) "Contraer" else "Expandir") { browse = !browse }
        }
        if (browse) IconArtworkBrowser(app, pack)
        if (!targetSelection) UiGroupCard {
            UiRow("Ámbitos de aplicación", "Selecciona las áreas que quieres sustituir; los estados que falten seguirán usando el icono predeterminado.")
            iconUiCategories.forEach { category ->
                val scopes = category.scopes.intersect(availableScopes)
                val chosen = scopes.intersect(selected)
                val summary = if (scopes.isEmpty()) "Esta biblioteca no tiene iconos que se puedan aplicar." else
                    (if (chosen.isEmpty()) "Sin seleccionar · " else if (chosen.size < scopes.size) "Selección parcial · " else "Seleccionado · ") +
                        scopes.joinToString("、") { iconSectionTitle(it) }
                UiSwitchRow(category.title, summary, chosen.isNotEmpty(), app.canEdit && !app.busy && scopes.isNotEmpty()) { checked ->
                    selected = if (checked) selected + scopes else selected - scopes
                }
            }
            UiRow("Ámbitos específicos", "Selecciona por separado los ajustes rápidos, el brillo, el pie de página y otros elementos.", if (editScopes) "Contraer" else "Expandir") { editScopes = !editScopes }
            if (editScopes) {
                iconUiCategories.forEach { category ->
                    val scopes = category.scopes.filter { it in availableScopes }
                    if (scopes.isNotEmpty()) {
                        UiRow(category.title)
                        scopes.forEach { scope -> UiSwitchRow(iconSectionTitle(scope), "Los estados que falten seguirán usando el icono predeterminado.", scope in selected, app.canEdit && !app.busy) { checked ->
                            selected = if (checked) selected + scope else selected - scope
                        } }
                    }
                }
            }
            preview?.let { result ->
                UiRow("Después de aplicar", "Se sustituirán ${result.replaceCount} estados · ${result.missingCount} estados sin icono usarán el predeterminado" +
                    if (result.preserveCount > 0) "\nSe conservarán ${result.preserveCount} selecciones individuales existentes" else "")
            }
            if (availableScopes.isEmpty()) UiRow("No hay destinos que se puedan aplicar directamente.", "Este paquete no tiene estados compatibles ya vinculados. Puedes añadirlo a la biblioteca y elegir manualmente los iconos.")
        }
        if (!targetSelection) UiGroupCard {
            UiRow("Preajustes de color de la biblioteca", "Previsualiza antes de aplicar. Solo afecta a los ámbitos seleccionados arriba; el tamaño y el origen de los iconos se conservan por separado.")
            UiRow("Conservar los colores actuales", "Al aplicar el paquete, conservar los colores y la transparencia existentes.", if (presetId == null) "Seleccionado" else "", enabled = !app.busy) { presetId = null }
            palettes.forEach { (id, palette) ->
                UiRow(palette.name, "${palette.appearances.size} esquemas de color", if (presetId == id) "Seleccionado" else "", enabled = !app.busy) { presetId = id }
            }
            UiContentTabs(tabs = listOf("Vista previa con fondo claro", "Vista previa con fondo oscuro"), selectedTabIndex = if (presetDark) 1 else 0,
                onTabSelected = { presetDark = it == 1 }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp))
            C17TextButton("Aplicar solo el preajuste de color", enabled = app.canEdit && !app.busy && preset != null && selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), onClick = {
                    preset?.let { palette ->
                        error = app.applyIconColorPreset(palette, selected)
                        if (error.isEmpty()) app.toast("Preajuste de color guardado; se mantienen los iconos y tamaños originales.")
                    }
                })
        }
        if (pending && (nameError.isNotEmpty() || app.iconImportError.isNotEmpty()))
            Text(nameError.ifEmpty { app.iconImportError }, color = MiuixTheme.colorScheme.error, fontSize = 13.sp)
        if (error.isNotEmpty()) Text(error, color = MiuixTheme.colorScheme.error, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            C17TextButton(text = "Cancelar", modifier = Modifier.weight(1f), enabled = !app.busy, onClick = onCancel)
            C17TextButton(text = if (targetSelection) { if (existing != null) "Elegir iconos de una biblioteca existente" else "Seleccionar icono" }
                else if (app.iconManagementEnabled()) { if (existing != null) "Usar una biblioteca existente y aplicar" else "Aplicar paquete" } else "Guardar selección",
                modifier = Modifier.weight(1f), enabled = app.canEdit && !app.busy && nameError.isEmpty() && (targetSelection || selected.isNotEmpty() && preview != null), onClick = {
                if (targetSelection) {
                    if (pending) app.confirmIconImport() else app.finishIconLibraryForTarget(pack.id)
                } else if (pending) app.commitIconImportAndApply(selected, clearOverrides, presetId)
                else {
                    error = app.applyIconPack(pack.id, selected, clearOverrides, presetId)
                    if (error.isEmpty()) onApplied()
                }
            })
        }
        UiGroupCard {
            UiRow(if (targetSelection) "Información del recurso" else "Más opciones", if (targetSelection) "Origen, formato y ámbitos compatibles" else "Selecciones individuales e información de origen", if (advanced) "Contraer" else "Expandir") { advanced = !advanced }
            if (advanced) {
                if (!targetSelection) UiSwitchRow("Sustituir los iconos individuales existentes", "Solo elimina las sustituciones de iconos de los ámbitos seleccionados; conserva los ajustes de color, transparencia y animación.", clearOverrides, app.canEdit && !app.busy) { clearOverrides = it }
                UiRow("Información del recurso", "${packFormats(pack)}\n${pack.license.ifBlank { "No se proporcionó información de licencia" }}")
                val unsupported = unsupportedArtworkCount(pack)
                if (unsupported > 0) UiRow("Opciones originales y recursos aún no vinculados", "$unsupported recursos no se asignan automáticamente al componente anfitrión nativo.")
                preview?.let { result ->
                    if (result.missingCount > 0) UiRow("Estados sin icono", result.missing.take(12).joinToString("、") { roleTitle(it) } + if (result.missingCount > 12) "…" else "")
                    if (result.unsupportedCount > 0) UiRow("Destino no vinculado", "${result.unsupportedCount} estados seguirán usando los iconos nativos.")
                }
                if (!targetSelection && pending) UiRow(if (existing != null) "Usar una biblioteca de iconos existente" else "Añadir solo a la biblioteca",
                    if (existing != null) "Abrir el contenido idéntico guardado sin cambiar los ámbitos donde se aplica." else "Guardar los recursos para personalizarlos más adelante.", if (existing != null) "Abrir" else "Guardar",
                    enabled = app.canEdit && !app.busy && nameError.isEmpty(), onClick = app::confirmIconImport)
            }
        }
        if (!pending) UiGroupCard {
            onRename?.let { UiRow("Cambiar el nombre de la biblioteca de iconos", enabled = app.canEdit && !app.busy, onClick = it) }
            UiRow("Eliminar biblioteca de iconos", "Antes de eliminarla, se mostrarán las selecciones afectadas.", enabled = app.canEdit && !app.busy) { app.deleteIconPack(pack.id, iconPackName(pack)) }
        }
        Text("La vista previa muestra los iconos del recurso; la posición, los estados y las animaciones dependen de la compatibilidad real. «Aplicar solo el preajuste de color» guarda los colores inmediatamente; el resto de las opciones se guarda al aplicar el paquete.", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun IconArtworkBrowser(app: ModernMainActivity, pack: IconManagementRepository.Pack,
    target: IconRoleRegistry.Target? = null, selected: IconManagementSettings.Override? = null, onChoose: ((String) -> Unit)? = null) {
    var query by remember(pack.id) { mutableStateOf("") }
    var category by remember(pack.id) { mutableStateOf(if (target == null) "all" else "matching") }
    var showRaw by remember(pack.id) { mutableStateOf(false) }
    var page by remember(pack.id, query, category, showRaw) { mutableIntStateOf(0) }
    val matching = target?.roles?.filter { sourceRoleInPack(pack, it) != null }?.toSet().orEmpty()
    val categories = buildList {
        if (target != null) add("matching" to "Destino actual")
        add("all" to "Todas las categorías")
        IconRoleRegistry.SCOPES.filter { scope -> availablePackRoles(pack).any { IconRoleRegistry.scopeOfRole(it) == scope } }
            .forEach { add(it to iconSectionTitle(it)) }
    }
    val roles = availablePackRoles(pack).filter { role ->
        !isOriginalCandidate(role) && (category == "all" || category == "matching" && role in matching || IconRoleRegistry.scopeOfRole(role) == category) &&
            (query.isBlank() || roleTitle(role).contains(query.trim(), true) || role.contains(query.trim(), true))
    }
    val raw = pack.manifest.icons.keys.filter { role ->
        isOriginalCandidate(role) && (category in listOf("all", "matching") || IconRoleRegistry.scopeOfRole(role) == category) &&
            (query.isBlank() || role.contains(query.trim(), true) || IconRoleRegistry.scopeOfRole(role)?.let { iconSectionTitle(it).contains(query.trim(), true) } == true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        C17TextField(value = query, onValueChange = { query = it }, label = "Buscar iconos o estados", modifier = Modifier.fillMaxWidth(), singleLine = true,
            useLabelAsPlaceholder = true, insideMargin = DpSize(12.dp, 10.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { (id, label) -> C17TextButton(text = if (id == category) "✓ $label" else label, onClick = { category = id }) }
        }
        UiGroupCard {
            if (roles.isEmpty()) UiRow("No se encontraron iconos coincidentes.", "Prueba otra categoría o busca otro nombre.")
            else IconArtworkGrid(app, pack, roles, page, selected, onChoose)
            val lastPage = (roles.size - 1).coerceAtLeast(0) / 24
            if (lastPage > 0) Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                C17TextButton(text = "Anterior", modifier = Modifier.weight(1f), enabled = page > 0, onClick = { page-- })
                Text("${page + 1}/${lastPage + 1}", Modifier.align(Alignment.CenterVertically), fontSize = 12.sp)
                C17TextButton(text = "Siguiente", modifier = Modifier.weight(1f), enabled = page < lastPage, onClick = { page++ })
            }
        }
        if (raw.isNotEmpty()) UiGroupCard {
            UiRow("Recursos originales", "${raw.size} imágenes originales de origen que puedes seleccionar manualmente; los estados verificados también aparecen en las categorías superiores.", if (showRaw) "Contraer" else "Expandir") { showRaw = !showRaw }
            if (showRaw) {
                Text("La selección solo usa el gráfico estático; no añade compatibilidad con componentes anfitriones ni animaciones.", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                IconRawArtwork(app, pack, raw, selected, onChoose)
            }
        }
    }
}

@Composable
private fun IconRawArtwork(app: ModernMainActivity, pack: IconManagementRepository.Pack, roles: List<String>,
    selected: IconManagementSettings.Override?, onChoose: ((String) -> Unit)?) {
    var page by remember(pack.id, roles) { mutableIntStateOf(0) }
    IconArtworkGrid(app, pack, roles, page, selected, onChoose)
    val lastPage = (roles.size - 1).coerceAtLeast(0) / 24
    if (lastPage > 0) Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        C17TextButton(text = "Anterior", modifier = Modifier.weight(1f), enabled = page > 0, onClick = { page-- })
        Text("${page + 1}/${lastPage + 1}", Modifier.align(Alignment.CenterVertically), fontSize = 12.sp)
        C17TextButton(text = "Siguiente", modifier = Modifier.weight(1f), enabled = page < lastPage, onClick = { page++ })
    }
}

@Composable
private fun IconArtworkGrid(app: ModernMainActivity, pack: IconManagementRepository.Pack, roles: List<String>, page: Int,
    selected: IconManagementSettings.Override?, onChoose: ((String) -> Unit)?) {
    roles.drop(page * 24).take(24).chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { role ->
                val source = sourceRoleInPack(pack, role) ?: role
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(enabled = onChoose != null && app.canEdit && !app.busy) { onChoose?.invoke(source) }
                    .padding(horizontal = 4.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconArtwork(app, pack, role, roleTitle(role))
                    val label = IconRoleRegistry.role(role)?.title ?: IconRoleRegistry.target(role)?.title ?: "Original · ${role.substringAfterLast('.')}"
                    Text((if (selected?.packId == pack.id && selected.sourceRole == source) "✓ " else "") + label,
                        fontSize = 12.sp, maxLines = 3, modifier = Modifier.padding(top = 6.dp))
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

private data class IconLookDraft(val mode: String = "inherit", val light: Int = 0xff000000.toInt(),
    val dark: Int = 0xffffffff.toInt(), val opacity: Float? = null, val sizePercent: Float? = null) {
    fun value(): IconManagementSettings.Appearance? = if (mode == "inherit" && opacity == null && sizePercent == null) null
        else IconManagementSettings.Appearance(mode, light or 0xff000000.toInt(), dark or 0xff000000.toInt(), opacity, sizePercent)
}
private fun iconLook(value: IconManagementSettings.Appearance?): IconLookDraft = if (value == null) IconLookDraft()
    else IconLookDraft(value.mode, value.lightColor, value.darkColor, value.opacity, value.sizePercent)

private data class IconTargetDraft(
    val sources: Map<String, IconManagementSettings.Override>, val look: IconLookDraft,
    val stateLooks: Map<String, IconLookDraft>, val animation: Boolean, val activeState: String,
    val pickingRole: String?, val previewDark: Boolean, val advanced: Boolean, val individual: Boolean
)

@Composable
private fun IconTargetEditor(app: ModernMainActivity, target: IconRoleRegistry.Target, state: IconManagementSettings.State,
    childBack: Array<(() -> Boolean)?>, initialDraft: IconTargetDraft?, initiallyPick: Boolean, preferredPackId: String?,
    onDraft: (IconTargetDraft) -> Unit, onApplied: () -> Unit) {
    var sources by remember(target.id) { mutableStateOf(initialDraft?.sources ?: target.roles.mapNotNull { role -> state.overrides[role]?.let { role to it } }.toMap()) }
    var look by remember(target.id) { mutableStateOf(initialDraft?.look ?: iconLook(state.appearances["target:${target.id}"])) }
    var stateLooks by remember(target.id) { mutableStateOf(initialDraft?.stateLooks ?: target.roles.mapNotNull { role -> state.appearances["role:$role"]?.let { role to iconLook(it) } }.toMap()) }
    var animation by remember(target.id) { mutableStateOf(initialDraft?.animation ?: (state.animations[target.id] != false)) }
    var activeState by remember(target.id) { mutableStateOf(initialDraft?.activeState ?: target.roles.first()) }
    var pickingSource by remember(target.id) { mutableStateOf(initiallyPick) }
    var pickingRole by remember(target.id) { mutableStateOf(initialDraft?.pickingRole) }
    var previewDark by remember(target.id) { mutableStateOf(initialDraft?.previewDark ?: false) }
    var advanced by remember(target.id) { mutableStateOf(initialDraft?.advanced ?: false) }
    var individual by remember(target.id) { mutableStateOf(initialDraft?.individual ?: false) }
    var error by remember(target.id) { mutableStateOf("") }
    fun snapshot() = IconTargetDraft(sources, look, stateLooks, animation, activeState, pickingRole, previewDark, advanced, individual)
    SideEffect { onDraft(snapshot()) }
    val back by rememberUpdatedState<() -> Boolean> { if (pickingSource) { pickingSource = false; true } else false }
    DisposableEffect(target.id) { childBack[0] = { back() }; onDispose { childBack[0] = null } }
    if (pickingSource) {
        IconSourcePicker(app, target, pickingRole, sources, preferredPackId, onBack = { pickingSource = false }, onOpenLibrary = {
            onDraft(snapshot())
            app.openIconLibraryForTarget(target.id)
        }) { selected ->
            sources = selected
            pickingSource = false
        }
        return
    }
    val draftState = remember(app.iconManagementRecords(), target.id, sources, look, stateLooks, animation) {
        runCatching { IconManagementSettings.parse(IconManagementSettings.applyTarget(app.iconManagementRecords(), target.id, sources, look.value(),
            stateLooks.mapNotNull { (role, value) -> value.value()?.let { role to it } }.toMap(), animation)) }.getOrDefault(state)
    }
    val manifests = remember(app.iconPacks) { app.iconPacks.associate { it.id to it.manifest } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UiGroupCard {
            UiRow("Vista previa del icono", if (sources.isEmpty()) "Seguir el paquete o el valor predeterminado del sistema" else "Se seleccionaron iconos para ${sources.size} estados.")
            UiContentTabs(tabs = listOf("Fondo claro", "Fondo oscuro"), selectedTabIndex = if (previewDark) 1 else 0,
                onTabSelected = { previewDark = it == 1 }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp))
            target.roles.chunked(3).forEach { row -> Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { role ->
                    val plan = IconManagementSelection.plan(draftState, role, manifests)
                    val asset = plan.candidates.firstOrNull()
                    val pack = app.iconPacks.firstOrNull { it.id == asset?.packId }
                    val color = IconManagementSelection.appearance(draftState, role, previewDark, asset?.asset?.render ?: "mask")
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        IconArtwork(app, pack, asset?.role, roleTitle(role), tint = when {
                            color.mode == "custom" -> color.color
                            color.artwork -> null
                            previewDark -> 0xffffffff.toInt()
                            else -> 0xff000000.toInt()
                        }, opacity = color.opacity, previewDark = previewDark, geometryRole = role, sizePercent = color.sizePercent, largePreview = true)
                        Text(IconRoleRegistry.role(role)?.let { target.stateLabels[it.state] } ?: roleTitle(role), fontSize = 11.sp,
                            maxLines = 2, modifier = Modifier.padding(top = 4.dp), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            } }
            UiRow("Seleccionar icono", "Selecciona un grupo de estados o un icono uniforme para todo el destino.", "Cambiar", enabled = app.canEdit && !app.busy) {
                pickingRole = null; pickingSource = true
            }
            if (target.reason.isNotBlank()) UiRow("Ámbito de visualización", target.reason)
        }
        IconAppearanceEditor(if (target.scope == "hint") "Tamaño del icono y colores de fondo claro/oscuro" else "Tamaño y color de este icono",
            look, app.canEdit && !app.busy, childBack) { look = it }
        if (target.scope == "tile" && target.roles.size > 1) {
            IconSectionLabel("Colores independientes por estado")
            UiGroupCard {
                UiContentTabs(tabs = target.roles.map { target.stateLabels[IconRoleRegistry.role(it)?.state] ?: roleTitle(it) },
                    selectedTabIndex = target.roles.indexOf(activeState).coerceAtLeast(0),
                    onTabSelected = { activeState = target.roles[it] }, modifier = Modifier.fillMaxWidth().padding(8.dp))
                UiRow("Ajustar solo el estado actual", roleTitle(activeState))
            }
            IconAppearanceEditor("Colores de ${roleTitle(activeState)}", stateLooks[activeState] ?: IconLookDraft(),
                app.canEdit && !app.busy, childBack, showSize = false) { value ->
                stateLooks = stateLooks.toMutableMap().apply { if (value.value() == null) remove(activeState) else put(activeState, value) }
            }
        }
        if (error.isNotEmpty()) Text(error, color = MiuixTheme.colorScheme.error, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            C17TextButton(text = "Cancelar", modifier = Modifier.weight(1f), enabled = !app.busy, onClick = onApplied)
            C17TextButton(text = if (app.iconManagementEnabled()) "Aplicar a este icono" else "Guardar selección", modifier = Modifier.weight(1f), enabled = app.canEdit && !app.busy && target.supported, onClick = {
                error = app.applyIconTarget(target.id, sources, look.value(), stateLooks.mapNotNull { (role, value) -> value.value()?.let { role to it } }.toMap(), animation)
                if (error.isEmpty()) onApplied()
            })
        }
        UiGroupCard {
            UiRow("Otros ajustes", "Iconos y animaciones de cada estado", if (advanced) "Contraer" else "Expandir") { advanced = !advanced }
        }
        if (advanced) {
            UiGroupCard {
                UiRow("Configurar cada estado por separado", "Solo ajustar el estado seleccionado", if (individual) "Contraer" else "Expandir") { individual = !individual }
                if (individual) {
                    target.roles.forEach { role -> UiRow(roleTitle(role), value = if (activeState == role) "Seleccionado" else "") { activeState = role } }
                    UiRow("Cambiar el icono del estado actual", roleTitle(activeState), "Seleccionar", enabled = app.canEdit && !app.busy) {
                        pickingRole = activeState; pickingSource = true
                    }
                }
                UiSwitchRow("Usar animación del paquete", if (target.dynamicSupported) "Seguir los cambios de estado reales; solo se anima cuando el componente anfitrión lo permite." else "Este destino solo admite iconos estáticos.", animation,
                    app.canEdit && !app.busy && target.dynamicSupported) { animation = it }
            }
            if (individual && target.scope != "tile") IconAppearanceEditor("Apariencia de ${roleTitle(activeState)}", stateLooks[activeState] ?: IconLookDraft(), app.canEdit && !app.busy, childBack) { value ->
                stateLooks = stateLooks.toMutableMap().apply { if (value.value() == null) remove(activeState) else put(activeState, value) }
            }
            UiGroupCard {
                UiRow("Restaurar los ajustes predeterminados de este destino", "Borrar el borrador de esta página; los cambios se guardarán al pulsar Aplicar.", "Restaurar", enabled = app.canEdit && !app.busy) {
                    sources = emptyMap(); look = IconLookDraft(); stateLooks = emptyMap(); animation = true
                }
            }
        }
        Text("Los iconos y los ajustes avanzados se guardan juntos; al cancelar o volver atrás, se descarta el borrador actual.", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun IconSourcePicker(app: ModernMainActivity, target: IconRoleRegistry.Target, role: String?,
    sources: Map<String, IconManagementSettings.Override>, preferredPackId: String?, onBack: () -> Unit, onOpenLibrary: () -> Unit,
    onChoose: (Map<String, IconManagementSettings.Override>) -> Unit) {
    val selected = sources[role ?: target.roles.first()]
    var selectedPack by remember(target.id, role, preferredPackId) { mutableStateOf(
        preferredPackId?.takeIf { id -> app.iconPacks.any { it.id == id } } ?: selected?.packId?.takeIf { it.isNotBlank() } ?: app.iconPacks.firstOrNull()?.id.orEmpty()) }
    var choosePack by remember { mutableStateOf(false) }
    fun chooseSingle(value: IconManagementSettings.Override?) {
        val roles = role?.let { listOf(it) } ?: target.roles
        onChoose((if (role == null) emptyMap() else sources).toMutableMap().apply { roles.forEach { current ->
            if (value == null) remove(current) else put(current, value)
        } })
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UiGroupCard {
            UiRow(if (role == null) target.title else roleTitle(role), if (role == null) "Por defecto, se configuran todos los estados a la vez. Después de elegir, volverás a la vista previa." else "Solo cambia el estado actual; los demás conservarán sus borradores.")
            UiRow("Seguir el paquete", "Usar el paquete de este ámbito; si falta un icono, usar el predeterminado.", enabled = !app.busy) { chooseSingle(null) }
            UiRow("Usar el icono del sistema", if (role == null) "Este destino no usa sustituciones del paquete." else "Este estado no usa sustituciones del paquete.", enabled = !app.busy) { chooseSingle(IconManagementSettings.Override("system", "", "")) }
        }
        if (role == null) {
            val matchingPacks = app.iconPacks.filter { pack -> target.roles.any { sourceRoleInPack(pack, it) != null } }
                .sortedBy { if (it.id == selectedPack) 0 else 1 }
            if (matchingPacks.isNotEmpty()) IconSectionLabel("Selección de grupo · conservar las diferencias entre estados")
            matchingPacks.forEach { pack ->
                val matches = target.roles.mapNotNull { current -> sourceRoleInPack(pack, current)?.let { current to IconManagementSettings.Override("pack", pack.id, it) } }.toMap()
                UiGroupCard {
                    UiRow(iconPackName(pack), "Coinciden ${matches.size}/${target.roles.size} estados; los estados sin coincidencia seguirán el paquete o al sistema.", "Usar selección", enabled = app.canEdit && !app.busy) { onChoose(matches) }
                    Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        matches.keys.take(5).forEach { IconArtwork(app, pack, it, roleTitle(it)) }
                    }
                }
            }
        }
        val pack = app.iconPacks.firstOrNull { it.id == selectedPack }
        if (pack != null) {
            IconSectionLabel(if (role == null) "O elegir un icono uniforme" else "Elegir el icono de este estado")
            UiGroupCard {
                UiRow("Origen del icono", iconPackName(pack), if (choosePack) "Contraer" else "Cambiar") { choosePack = !choosePack }
                if (choosePack) app.iconPacks.forEach { entry -> UiRow(iconPackName(entry), value = if (selectedPack == entry.id) "Seleccionado" else "") {
                    selectedPack = entry.id; choosePack = false
                } }
            }
            IconArtworkBrowser(app, pack, target, selected) { source -> chooseSingle(IconManagementSettings.Override("pack", pack.id, source)) }
        }
        UiGroupCard {
            UiRow(if (pack == null) "Seleccionar un icono de la biblioteca" else "Añadir o seleccionar otra biblioteca", "Selecciona una biblioteca existente, el paquete integrado actual o importa tu propio paquete de iconos v3.", "Seleccionar", Icons.Outlined.Collections,
                enabled = app.canEdit && !app.busy, onClick = onOpenLibrary)
        }
        C17TextButton(text = "Volver a la vista previa de iconos", onClick = onBack, enabled = !app.busy, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun IconScopeAppearance(app: ModernMainActivity, scope: String, state: IconManagementSettings.State,
    childBack: Array<(() -> Boolean)?>, onApplied: () -> Unit) {
    var draft by remember(scope) { mutableStateOf(iconLook(state.appearances["scope:$scope"])) }
    var error by remember { mutableStateOf("") }
    DisposableEffect(scope) { childBack[0] = null; onDispose { childBack[0] = null } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IconAppearanceEditor("Apariencia del ámbito", draft, app.canEdit && !app.busy, childBack) { draft = it }
        Text("Solo cambia la apariencia predeterminada de este ámbito; los ajustes independientes de destinos y estados tienen prioridad. Volver atrás no guarda el borrador.", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (error.isNotEmpty()) Text(error, color = MiuixTheme.colorScheme.error, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            C17TextButton(text = "Cancelar", modifier = Modifier.weight(1f), enabled = !app.busy, onClick = onApplied)
            C17TextButton(text = if (app.iconManagementEnabled()) "Aplicar apariencia" else "Guardar apariencia", modifier = Modifier.weight(1f), enabled = app.canEdit && !app.busy, onClick = {
                error = app.applyIconAppearance("scope:$scope", draft.value())
                if (error.isEmpty()) onApplied()
            })
        }
    }
}

@Composable
private fun IconAppearanceEditor(title: String, draft: IconLookDraft, enabled: Boolean, childBack: Array<(() -> Boolean)?>,
    showSize: Boolean = true, onChange: (IconLookDraft) -> Unit) {
    var colorSide by remember { mutableStateOf<String?>(null) }
    val closeColor by rememberUpdatedState<() -> Boolean> { colorSide = null; true }
    DisposableEffect(colorSide) {
        if (colorSide != null) {
            val previous = childBack[0]
            val dismiss: () -> Boolean = { closeColor() }
            childBack[0] = dismiss
            onDispose { if (childBack[0] === dismiss) childBack[0] = previous }
        } else onDispose { }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.padding(horizontal = 12.dp), fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        UiGroupCard {
            if (showSize) {
                UiSwitchRow("Tamaño de icono independiente", "100 % por defecto; solo cambia la escala del icono, conservando la posición del sistema y el área táctil.", draft.sizePercent != null, enabled) {
                    onChange(draft.copy(sizePercent = if (it) 100f else null))
                }
                draft.sizePercent?.let { size ->
                    UiSliderRow("Tamaño del icono", size, 25f..200f, "${size.toInt()}%", enabled = enabled) { onChange(draft.copy(sizePercent = it)) }
                    C17TextButton("Restaurar tamaño al 100 %", enabled = enabled, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), onClick = { onChange(draft.copy(sizePercent = 100f)) })
                }
            }
            val modes = listOf("inherit" to "Seguir los ajustes superiores", "system" to "Seguir los colores del sistema", "custom" to "Color personalizado", "artwork" to "Conservar los colores originales del recurso")
            modes.forEach { (mode, label) -> UiRow(label,
                if (mode == "artwork") "Los recursos multicolor conservan sus colores originales; la máscara sigue usando el color del sistema." else "", if (draft.mode == mode) "Seleccionado" else "", enabled = enabled) { onChange(draft.copy(mode = mode)) } }
            if (draft.mode == "custom") UiColorPair("Fondo claro / fondo oscuro", draft.light, draft.dark, enabled,
                onLight = { colorSide = "light" }, onDark = { colorSide = "dark" })
            UiSwitchRow("Transparencia independiente", "Al desactivarla, hereda el valor superior. Por defecto es 100 %; se conservan el estado del sistema y la transparencia de transición.", draft.opacity != null, enabled) {
                onChange(draft.copy(opacity = if (it) 100f else null))
            }
            draft.opacity?.let { opacity -> UiSliderRow("Transparencia del icono", opacity, 0f..100f, "${opacity.toInt()}%", enabled = enabled) { onChange(draft.copy(opacity = it)) } }
        }
    }
    colorSide?.let { side -> IconRgbDialog(if (side == "light") "Color del fondo claro" else "Color del fondo oscuro", if (side == "light") draft.light else draft.dark,
        onDismiss = { colorSide = null }) { color ->
        onChange(if (side == "light") draft.copy(light = color) else draft.copy(dark = color)); colorSide = null
    } }
}

@Composable
private fun IconRgbDialog(title: String, initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var color by remember(initial) { mutableIntStateOf(initial or 0xff000000.toInt()) }
    fun hex(value: Int) = "#%06X".format(java.util.Locale.ROOT, value and 0xffffff)
    var text by remember(initial) { mutableStateOf(hex(initial)) }
    var error by remember { mutableStateOf("") }
    C17OverlayDialog(show = true, title = title, summary = "Selecciona solo el color RGB; la transparencia del icono se configura por separado en Apariencia.", onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(color)))
            listOf(16 to "Rojo", 8 to "Verde", 0 to "Azul").forEach { (shift, label) ->
                val value = (color ushr shift) and 255
                UiSliderRow(label, value.toFloat(), 0f..255f, value.toString()) { selected ->
                    color = (color and (255 shl shift).inv()) or (selected.toInt().coerceIn(0, 255) shl shift) or 0xff000000.toInt()
                    text = hex(color); error = ""
                }
            }
            C17TextField(value = text, onValueChange = { input ->
                text = input; error = ""
                val digits = input.removePrefix("#")
                if (digits.matches(Regex("[0-9a-fA-F]{6}"))) color = digits.toInt(16) or 0xff000000.toInt()
            }, label = "#RRGGBB", singleLine = true, modifier = Modifier.fillMaxWidth(), insideMargin = DpSize(12.dp, 10.dp))
            if (error.isNotEmpty()) Text(error, color = MiuixTheme.colorScheme.error, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                C17TextButton(text = "Cancelar", modifier = Modifier.weight(1f), onClick = onDismiss)
                C17TextButton(text = "Aceptar", modifier = Modifier.weight(1f), onClick = {
                    if (!text.removePrefix("#").matches(Regex("[0-9a-fA-F]{6}"))) error = "Introduce un color RGB de 6 dígitos."
                    else onConfirm(color)
                })
            }
        }
    }
}
