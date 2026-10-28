// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import java.util.Locale
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.theme.*

/** The top native surface owns every app popup, above both the page and navigation. */
@Composable
fun C17OverlayLayer(app: ModernMainActivity) {
    val visible = app.freeNoticeRequired || app.thanksOpen || app.performanceOpen || app.donationImage != null || app.confirmation != null || app.editing != null || app.statusBarOrderChoice != null || app.colorEditing != null || app.fontCatalogOpen || app.fontScopeSelection != null || app.marqueePickerOpen || app.configScope != null || app.fontRenameTarget != null || app.downloadingFont != null || app.notificationOverridesOpen || (app.wallpaperEdit != null) || app.clockLaunchEditing != null
    SideEffect { app.syncModalLayer(visible) }
    C17Scaffold(containerColor = Color.Transparent) {
        C17Dialogs(app)
    }
}

@Composable
private fun C17Dialogs(app: ModernMainActivity) {
    if (app.freeNoticeRequired) {
        FreeNoticeDialog(app)
        // The thanks wall can be opened from the notice; it lies above it.
        if (app.thanksOpen) ThanksWallDialog(app)
        return
    }
    if (app.thanksOpen) { ThanksWallDialog(app); return }
    if (app.performanceOpen && app.confirmation == null) PerformanceSheet(app)
    app.donationImage?.let { DonationDialog(app, it) }
    if (app.notificationOverridesOpen) NotificationOverridesDialog(app)
    app.clockLaunchEditing?.let { ClockLaunchAppDialog(app, it) }
    app.wallpaperEdit?.let { ShadeWallpaperCropDialog(app, it) }
    if (app.fontCatalogOpen) FontCatalogDialog(app)
    app.fontScopeSelection?.let { FontScopeDialog(app, it) }
    if (app.marqueePickerOpen) MarqueePresetDialog(app)
    app.configScope?.let { ConfigScopeDialog(app, it) }
    app.fontRenameTarget?.let { entry ->
        UiInputDialog("Cambiar nombre de fuente", entry.name, "Solo cambia el nombre que aparece en la lista; no modifica los glifos, el grosor ni otras fuentes importadas.",
            onDismiss = { app.fontRenameTarget = null; app.fontCatalogOpen = true }) { app.renameLibraryFont(entry, it) }
    }
    app.downloadingFont?.let { entry ->
        C17OverlayDialog(show = true, title = "Descargar ${entry.displayName}",
            summary = "${app.fontDownloadPercent}% · ${if (app.fontDownloadPercent == 100) "Verificando e incorporando a la biblioteca" else "Archivo de fuente oficial de GitHub"}\nDespués de descargarla, elige el ámbito de aplicación. La fuente original se conserva hasta que apliques los cambios.",
            onDismissRequest = app::cancelFontDownload) {
            C17TextButton(text = "Cancelar descarga", onClick = app::cancelFontDownload, modifier = Modifier.fillMaxWidth())
        }
    }
    app.confirmation?.let { prompt ->
        UiConfirmDialog(prompt.title, prompt.summary, prompt.button, delaySeconds = prompt.delaySeconds,
            expiresAt = prompt.expiresAt, linkLabel = prompt.linkLabel,
            onLink = prompt.linkUrl.takeIf { it.isNotEmpty() }?.let { url -> { app.openUrl(url) } },
            onDismiss = app::dismissConfirmation) {
            app.confirmation = null; prompt.action()
        }
    }
    app.statusBarOrderChoice?.let { choice ->
        UiChoiceDialog(choice.title, choice.values, choice.labels, choice.selected,
            description = choice.description, onDismiss = { app.statusBarOrderChoice = null }, onChoice = choice.onChoice)
    }
    app.editing?.let { EditSettingDialog(app, it) }
    app.colorEditing?.let { item ->
        UiColorDialog(item.title, (app.value(item.key) as? Number)?.toInt() ?: 0,
            StatusBarSettings.customAlpha(app.values, item.key), item.description,
            onDismiss = { app.colorEditing = null }) { color, customAlpha ->
                app.saveColor(item, color, customAlpha); app.colorEditing = null
            }
    }
}

@Composable
private fun ClockLaunchAppDialog(app: ModernMainActivity, key: String) {
    val calendar = key == NotificationBigClockSettings.DATE_LAUNCH_APP
    val component = BigClockWidgetsSettings.launchAppKey(key)
    val allowed = app.canEdit && !app.busy && app.clockLaunchEnabled(key)
    val selected = app.value(key)?.toString().orEmpty()
    val selectedLabel = remember(key, selected, app.clockLaunchChoices) { app.clockLaunchLabel(key) }
    var query by remember(key) { mutableStateOf("") }
    var category by remember(key) { mutableIntStateOf(0) }
    var error by remember(key) { mutableStateOf("") }
    val choices = remember(app.clockLaunchChoices, selected, query, category) {
        app.clockLaunchChoices.filter { choice ->
            (category == 0 || choice.system == (category == 2)) &&
                (query.isBlank() || choice.label.contains(query, true) || choice.packageName.contains(query, true))
        }.sortedBy { if (it.packageName == selected) 0 else 1 }
    }
    val window = LocalWindowInfo.current.containerDpSize
    // Only a short window moves the filters into the list. The keyboard opening must not: the search field would be rebuilt there,
    // lose its focus and close the keyboard again, and the dialog jumped between the two layouts.
    val compact = window.height < 500.dp
    val listState = key(key, query, category) { rememberLazyListState() }
    val defaultRow: @Composable () -> Unit = {
        C17RadioButtonPreference(title = if (component) "No abrir ninguna aplicación" else "Predeterminado del sistema", selected = selected.isEmpty(), enabled = allowed,
            summary = if (component) "Mantener el texto visible sin ocupar la zona táctil" else app.clockLaunchDefault?.let { "Abrir ${it.label}" }
                ?: if (app.clockLaunchLoading) "Leyendo la aplicación predeterminada del sistema…" else "Usar la aplicación nativa del sistema ${if (calendar) "calendario" else "reloj"} como destino",
            radioButtonLocation = RadioButtonLocation.End,
            insideMargin = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            onClick = { error = app.saveClockLaunchApp(key, "") })
    }
    val controls: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            C17TextField(value = query, onValueChange = { query = it }, label = "Buscar por nombre de aplicación o nombre de paquete", singleLine = true,
                useLabelAsPlaceholder = true, modifier = Modifier.fillMaxWidth(), insideMargin = DpSize(12.dp, 10.dp))
            UiContentTabs(tabs = listOf("Todo", "No del sistema", "Sistema"), selectedTabIndex = category,
                onTabSelected = { category = it }, modifier = Modifier.fillMaxWidth())
        }
    }
    val dialogTitle = if (component) "Aplicación que se abre al tocar el texto ${if (BigClockWidgetsSettings.textSide(key) == BigClockWidgetsSettings.LEFT) "izquierdo" else "derecho"}"
        else if (calendar) "Aplicación que se abre al tocar la fecha" else "Aplicación que se abre al tocar la hora"
    C17OverlayDialog(show = true, title = dialogTitle,
        modifier = Modifier.heightIn(max = (window.height * .82f).coerceAtMost(640.dp)),
        insideMargin = DpSize(18.dp, 18.dp), outsideMargin = DpSize(16.dp, 12.dp),
        onDismissRequest = app::dismissClockLaunchPicker) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!compact) {
                Text("Actual: $selectedLabel", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                defaultRow()
                controls()
            }
            // Keep the result list bounded. In a short window or above the keyboard,
            // the filters join this same scroll area rather than squeezing its rows.
            LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth(), state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (compact) {
                    item { Text("Actual: $selectedLabel", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
                    item { defaultRow() }
                    item { controls() }
                }
                item { Text(if (app.clockLaunchLoading) "Leyendo aplicaciones que se pueden abrir…" else "Aplicaciones que se pueden abrir · ${choices.size}",
                    fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)) }
                items(choices, key = { it.packageName }) { choice ->
                    C17RadioButtonPreference(title = choice.label, summary = choice.packageName,
                        selected = selected == choice.packageName, enabled = allowed,
                        startAction = { Box(Modifier.padding(end = 12.dp)) { AppIcon(app, choice.packageName) } },
                        radioButtonLocation = RadioButtonLocation.End,
                        insideMargin = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        onClick = { error = app.saveClockLaunchApp(key, choice.packageName) })
                }
                if (!app.clockLaunchLoading && choices.isEmpty()) item {
                    Text("No hay aplicaciones disponibles que cumplan los criterios.", fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(vertical = 12.dp))
                }
                item { Text("Si la aplicación seleccionada se desinstala o desactiva, se volverá automáticamente a la predeterminada del sistema.", fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(top = 8.dp)) }
            }
            if (error.isNotBlank()) Text(error, fontSize = 13.sp, color = MiuixTheme.colorScheme.error)
            C17TextButton(text = "Cerrar", onClick = app::dismissClockLaunchPicker, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun C17Pages(app: ModernMainActivity) {
    val homeVisible = app.page == 0 && app.selectedGroup == null && !app.freeNoticeRequired &&
        !app.uiSettingsOpen && !app.diagnosticsOpen && !app.iconManagementOpen
    DisposableEffect(app, homeVisible) {
        app.setHomeStatusVisible(homeVisible)
        onDispose { app.setHomeStatusVisible(false) }
    }
    if (app.uiSettingsOpen) {
        val visible = app.freeNoticeRequired || app.thanksOpen || app.performanceOpen || app.confirmation != null || app.editing != null || app.statusBarOrderChoice != null || app.colorEditing != null
        SideEffect { app.syncModalLayer(visible) }
        UiSettingsPage(app)
        return
    }
    if (app.diagnosticsOpen) {
        val visible = app.freeNoticeRequired || app.thanksOpen || app.performanceOpen || app.confirmation != null || app.editing != null || app.statusBarOrderChoice != null || app.colorEditing != null
        SideEffect { app.syncModalLayer(visible) }
        DiagnosticsPage(app)
        return
    }
    if (app.iconManagementOpen) {
        val visible = app.freeNoticeRequired || app.thanksOpen || app.performanceOpen || app.confirmation != null || app.editing != null || app.statusBarOrderChoice != null || app.colorEditing != null
        SideEffect { app.syncModalLayer(visible) }
        IconManagementPage(app)
        return
    }
    val modalVisible = app.freeNoticeRequired || app.thanksOpen || app.performanceOpen || app.donationImage != null || app.confirmation != null || app.editing != null || app.statusBarOrderChoice != null || app.colorEditing != null || app.fontCatalogOpen || app.fontScopeSelection != null || app.marqueePickerOpen || app.configScope != null || app.fontRenameTarget != null || app.downloadingFont != null || app.notificationOverridesOpen || (app.wallpaperEdit != null) || app.clockLaunchEditing != null
    SideEffect { app.syncModalLayer(modalVisible) }
    val group = app.selectedGroup?.let { SettingsCatalog.group(it) }
    var requestedSetting by remember { mutableStateOf<String?>(null) }
    val revealedSetting = remember(group?.id, app.category) { requestedSetting }
    if (group?.id == StatusBarLayoutSettings.GROUP) {
        StatusBarOrderPage(app, requestedSetting, onRequestConsumed = { requestedSetting = null })
        return
    }
    val detailPages = remember(group?.id, app.category, app.panelMode, revealedSetting) {
        SettingsCatalog.editorDetailPages(group, app.panelMode, revealedSetting).filter { group?.id != "carrier" || it.id == carrierScene(app.category) }
    }
    var detailPageId by rememberSaveable(group?.id, app.category) {
        mutableStateOf(detailPages.firstOrNull { page -> page.items.any { it.key == requestedSetting } }?.id
            ?: detailPages.firstOrNull()?.id ?: "all")
    }
    val activeDetail = detailPages.firstOrNull { it.id == detailPageId } ?: detailPages.firstOrNull()
    val detailMaster = when (group?.id) {
        "carrier" -> carrierPrefix(app.category) + "_enabled"
        "classic_text" -> if (activeDetail?.id == "clock") ClassicTextSettings.CLOCK_MASTER else ClassicTextSettings.CARRIER + "_enabled"
        "clock" -> when (activeDetail?.id) {
            "common" -> null
            "dual" -> ClockLayoutSettings.DUAL_ENABLED
            else -> group.masterKey
        }
        "native_network_badge" -> if (activeDetail?.id == "activity") NativeDataActivity.MASTER else NativeNetworkBadgeControls.MASTER
        else -> group?.takeUnless { it.independentSwitches }?.masterKey
    }
    val commonItems = SettingsCatalog.commonDetailItems(group).filter { SettingsCatalog.applicable(it, app.values) || it.key == requestedSetting }
    val commonKeys = commonItems.map { it.key }.toSet()
    val detailIconFamily = when (group?.id) {
        "battery" -> "battery"
        "wifi" -> "wifi"
        "data" -> "cellular"
        "status_hint_icons" -> "hint"
        else -> null
    }
    val detailSections = activeDetail?.items?.filterNot { it.key == detailMaster }
        ?.filterNot { it.key in commonKeys }
        ?.filterNot { group?.id == "font" }
        ?.filter { SettingsCatalog.applicable(it, app.values) || it.key == revealedSetting || it.pairedKey == revealedSetting }
        ?.filter { item -> group?.id != "notification_icons" || when (item.key) {
            NotificationIconArea.TEXT -> app.value(NotificationIconArea.MODE) == "text" || item.key == revealedSetting
            NotificationIconArea.IMAGE_NAME -> app.value(NotificationIconArea.MODE) == "image" || item.key == revealedSetting
            else -> true
        } }?.groupBy { it.section } ?: emptyMap()
    val homeTopBar = rememberC17TopAppBarState()
    val configTopBar = key(app.category, app.query.isNotBlank()) { rememberC17TopAppBarState() }
    val aboutTopBar = rememberC17TopAppBarState()
    val detailTopBar = key(group?.id, activeDetail?.id) { rememberC17TopAppBarState(initialHeightOffsetLimit = 0f) }
    val topBarState = if (group != null) detailTopBar else listOf(homeTopBar, configTopBar, aboutTopBar)[app.page]
    val scroll = rememberC17ScrollBehavior(state = topBarState)
    val title = group?.let { destinationTitle(it, app.category) } ?: listOf("Inicio", "Configuración", "Acerca de")[app.page]
    val homeScroll = rememberLazyListState()
    val configScroll = key(app.category, app.query.isNotBlank()) { rememberLazyListState() }
    val aboutScroll = rememberLazyListState()
    val detailScroll = key(group?.id, activeDetail?.id) { rememberLazyListState() }
    val currentScroll = if (group != null) detailScroll else listOf(homeScroll, configScroll, aboutScroll)[app.page]
    val searching = group == null && app.page == 1 && app.query.isNotBlank()
    // A saved state with both clock modes on is shown and run as the default single row; settle the stored switch too.
    val clockConflict = group?.id == "clock" && app.canEdit && app.bool(ClockLayoutSettings.DUAL_ENABLED) &&
        app.bool(FeatureOptions.masterKey(ClockLayoutSettings.STATUS))
    LaunchedEffect(clockConflict) { if (clockConflict) app.save(ClockLayoutSettings.DUAL_ENABLED, false) }
    LaunchedEffect(group?.id, activeDetail?.id, requestedSetting) {
        val requested = requestedSetting ?: return@LaunchedEffect
        if (group == null) return@LaunchedEffect
        val requestedPage = detailPages.firstOrNull { page -> page.items.any { it.key == requested } }
        if (requestedPage != null && requestedPage.id != activeDetail?.id) {
            detailPageId = requestedPage.id; return@LaunchedEffect
        }
        if (requested == detailMaster) {
            detailScroll.scrollToItem(0); requestedSetting = null; return@LaunchedEffect
        }
        if (requested in commonKeys) {
            detailScroll.scrollToItem(1); requestedSetting = null; return@LaunchedEffect
        }
        val sectionIndex = detailSections.values.indexOfFirst { items -> items.any { it.key == requested } }
        if (sectionIndex >= 0) {
            val beforeSections = 1 + (if (commonItems.isNotEmpty()) 1 else 0) +
                (if (group.id == "clock" && detailMaster != null) 1 else 0) + (if (detailIconFamily != null) 1 else 0) +
                (if (group.id == "status_hint_icons") 1 else 0)
            detailScroll.scrollToItem(beforeSections + sectionIndex)
            requestedSetting = null
        }
    }
    C17Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            if (group == null) C17TopAppBar(title = title, scrollBehavior = scroll)
            else C17SmallTopAppBar(title = title, scrollBehavior = scroll, navigationIcon = {
                C17IconButton(onClick = app::closeGroup) { Icon(MiuixIcons.Back, "Volver") }
            })
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        if (group != null && detailPages.size > 1) {
            UiContentTabs(tabs = detailPages.map { it.title }, selectedTabIndex = detailPages.indexOf(activeDetail),
                onTabSelected = { requestedSetting = null; detailPageId = detailPages[it].id },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .nestedScroll(scroll.nestedScrollConnection).edgeFade(currentScroll, app.ui.edgeFade),
            state = currentScroll,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = if (searching) 8.dp else 12.dp,
                bottom = if (group == null) maxOf(padding.calculateBottomPadding(), app.navigationHeight.dp) + 12.dp
                    else padding.calculateBottomPadding() + 32.dp),
            verticalArrangement = Arrangement.spacedBy(if (searching) 8.dp else 12.dp),
        ) {
            if (group != null) {
                item { DetailIntro(app, group, detailMaster?.takeIf { group.id != "clock" }, SettingsCatalog.detailModeReason(group, activeDetail, app.panelMode)) }
                if (group.id == "status_hint_icons") item {
                    UiGroupCard { UiRow("Posición y orden por elemento", "Elige si cada icono indicador aparece a la izquierda o a la derecha y ordénalos. Se guardan junto con el orden de la barra de estado.", "Abrir",
                        enabled = !app.busy) { app.openHintIconOrder() } }
                }
                if (commonItems.isNotEmpty()) item(key = "${group.id}-common") {
                    DetailSettingsSection(app, group, "Ajustes compartidos", commonItems, revealedSetting)
                }
                if (group.id == "clock" && detailMaster != null) item { ClockModeSwitch(app, detailMaster, activeDetail?.id == "dual") }
                val iconFamily = detailIconFamily
                if (iconFamily != null) item {
                    val state = app.iconManagementState()
                    val assigned = state.overrides.keys.count { IconRoleRegistry.scopeOfRole(it) == iconFamily }
                    UiGroupCard {
                        UiRow("Icono y color", "Elige el icono desde Administración de iconos; la posición y el tamaño siguen usando los ajustes de esta página.", if (assigned > 0) "$assigned elementos" else "Seleccionar", Icons.Outlined.Collections,
                            enabled = app.canEdit && !app.busy) { app.openIconAssignments(iconFamily) }
                    }
                }

                detailSections.forEach { (section, items) ->
                    item(key = "${group.id}-$section") {
                        DetailSettingsSection(app, group, section, items, revealedSetting)
                    }
                }
                if (group.id == "font") item { FontSettingsPanel(app) }
                val resetItems = (if (group.id == "carrier" || detailPages.size > 1) activeDetail?.items ?: emptyList() else group.items).distinctBy { it.key }
                val resetTitle = title + if (detailPages.size > 1) " · ${activeDetail?.title}" else ""
                item { UiGroupCard { UiRow(if (detailPages.size > 1) "Restaurar valores predeterminados de esta pestaña" else "Restaurar valores predeterminados de esta página",
                    "Restablecer solo $resetTitle" + if (commonItems.isNotEmpty() && detailPages.size > 1) "; no incluye los ajustes compartidos" else "", icon = MiuixIcons.Reset,
                    enabled = app.canEdit && !app.busy) {
                    app.resetGroup(group, items = resetItems, title = resetTitle)
                }
                    if (commonItems.isNotEmpty() && detailPages.size > 1) UiRow("Restaurar valores predeterminados de los ajustes compartidos", "Solo restablece los ajustes compartidos; no incluye la pestaña actual.", icon = MiuixIcons.Reset,
                        enabled = app.canEdit && !app.busy) { app.resetGroup(group, items = commonItems, title = "$title · Ajustes compartidos") }
                } }
            } else when (app.page) {
                0 -> {
                    item(key = "home-hero") {
                        val heroVisible by remember(currentScroll) { derivedStateOf {
                            val layout = currentScroll.layoutInfo
                            !currentScroll.isScrollInProgress && layout.visibleItemsInfo.any {
                                it.key == "home-hero" && it.offset + it.size > layout.viewportStartOffset && it.offset < layout.viewportEndOffset
                            }
                        } }
                        ActivationHero(app, heroVisible && !modalVisible)
                    }
                    item { SectionTitle("Estado de ejecución") }
                    item { UiGroupCard {
                        UiRow("LSPosed", if (app.lspEnabled) "${app.framework.frameworkName} ${app.framework.frameworkVersion}" else app.framework.message.take(42),
                            if (app.lspEnabled) "Activado" else "Sin conexión", Icons.Outlined.Extension) {
                            app.confirmation = UiConfirmation("Estado de LSPosed", app.framework.message + if (app.lspEnabled) "\nAPI ${app.framework.apiVersion}" else "", "Entendido") {}
                        }
                        UiRow("Versión actual", if (app.runtimeActive) "Activo en la interfaz del sistema" else "Esperando que esta versión se inyecte en la interfaz del sistema",
                            if (app.runtimeActive) "Activo" else "Pendiente de inyección", Icons.Outlined.CheckCircle) {
                            app.confirmation = UiConfirmation("Versión actual", app.runtimeMessage, "Entendido") {}
                        }
                        UiRow("Permiso Root", if (app.rootGranted) "Permitir guardar la configuración por separado aunque el módulo no esté activado" else app.rootMessage.take(36), if (app.rootGranted) "Autorizado" else if (app.checkingRoot) "Comprobando…" else "Autorizar", Icons.Outlined.VerifiedUser, enabled = !app.checkingRoot) { app.requestRoot() }
                        UiRow("Interfaz del sistema", "Aplicar la versión actual del módulo", if (app.busy) "Procesando…" else "Reiniciar", Icons.Outlined.RestartAlt, enabled = !app.busy) { app.restartSystemUi() }
                    } }
                    item { SectionTitle("Control del módulo") }
                    item { UiGroupCard {
                        UiSwitchRow("Modo seguro", "Pausa todas las modificaciones y conserva la configuración actual.", app.bool(StatusBarSettings.SAFE_MODE), app.canEdit && !app.busy) { app.save(StatusBarSettings.SAFE_MODE, it) }
                    } }
                    item { Text(if (app.canEdit) "Los ajustes se guardan en tiempo real. Los cambios se aplicarán cuando el módulo esté activo en la interfaz del sistema." else "Activa el módulo en LSPosed y marca «Interfaz del sistema», o concede Root para guardar la configuración por separado.",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
                }
                1 -> {
                    item { ConfigurationHeader(app) }
                    if (!app.canEdit) item { UiGroupCard { UiRow("Todavía no se han concedido permisos para modificar la configuración.", "Ve a Inicio para activar el módulo o conceder Root.", "Ir", MiuixIcons.Unlock) { app.selectPage(0) } } }
                    if (app.query.isBlank()) {
                        SettingsCatalog.navigationSections(app.category).forEach { section ->
                            item(key = "category-${app.category}-${section.title}") {
                                Column {
                                    SectionTitle(section.title)
                                    UiGroupCard {
                                        section.groups.forEach { destination ->
                                            UiRow(destinationTitle(destination, app.category), concise(destination.description),
                                                destinationStatus(app, destination, app.category),
                                                groupIcon(destination.id)) { app.openGroup(destination.id) }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val term = app.query.trim()
                        val matches = SettingsCatalog.groups().mapNotNull { destination ->
                            val matchingItems = SettingsCatalog.searchItems(destination).filter {
                                (SettingsCatalog.displayTitle(it, app.values) + it.description + it.section + it.labels.joinToString(" ")).contains(term, true)
                            }
                            if ((destination.title + destination.description).contains(term, true) || matchingItems.isNotEmpty()) destination to matchingItems else null
                        }
                        matches.forEach { (destination, matchingItems) ->
                            item(key = "search-${destination.id}") {
                                Column {
                                    Text(destination.title, Modifier.padding(start = 16.dp, top = 2.dp, bottom = 6.dp),
                                        fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                    UiGroupCard {
                                        if (matchingItems.isEmpty()) SearchResultRow(destination.title, destination.description, term,
                                            groupIcon(destination.id)) { app.openGroup(destination.id) }
                                        matchingItems.forEach { setting ->
                                            val displayTitle = SettingsCatalog.displayTitle(setting, app.values)
                                            val summary = if (displayTitle.contains(term, true) || setting.section.contains(term, true)) setting.section
                                                else setting.labels.firstOrNull { it.contains(term, true) }
                                                    ?: setting.description.takeIf { it.contains(term, true) } ?: setting.section
                                            SearchResultRow(displayTitle, summary, term, groupIcon(destination.id)) {
                                                if (destination.id == "carrier") app.category = when {
                                                    setting.key.startsWith(CarrierPanels.LOCKSCREEN + "_") -> SettingsCatalog.LOCK_SCREEN
                                                    setting.key.startsWith(CarrierPanels.CONTROL + "_") -> SettingsCatalog.CONTROL_CENTER
                                                    else -> SettingsCatalog.NOTIFICATION
                                                }
                                                requestedSetting = setting.key; app.openGroup(destination.id)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (matches.isEmpty()) item { Text("No se encontraron ajustes relacionados.", Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
                    }
                }
                2 -> {
                    item { AboutHero(app) }
                    item { AboutActions(app) }
                    item { Text("Copyright © 2026 aiingjie", Modifier.fillMaxWidth().padding(12.dp), fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
                }
            }
        }
        }
    }
}

@Composable
private fun FreeNoticeDialog(app: ModernMainActivity) {
    var value by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val textColor = MiuixTheme.colorScheme.onSurface.toArgb()
    val hintColor = MiuixTheme.colorScheme.onSurfaceVariantSummary.toArgb()
    val confirm = {
        error = app.acceptFreeNotice(value)
        if (error == null) keyboard?.hide()
    }
    C17OverlayDialog(show = true, title = "Declaración de gratuidad permanente", onDismissRequest = app::finish) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(FreeNotice.DESCRIPTION, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text("Muro de agradecimientos", Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable { app.openThanks() }.padding(vertical = 4.dp), fontSize = 14.sp, color = MiuixTheme.colorScheme.primary)
                Text("Escribe manualmente la siguiente frase, incluidos los signos de puntuación:", fontSize = 14.sp)
                Text(FreeNotice.PHRASE, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                AndroidView(factory = { context ->
                    ManualConfirmationEditText(context).apply {
                        hint = "Escribe aquí manualmente"
                        setTextSize(15f)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setPadding(0, 0, 0, 0)
                        setText(value)
                        addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { value = s?.toString() ?: ""; error = null }
                            override fun afterTextChanged(s: android.text.Editable?) {}
                        })
                        setOnEditorActionListener { _, action, _ ->
                            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE && FreeNotice.matches(value)) { confirm(); true } else false
                        }
                    }
                }, update = { view ->
                    view.setTextColor(textColor); view.setHintTextColor(hintColor)
                    if (view.text.toString() != value) { view.setText(value); view.setSelection(view.text.length) }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                    .border(1.dp, MiuixTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp))
                Text("Este campo no permite copiar, pegar ni arrastrar y soltar.", fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                error?.let { Text(it, color = Color(0xffd94a4a), fontSize = 13.sp) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                C17TextButton(text = "Salir", modifier = Modifier.weight(1f).heightIn(min = 48.dp), onClick = app::finish)
                C17TextButton(text = "Confirmar y continuar", enabled = FreeNotice.matches(value),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp), onClick = { confirm() })
            }
        }
    }
}

/** Installation and activation are distinct from the result of the latest compilation. */
private fun performanceHomeStatus(app: ModernMainActivity): String = RootPerformanceModule.homeStatus(
    app.performanceReport, app.performanceBusy, app.performanceError, app.performanceAction)

private fun performanceRecordStatus(app: ModernMainActivity): String {
    val module = app.performanceReport
    return when {
        app.performanceBusy && app.performanceAction != RootPerformanceModule.Action.RUN && app.performanceAction != RootPerformanceModule.Action.AUTOMATIC -> "Procesando…"
        app.performanceError -> "Estado pendiente de consulta"
        module == null || !module.root -> "Estado pendiente de consulta"
        module.remove -> "Pendiente de desinstalación"
        module.disabled -> "Desactivado"
        module.pending -> "Pendiente de reinicio"
        module.outdated -> "Actualización disponible"
        !module.installed -> "No instalado"
        app.performanceBusy && app.performanceAction in listOf(RootPerformanceModule.Action.RUN, RootPerformanceModule.Action.AUTOMATIC) -> "Optimizando…"
        !module.currentBoot && module.lastState != "unknown" -> "Historial"
        module.recordVersionCode != module.moduleVersionCode && module.lastState != "unknown" -> "Registro antiguo"
        module.lastState == "waiting" -> "Esperando el momento adecuado"
        module.lastState == "running" -> if (module.homeStatus() == "Optimizando…") "Optimizando…" else "Todavía no hay registros válidos de ejecución."
        module.lastState == "deferred" -> if (module.trigger == "run") "No se ejecutó esta vez." else "La optimización automática se ha pospuesto."
        module.lastState == "failed" -> "Esta ejecución no se completó."
        module.lastState == "ok" -> if (module.verification == "verified") "Optimización completada" else "Compilación completada; pendiente de verificación"
        module.lastState == "current" -> if (module.verification == "verified") "Se reutiliza el resultado verificado" else "Se reutiliza el registro anterior"
        else -> "Todavía no hay registros de ejecución."
    }
}

@Composable
private fun PerformanceSheet(app: ModernMainActivity) {
    val window = LocalWindowInfo.current.containerDpSize
    val detailScroll = rememberScrollState()
    C17OverlayDialog(show = true, title = RootPerformanceModule.DISPLAY_NAME,
        modifier = Modifier.heightIn(max = (window.height * .86f).coerceAtMost(720.dp)),
        insideMargin = DpSize(18.dp, 18.dp), outsideMargin = DpSize(16.dp, 12.dp),
        onDismissRequest = { app.performanceOpen = false }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(detailScroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PerformanceModuleCard(app)
            }
            C17TextButton(text = "Cerrar", onClick = { app.performanceOpen = false }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PerformanceModuleCard(app: ModernMainActivity) {
    PerformanceDashboard(app, performanceHomeStatus(app), performanceRecordStatus(app))
}

@Composable
private fun DonationImage(app: ModernMainActivity, asset: String, ratio: Float, modifier: Modifier = Modifier) {
    val bitmap = remember(asset) {
        app.assets.open(asset).use { android.graphics.BitmapFactory.decodeStream(it) }.asImageBitmap()
    }
    Image(bitmap, if (asset.endsWith("wechat.png")) "Código QR de donación voluntaria de WeChat Pay" else "Código QR de donación voluntaria de Alipay",
        modifier = modifier.fillMaxWidth().aspectRatio(ratio), contentScale = ContentScale.Fit)
}

@Composable
private fun DonationDialog(app: ModernMainActivity, asset: String) {
    val wechat = asset == "donation/wechat.png"
    C17OverlayDialog(show = true, title = if (wechat) "WeChat Pay · Donación voluntaria" else "Alipay · Donación voluntaria",
        summary = "Este módulo es siempre gratuito. Las donaciones son voluntarias y no desbloquean ni afectan a ninguna función." + ProjectContact.DONATION_NOTE, onDismissRequest = { app.donationImage = null }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                DonationImage(app, asset, if (wechat) 1490f / 2030f else 1440f / 2160f)
            }
            C17TextButton(text = "Cerrar", modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = { app.donationImage = null })
        }
    }
}

@Composable
private fun ActivationHero(app: ModernMainActivity, motionVisible: Boolean) {
    val homeTime = rememberHomeCardTime(app, motionVisible)
    val companionStatus = performanceHomeStatus(app)
    val companionPalette = homeCardPalette(companionStatus, LocalC17Dark.current)
    val thanksPalette = homeCardPalette("Muro de agradecimientos", LocalC17Dark.current)
    val safe = app.bool(StatusBarSettings.SAFE_MODE)
    val dark = LocalC17Dark.current
    val activated = app.runtimeActive || app.lspEnabled
    val background = when {
        safe -> if (dark) Color(0xff3b2d16) else Color(0xfffff2d9)
        activated -> if (dark) Color(0xff173820) else Color(0xffdffbe4)
        app.rootGranted -> if (dark) Color(0xff182d49) else Color(0xffe8f1ff)
        else -> if (dark) Color(0xff282b31) else Color(0xffeeeff3)
    }
    val accent = when {
        safe -> Color(0xffd79e25)
        activated -> if (dark) Color(0xff46d970) else Color(0xff2fce60)
        app.rootGranted -> MiuixTheme.colorScheme.primary
        else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    val mainPalette = homeCardPalette(if (safe) "Desactivado" else if (activated) "Activado" else "Pendiente de consulta", dark)
    val statusTitle = when {
        safe -> "Modo seguro"
        activated -> "Activado"
        app.rootGranted -> "Root autorizado"
        app.framework.state == AppFrameworkStatus.State.CHECKING -> "Comprobando…"
        else -> "Pendiente de activación"
    }
    val frameworkInfo = when {
        app.framework.connected -> buildString {
            append(app.framework.frameworkVersion.ifBlank { app.framework.frameworkName })
            if (app.framework.frameworkVersionCode > 0) append(" (${app.framework.frameworkVersionCode})")
            append(" · API ${app.framework.apiVersion}")
        }
        activated -> "La interfaz del sistema ha cargado el módulo."
        app.rootGranted -> "La configuración se puede editar de forma independiente."
        app.framework.state == AppFrameworkStatus.State.CHECKING -> "Conectando con el framework…"
        else -> "Toca para comprobar el estado."
    }
    // Measure the same label/title slots for both entrances, including the user's font scale.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textWidth = ((maxWidth - 12.dp) / 2 - 32.dp).coerceAtLeast(1.dp)
    val thanksTitleWidth = with(density) { textMeasurer.measure("Muro de agradecimientos", C17CardTypography.title).size.width.toDp() }
    // Only the thanks title shares space with the medal. Other cards have no medal reserve.
    val badgeWidth = (textWidth - thanksTitleWidth + 2.dp).coerceIn(32.dp, 42.dp)
    val inlineBadge = textWidth >= thanksTitleWidth + badgeWidth - 2.dp
    val titleEnd = if (inlineBadge) badgeWidth - 2.dp else 0.dp
    val labelConstraints = Constraints(maxWidth = with(density) { textWidth.roundToPx().coerceAtLeast(1) })
    val titleConstraints = Constraints(maxWidth = with(density) { (textWidth - titleEnd).roundToPx().coerceAtLeast(1) })
    val labelHeight = with(density) { maxOf(
        textMeasurer.measure("Módulo complementario", C17CardTypography.label, constraints = labelConstraints).size.height,
        textMeasurer.measure("¡Toca aquí para echar un vistazo!", C17CardTypography.label, constraints = labelConstraints).size.height).toDp() }
    val titleHeight = with(density) { maxOf(
        textMeasurer.measure(statusTitle, C17CardTypography.title, constraints = labelConstraints).size.height,
        textMeasurer.measure(companionStatus, C17CardTypography.title, constraints = labelConstraints).size.height,
        textMeasurer.measure("Muro de agradecimientos", C17CardTypography.title, constraints = titleConstraints).size.height).toDp() }
    val thanksLabelWidth = with(density) { textMeasurer.measure("¡Toca aquí para echar un vistazo!", C17CardTypography.label,
        constraints = labelConstraints).size.width.toDp() }
    val badgeHeight = badgeWidth * (17f / 13f)
    val textHeight = 28.dp + labelHeight + 3.dp + titleHeight
    val thanksHeight = when {
        !inlineBadge -> textHeight + badgeHeight + 14.dp
        // At narrow widths the medal starts below the label, with its float travel included.
        thanksLabelWidth + 6.dp > textWidth + 8.dp - badgeWidth ->
            maxOf(textHeight, 14.dp + labelHeight + 4.dp + badgeHeight + 10.dp)
        else -> textHeight
    }
    val smallCardHeight = maxOf(84.dp, textHeight, thanksHeight)
    val supportingHeight = with(density) { textMeasurer.measure(frameworkInfo, C17CardTypography.supporting,
        constraints = labelConstraints).size.height.toDp() }
    val rowHeight = maxOf(180.dp, smallCardHeight * 2 + 12.dp, textHeight + supportingHeight)
    // A measured, finite height avoids weighted intrinsic children magnifying the artwork reserve.
    Row(Modifier.fillMaxWidth().height(rowHeight),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        C17Card(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(22.dp)),
            cornerRadius = 22.dp, colors = CardDefaults.defaultColors(color = background), onClick = app::checkActivation) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.size(128.dp).align(Alignment.BottomEnd).offset(x = 24.dp, y = 35.dp),
                    contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = size.width * .075f
                        drawCircle(accent, radius = (size.minDimension - stroke) / 2f,
                            style = Stroke(width = stroke))
                        if (activated && !safe) {
                            val tick = Path().apply {
                                moveTo(size.width * .31f, size.height * .54f)
                                lineTo(size.width * .45f, size.height * .68f)
                                lineTo(size.width * .77f, size.height * .35f)
                            }
                            drawPath(tick, accent, style = Stroke(width = stroke, cap = StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        }
                    }
                    if (safe || !activated) Icon(if (safe) MiuixIcons.Pause else if (app.rootGranted) MiuixIcons.Unlock else MiuixIcons.Layers,
                        null, Modifier.size(64.dp), tint = accent)
                }
                Column(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("Módulo principal", Modifier.fillMaxWidth().heightIn(min = labelHeight),
                        style = C17CardTypography.label, color = mainPalette.secondary)
                    Spacer(Modifier.height(3.dp))
                    Text(statusTitle, Modifier.fillMaxWidth().heightIn(min = titleHeight),
                        style = C17CardTypography.title, color = mainPalette.foreground)
                    Text(frameworkInfo, style = C17CardTypography.supporting,
                        color = mainPalette.secondary)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            C17Card(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)), cornerRadius = 22.dp,
                colors = CardDefaults.defaultColors(color = companionPalette.base), onClick = app::openPerformance) {
                Box(Modifier.fillMaxSize()) {
                HomeCardBackground(companionStatus, homeTime, Modifier.matchParentSize())
                HomeHeroText("Módulo complementario", companionStatus, labelHeight, titleHeight, 0.dp,
                    companionPalette.secondary, companionPalette.foreground)
                }
            }
            C17Card(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)), cornerRadius = 22.dp,
                colors = CardDefaults.defaultColors(color = thanksPalette.base), onClick = app::openThanks) {
                Box(Modifier.fillMaxSize()) {
                HomeCardBackground("Muro de agradecimientos", homeTime, Modifier.matchParentSize(), badgeWidth = badgeWidth)
                HomeHeroText("¡Toca aquí para echar un vistazo!", "Muro de agradecimientos", labelHeight, titleHeight, titleEnd,
                    thanksPalette.secondary, thanksPalette.foreground)
                }
            }
        }
    }
    }
}

@Composable
private fun HomeHeroText(label: String, title: String, labelHeight: Dp, titleHeight: Dp,
    titleEnd: Dp, labelColor: Color, titleColor: Color) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(label, Modifier.fillMaxWidth().heightIn(min = labelHeight),
            style = C17CardTypography.label, color = labelColor)
        Spacer(Modifier.height(3.dp))
        Text(title, Modifier.fillMaxWidth().padding(end = titleEnd).heightIn(min = titleHeight),
            style = C17CardTypography.title, color = titleColor)
    }
}

@Composable
private fun ConfigurationHeader(app: ModernMainActivity) {
    val categories = listOf(SettingsCatalog.STATUSBAR, SettingsCatalog.NOTIFICATION, SettingsCatalog.CONTROL_CENTER, SettingsCatalog.LOCK_SCREEN, SettingsCatalog.OTHER)
    val labels = listOf("Barra de estado", "Panel de notificaciones", "Centro de control", "Pantalla de bloqueo", "Otros")
    // The tab row takes the search field's real height, so both bars line up.
    val density = LocalDensity.current
    var searchHeight by remember { mutableStateOf<Dp?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UiContentTabs(tabs = labels, selectedTabIndex = categories.indexOf(app.category).coerceAtLeast(0),
            onTabSelected = { app.category = categories[it] }, modifier = Modifier.fillMaxWidth(), contour = true,
            height = searchHeight)
        C17TextField(value = app.query, onValueChange = { app.query = it }, label = "Buscar en todos los ajustes", singleLine = true,
            useLabelAsPlaceholder = true, insideMargin = DpSize(16.dp, 12.dp),
            leadingIcon = { Icon(MiuixIcons.Search, null, Modifier.padding(start = 16.dp, end = 12.dp).size(20.dp)) },
            modifier = Modifier.fillMaxWidth().onSizeChanged { size ->
                if (size.height > 0) with(density) { size.height.toDp() }.let { if (searchHeight != it) searchHeight = it }
            })
    }
}

@Composable
private fun SearchResultRow(title: String, summary: String, term: String, icon: ImageVector, onClick: () -> Unit) {
    val accent = MiuixTheme.colorScheme.primary
    fun highlighted(value: String) = buildAnnotatedString {
        append(value)
        var start = value.indexOf(term, ignoreCase = true)
        while (term.isNotEmpty() && start >= 0) {
            addStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold), start, start + term.length)
            start = value.indexOf(term, start + term.length, ignoreCase = true)
        }
    }
    val shownTitle = remember(title, term, accent) { highlighted(title) }
    val shownSummary = remember(summary, term, accent) {
        val match = summary.indexOf(term, ignoreCase = true)
        val visibleSummary = if (match > 36) "…" + summary.substring(match - 18) else summary
        highlighted(visibleSummary)
    }
    C17BasicComponent(insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp), onClick = onClick,
        startAction = { Icon(icon, null, Modifier.padding(end = 12.dp).size(20.dp), tint = MiuixTheme.colorScheme.onSurface) },
        endActions = { Icon(MiuixIcons.Basic.ArrowRight, null, Modifier.padding(start = 8.dp).size(width = 10.dp, height = 16.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions) }) {
        Text(shownTitle, fontSize = MiuixTheme.textStyles.headline1.fontSize, fontWeight = FontWeight.Medium,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (summary.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(shownSummary, fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The default clock and the two-row clock exclude each other: switching one on switches the other off. */
@Composable
private fun ClockModeSwitch(app: ModernMainActivity, key: String, dual: Boolean) {
    UiGroupCard {
        UiSwitchRow(if (dual) "Activar reloj de dos filas" else "Activar reloj predeterminado", if (dual) "Al activarlo, se desactivará automáticamente el reloj predeterminado." else "Al activarlo, se desactivará automáticamente el reloj de dos filas.",
            app.bool(key), app.canEdit && !app.busy && PanelMode.freezeReason(key).isEmpty()) { app.save(key, it) }
    }
}

/** One short line for a row: the first sentence, cut at a comma if it is still long. The full text lives in 详细说明. */
private fun brief(text: String, limit: Int = 30): String {
    val sentence = text.substringBefore('\n').split('。', '；').firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    if (sentence.length <= limit) return sentence
    val cut = sentence.substring(0, limit).lastIndexOf('，')
    return if (cut >= 8) sentence.substring(0, cut) else sentence.substring(0, limit).trimEnd('，', '、', ' ') + "…"
}

/** Everything a page can say, for the 详细说明 dialog: the page text, how it interacts with others, then each item. */
private fun fullHelp(app: ModernMainActivity, group: SettingsCatalog.Group, description: String, conflicts: String): String {
    val lines = ArrayList<String>()
    lines.add(description)
    if (conflicts.isNotBlank()) lines.add(conflicts)
    val items = group.items.filter { !it.master && it.description.isNotBlank() && SettingsCatalog.applicable(it, app.values) }
        .filter { it.description != brief(it.description) }
    if (items.isNotEmpty()) lines.add("Descripción de las opciones:\n" + items.joinToString("\n") { "· ${it.title.substringBefore(" ·")}：${it.description}" })
    return lines.joinToString("\n\n")
}

@Composable
private fun DetailIntro(app: ModernMainActivity, group: SettingsCatalog.Group, master: String?, modeReason: String) {
    val description = if (group.id == "carrier") "Solo se aplica a ${when (app.category) {
        SettingsCatalog.LOCK_SCREEN -> "pantalla de bloqueo"
        SettingsCatalog.CONTROL_CENTER -> "centro de control"
        else -> "panel de notificaciones"
    }}; puede mostrar el operador, la hora o un texto personalizado." else if (group.id == "notification_icons" &&
        app.value(NotificationIconArea.MODE) == "image" && (app.value(NotificationIconArea.IMAGE_REVISION) as? String).isNullOrBlank())
        group.description + "\nAún no se ha importado ninguna imagen; se conservará el icono de notificación nativo." else group.description
    val conflicts = SettingsCatalog.conflictDescription(group.id, app.category)
    val dependency = group.masterKey?.takeIf { it.isNotBlank() }?.let { SettingsCatalog.unavailableReason(it, app.values) }.orEmpty()
    val modeLabel = when (app.panelMode) { PanelMode.CLASSIC -> "Modo actual del sistema: clásico"; PanelMode.SEPARATE -> "Modo actual del sistema: separado"; else -> "Modo actual del sistema: aún sin confirmar" }
    val short = brief(description, 56)
    val help = fullHelp(app, group, description, conflicts)
    val more = help.length > short.length + 2
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(MiuixTheme.colorScheme.primary.copy(alpha = .055f)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(short, fontSize = 14.sp, lineHeight = 20.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        val guide = SettingsOrganization.guide(group.id)
        if (guide.isNotEmpty()) Text(guide, fontSize = 13.sp, lineHeight = 19.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (app.bool(StatusBarSettings.SAFE_MODE)) Text("Pausado: el modo seguro está activado. A continuación se muestran las opciones guardadas.", fontSize = 13.sp,
            lineHeight = 19.sp, color = MiuixTheme.colorScheme.primary)
        if (group.id.startsWith("shade_wallpaper"))
            Text("Función aún no implementada", fontSize = 13.sp, color = MiuixTheme.colorScheme.primary)
        if (group.id == "fluid_cloud_capsule")
            Text("El interruptor de apariencia solo controla el aspecto; Fluid Cloud permanente tiene un interruptor independiente en los ajustes compartidos.", fontSize = 13.sp, lineHeight = 19.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (group.id == "classic_text" || group.id.startsWith("shade_wallpaper") || group.id.startsWith("notification_big_clock"))
            Text(modeLabel, fontSize = 13.sp, color = MiuixTheme.colorScheme.primary)
        if (modeReason.isNotEmpty()) Text(modeReason, fontSize = 13.sp, lineHeight = 19.sp, color = MiuixTheme.colorScheme.primary)
        if (dependency.isNotBlank()) Text(dependency, fontSize = 13.sp, lineHeight = 19.sp, color = MiuixTheme.colorScheme.primary)
        // Permanent source line of the same-app mini stack; the whole line opens the author's Coolapk page.
        if (group.id == MINI_STACK_GROUP) Text("${NotificationMiniStackSource.INSIDE} · ${NotificationMiniStackSource.LINK}",
            Modifier.clip(RoundedCornerShape(8.dp)).clickable { app.openUrl(NotificationMiniStackSource.URL) }.padding(vertical = 4.dp),
            fontSize = 13.sp, lineHeight = 19.sp, color = MiuixTheme.colorScheme.primary)
        if (more) Text("Detalles", Modifier.clip(RoundedCornerShape(8.dp))
            .clickable { app.confirmation = UiConfirmation(group.title, help, "Entendido") {} }.padding(vertical = 4.dp),
            fontSize = 13.sp, color = MiuixTheme.colorScheme.primary)
    }
    master?.let { key -> SettingsCatalog.item(key)?.let { item -> UiGroupCard { SettingsItem(app, item) } } }
    if (!app.canEdit) UiGroupCard { UiRow("Activa el módulo o concede acceso Root para modificarlo.", "Toca para ir a Inicio.", icon = MiuixIcons.Unlock) { app.closeGroup(); app.selectPage(0) } }
    }
}

@Composable
private fun DetailSettingsSection(app: ModernMainActivity, group: SettingsCatalog.Group, section: String,
    items: List<SettingsCatalog.Item>, requested: String?) {
    val advanced = SettingsOrganization.advanced(group.id, section)
    val switches = items.filter { it.type == SettingsCatalog.BOOLEAN }
    val parameters = items.filterNot { it.type == SettingsCatalog.BOOLEAN }
    val parameterCount = parameters.count { !(it.type == SettingsCatalog.COLOR && it.scene == "dark" &&
        parameters.any { other -> other.key == it.pairedKey }) && !FluidCloudCapsule.MARQUEE_COLOR_KEYS.drop(1).contains(it.key) }
    val folding = parameterCount >= 3 || advanced && parameterCount >= 2
    var expanded by rememberSaveable(group.id, section) { mutableStateOf((!advanced && (switches.firstOrNull()?.let { app.bool(it.key) } ?: true)) || items.any { it.key == requested }) }
    LaunchedEffect(requested) { if (items.any { it.key == requested }) expanded = true }
    Column {
        SectionTitle(section.substringAfter(" · "))
        UiGroupCard {
            switches.forEach { item -> SettingsItem(app, item) }
            if (folding) UiRow(if (advanced) "Parámetros avanzados" else "Ajustes de parámetros", "$parameterCount parámetros · expandir o contraer no cambia los interruptores de funciones", if (expanded) "Contraer" else "Expandir") { expanded = !expanded }
            if (!folding || expanded) parameters.forEach { item ->
                val paired = item.type == "color" && item.pairedKey.isNotEmpty() && items.any { it.key == item.pairedKey }
                if (paired && item.scene == "light") {
                    val dark = SettingsCatalog.item(item.pairedKey)
                    val componentAvailable = BigClockWidgetsSettings.textSide(item.key) < 0 || SettingsCatalog.unavailableReason(item.key, app.values).isEmpty()
                    val pairUnavailable = if (group.id.startsWith("notification_big_clock"))
                        SettingsCatalog.unavailableReason(item.key, app.values).ifEmpty { SettingsCatalog.unavailableReason(dark.key, app.values) }
                        else ""
                    UiColorPair(item.title.substringBefore(" ·"),
                        (app.value(item.key) as? Number)?.toInt() ?: 0,
                        (app.value(dark.key) as? Number)?.toInt() ?: 0,
                        app.canEdit && !app.busy && componentAvailable && pairUnavailable.isEmpty(),
                        onLight = { app.pickColor(item) }, onDark = { app.pickColor(dark) })
                    val pairNote = pairUnavailable.ifEmpty { SettingsCatalog.inactivePresentationReason(item, app.values) }
                    if (pairNote.isNotEmpty()) Text(pairNote, Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 13.sp, lineHeight = 19.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                } else if (!paired || item.scene != "dark") SettingsItem(app, item)
            }
        }
    }
}

@Composable
private fun SettingsItem(app: ModernMainActivity, item: SettingsCatalog.Item) {
    val unavailable = SettingsCatalog.unavailableReason(item.key, app.values)
    val paused = SettingsCatalog.inactivePresentationReason(item, app.values)
    // The switch always shows saved intent, even while a dependency pauses its runtime effect.
    val value = if (item.type == "numeric") SettingEditor.storedValue(item, app.values) else app.value(item.key)
    val enabled = app.canEdit && !app.busy && PanelMode.freezeReason(item.key).isEmpty() && (unavailable.isEmpty() || value == true && item.type == SettingsCatalog.BOOLEAN)
    if (item.key == NativeStatusIcons.LAYOUT) {
        UiRow(item.title, brief(item.description), "Abrir", enabled = !app.busy) { app.openHintIconOrder() }; return
    }
    if (item.key == NativeStatusIcons.PRIORITY) {
        UiRow(item.title, brief(item.description), "Arrastra para ordenar", Icons.Outlined.SwapVert, enabled = enabled) { app.editing = item }
        return
    }
    if (StatusBarLayoutSettings.orderKey(item.key)) {
        if (item.key == StatusBarLayoutSettings.key(StatusBarLayoutSettings.LABEL, "order")) {
            if (app.bool(StatusBarLayoutSettings.WIFI_LABEL_SHARED)) {
                UiRow(item.title, "Ajusta el orden compartido en la misma zona que Wi-Fi.", "Compartir con Wi-Fi", enabled = false) {}
                return
            }
            val order = (value as? Number)?.toInt() ?: 0
            UiRow(item.title, "Seguir la zona de la red móvil", listOf("Seguir al sistema", "Antes de la señal", "Después de la señal").getOrElse(order) { "Seguir al sistema" },
                enabled = enabled) { app.editing = item }
        } else {
            val order = (value as? Number)?.toInt() ?: 0
            UiRow(item.title, "En cada lado, se ordena por prioridad; el sistema decide el orden entre elementos del mismo nivel.", if (order == 0) "Orden del sistema" else "Prioridad $order",
                Icons.Outlined.SwapVert, enabled = enabled) { app.editing = item }
        }
        return
    }
    if (SettingEditor.appPickerKey(item.key)) {
        val selectedTitle = remember(item.key, value, app.clockLaunchChoices) { app.clockLaunchLabel(item.key) }
        UiRow(item.title, brief(item.description), selectedTitle, Icons.Outlined.Apps,
            enabled = enabled && app.clockLaunchEnabled(item.key)) { app.openClockLaunchPicker(item.key) }
        return
    }
    if (item.key == IconManagementSettings.KEY) {
        UiRow("Gestión de iconos", "Biblioteca de iconos, paquetes y apariencia individual", "Abrir", Icons.Outlined.Collections, enabled = !app.busy) { app.openIconManagement() }
        return
    }
    ShadeWallpaperSettings.sceneOfRevisionKey(item.key)?.let { scene ->
        ShadeWallpaperCard(app, scene)
        return
    }
    if (item.key == NotificationIconOverrides.RULES) {
        UiRow(item.title, "Sustituir el icono por texto, emoji o el icono de otra aplicación", "Abrir", Icons.Outlined.EmojiEmotions, enabled = enabled) { app.openNotificationOverrides() }
        return
    }
    if (item.key == FluidCloudCapsule.MARQUEE_PRESET) {
        MarqueePresetRow(app, item, enabled, unavailable)
        return
    }
    if (item.key == FluidCloudCapsule.MARQUEE_COLOR_1) {
        MarqueeColorsRow(app, enabled, unavailable)
        return
    }
    // 颜色 2 到 6 和颜色 1 合并成一排圆点，不再各占一行。
    if (FluidCloudCapsule.MARQUEE_COLOR_KEYS.drop(1).contains(item.key)) return
    if (item.key == StatusBarSettings.FONT_MODE) {
        UiRow("Origen del texto", "", app.currentFontTitle(), Icons.Outlined.TextFields,
            enabled = !app.busy) { app.chooseOpenFont() }
        return
    }
    when (item.type) {
        "boolean" -> UiSwitchRow(item.title, if (unavailable.isNotEmpty() || paused.isNotEmpty())
            (if (value == true) "Activado y guardado · " else "") + unavailable.ifEmpty { paused } else brief(item.description), value == true, enabled) { app.save(item.key, it) }
        "numeric" -> {
            val editingKey = StatusBarLayoutSettings.editingKey(item.key, app.values)
            var current by remember(item.key, editingKey, value) { mutableStateOf<Number>((value as? Number) ?: 0f) }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(SettingsCatalog.displayTitle(item, app.values), Modifier.weight(1f).clickable(enabled = enabled) { app.editing = item }, fontSize = 16.sp)
                    Text(SettingEditor.formatted(item, current), modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = enabled) { app.editing = item }.padding(horizontal = 8.dp, vertical = 6.dp), fontSize = 14.sp, color = MiuixTheme.colorScheme.primary)
                }
                C17Slider(value = current.toFloat().coerceIn(item.min, item.max), onValueChange = {
                    current = if (item.key == "qs_tile_corner_radius") kotlin.math.round(it * 2f) / 2f else roundToStep(it, item.step)
                }, valueRange = item.min..item.max, enabled = enabled, onValueChangeFinished = {
                    app.save(item.key, current)
                })
                val note = unavailable.ifEmpty { paused }.ifEmpty {
                    if (StatusBarLayoutSettings.marginKey(item.key) && item.groupId in listOf("wifi", "label") &&
                        app.bool(StatusBarLayoutSettings.WIFI_LABEL_SHARED)) "Se comparte con Wi-Fi y el tipo de red. Los cambios se sincronizan en ambas páginas."
                    else brief(item.description)
                }
                if (note.isNotEmpty()) Text(note, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        "color" -> C17ArrowPreference(title = item.title.substringBefore(" ·"), summary = null, enabled = enabled,
            onClick = { app.pickColor(item) }, endActions = {
                Box(Modifier.size(30.dp).clip(CircleShape).background(Color((value as? Number)?.toInt() ?: 0))
                    .border(0.75.dp, MiuixTheme.colorScheme.dividerLine, CircleShape))
            })
        "font" -> {
            UiRow("Importar fuente", "TTF / OTF / TTC", "Importar", Icons.Outlined.FileUpload,
                enabled = app.canEdit && !app.busy) { app.chooseFont() }
        }
        "image" -> UiRow(item.title, brief(item.description), value?.toString()?.takeIf { it.isNotBlank() }
            ?: if ((app.value(NotificationIconArea.IMAGE_REVISION) as? String).isNullOrBlank()) "Seleccionar imagen" else "Imagen importada",
            icon = Icons.Outlined.Image, enabled = enabled) { app.chooseNotificationIcon() }
        else -> UiRow(item.title, unavailable.ifEmpty { paused }.ifEmpty { brief(item.description) }, displayValue(item, value), enabled = enabled) { app.editing = item }
    }
}

@Composable
private fun EditSettingDialog(app: ModernMainActivity, item: SettingsCatalog.Item) {
    if (item.key == NativeStatusIcons.LAYOUT) { LaunchedEffect(item.key) { app.editing = null; app.openHintIconOrder() }; return }
    if (item.key == IconManagementSettings.KEY) { LaunchedEffect(item.key) { app.editing = null; app.openIconManagement() }; return }
    if (item.key == NotificationIconOverrides.RULES) { LaunchedEffect(item.key) { app.editing = null; app.openNotificationOverrides() }; return }
    if (item.key == NativeStatusIcons.PRIORITY) { HintPriorityDialog(app); return }
    if (StatusBarLayoutSettings.orderKey(item.key)) {
        if (item.key == StatusBarLayoutSettings.key(StatusBarLayoutSettings.LABEL, "order")) {
            val selected = (app.value(item.key) as? Number)?.toInt() ?: 0
            UiChoiceDialog(title = "Orden del tipo de red y la señal", values = listOf("0", "1", "2"),
                labels = listOf("Seguir al sistema", "Antes de la señal", "Después de la señal"), selected = selected.toString(),
                description = "El tipo de red sigue la zona de la red móvil; no fuerza la visualización de un tipo de red oculto por el sistema.",
                onDismiss = { app.editing = null }, onChoice = { app.save(item.key, it.toFloat()) })
        } else LaunchedEffect(item.key) { app.editing = null; app.openGroup(StatusBarLayoutSettings.GROUP) }
        return
    }
    val editor = remember(item.key, app.values) { SettingEditor.forItem(item, app.values) }
    val displayTitle = SettingsCatalog.displayTitle(item, app.values)
    if (item.type == "options") {
        UiChoiceDialog(displayTitle, item.values.toList(), item.labels.toList(), app.value(item.key)?.toString() ?: "",
            description = editor.context + if (item.description.isBlank()) "" else "\n${item.description}",
            optionDisabledReason = { app.optionUnavailableReason(item.key, it) },
            onDismiss = { app.editing = null }) {
            if (it == "custom" && (item.key == StatusBarSettings.FONT_MODE || NativeNetworkBadgeSettings.STRINGS.containsKey(item.key) || item.key == NotificationBigClockSettings.FONT || item.key == NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.FONT))
                && !ConfigTransfer.customFontAvailable(app)) {
                app.toast("Primero importa una fuente personalizada desde la página Fuentes de texto.")
            } else app.save(item.key, it)
            app.editing = null
        }
    } else UiInputDialog(displayTitle, editor.initial, editor.description, editor.numeric,
        label = editor.label, context = editor.context, valueSummary = editor.valueSummary,
        integer = editor.integer, allowNegative = !editor.integer && item.key != QsTileCorners.RADIUS,
        confirmLabelForValue = { input ->
            if (editor.numeric) {
                val proposed = runCatching { SettingsCatalog.customNumber(item, input) }.getOrNull()
                if (proposed != null && SettingsCatalog.requiresNumericTrial(item, proposed)) "Probar durante 20 segundos" else "Aceptar"
            } else "Aceptar"
        },
        pattern = editor.pattern, preview = if (editor.previewable) { input -> SettingEditor.preview(item, input) } else null,
        presets = remember(item.key) { SettingEditor.presets(item).map { it[0] to it[1] } },
        onDismiss = { app.editing = null }) { input ->
        val result: Any = if (item.type == "numeric") {
            try { SettingsCatalog.customNumber(item, input) } catch (error: IllegalArgumentException) { return@UiInputDialog error.message ?: "Introduce un número finito válido." }
        } else input
        val error = SettingsCatalog.validationError(item, result)
        if (error != null) error else {
            if (result is Float && SettingsCatalog.requiresNumericTrial(item, result)) app.tryNumber(item, result)
            else { app.save(item.key, result); app.editing = null }
            null
        }
    }
}

@Composable
private fun AboutHero(app: ModernMainActivity) {
    val version = remember(app) { app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "" }
    val versionLabel = remember(version) {
        val suffix = version.substringBefore('+').substringAfter('-', "").lowercase(Locale.ROOT)
        if (suffix.split('.', '-').any { it.startsWith("alpha") || it.startsWith("beta") || it.startsWith("rc") }) "Versión beta" else "Versión estable"
    }
    UiGroupCard {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val iconPx = with(LocalDensity.current) { 96.dp.roundToPx() }
            val icon = remember(app, iconPx) { drawableBitmap(app.packageManager.getApplicationIcon(app.packageName), iconPx) }
            EasterEggIcon(icon, app.appDisplayName, Modifier.size(56.dp))
            Spacer(Modifier.height(14.dp))
            Text(app.appDisplayName, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text("$version · $versionLabel", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Spacer(Modifier.height(6.dp))
            Text("Autor: ${ProjectContact.AUTHOR}", fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Text("Siempre gratis · donaciones voluntarias", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

@Composable
private fun AboutActions(app: ModernMainActivity) {
    var section by rememberSaveable { mutableStateOf<String?>(null) }
    fun toggle(name: String) { section = if (section == name) null else name }
    fun fold(name: String) = if (section == name) "Contraer" else "Expandir"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UiGroupCard {
            UiRow("Muro de agradecimientos", "Gracias a todas las personas que colaboran", "Ver", Icons.Outlined.FavoriteBorder) { app.openThanks() }
        }
        UiGroupCard {
            UiRow("Buscar actualizaciones", app.updateMessage, if (app.checkingUpdates) "Comprobando…" else "Comprobar", Icons.Outlined.SystemUpdate,
                enabled = !app.checkingUpdates && !app.busy) { app.checkUpdates() }
        }
        UiGroupCard {
            UiRow("Contacto y donaciones", value = fold("contact"), icon = Icons.Outlined.FavoriteBorder) { toggle("contact") }
            if (section == "contact") {
                UiRow("Contactar con el autor", "QQ ${ProjectContact.QQ}", "Copiar", Icons.Outlined.ContentCopy) { app.toast(if (ProjectContact.copyQq(app)) "Número de QQ copiado." else "No se pudo copiar.") }
                UiRow("CoolAPK", "Perfil del autor: @${ProjectContact.COOLAPK}", "Abrir", Icons.Outlined.OpenInNew) { app.openUrl(ProjectContact.COOLAPK_URL) }
                UiRow("Información sobre donaciones", ProjectContact.DONATION_NOTE, icon = Icons.Outlined.Info) {
                    app.confirmation = UiConfirmation("Información sobre donaciones", ProjectContact.DONATION_NOTE + "\n\nContacto del autor: QQ ${ProjectContact.QQ}", "Entendido") {}
                }
                UiRow("Donar con WeChat Pay", value = "Ver", icon = Icons.Outlined.FavoriteBorder) { app.donationImage = "donation/wechat.png" }
                UiRow("Donar con Alipay", value = "Ver", icon = Icons.Outlined.FavoriteBorder) { app.donationImage = "donation/alipay.jpg" }
                UiRow("Declaración de gratuidad", "Siempre gratis; las donaciones son completamente voluntarias.", icon = Icons.Outlined.Info) { app.confirmation = UiConfirmation("Declaración de gratuidad", FreeNotice.DESCRIPTION, "Entendido") {} }
            }
        }
        UiGroupCard {
            UiRow("Ajustes de interfaz", "Estilo de interfaz, colores, tamaño de texto y nombre", app.ui.uiStyleTitle, Icons.Outlined.Palette) { app.openUiSettings() }
        }
        UiGroupCard {
            UiRow("Módulo complementario de optimización", "Instalación y optimización automática", performanceHomeStatus(app), Icons.Outlined.Speed) { app.openPerformance() }
        }
        UiGroupCard {
            UiRow("Administrar configuración", "Copia de seguridad, compartir e importar", value = fold("config"), icon = Icons.Outlined.Settings) { toggle("config") }
            if (section == "config") {
                UiRow("Exportar configuración", "Guardar en un archivo; puedes exportar solo algunas páginas.", icon = Icons.Outlined.FileUpload, enabled = app.canEdit && !app.busy) { app.chooseExport() }
                UiRow("Compartir configuración", "Enviar a amigos u otras aplicaciones; puedes compartir solo algunas páginas.", icon = Icons.Outlined.Share, enabled = app.canEdit && !app.busy) { app.shareConfig() }
                UiRow("Importar configuración", "Tras leer un archivo, puedes importar solo algunas páginas.", icon = Icons.Outlined.FileDownload, enabled = app.canEdit && !app.busy) { app.chooseImport() }
                UiRow("Eliminar toda la configuración", "No se puede deshacer. Se recomienda exportar una copia de seguridad primero.", icon = Icons.Outlined.DeleteOutline, enabled = app.canEdit && !app.busy) { app.deleteAllSettings() }
            }
        }
        UiGroupCard {
            UiRow("Diagnóstico de problemas", "Registros y estado de los hooks", "Ver", Icons.Outlined.Description) { app.openDiagnostics() }
        }
        UiGroupCard {
            UiRow("Proyecto y código abierto", value = fold("project"), icon = Icons.Outlined.Code) { toggle("project") }
            if (section == "project") {
                UiRow("Código fuente del proyecto", "SANWU5/c17-statusbar", icon = Icons.Outlined.Code) { app.openUrl("https://github.com/SANWU5/c17-statusbar") }
                UiRow("Versiones estables", icon = Icons.Outlined.OpenInNew) { app.openLatestRelease() }
                UiRow("Licencias de código abierto", icon = Icons.Outlined.Article) {
                    app.confirmation = UiConfirmation("Licencias de código abierto", "C17 Statusbar: GPL-3.0-only\nMiuix: Apache-2.0\nCOUI: suqi8/coui · Apache-2.0 (basado en Miuix)\nBarra inferior de vidrio líquido: adaptación de los ejemplos de Miuix y Kyant0/AndroidLiquidGlass · Apache-2.0\nFuentes: SIL Open Font License 1.1\nReferencia técnica de esquinas redondeadas: MCGA · Zhuangzhi Meng (Gustate XiaoMeng) · GPL-3.0-or-later\nLa aplicación conserva las licencias y los avisos de copyright completos.", "Entendido") {}
                }
                UiRow("Miuix", value = "0.9.4", icon = Icons.Outlined.Palette) { app.openUrl("https://github.com/compose-miuix-ui/miuix") }
                UiRow("COUI", "Componentes con estilo ColorOS", value = "1.1.0", icon = Icons.Outlined.Palette) { app.openUrl("https://github.com/suqi8/coui") }
                UiRow("Agradecimientos por las funciones", NotificationMiniStackSource.SHORT, "CoolAPK", Icons.Outlined.OpenInNew) {
                    app.confirmation = UiConfirmation("Agradecimientos por las funciones", NotificationMiniStackSource.DECLARATION + "\n\n" + NotificationMiniStackSource.TECHNICAL_ORIGIN
                        + "Los agradecimientos no sustituyen los avisos de licencia.", "Entendido", linkLabel = "${NotificationMiniStackSource.LINK} (CoolAPK: @${NotificationMiniStackSource.AUTHOR})",
                        linkUrl = NotificationMiniStackSource.URL) {}
                }
            }
        }
        UiGroupCard {
            UiSwitchRow("Ocultar icono de la aplicación", "Puedes acceder y restaurarlo desde los ajustes del módulo en LSPosed.", app.desktopIconHidden,
                enabled = app.desktopIconAvailable && !app.freeNoticeRequired && !app.busy && !app.maintenanceRunning,
                onChecked = app::requestDesktopIconHidden)
        }
    }
}

@Composable
private fun HintPriorityDialog(app: ModernMainActivity) {
    val saved = app.value(NativeStatusIcons.PRIORITY)?.toString() ?: ""
    var ordered by remember(saved) { mutableStateOf(NativeStatusIcons.editablePriority(saved).toList()) }
    var dragging by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val step = with(density) { 56.dp.toPx() }
    val move: (String, Int) -> Boolean = { slot, delta ->
        val index = ordered.indexOf(slot)
        val target = (index + delta).coerceIn(0, ordered.lastIndex)
        if (index < 0 || index == target) false else {
            ordered = ordered.toMutableList().apply { removeAt(index); add(target, slot) }
            true
        }
    }
    C17OverlayDialog(show = true, title = "Prioridad de visualización", onDismissRequest = { app.editing = null }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Mantén pulsado el nombre del icono y arrástralo hacia arriba o abajo; los que estén más arriba tendrán prioridad. No se forzará la visualización de indicadores que el sistema no tenga activos.", fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState(), enabled = dragging == null)) {
                ordered.forEach { slot -> key(slot) {
                    var distance by remember { mutableFloatStateOf(0f) }
                    Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (dragging == slot) MiuixTheme.colorScheme.primary.copy(alpha = .12f) else Color.Transparent)
                        .semantics { customActions = listOf(CustomAccessibilityAction("Mover arriba") { move(slot, -1) }, CustomAccessibilityAction("Mover abajo") { move(slot, 1) }) }
                        .pointerInput(slot) {
                            detectDragGesturesAfterLongPress(onDragStart = { dragging = slot; distance = 0f },
                                onDragEnd = { dragging = null }, onDragCancel = { dragging = null }) { change, amount ->
                                change.consume(); distance += amount.y
                                while (distance >= step) { if (!move(slot, 1)) { distance = 0f; break }; distance -= step }
                                while (distance <= -step) { if (!move(slot, -1)) { distance = 0f; break }; distance += step }
                            }
                        }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(NativeStatusIcons.priorityLabel(slot), Modifier.weight(1f), fontSize = 16.sp)
                        Icon(Icons.Outlined.DragHandle, "Mantener pulsado y arrastrar", Modifier.size(22.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                C17TextButton(text = "Seguir al sistema", modifier = Modifier.weight(1f), onClick = { app.save(NativeStatusIcons.PRIORITY, ""); app.editing = null })
                C17TextButton(text = "Aceptar", modifier = Modifier.weight(1f), onClick = { app.save(NativeStatusIcons.PRIORITY, ordered.joinToString(",")); app.editing = null })
            }
        }
    }
}

@Composable private fun SectionTitle(title: String) { Text(title, Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp), fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
private fun concise(description: String) = brief(description, 34)
private fun carrierScene(category: String) = when (category) {
    SettingsCatalog.LOCK_SCREEN -> "lockscreen"
    SettingsCatalog.CONTROL_CENTER -> "control"
    else -> "notification"
}
private fun carrierPrefix(category: String) = when (category) {
    SettingsCatalog.LOCK_SCREEN -> CarrierPanels.LOCKSCREEN
    SettingsCatalog.CONTROL_CENTER -> CarrierPanels.CONTROL
    else -> CarrierPanels.NOTIFICATION
}
private fun destinationTitle(group: SettingsCatalog.Group, category: String): String =
    if (group.id != "carrier") group.title else when (category) {
        SettingsCatalog.LOCK_SCREEN -> "Personalizar el texto del operador en la pantalla de bloqueo"
        SettingsCatalog.CONTROL_CENTER -> "Personalizar el texto del operador en el centro de control"
        else -> "Personalizar el texto del operador en el panel de notificaciones"
    }
private fun destinationStatus(app: ModernMainActivity, group: SettingsCatalog.Group, category: String): String = when {
    app.bool(StatusBarSettings.SAFE_MODE) && destinationEnabled(app, group, category) -> "Pausado por el modo seguro"
    group.id == "status_icon_library" -> if (destinationEnabled(app, group, category)) "Configurado" else "Seguir al sistema"
    destinationEnabled(app, group, category) -> "Activado"
    else -> "Desactivado"
}
private fun destinationEnabled(app: ModernMainActivity, group: SettingsCatalog.Group, category: String? = null): Boolean =
    when (group.id) {
        "status_icon_library" -> app.iconManagementState().let { it.scopePacks.isNotEmpty() || it.overrides.isNotEmpty() || it.appearances.isNotEmpty() || it.animations.isNotEmpty() }
        "carrier" -> if (category != null) app.bool(carrierPrefix(category) + "_enabled")
            else CarrierPanels.GROUPS.any { app.bool(it + "_enabled") }
        "native_network_badge" -> app.bool(group.masterKey) || app.bool(NativeDataActivity.MASTER)
        "notification_clear_landscape" -> app.bool(group.masterKey) || app.bool(NotificationClearAppearance.LANDSCAPE_MASTER)
        "classic_text" -> app.bool(group.masterKey) || app.bool(ClassicTextSettings.CLOCK_MASTER)
        "clock" -> app.bool(group.masterKey) || app.bool(ClockLayoutSettings.DUAL_ENABLED)
        "fluid_cloud_style" -> app.bool(FluidCloudStyle.MASTER) && (app.bool(FluidCloudStyle.FOLLOW_NOTIFICATION) || app.bool(FluidCloudStyle.DESKTOP_BACKGROUND))
        "fluid_cloud_capsule" -> app.bool(group.masterKey) || app.bool(CapsulePersistence.ENABLED)
        else -> group.masterKey?.takeIf { it.isNotBlank() }?.let(app::bool) ?: false
    }
private fun formatNumber(value: Float): String = if (kotlin.math.abs(value) >= 1e7f || value != 0f && kotlin.math.abs(value) < .001f) value.toString()
    else java.math.BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
private fun roundToStep(value: Float, step: Float): Float = NumericInput.parse(value.toString(), if (step >= 1f) 0 else 2)
private fun displayValue(item: SettingsCatalog.Item, value: Any?): String {
    val index = item.values.indexOf(value?.toString())
    return if (index >= 0) item.labels[index] else value?.toString()?.take(24) ?: "Predeterminado"
}
private fun groupIcon(id: String): ImageVector = when (id) {
    "speed" -> Icons.Outlined.Speed
    "data" -> Icons.Outlined.SignalCellularAlt
    "wifi" -> Icons.Outlined.Wifi
    "label" -> C17SectionIcons.Network5G
    "native_network_badge" -> Icons.Outlined.TextFields
    "network_order", StatusBarLayoutSettings.GROUP -> Icons.Outlined.SwapHoriz
    "clock", "shade_clock", "notification_big_clock", "notification_big_clock_landscape", "lockscreen_date", "classic_text" -> Icons.Outlined.Schedule
    "lockscreen_lock_icon" -> Icons.Outlined.Lock
    "lockscreen_status_blur" -> Icons.Outlined.BlurOn
    "fluid_cloud_accent", "fluid_cloud_style", "fluid_cloud_capsule" -> Icons.Outlined.Palette
    "carrier" -> Icons.Outlined.CellTower
    "font" -> Icons.Outlined.TextFields
    "battery" -> Icons.Outlined.BatteryFull
    "tiles" -> Icons.Outlined.Swipe
    "qs_appearance" -> Icons.Outlined.Palette
    "tile_2x1_fill" -> Icons.Outlined.ViewModule
    "tile_corners" -> Icons.Outlined.RoundedCorner
    "tile_icon_size" -> Icons.Outlined.GridView
    QsEntrances.GROUP -> Icons.Outlined.Edit
    "shade_status_icons" -> Icons.Outlined.SwapHoriz
    "notification_clear", "notification_clear_landscape" -> Icons.Outlined.DeleteOutline
    "notification_icons" -> C17SectionIcons.NotificationCenter
    "notification_icon_overrides" -> Icons.Outlined.EmojiEmotions
    "status_icon_library" -> Icons.Outlined.Collections
    "shade_wallpaper_portrait", "shade_wallpaper_landscape" -> Icons.Outlined.Wallpaper
    "notification_group_stack", "notification_stack", MINI_STACK_GROUP -> MiuixIcons.Layers
    "status_hint_icons" -> Icons.Outlined.Settings
    "qs_media" -> Icons.Outlined.MusicNote
    else -> Icons.Outlined.Tune
}
