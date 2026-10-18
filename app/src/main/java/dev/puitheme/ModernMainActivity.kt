// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme

import android.content.ClipData
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.Executors

/** Settings app with selectable Miuix and COUI component systems. */
class ModernMainActivity : ComponentActivity() {
    var page by mutableIntStateOf(0)
    var freeNoticeRequired by mutableStateOf(true)
        private set
    var donationImage by mutableStateOf<String?>(null)
    /** 鸣谢墙: opened from the home card, free notice and 关于; the list is the last valid download or the APK's own. */
    var thanksOpen by mutableStateOf(false)
    var thanksWall by mutableStateOf<ThanksWall.Wall?>(null)
        private set
    var thanksStatus by mutableStateOf("")
        private set
    var thanksRefreshing by mutableStateOf(false)
        private set
    /** The launcher entry carries the old name (关于 → 恢复旧名称). */
    var legacyName by mutableStateOf(false)
        private set
    var selectedGroup by mutableStateOf<String?>(null)
    var query by mutableStateOf("")
    var category by mutableStateOf("statusbar")
    var values by mutableStateOf<Map<String, *>>(emptyMap<String, Any>())
        private set
    var runtimeActive by mutableStateOf(false)
        private set
    var runtimeMessage by mutableStateOf("待注入")
        private set
    private var checkingRuntime = false
    var performanceReport by mutableStateOf<RootPerformanceModule.Snapshot?>(null)
        private set
    var performanceOpen by mutableStateOf(false)
    var performanceMessage by mutableStateOf("待确认")
        private set
    var performanceDetail by mutableStateOf("")
        private set
    var performanceBusy by mutableStateOf(false)
        private set
    var performanceAction by mutableStateOf<RootPerformanceModule.Action?>(null)
        private set
    private var checkingPerformance = false
    var performanceError by mutableStateOf(false)
        private set
    private var performanceRequest: RootPerformanceModule.Request? = null
    private var performanceReadRequest: RootPerformanceModule.Request? = null
    private var performanceWritePending = false
    private var performanceSubscription: RootPerformanceModule.Subscription? = null
    var framework by mutableStateOf(AppFrameworkStatus.current())
        private set
    val lspEnabled get() = framework.connected && framework.compatible
    var rootGranted by mutableStateOf(false)
        private set
    var rootMessage by mutableStateOf("尚未授予 Root 权限")
        private set
    var checkingRoot by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var checkingUpdates by mutableStateOf(false)
        private set
    var updateMessage by mutableStateOf("手动查看 GitHub 最新正式发布")
        private set
    private var latestUpdate: GitHubUpdates.Result? = null
    private var updateRequest: AppUpdateInstaller.Request? = null
    var maintenanceRunning by mutableStateOf(false)
        private set
    var desktopIconHidden by mutableStateOf(false)
        private set
    var desktopIconAvailable by mutableStateOf(true)
        private set
    var confirmation by mutableStateOf<UiConfirmation?>(null)
    var editing by mutableStateOf<SettingsCatalog.Item?>(null)
    /** 排序页的区域选择仍由最上层原生弹窗宿主管理。 */
    var statusBarOrderChoice by mutableStateOf<StatusBarOrderChoice?>(null)
    var statusBarOrderCloseRequest: (() -> Unit)? = null
    var hintIconOrderRequested by mutableStateOf(false)
    var colorEditing by mutableStateOf<SettingsCatalog.Item?>(null)
    var fontCatalogOpen by mutableStateOf(false)
    /** Selecting a font changes only this draft until its scopes are applied. */
    var fontScopeSelection by mutableStateOf<FontScopeSelection?>(null)
        private set
    /** 摄像头区域流体云：跑马灯预设选择弹窗。 */
    var marqueePickerOpen by mutableStateOf(false)
    /** 分享、导出、导入配置前“是否自定义项”的选择；为空表示没有在选择。 */
    var configScope by mutableStateOf<ConfigScopeRequest?>(null)
        private set
    /** 导出时选中的页面（空表示全部）；文件选择器返回前一直保留。 */
    private var exportSections: Set<String>? = null
    var fontRenameTarget by mutableStateOf<FontLibrary.Entry?>(null)
    var fontLibraryEntries by mutableStateOf<List<FontLibrary.Entry>>(emptyList())
        private set
    private var fontLibraryRequest = 0
    var notificationOverridesOpen by mutableStateOf(false)
    var clockLaunchEditing by mutableStateOf<String?>(null)
    var clockLaunchChoices by mutableStateOf<List<ApplicationChoice>>(emptyList())
        private set
    var clockLaunchLoading by mutableStateOf(false)
        private set
    var clockLaunchDefault by mutableStateOf<ApplicationChoice?>(null)
        private set
    private var clockLaunchRequest = 0
    var iconManagementOpen by mutableStateOf(false)
    var iconManagementDestination by mutableStateOf(IconManagementDestination())
    var iconManagementBackRequest: (() -> Unit)? = null
    var iconMigrationNotice by mutableStateOf<String?>(null)
        private set
    var iconMigrationCompleted by mutableStateOf(false)
        private set
    var iconLibrariesLoading by mutableStateOf(false)
        private set
    var iconPendingImport by mutableStateOf<IconManagementRepository.PreparedPack?>(null)
    var iconPendingBuiltin by mutableStateOf<String?>(null)
    var iconImportName by mutableStateOf("")
    var iconImportError by mutableStateOf("")
    var iconImportExisting by mutableStateOf<IconManagementRepository.Pack?>(null)
        private set
    var iconLibraryTarget by mutableStateOf<String?>(null)
        internal set
    private var iconLibraryRequest = 0
    private val iconResourcesLock = Any()
    private val preparedIconImports = mutableSetOf<IconManagementRepository.PreparedPack>()
    /** The picture being turned and cut (null: no editor open). */
    var wallpaperEdit by mutableStateOf<WallpaperEdit?>(null)
    var panelMode by mutableStateOf(PanelMode.UNKNOWN)
        private set
    private var batteryPercentStyle by mutableIntStateOf(0)
    var iconPacks by mutableStateOf<List<IconManagementRepository.Pack>>(emptyList())
        private set
    var applicationChoices by mutableStateOf<List<ApplicationChoice>>(emptyList())
        private set
    var iconPackDownloading by mutableStateOf(false)
        private set
    private var iconPackCancellation: IconManagementDownload.Cancellation? = null
    var downloadingFont by mutableStateOf<FontCatalog.Entry?>(null)
        private set
    var fontDownloadPercent by mutableIntStateOf(0)
        private set
    private var fontCancellation: FontDownloadRepository.Cancellation? = null
    val canEdit get() = !freeNoticeRequired && !maintenanceRunning && (runtimeActive || rootGranted || lspEnabled)
    @Volatile private var resumed = false
    @Volatile private var destroyed = false
    private lateinit var raw: SharedPreferences
    private lateinit var preferences: SharedPreferences
    private var probe: ModuleRuntimeStatus.ProbeHandle? = null
    private var frameworkSubscription: AppFrameworkStatus.Subscription? = null
    private var rootRequest: RootAccess.Request? = null
    private var restartRequest: SystemUiRestart.Request? = null
    private lateinit var navHost: ComposeView
    private var navigationInsets by mutableStateOf(PaddingValues())
    var navigationHeight by mutableFloatStateOf(72f)
        private set
    /** Look of this app only (theme, colour scheme, edge fade, text size); never sent to SystemUI. */
    val ui: UiSettings by lazy(LazyThreadSafetyMode.NONE) { UiSettings(this) }
    /** 关于 → 界面设置 is a page of its own. */
    var uiSettingsOpen by mutableStateOf(false)
    var diagnosticsOpen by mutableStateOf(false)
    var diagnostics by mutableStateOf<DiagnosticCenter.Result?>(null)
    var diagnosticsHooks by mutableStateOf<android.os.Bundle?>(null)
    var diagnosticsLoading by mutableStateOf(false)
    var diagnosticsStats by mutableStateOf<DiagnosticCenter.Stats?>(null)
        private set
    /** Floating liquid glass navigation (界面设置). Settings-app UI only. */
    var glassNavigation by mutableStateOf(false)
        private set
    /** Same visibility rule for both navigation styles; the glass bar lives in the page composition. */
    var navigationShown by mutableStateOf(false)
        private set
    private lateinit var modalLayer: ComposeView
    private lateinit var backCallback: OnBackPressedCallback
    private var keyboardOpen = false
    private val handler = Handler(Looper.getMainLooper())
    private val homeStatus = HomeStatusRefreshPolicy()
    private var homeStatusVisible = false
    private var runtimeRequestId = 0L
    private val homeStatusPoll = Runnable { refreshHomeStatus() }
    private val work = Executors.newSingleThreadExecutor()
    private var pendingFile: Pair<String, Uri>? = null
    private var wallpaperImportScene: String? = null
    private val wallpaperResourcesLock = Any()
    private val preparedWallpapers = mutableSetOf<ShadeWallpaperRepository.PreparedWallpaper>()
    private val panelModeObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) { refreshPanelMode() }
    }
    private fun refreshPanelMode() {
        if (!destroyed) panelMode = PanelMode.query(this).getString("mode", PanelMode.UNKNOWN)
    }
    private val batteryPercentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) { refreshBatteryPercent() }
    }
    private fun refreshBatteryPercent() {
        if (!destroyed) batteryPercentStyle = BatteryNativeStyle.percentStyle(this)
    }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        handler.post {
            if (!destroyed) {
                values = raw.all
                handler.removeCallbacks(notifySettings)
                if (SettingsSnapshot.notificationAllowed(raw)) handler.post(notifySettings)
            }
        }
    }
    private val notifySettings = Runnable {
        if (!destroyed && SettingsSnapshot.notificationAllowed(raw))
            contentResolver.notifyChange(Uri.parse(StatusBarSettings.CONTENT_URI), null)
    }
    private val importConfig = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) queueFile("import", uri)
    }
    private val exportConfig = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) queueFile("export", uri) else exportSections = null
    }
    private val importFont = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) queueFile("font", uri)
    }
    private val importNotificationIcon = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) queueFile("notification_icon", uri)
    }
    private val importIconPack = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) queueFile("icon_pack", uri)
    }
    /** The album: the system photo picker (or, where there is none, the document picker) for the panel's picture. */
    private val pickWallpaperImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val scene = wallpaperImportScene
        if (uri != null && scene != null && ShadeWallpaperSettings.available() && ShadeWallpaperSettings.validScene(scene)) wallpaperEdit = WallpaperEdit(scene, uri)
    }
    private val exportLog = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) work.execute {
            runCatching { ModuleDiagnostics.writeExport(this, uri) }
                .onSuccess { runOnUiThread { toast("日志已保存") } }
                .onFailure { runOnUiThread { toast("日志保存失败：${it.message}") } }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        freeNoticeRequired = !FreeNotice.accepted(this)
        raw = StatusBarSettings.preferences(this)
        work.execute { RetiredClockWidgets.release(applicationContext) }
        AppFrameworkStatus.start(this)
        refreshPanelMode()
        refreshBatteryPercent()
        runCatching {
            contentResolver.registerContentObserver(Settings.System.getUriFor(PanelMode.SETTING), false, panelModeObserver)
            contentResolver.registerContentObserver(Settings.System.getUriFor(PanelMode.DEFAULT_SETTING), false, panelModeObserver)
        }
        runCatching {
            contentResolver.registerContentObserver(Settings.System.getUriFor(BatteryNativeStyle.PERCENT_SETTING), false, batteryPercentObserver)
        }
        wallpaperImportScene = savedInstanceState?.getString("wallpaper_scene")?.takeIf { ShadeWallpaperSettings.validScene(it) }
        preferences = ActivationGuardPreferences(raw, { resumed && canEdit && !destroyed }, {
            handler.post { toast("激活模块或授予 Root 后即可保存配置") }
        }).withPersistence(this) { handler.post { toast("设置保存失败，请稍后重新保存") } }
        values = raw.all
        raw.registerOnSharedPreferenceChangeListener(listener)
        frameworkSubscription = AppFrameworkStatus.observe { status ->
            if (!destroyed) {
                framework = status
                if (resumed) replayFile()
            }
        }
        page = savedInstanceState?.getInt("page") ?: 0
        uiSettingsOpen = savedInstanceState?.getBoolean("ui_settings_open") ?: false
        val restoredGroup = savedInstanceState?.getString("group")
        selectedGroup = (if (restoredGroup == "network") "wifi" else restoredGroup)
            ?.takeIf { SettingsCatalog.group(it) != null }
        performanceOpen = savedInstanceState?.getBoolean("performance_open") == true && page == 0 &&
            selectedGroup == null && !freeNoticeRequired
        query = savedInstanceState?.getString("query") ?: ""
        category = savedInstanceState?.getString("category") ?: "statusbar"
        savedInstanceState?.getString("file_uri")?.let {
            pendingFile = (savedInstanceState.getString("file_kind") ?: "import") to Uri.parse(it)
        }
        savedInstanceState?.getStringArrayList("export_sections")?.let { exportSections = it.toSet() }
        glassNavigation = GlassNavigationPreference.read(this)
        ui.note("start")
        val density = resources.displayMetrics.density
        val root = FrameLayout(this)
        val content = ComposeView(this).apply {
            consumeWindowInsets = false
            setContent { C17Theme(ui) { C17MainContent(this@ModernMainActivity) } }
        }
        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        navHost = ComposeView(this).apply {
            consumeWindowInsets = false
            setContent { C17Theme(ui) { C17Navigation(page, navigationInsets) { selectPage(it) } } }
        }
        root.addView(navHost, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            leftMargin = 0; rightMargin = 0; bottomMargin = 0
        })
        navHost.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val height = (bottom - top) / density
            if (height > 0 && navigationHeight != height) navigationHeight = height
        }
        modalLayer = ComposeView(this).apply {
            consumeWindowInsets = false
            setContent { C17Theme(ui) { C17OverlayLayer(this@ModernMainActivity) } }
            visibility = View.GONE
        }
        // Native z-order also matches touch and accessibility order: all app popups own this layer.
        root.addView(modalLayer, FrameLayout.LayoutParams(-1, -1))
        root.setOnApplyWindowInsetsListener { _, insets ->
            val compat = WindowInsetsCompat.toWindowInsetsCompat(insets, root)
            keyboardOpen = compat.isVisible(WindowInsetsCompat.Type.ime())
            val safeSides = compat.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout())
            val safeLeft = safeSides.left
            val safeRight = safeSides.right
            val bottom = compat.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val contentParams = content.layoutParams as FrameLayout.LayoutParams
            if (contentParams.leftMargin != safeLeft || contentParams.rightMargin != safeRight) {
                contentParams.leftMargin = safeLeft
                contentParams.rightMargin = safeRight
                content.layoutParams = contentParams
            }
            // Keep the opaque surface full width and down to the window edge. Only the tab
            // content uses safe insets, so the system gesture area never exposes the page.
            val rtl = root.layoutDirection == View.LAYOUT_DIRECTION_RTL
            navigationInsets = PaddingValues(start = ((if (rtl) safeRight else safeLeft) / density).dp,
                end = ((if (rtl) safeLeft else safeRight) / density).dp, bottom = (bottom / density).dp)
            updateNavigation()
            insets
        }
        setContentView(root)
        updateNavigation()
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    thanksOpen -> thanksOpen = false
                    freeNoticeRequired -> finish()
                    donationImage != null -> donationImage = null
                    colorEditing != null -> colorEditing = null
                    marqueePickerOpen -> marqueePickerOpen = false
                    configScope != null -> cancelConfigScope()
                    downloadingFont != null -> cancelFontDownload()
                    fontRenameTarget != null -> { fontRenameTarget = null; fontCatalogOpen = true }
                    fontScopeSelection != null -> cancelFontScopeSelection()
                    fontCatalogOpen -> fontCatalogOpen = false
                    iconPackDownloading -> cancelIconPackDownload()
                    wallpaperEdit != null -> if (!busy) wallpaperEdit = null
                    iconManagementOpen && confirmation == null -> if (!busy) iconManagementBackRequest?.invoke() ?: closeIconManagement()
                    notificationOverridesOpen -> notificationOverridesOpen = false
                    clockLaunchEditing != null -> clockLaunchEditing = null
                    statusBarOrderChoice != null -> statusBarOrderChoice = null
                    editing != null -> editing = null
                    confirmation != null -> dismissConfirmation()
                    performanceOpen -> performanceOpen = false
                    diagnosticsOpen -> closeDiagnostics()
                    uiSettingsOpen -> closeUiSettings()
                    selectedGroup != null -> closeGroup()
                    page != 0 -> selectPage(0)
                    // Root page: let the system leave. Android 12+ keeps a root activity alive in the
                    // background, so the callback must be armed again or the next back press from a
                    // sub page would skip every level and go straight to the launcher.
                    else -> { isEnabled = false; onBackPressedDispatcher.onBackPressed(); isEnabled = true }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)
    }

    override fun onResume() {
        super.onResume(); resumed = true
        performanceAction = RootPerformanceModule.activeAction()?.takeIf { it != RootPerformanceModule.Action.CHECK }
        performanceWritePending = performanceAction != null
        performanceBusy = performanceWritePending
        if (::backCallback.isInitialized) backCallback.isEnabled = true
        refreshPanelMode()
        refreshBatteryPercent()
        val iconState = LauncherIcon.read(this)
        desktopIconHidden = iconState.hidden
        desktopIconAvailable = iconState.success
        legacyName = iconState.legacyName
        values = raw.all
        updateHomeStatusRefresh()
        checkActivation()
        if (!freeNoticeRequired && getSharedPreferences("app_migrations", MODE_PRIVATE).getBoolean("root_requested", false) && !rootGranted) requestRoot()
        replayFile()
    }

    override fun onPause() {
        if (downloadingFont != null) cancelFontDownload()
        if (iconPackDownloading) cancelIconPackDownload()
        if (updateRequest?.isInstalling == false) { updateRequest?.cancel(); updateRequest = null; busy = false }
        if (NumericTrial.active()) {
            NumericTrial.rollback()
            confirmation = null
        }
        handler.removeCallbacks(notifySettings); notifySettings.run()
        SettingsSnapshot.flushPending(this)
        resumed = false
        updateHomeStatusRefresh()
        if (!checkingRoot) rootGranted = false
        probe?.cancel(); probe = null
        super.onPause()
    }

    override fun onDestroy() {
        fontCancellation?.cancel()
        iconPackCancellation?.cancel()
        destroyed = true; raw.unregisterOnSharedPreferenceChangeListener(listener)
        runCatching { contentResolver.unregisterContentObserver(panelModeObserver) }
        runCatching { contentResolver.unregisterContentObserver(batteryPercentObserver) }
        probe?.cancel(); frameworkSubscription?.close(); rootRequest?.cancel(); restartRequest?.cancel(); updateRequest?.cancel()
        performanceRequest?.cancel(); performanceReadRequest?.cancel(); performanceSubscription?.close(); performanceSubscription = null
        performanceAction = null
        handler.removeCallbacksAndMessages(null); work.shutdown()
        val abandonedWallpapers = synchronized(wallpaperResourcesLock) {
            preparedWallpapers.toList().also { preparedWallpapers.clear() }
        }
        abandonedWallpapers.forEach { it.close() }
        val abandonedIcons = synchronized(iconResourcesLock) { preparedIconImports.toList().also { preparedIconImports.clear() } }
        abandonedIcons.forEach { it.close() }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("page", page); outState.putString("group", selectedGroup)
        outState.putBoolean("performance_open", performanceOpen)
        outState.putBoolean("ui_settings_open", uiSettingsOpen)
        outState.putString("query", query); outState.putString("category", category)
        outState.putString("wallpaper_scene", wallpaperImportScene)
        pendingFile?.let { outState.putString("file_kind", it.first); outState.putString("file_uri", it.second.toString()) }
        exportSections?.let { outState.putStringArrayList("export_sections", ArrayList(it)) }
        super.onSaveInstanceState(outState)
    }

    fun selectPage(index: Int) {
        page = index; selectedGroup = null; updateNavigation()
        setHomeStatusVisible(index == 0 && !freeNoticeRequired)
    }
    fun openGroup(id: String) {
        if (id == "status_icon_library") { openIconManagement(); return }
        selectedGroup = (if (id == "network") "wifi" else id)
            .takeIf { SettingsCatalog.group(it) != null }
        if (id == "font") refreshFontLibrary(); updateNavigation()
        // First entry: the source declaration once. Confirming only records it; it never switches the feature on.
        if (id == MINI_STACK_GROUP && confirmation == null && !NotificationMiniStackSource.acknowledged(this)) showMiniStackSource(null)
    }
    fun openHintIconOrder() { hintIconOrderRequested = true; openGroup(StatusBarLayoutSettings.GROUP) }
    /** The mini stack's source declaration with the author's Coolapk link. "知道了" records that it was seen; when the
     *  user's own switch (or import) triggered it, that one action then continues. Nothing restarts SystemUI. */
    fun showMiniStackSource(then: (() -> Unit)?, extra: String = "", cancel: (() -> Unit)? = null) {
        confirmation = UiConfirmation(NotificationMiniStackSource.TITLE, NotificationMiniStackSource.NOTICE + extra, "知道了",
            linkLabel = "${NotificationMiniStackSource.LINK}（酷安 @${NotificationMiniStackSource.AUTHOR}）",
            linkUrl = NotificationMiniStackSource.URL, cancelAction = cancel) {
            if (!NotificationMiniStackSource.acknowledge(this)) { toast("确认记录保存失败，请重试"); cancel?.invoke(); return@UiConfirmation }
            then?.invoke()
        }
    }
    fun closeGroup() {
        if (selectedGroup == StatusBarLayoutSettings.GROUP) statusBarOrderCloseRequest?.let { it(); return }
        selectedGroup = null; updateNavigation()
    }
    fun finishStatusBarOrder() {
        hintIconOrderRequested = false
        statusBarOrderChoice = null; statusBarOrderCloseRequest = null
        selectedGroup = null; updateNavigation()
    }
    fun openUiSettings() { uiSettingsOpen = true; updateNavigation() }
    fun closeUiSettings() { uiSettingsOpen = false; updateNavigation() }
    /** 关于 → 问题诊断. Opening it re-reads the log and asks SystemUI which hooks really installed. */
    fun openDiagnostics() {
        diagnosticsOpen = true; updateNavigation()
        refreshDiagnostics()
        if (resumed) checkActivation()
    }
    fun closeDiagnostics() { diagnosticsOpen = false; updateNavigation() }
    fun refreshDiagnostics() {
        diagnosticsLoading = true
        work.execute {
            val context = applicationContext
            val events = ModuleDiagnostics.recentEvents(context)
            val stats = runCatching { ModuleDiagnostics.stats(context, events) }.getOrNull()
            val result = runCatching {
                DiagnosticCenter.analyze(events, ModuleDiagnostics.hookHealth(), ModuleDiagnostics.runtimeActive(),
                    ModuleDiagnostics.runtimeMessage(), StatusBarSettings.preferences(context).all, DiagnosticReport.app(context),
                    DiagnosticReport.packages(context), StatusBarSettings.bool(StatusBarSettings.preferences(context).all, StatusBarSettings.DIAGNOSTICS_ENABLED))
            }.getOrNull()
            handler.post { if (!destroyed) { diagnostics = result; diagnosticsStats = stats; diagnosticsHooks = ModuleDiagnostics.hookHealth(); diagnosticsLoading = false } }
        }
    }
    fun shareLog() { ModuleDiagnostics.share(this) }
    fun updateUiStyle(style: String) {
        if (!ui.updateUiStyle(style)) { toast("界面设置保存失败，请重试"); return }
        updateNavigation()
        if (::navHost.isInitialized) navHost.requestLayout()
    }
    fun resetUi() { ui.reset(); updateNavigation() }
    fun syncModalLayer(visible: Boolean) {
        updateNavigation()
        if (::modalLayer.isInitialized) modalLayer.visibility = if (visible) View.VISIBLE else View.GONE
    }
    private fun updateNavigation() {
        val shown = !freeNoticeRequired && selectedGroup == null && !uiSettingsOpen && !diagnosticsOpen && !keyboardOpen && !iconManagementOpen
        if (navigationShown != shown) navigationShown = shown
        if (::navHost.isInitialized) navHost.visibility = if (shown && !glassNavigation) View.VISIBLE else View.GONE
    }
    /** Height in dp reported by the floating glass bar, so page lists end above it. */
    fun reportNavigationHeight(height: Float) {
        if (glassNavigation && height > 0 && navigationHeight != height) navigationHeight = height
    }
    fun updateGlassNavigation(enabled: Boolean) {
        val target = enabled && GlassNavigationPreference.supported()
        if (target == glassNavigation) return
        if (!GlassNavigationPreference.write(this, target)) { toast("界面设置保存失败"); return }
        glassNavigation = target
        // Classic mode re-measures its own height through the navHost layout listener.
        if (!target && ::navHost.isInitialized) navHost.requestLayout()
        updateNavigation()
    }
    fun acceptFreeNotice(input: String): String? {
        if (!FreeNotice.matches(input)) return "请完整输入上面的短句，包含标点。"
        if (!FreeNotice.accept(this, input)) return "确认记录保存失败，请重试。"
        freeNoticeRequired = false
        updateNavigation()
        replayFile()
        if (resumed && !rootGranted && !checkingRoot && getSharedPreferences("app_migrations", MODE_PRIVATE).getBoolean("root_requested", false)) requestRoot()
        return null
    }
    fun value(key: String): Any? {
        val stored = SettingsCatalog.value(values, StatusBarLayoutSettings.editingKey(key, values))
        // PUI needs the system percentage; retired selections are normalized to PUI.
        return if (key == StatusBarSettings.BATTERY_STYLE && batteryPercentStyle !in 1..2 && stored == "pui") "native" else stored
    }
    fun optionUnavailableReason(key: String, option: String): String =
        if (key == StatusBarSettings.BATTERY_STYLE && option == "pui" && batteryPercentStyle !in 1..2)
            "系统已关闭电池百分比，使用系统原生样式；开启百分比后可选 PUI" else ""
    fun bool(key: String): Boolean = if (key.startsWith("shade_wallpaper_") && !ShadeWallpaperSettings.available()) false else StatusBarSettings.bool(values, key)
    fun save(requestedKey: String, value: Any): Boolean {
        val key = StatusBarLayoutSettings.editingKey(requestedKey, raw.all)
        if (!resumed || !canEdit) { toast("激活模块或授予 Root 后即可保存配置"); return false }
        if (key == StatusBarSettings.SPEED_OFFSET_X &&
            StatusBarSettings.bool(raw.all, SpeedPosition.SAFE_GAP_ENABLED)) {
            toast(SettingsCatalog.unavailableReason(key, raw.all)); return false
        }
        val featureUnavailable = ShadeWallpaperSettings.unavailableReason(key)
        if (featureUnavailable.isNotEmpty()) { toast(featureUnavailable); return false }
        if (key == StatusBarSettings.BATTERY_STYLE && value == "pui") {
            refreshBatteryPercent()
            val unavailable = optionUnavailableReason(key, "pui")
            if (unavailable.isNotEmpty()) { toast(unavailable); return false }
        }
        if (value is String) {
            val invalid = when (key) {
                NotificationIconOverrides.RULES -> NotificationIconOverrides.validationError(value)
                IconManagementSettings.KEY -> runCatching { IconManagementSettings.parse(value); null }.getOrElse { it.message ?: "图标管理配置无效" }
                else -> null
            }
            if (invalid != null) { toast(invalid); return false }
        }
        if (value == true) {
            val unavailable = SettingsCatalog.unavailableReason(key, raw.all)
            if (unavailable.isNotEmpty()) { toast(unavailable); return false }
        }
        // Every way to switch the mini stack on (page switch, search) first shows its source declaration once.
        if (NotificationMiniStackSource.requiresDeclaration(key, value) && !NotificationMiniStackSource.acknowledged(this)) {
            showMiniStackSource({ save(key, value) })
            return false
        }
        val editor = preferences.edit()
        // Pin the existing choice before this renamed toggle is edited. A first
        // code80 opt-in must not be mistaken for the old remove-background switch.
        if (key == FluidCloudStyle.DESKTOP_BACKGROUND && !raw.contains(FluidCloudStyle.BACKGROUND_MODE)) {
            val before = raw.all
            editor.putString(FluidCloudStyle.BACKGROUND_MODE, StatusBarSettings.string(before, FluidCloudStyle.BACKGROUND_MODE))
            editor.putFloat(FluidCloudStyle.BACKGROUND_OPACITY, StatusBarSettings.settingNumber(before, FluidCloudStyle.BACKGROUND_OPACITY, 100f))
        }
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Float -> editor.putFloat(key, value)
            is Int -> editor.putInt(key, value)
            is String -> editor.putString(key, value)
            else -> return false
        }
        if (value is Int) StatusBarSettings.markColorCustomized(editor, key)
        // 默认时钟与双排时钟二选一：开启其中一个，同一次保存里关闭另一个。
        if (value == true) ClockLayoutSettings.turnsOff(key)?.let { editor.putBoolean(it, false) }
        // 统一制式名称与自定义制式内容二选一：开启其中一个，同一次保存里关闭另一个。
        if (value == true) NetworkLabelFormats.turnsOff(key)?.let { editor.putBoolean(it, false) }
        // 关闭回弹效果与回弹时模糊二选一：开启前者，同一次保存里关闭后者。
        if (value == true) NotificationBigClockSettings.turnsOff(key)?.let { editor.putBoolean(it, false) }
        editor.apply()
        return true
    }
    /** One transaction: a drag reorder must never publish half of the new layout. */
    fun saveLayoutOrders(orders: Map<String, Int>): Boolean {
        if (!resumed || !canEdit || busy) { toast("激活模块或授予 Root 后即可修改"); return false }
        if (orders.isEmpty() || orders.any { (token, order) -> token !in StatusBarLayoutSettings.TOKENS || order < 0 }) {
            toast("状态栏顺序无效"); return false
        }
        val editor = preferences.edit()
        orders.forEach { (token, order) -> editor.putFloat(StatusBarLayoutSettings.key(token, "order"), order.toFloat()) }
        if (!editor.commit()) { toast("状态栏顺序保存失败"); return false }
        return true
    }
    /** UI-only snapshot: neither the enable switch nor independent margins enter the draft. */
    fun statusBarOrderValues(): HashMap<String, Any> {
        val saved = raw.all
        val draft = StatusBarOrderDraft.from(saved)
        return HashMap<String, Any>().apply {
            putAll(draft.sideValues()); putAll(draft.orderValues())
            put(NativeStatusIcons.LAYOUT, HintIconOrderDraft.from(saved[NativeStatusIcons.LAYOUT] as? String).serialize())
            val key = StatusBarLayoutSettings.key(StatusBarLayoutSettings.LABEL, "order")
            val number = (saved[key] as? Number)?.toFloat() ?: 0f
            put(key, if (number.isFinite() && number in 0f..2f && number == kotlin.math.floor(number)) number else 0f)
        }
    }
    /** Publish region and order together; a prepared draft never enables status-bar sorting. */
    fun saveStatusBarOrderDraft(proposed: Map<String, Any>, expected: Map<String, Any>): Boolean {
        if (!resumed || !canEdit || busy) { toast("激活模块或授予 Root 后即可保存排列"); return false }
        val keys = StatusBarLayoutSettings.STRINGS.keys + StatusBarLayoutSettings.NUMBERS.keys.filter(StatusBarLayoutSettings::orderKey) + NativeStatusIcons.LAYOUT
        if (proposed.keys != keys.toSet() || expected.keys != keys.toSet()) {
            toast("状态栏排列无效"); return false
        }
        for ((key, value) in proposed) {
            val valid = if (key == NativeStatusIcons.LAYOUT) value is String && runCatching { NativeStatusIconLayout.validate(value) }.isSuccess
                else if (StatusBarLayoutSettings.sideKey(key)) StatusBarLayoutSettings.validSide(value)
                else value is Number && value.toDouble().isFinite() && value.toDouble() >= 0.0 &&
                    value.toDouble() <= Int.MAX_VALUE.toDouble() && value.toDouble() == kotlin.math.floor(value.toDouble()) &&
                    (key != StatusBarLayoutSettings.key(StatusBarLayoutSettings.LABEL, "order") || value.toDouble() <= 2.0)
            if (!valid) { toast("状态栏排列无效"); return false }
        }
        return synchronized(raw) {
            if (statusBarOrderValues() != expected) {
                toast("已保存的排列发生变化，请先重新载入"); return@synchronized false
            }
            val editor = preferences.edit()
            proposed.forEach { (key, value) ->
                if (value is String) editor.putString(key, value) else editor.putFloat(key, (value as Number).toFloat())
            }
            if (!editor.commit()) { toast("排列保存失败，草稿已保留"); return@synchronized false }
            true
        }
    }
    fun tryNumber(item: SettingsCatalog.Item, value: Float) {
        if (!resumed || !canEdit || busy) { toast("激活模块或授予 Root 后即可修改"); return }
        val trialContext = raw.all
        val confirmed = SettingEditor.storedValue(item, trialContext)
        val trialKey = StatusBarLayoutSettings.editingKey(item.key, trialContext)
        if (!NumericTrial.begin(this, raw, preferences, trialKey, value)) {
            toast("无法开始试用，已保留原来的数值"); return
        }
        editing = null
        confirmation = UiConfirmation("试用${item.title}",
            SettingEditor.trialSummary(item, confirmed, value, trialContext),
            "保存此数值", expiresAt = NumericTrial.deadline(), cancelAction = { NumericTrial.rollback() }) {
                toast(if (NumericTrial.keep()) "数值已保存" else if (NumericTrial.active()) "保存失败，正在恢复原数值" else "试用已结束，原数值已恢复")
            }
    }
    /** Reads only while the home cards are visible; the shared workers keep each read bounded. */
    fun setHomeStatusVisible(visible: Boolean) {
        homeStatusVisible = visible
        updateHomeStatusRefresh()
    }
    private fun updateHomeStatusRefresh() {
        val visible = homeStatusVisible && resumed && !destroyed && !freeNoticeRequired
        val changed = homeStatus.setVisible(visible)
        handler.removeCallbacks(homeStatusPoll)
        if (!visible) {
            performanceReadRequest?.cancel(); performanceReadRequest = null
            performanceSubscription?.close(); performanceSubscription = null
            checkingPerformance = false
            if (changed || !resumed || destroyed) {
                probe?.cancel(); probe = null; checkingRuntime = false; runtimeRequestId++
            }
            return
        }
        if (performanceSubscription == null) performanceSubscription = RootPerformanceModule.observe { result ->
            // A polling reply must go through its ticket; only an authorized write uses the observer.
            if (performanceWritePending && !checkingPerformance) applyPerformanceResult(result)
        }
        handler.post(homeStatusPoll)
    }
    private fun scheduleHomeStatus() {
        handler.removeCallbacks(homeStatusPoll)
        val delay = homeStatus.nextDelay(SystemClock.elapsedRealtime())
        if (delay >= 0L && resumed && !destroyed) handler.postDelayed(homeStatusPoll, delay)
    }
    private fun refreshHomeStatus() {
        if (!homeStatus.visible() || !resumed || destroyed) return
        val now = SystemClock.elapsedRealtime()
        val rootTicket = homeStatus.begin(HomeStatusRefreshPolicy.Kind.PERFORMANCE, now, RootPerformanceModule.busy())
        if (rootTicket != 0L) {
            // Reads never publish a loading/busy state: retain the last real report while the worker checks.
            checkingPerformance = true
            performanceReadRequest = RootPerformanceModule.submit(applicationContext, RootPerformanceModule.Action.CHECK) { result ->
                val module = result.snapshot
                val changing = module?.pending == true || module?.lastState in listOf("waiting", "running")
                val denied = !result.success && (result.message.contains("未获得 Root") || result.message.contains("未找到支持的 Root"))
                if (homeStatus.complete(HomeStatusRefreshPolicy.Kind.PERFORMANCE, rootTicket,
                        SystemClock.elapsedRealtime(), result.success, changing, denied)) {
                    performanceReadRequest = null; checkingPerformance = false
                    applyPerformanceResult(result); scheduleHomeStatus()
                }
            }
        }
        val runtimeTicket = homeStatus.begin(HomeStatusRefreshPolicy.Kind.RUNTIME, now, checkingRuntime)
        if (runtimeTicket != 0L) beginActivationCheck(runtimeTicket)
        scheduleHomeStatus()
    }
    private fun applyPerformanceResult(result: RootPerformanceModule.Result) {
        // A newly attached observer may deliver the previous result while another authorized operation is still running.
        if (RootPerformanceModule.busy()) return
        performanceBusy = false
        performanceAction = null
        performanceWritePending = false
        if (destroyed || !resumed || !homeStatus.visible()) return
        // A complete rooted report can still be useful when the authorized action itself failed.
        // Permission failures and partial output must never imply that the module is uninstalled.
        val reliable = result.snapshot?.takeIf { it.root }
        if (result.success && reliable != null) PerformanceScheduler.observe(applicationContext, reliable)
        val error = !result.success || reliable == null
        val message = if (!error) result.message else when {
            result.message.contains("未获得 Root") -> "Root 未授权"
            result.message.contains("未找到支持的 Root") -> "未识别管理器"
            result.message.contains("超时") -> "获取超时"
            else -> "状态暂未确认"
        }
        val report = reliable ?: performanceReport
        // Snapshot has identity equality. Compare all real fields before publishing it to Compose.
        val state = listOf(report?.root, report?.installed, report?.pending, report?.disabled, report?.remove,
            report?.legacy, report?.currentBoot, report?.outdated, report?.manager, report?.lastState,
            report?.error, report?.moduleVersion, report?.trigger, report?.time, report?.compiled,
            report?.packages, report?.moduleVersionCode, report?.recordVersionCode, report?.ready,
            report?.reused, report?.verified, report?.deferred, report?.verification, report?.priority,
            error, message, result.message)
        if (homeStatus.publish(HomeStatusRefreshPolicy.Kind.PERFORMANCE, state)) {
            if (reliable != null) performanceReport = reliable
            performanceError = error
            performanceDetail = result.message
            performanceMessage = message
        }
        if (result.success && result.snapshot?.root == true) rememberResidentRootGrant()
    }
    private fun rememberResidentRootGrant() {
        val migration = getSharedPreferences("app_migrations", MODE_PRIVATE)
        if (!migration.getBoolean("resident_root_granted", false)) migration.edit().putBoolean("resident_root_granted", true).apply()
    }
    fun refreshPerformance() {
        homeStatus.retry(HomeStatusRefreshPolicy.Kind.PERFORMANCE, SystemClock.elapsedRealtime())
        if (homeStatus.visible()) refreshHomeStatus()
    }
    fun openPerformance() {
        performanceOpen = true
        refreshPerformance()
    }
    private fun confirmPerformance(action: RootPerformanceModule.Action, title: String, summary: String, button: String) {
        if (!canEdit || performanceBusy || RootPerformanceModule.busy()) return
        confirmation = UiConfirmation(title, summary, button) {
            if (!canEdit || RootPerformanceModule.busy()) return@UiConfirmation
            performanceAction = action
            performanceBusy = true; performanceWritePending = true
            performanceRequest = RootPerformanceModule.submit(applicationContext, action) { result ->
                performanceAction = null
                performanceBusy = false
                applyPerformanceResult(result)
                performanceWritePending = false
                homeStatus.afterAction(HomeStatusRefreshPolicy.Kind.PERFORMANCE, SystemClock.elapsedRealtime())
                scheduleHomeStatus()
                if (!destroyed) toast(result.message)
            }
        }
    }
    fun installPerformance() {
        val updating = performanceReport?.outdated == true
        confirmPerformance(RootPerformanceModule.Action.INSTALL, (if (updating) "更新 " else "安装 ") + RootPerformanceModule.DISPLAY_NAME,
            "安装后按需检查。需要编译时等待息屏或连续 10 分钟无操作，电量高于 15%、电池温度低于 55℃；不支持输入时间时等待息屏。需 Root 授权。",
            if (updating) "更新" else "安装")
    }
    fun removePerformance() { confirmPerformance(RootPerformanceModule.Action.REMOVE, "卸载 " + RootPerformanceModule.DISPLAY_NAME, "请求卸载，重启时尝试恢复本模块处理过的组件的编译策略；恢复失败保留记录。需 Root 授权。", "卸载") }
    fun enablePerformance() { confirmPerformance(RootPerformanceModule.Action.ENABLE, "启用 " + RootPerformanceModule.DISPLAY_NAME, "启用按需检查，等待息屏或连续 10 分钟无操作。自动处理要求电量高于 15%、电池温度低于 55℃。需 Root 授权。", "启用") }
    fun disablePerformance() { confirmPerformance(RootPerformanceModule.Action.DISABLE, "停用 " + RootPerformanceModule.DISPLAY_NAME, "停止自动任务，已有编译结果保留。需 Root 授权。", "停用") }
    fun runPerformance() { confirmPerformance(RootPerformanceModule.Action.RUN, "立即优化", "立即检查并按需处理，不受屏幕、充电、电量、电池温度或 Thermal Service 限制。仍保留操作超时、可靠取消与防重叠，缺少必要 ART 能力时无法执行。新结果随系统自然加载使用，需 Root 授权。", "开始") }
    fun checkActivation() {
        if (destroyed || !resumed || checkingRuntime) return
        if (homeStatus.visible()) {
            homeStatus.retry(HomeStatusRefreshPolicy.Kind.RUNTIME, SystemClock.elapsedRealtime())
            refreshHomeStatus()
        } else beginActivationCheck(0L)
    }
    private fun beginActivationCheck(ticket: Long) {
        val requestId = ++runtimeRequestId
        checkingRuntime = true
        probe = ModuleRuntimeStatus.probe(this) { result ->
            if (!destroyed && resumed && requestId == runtimeRequestId) {
                // probe emits CHECKING before its verified reply; it is not an inactive result.
                if (result.state == ModuleRuntimeStatus.State.CHECKING) return@probe
                val now = SystemClock.elapsedRealtime()
                if (ticket != 0L && !homeStatus.complete(HomeStatusRefreshPolicy.Kind.RUNTIME, ticket, now, result.active, false, false)) return@probe
                if (ticket == 0L) homeStatus.note(HomeStatusRefreshPolicy.Kind.RUNTIME, now, result.active, false, false)
                probe = null
                checkingRuntime = false
                val message = if (result.active) "${result.frameworkName} ${result.frameworkVersion}" else result.message
                // Primitive/string state equality already suppresses unchanged probe replies.
                runtimeActive = result.active
                runtimeMessage = message
                ModuleDiagnostics.noteRuntime(result.active, message, result.hookHealth)
                if (diagnosticsOpen) refreshDiagnostics()
                replayFile()
                scheduleHomeStatus()
            }
        }
    }
    fun requestRoot() {
        if (checkingRoot) return
        checkingRoot = true; rootMessage = "正在检查 Root 授权…"
        getSharedPreferences("app_migrations", MODE_PRIVATE).edit().putBoolean("root_requested", true).apply()
        rootRequest = RootAccess.check { result ->
            checkingRoot = false; rootRequest = null
            if (!destroyed) {
                rootGranted = result.granted; rootMessage = result.message
                if (resumed) {
                    replayFile()
                    if (result.granted && !freeNoticeRequired) { rememberResidentRootGrant(); refreshPerformance() }
                }
            }
        }
    }
    fun restartSystemUi() {
        if (busy) return
        confirmation = UiConfirmation("重启系统界面", "状态栏与通知中心会短暂重新加载，随后应用当前模块和配置。", "重启") {
            busy = true; runtimeActive = false
            restartRequest = SystemUiRestart.restart { result ->
                if (!destroyed) {
                    busy = false; toast(result.message)
                    handler.postDelayed({ if (resumed) checkActivation() }, if (result.success) 1800 else 0)
                }
            }
        }
    }
    /** 导入先选文件，读出内容后再问是否自定义导入项。 */
    fun chooseImport() { if (canEdit) importConfig.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) else toast("请先激活模块或授予 Root") }
    /** 导出和分享先问是否自定义，再选保存位置或分享对象。 */
    fun chooseExport() { askConfigScope("export") }
    fun shareConfig() { askConfigScope("share") }
    private fun askConfigScope(kind: String) {
        if (!canEdit) { toast("请先激活模块或授予 Root"); return }
        if (busy) return
        configScope = ConfigScopeRequest(kind)
    }
    fun customizeConfigScope() {
        val request = configScope ?: return
        val ids = if (request.prepared != null) ConfigScope.present(request.prepared.keys()).keys.map { it.id }
            else ConfigScope.sections().map { it.id }
        configScope = request.copy(customizing = true, selected = ids.toSet())
    }
    fun backConfigScope() { configScope = configScope?.copy(customizing = false) }
    fun toggleConfigScope(id: String, on: Boolean) {
        val request = configScope ?: return
        configScope = request.copy(selected = if (on) request.selected + id else request.selected - id)
    }
    fun selectAllConfigScope(all: Boolean) {
        val request = configScope ?: return
        val ids = if (request.prepared != null) ConfigScope.present(request.prepared.keys()).keys.map { it.id }
            else ConfigScope.sections().map { it.id }
        configScope = request.copy(selected = if (all) ids.toSet() else emptySet())
    }
    fun cancelConfigScope() {
        val request = configScope
        configScope = null
        if (request?.kind == "import") pendingFile = null
    }
    fun confirmConfigScopeAll() { val request = configScope ?: return; configScope = null; continueConfig(request, null) }
    fun confirmConfigScopeChoice() {
        val request = configScope ?: return
        if (request.selected.isEmpty()) { toast("请至少选择一个页面"); return }
        configScope = null
        continueConfig(request, request.selected)
    }
    private fun continueConfig(request: ConfigScopeRequest, sections: Set<String>?) {
        when (request.kind) {
            "export" -> { exportSections = sections; exportConfig.launch("C17-settings.json") }
            "share" -> shareConfigFile(sections)
            else -> request.prepared?.let { reviewImport(it, sections) }
        }
    }
    private fun shareConfigFile(sections: Set<String>?) {
        if (busy) return
        busy = true
        val snapshot = values
        work.execute {
            try {
                val json = ConfigTransfer.exportJson(snapshot, sections?.let { ConfigScope.keys(it) })
                val uri = ConfigShare.prepare(this, json)
                runOnUiThread {
                    busy = false
                    if (destroyed) return@runOnUiThread
                    val send = Intent(Intent.ACTION_SEND).setType("application/json")
                        .putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_SUBJECT, "C17 状态栏配置")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    send.clipData = ClipData.newRawUri(ConfigShare.FILE_NAME, uri)
                    runCatching { startActivity(Intent.createChooser(send, "分享配置")) }.onFailure { toast("无法打开分享面板") }
                }
            } catch (error: Exception) { runOnUiThread { busy = false; toast("分享失败：${error.message}") } }
        }
    }
    /** 最后一步确认：只提交用户选的页面，其余设置保持原样。 */
    private fun reviewImport(prepared: ConfigTransfer.PreparedImport, sections: Set<String>?) {
        val chosen = if (sections == null) prepared else prepared.only(ConfigScope.keys(sections))
        val legacyIcons = if (chosen.skippedLegacyIcons > 0) "\n旧版图标规则不会恢复，请在图标管理中重新选择 v3 套装。" else ""
        if (chosen.count == 0) {
            pendingFile = null
            toast(if (chosen.skippedLegacyIcons > 0) "已跳过 ${chosen.skippedLegacyIcons} 项旧版图标设置，没有其他可导入设置。$legacyIcons" else "没有可导入的设置")
            return
        }
        confirmation = UiConfirmation("导入配置", "将更新 ${chosen.count} 项设置。${chosen.warning}$legacyIcons\n现有字体文件和安全模式会保留。", "导入", cancelAction = { pendingFile = null }) {
            pendingFile = null
            if (!resumed || !canEdit) { toast("权限状态已变化，请重新授权后导入"); return@UiConfirmation }
            // An imported "on" is not the user having seen the mini stack's source declaration.
            if (NotificationMiniStackSource.importNeedsDeclaration(chosen.values(), NotificationMiniStackSource.acknowledged(this))) {
                val rest = chosen.only(chosen.keys() - NotificationMiniStack.ENABLED)
                showMiniStackSource({ commitImport(chosen) }, "\n\n导入的配置会开启同应用小堆叠；点“取消”则导入其余设置，小堆叠保持原状。",
                    cancel = { if (rest.count > 0) commitImport(rest) else toast("未导入：同应用小堆叠保持原状") })
                return@UiConfirmation
            }
            commitImport(chosen)
        }
    }
    private fun commitImport(chosen: ConfigTransfer.PreparedImport) {
        if (!resumed || !canEdit) { toast("权限状态已变化，请重新授权后导入"); return }
        busy = true
        val result = runCatching { ConfigTransfer.commit(preferences, chosen) }
        busy = false; toast(if (result.getOrDefault(false)) "配置已导入" else "导入未完成，请重试")
    }
    fun chooseFont() { if (canEdit && !busy) { fontScopeSelection = null; fontCatalogOpen = false; importFont.launch(arrayOf("*/*")) } else toast("请先激活模块或授予 Root") }
    fun chooseOpenFont() { fontScopeSelection = null; refreshFontLibrary(); fontCatalogOpen = true }
    fun refreshFontLibrary() {
        if (destroyed || maintenanceRunning) return
        val request = ++fontLibraryRequest
        val snapshot = values
        work.execute {
            val result = runCatching {
                FontLibrary.retainCurrent(applicationContext, snapshot[StatusBarSettings.FONT_REVISION]?.toString(), snapshot[StatusBarSettings.FONT_NAME]?.toString())
                FontLibrary.retainDownloads(applicationContext)
                FontLibrary.entries(applicationContext)
            }
            handler.post { if (!destroyed && request == fontLibraryRequest && !maintenanceRunning) {
                result.onSuccess { fontLibraryEntries = it }.onFailure { ModuleDiagnostics.error("font", "Font library unavailable", it) }
            } }
        }
    }
    fun currentFontTitle(): String {
        val assignments = runCatching { FontScopes.parse(value(FontScopes.KEY)?.toString() ?: "{}") }.getOrDefault(emptyMap())
        val sources = assignments.values.distinct()
        if (sources.size == 1 && assignments.size == FontScopes.SCOPES.size) return fontSourceTitle(sources.first())
        if (assignments.isNotEmpty()) return "已设置 ${assignments.size} 个范围"
        return when (value(StatusBarSettings.FONT_MODE)?.toString()) {
            "pingfang" -> "苹方（原有选择）"
            "custom" -> (value(StatusBarSettings.FONT_NAME)?.toString()?.takeIf { it.isNotBlank() } ?: "导入字体") + "（原有选择）"
            else -> "跟随系统"
        }
    }
    fun fontSourceTitle(source: String): String = when {
        source == "system" -> "跟随系统"
        source == "pingfang" -> "苹方"
        source.startsWith("custom:") -> {
            val revision = source.removePrefix("custom:")
            fontLibraryEntries.firstOrNull { it.revision == revision }?.name
                ?: FontCatalog.forRevision(revision)?.displayName ?: "导入字体 · ${revision.take(8)}"
        }
        else -> "原有字体选择"
    }
    fun fontUsage(source: String): String {
        val assignments = runCatching { FontScopes.parse(value(FontScopes.KEY)?.toString() ?: "{}") }.getOrDefault(emptyMap())
        val scopes = FontScopes.SCOPES.filter { assignments[it] == source }
        return if (scopes.isEmpty()) "尚未分配范围" else
            (if (bool(FeatureOptions.masterKey("font"))) "应用范围：" else "已保存范围：") + scopes.joinToString("、") { FontScopes.title(it) }
    }
    fun selectFontScopes(mode: String, title: String, revision: String = "") {
        if (!resumed || !canEdit || busy) return
        val source = runCatching { FontScopes.source(mode, revision) }.getOrElse { toast("字体来源无效，请重新选择"); return }
        fontCatalogOpen = false
        fontScopeSelection = FontScopeSelection(source, title)
    }
    fun selectLibraryFont(entry: FontLibrary.Entry) {
        selectFontScopes("custom", entry.name, entry.revision)
    }
    fun cancelFontScopeSelection() {
        if (busy) return
        fontScopeSelection = null
        fontCatalogOpen = true
    }
    /** Only the final action writes the scope map and font master; existing feature switches stay untouched. */
    fun applyFontScopes(selection: FontScopeSelection, selected: Set<String>) {
        if (!resumed || !canEdit || busy || fontScopeSelection !== selection || selected.isEmpty()) return
        val scopes = FontScopes.SCOPES.filter { it in selected }
        if (scopes.isEmpty() || scopes.size != selected.size) { toast("作用范围无效，请重新选择"); return }
        busy = true
        work.execute {
            val ready = runCatching {
                if (selection.source.startsWith("custom:")) check(FontLibrary.contains(applicationContext, selection.source.removePrefix("custom:"))) {
                    "字体文件已缺失，请重新导入或下载"
                }
            }
            handler.post {
                busy = false
                if (destroyed || fontScopeSelection !== selection) return@post
                if (ready.isFailure) { toast(ready.exceptionOrNull()?.message ?: "字体不可用"); refreshFontLibrary(); return@post }
                if (!resumed || !canEdit || maintenanceRunning) { toast("编辑权限已变化，字体设置未改变"); return@post }
                val previous = raw.all
                val result = runCatching {
                    val master = FeatureOptions.masterKey("font")
                    val wasEnabled = StatusBarSettings.bool(previous, master)
                    val plan = FontScopes.planApplication(previous[FontScopes.KEY]?.toString() ?: "{}", wasEnabled, scopes, selection.source)
                    val editor = preferences.edit().putString(FontScopes.KEY, plan.assignments).putBoolean(master, true)
                    // Applying a subset must not reactivate disabled scope assignments or the old global source.
                    // Keep its file/name/revision and every independent feature choice available.
                    if (plan.resetLegacySource) editor.putString(StatusBarSettings.FONT_MODE, "system")
                    if (!editor.commit()) {
                        val restore = raw.edit()
                        if (previous.containsKey(FontScopes.KEY)) restore.putString(FontScopes.KEY, previous[FontScopes.KEY] as String?) else restore.remove(FontScopes.KEY)
                        if (previous.containsKey(master)) restore.putBoolean(master, previous[master] == true) else restore.remove(master)
                        if (previous.containsKey(StatusBarSettings.FONT_MODE)) restore.putString(StatusBarSettings.FONT_MODE, previous[StatusBarSettings.FONT_MODE] as String?) else restore.remove(StatusBarSettings.FONT_MODE)
                        check(restore.commit()) { "字体保存失败，原设置未能完整恢复，请重试" }
                        error("作用范围未能保存，已恢复原字体设置")
                    }
                }
                if (result.isSuccess) {
                    fontScopeSelection = null
                    toast("${selection.title} 已应用到 ${scopes.size} 个范围")
                } else toast(result.exceptionOrNull()?.message ?: "字体设置未保存")
            }
        }
    }
    fun renameLibraryFont(entry: FontLibrary.Entry, input: String): String? {
        if (!resumed || !canEdit || busy) return "请先激活模块或授予 Root"
        val name = FontLibrary.displayName(input)
        if (input.isBlank()) return "请输入字体名称"
        busy = true; fontRenameTarget = null
        work.execute {
            val result = runCatching { FontLibrary.rename(applicationContext, entry.revision, name) }
            handler.post {
                if (!destroyed) {
                    if (result.isSuccess && value(StatusBarSettings.FONT_REVISION) == entry.revision)
                        preferences.edit().putString(StatusBarSettings.FONT_NAME, name).apply()
                    busy = false; refreshFontLibrary()
                    if (result.isFailure) toast("重命名失败：${result.exceptionOrNull()?.message}")
                }
            }
        }
        return null
    }
    fun deleteLibraryFont(entry: FontLibrary.Entry) {
        if (!canEdit || busy) return
        val active = value(StatusBarSettings.FONT_REVISION) == entry.revision ||
            FontScopes.references(value(FontScopes.KEY)?.toString() ?: "{}", entry.revision)
        confirmation = UiConfirmation("删除 ${entry.name}", if (active) "这款字体正在使用。删除后改为跟随系统，其他导入字体会保留。" else "删除这款字体，其他已导入或下载的字体会保留。", "删除") {
            if (!resumed || !canEdit || busy) return@UiConfirmation
            val current = value(StatusBarSettings.FONT_REVISION) == entry.revision
            val previous = raw.all
            val changes = FontLibrary.deletionFallback(previous, entry.revision)
            busy = true
            work.execute {
                try {
                    val prepared = FontLibrary.prepareDeletion(applicationContext, entry.revision, current)
                    handler.post {
                        val result = runCatching { prepared.use {
                            prepared.commit({ resumed && canEdit && !destroyed && !maintenanceRunning }, {
                                if (changes.isEmpty()) true else preferences.edit().also { editor -> changes.forEach { (key, selected) -> editor.putString(key, selected) } }.commit()
                            }, {
                                val restore = raw.edit()
                                changes.keys.forEach { key -> if (previous.containsKey(key)) restore.putString(key, previous[key] as String?) else restore.remove(key) }
                                check(restore.commit()) { "原字体配置未能恢复" }
                            })
                        } }
                        busy = false
                        if (!destroyed) { refreshFontLibrary(); fontCatalogOpen = true; toast(if (result.isSuccess) "字体已删除" else "删除未完成：${result.exceptionOrNull()?.message}") }
                    }
                } catch (error: Exception) { handler.post { busy = false; if (!destroyed) { refreshFontLibrary(); fontCatalogOpen = true; toast("字体未删除：${error.message}") } } }
            }
        }
    }
    fun downloadFont(entry: FontCatalog.Entry) {
        if (!resumed || !canEdit || busy) return
        val token = FontDownloadRepository.Cancellation()
        fontCatalogOpen = false; downloadingFont = entry; fontDownloadPercent = 0; fontCancellation = token; busy = true
        work.execute {
            try {
                val snapshot = values
                FontLibrary.retainCurrent(applicationContext, snapshot[StatusBarSettings.FONT_REVISION]?.toString(), snapshot[StatusBarSettings.FONT_NAME]?.toString())
                val prepared = FontDownloadRepository.prepare(applicationContext, entry, { received, total ->
                    runOnUiThread { if (fontCancellation === token) fontDownloadPercent = if (total > 0) (received * 100 / total).toInt().coerceIn(0, 100) else 0 }
                }, token)
                prepared.use { FontLibrary.remember(applicationContext, prepared, entry.displayName) }
                runOnUiThread {
                    if (fontCancellation !== token) return@runOnUiThread
                    fontCancellation = null; downloadingFont = null; busy = false
                    if (destroyed) return@runOnUiThread
                    refreshFontLibrary()
                    if (token.asBoolean || !resumed || !canEdit) {
                        toast("字体已入库，原字体设置保持不变")
                    } else {
                        selectFontScopes("custom", entry.displayName, entry.sha256)
                        toast("字体已下载，请选择作用范围")
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (fontCancellation === token) {
                        fontCancellation = null; downloadingFont = null; busy = false
                        toast("下载失败，原字体已保留：${error.message}")
                    }
                }
            }
        }
    }
    fun cancelFontDownload() {
        fontCancellation?.cancel(); fontCancellation = null; downloadingFont = null; busy = false
    }
    fun chooseNotificationIcon() { if (canEdit) importNotificationIcon.launch(arrayOf("image/png", "image/jpeg", "image/webp")) else toast("请先激活模块或授予 Root") }
    fun chooseLog() { exportLog.launch("C17-diagnostics.txt") }
    fun clearLogHistory() {
        if (busy || maintenanceRunning) return
        confirmation = UiConfirmation("清空日志", "删除本模块保存的历史诊断日志及分享副本，配置不受影响。", "清空") {
            busy = true
            work.execute {
                val result = runCatching { MaintenanceReset.clearLogs(applicationContext) }
                handler.post { if (!destroyed) { busy = false; toast(if (result.isSuccess) "历史日志已清空" else "清空失败，请稍后重试") } }
            }
        }
    }
    fun deleteAllSettings() {
        if (busy || !canEdit || maintenanceRunning) return
        confirmation = UiConfirmation("删除所有配置", "删除当前配置、恢复备份、导入字体和自定义图标，并恢复全部默认设置。免费声明确认、小堆叠来源声明确认与 Root 请求标识保留。此操作无法撤销，可先导出配置。", "删除并恢复默认") {
            if (!resumed || !canEdit || busy) return@UiConfirmation
            pendingFile = null; editing = null; colorEditing = null; fontCatalogOpen = false; fontScopeSelection = null; marqueePickerOpen = false
            configScope = null; exportSections = null
            iconManagementOpen = false; notificationOverridesOpen = false
            clockLaunchEditing = null
            cancelIconImport(); wallpaperEdit = null
            cancelIconPackDownload()
            cancelFontDownload(); handler.removeCallbacks(notifySettings)
            maintenanceRunning = true; busy = true
            work.execute {
                val result = runCatching { MaintenanceReset.reset(applicationContext) }
                handler.post {
                    if (!destroyed) {
                        maintenanceRunning = false; busy = false; values = raw.all; selectedGroup = null; iconPacks = emptyList()
                        toast(result.getOrNull()?.message ?: "删除未完成，请稍后重试；未确认的旧配置不会重新发布")
                        if (resumed) { notifySettings.run(); updateNavigation() }
                    }
                }
            }
        }
    }
    private fun installUpdate(result: GitHubUpdates.Result) {
        if (!resumed || destroyed || busy || maintenanceRunning || !rootGranted) { toast("请先授予 Root 权限"); return }
        busy = true; updateMessage = "准备下载更新…"
        updateRequest = AppUpdateInstaller.install(applicationContext, result,
            { !destroyed && resumed && rootGranted && !maintenanceRunning }, object : AppUpdateInstaller.Callback {
                override fun onStage(stage: AppUpdateInstaller.Stage) {
                    if (!destroyed) updateMessage = when (stage) {
                        AppUpdateInstaller.Stage.DOWNLOADING -> "正在下载正式更新…"
                        AppUpdateInstaller.Stage.VERIFYING -> "正在校验安装包…"
                        AppUpdateInstaller.Stage.INSTALLING -> "正在覆盖安装，配置会保留…"
                    }
                }
                override fun onResult(success: Boolean, message: String) {
                    if (!destroyed) { updateRequest = null; busy = false; updateMessage = message; toast(message) }
                }
            })
    }
    fun requestDesktopIconHidden(hidden: Boolean) {
        if (freeNoticeRequired || busy || maintenanceRunning) return
        val apply = {
            val result = LauncherIcon.setHidden(this, hidden)
            if (result.success) desktopIconHidden = result.hidden
            toast(result.message)
        }
        if (hidden) confirmation = UiConfirmation("隐藏桌面图标", "隐藏后，模块和配置继续生效。可从 LSPosed 管理器的“模块设置”重新打开本应用，并在这里恢复图标。", "隐藏", action = apply)
        else apply()
    }

    fun dismissConfirmation() { confirmation?.cancelAction?.invoke(); confirmation = null }

    fun requestLegacyName(legacy: Boolean) {
        if (busy || maintenanceRunning) return
        val result = LauncherIcon.setLegacyName(this, legacy)
        if (result.success) { legacyName = result.legacyName; desktopIconHidden = result.hidden }
        toast(result.message)
    }

    /** The name shown in the app: the launcher name that is chosen. */
    val appDisplayName get() = if (legacyName) LauncherIcon.LEGACY_NAME else LauncherIcon.NAME

    fun openThanks() {
        thanksWall = ThanksWall.load(this)
        thanksOpen = true
        thanksStatus = thanksCheckedLabel()
        // Only when the last successful check is more than a day old; the page has its own refresh.
        if (ThanksWall.due(this)) refreshThanks()
    }

    fun refreshThanks() {
        if (thanksRefreshing || destroyed) return
        thanksRefreshing = true
        thanksStatus = "正在检查名单更新…"
        ThanksWall.refresh(this) { result ->
            if (destroyed) return@refresh
            thanksRefreshing = false
            result.wall?.let { thanksWall = it }
            thanksStatus = result.message
        }
    }

    private fun thanksCheckedLabel(): String {
        val last = ThanksWall.lastCheck(this)
        return if (last <= 0L) "尚未联网检查"
            else "检查于 " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.ROOT).format(java.util.Date(last))
    }
    fun openUrl(url: String) { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.onFailure { toast("没有可用的浏览器") } }
    fun checkUpdates() {
        if (!resumed || destroyed || checkingUpdates || busy || maintenanceRunning) return
        checkingUpdates = true
        updateMessage = "正在检查 GitHub 正式发布…"
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName ?: "" }.getOrDefault("")
        // The existing checker performs bounded HTTP work off the main thread and posts its result to it.
        GitHubUpdates.fetch(version) { result ->
            if (destroyed) return@fetch
            checkingUpdates = false
            latestUpdate = result
            updateMessage = result.message
            // A delayed result must not replace a parameter trial, file confirmation or font dialog.
            if (resumed && page == 2 && confirmation == null && editing == null && colorEditing == null &&
                !fontCatalogOpen && fontScopeSelection == null && downloadingFont == null && !busy) {
                val details = buildString {
                    append(result.message)
                    if (result.version.isNotEmpty()) append("\n发布版本：${result.version}")
                    if (result.body.isNotEmpty()) append("\n\n${result.body}")
                }
                val download = result.newer && GitHubUpdates.isApkUrl(result.apkUrl)
                val rootInstall = download && rootGranted && result.canInstall()
                confirmation = UiConfirmation(if (result.newer) "发现新版本" else "更新检查", details,
                    if (rootInstall) "下载并安装" else if (download) "下载 APK" else "查看正式发布") {
                    if (rootInstall) installUpdate(result) else if (download) openUrl(result.apkUrl) else openLatestRelease()
                }
            }
        }
    }
    fun openLatestRelease() {
        val url = latestUpdate?.releaseUrl?.takeIf(GitHubUpdates::isReleaseUrl)
            ?: "${GitHubUpdates.REPO_URL}/releases/latest"
        openUrl(url)
    }
    private fun queueFile(kind: String, uri: Uri) {
        val permission = if (kind == "export") Intent.FLAG_GRANT_WRITE_URI_PERMISSION else Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (kind != "notification_icon") runCatching { contentResolver.takePersistableUriPermission(uri, permission) }
        pendingFile = kind to uri; replayFile()
    }
    private fun replayFile() {
        if (!resumed || !canEdit || busy || destroyed || confirmation != null || configScope != null) return
        val action = pendingFile ?: return
        busy = true
        if (action.first == "font") { prepareFont(action.second); return }
        if (action.first == "notification_icon") { prepareNotificationIcon(action.second); return }
        if (action.first == "icon_pack") { prepareIconPack(action.second); return }
        if (action.first == "pui_theme") { pendingFile = null; busy = false; toast("旧版主题导入已移除，请在图标库中安装内置 PUI 套装"); return }
        val snapshot = values
        work.execute {
            try {
                if (action.first == "export") {
                    val keys = exportSections?.let { ConfigScope.keys(it) }
                    contentResolver.openOutputStream(action.second, "wt").use { ConfigTransfer.write(it, snapshot, keys) }
                    runOnUiThread { pendingFile = null; exportSections = null; busy = false; toast("配置已导出") }
                } else {
                    val prepared = contentResolver.openInputStream(action.second).use { ConfigTransfer.read(it, this) }
                    runOnUiThread {
                        busy = false
                        // The file is read; now ask whether to take everything or only some pages.
                        if (!destroyed) configScope = ConfigScopeRequest("import", prepared)
                    }
                }
            } catch (error: Exception) {
                runOnUiThread { pendingFile = null; exportSections = null; busy = false; toast("${if (action.first == "export") "导出" else "导入"}失败：${error.message}") }
            }
        }
    }
    private fun prepareFont(uri: Uri) {
        var name = "自选字体"
        runCatching { contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0).take(120)
        } }
        val displayName = FontLibrary.displayName(name)
        val previousFont = values
        work.execute {
            try {
                FontLibrary.retainCurrent(applicationContext, previousFont[StatusBarSettings.FONT_REVISION]?.toString(), previousFont[StatusBarSettings.FONT_NAME]?.toString())
                val prepared = FontRepository.prepareFont(applicationContext, uri)
                val revision = prepared.revision
                prepared.use { FontLibrary.remember(applicationContext, prepared, displayName) }
                runOnUiThread {
                    pendingFile = null; busy = false
                    if (!destroyed) {
                        refreshFontLibrary()
                        if (resumed && canEdit) selectFontScopes("custom", displayName, revision)
                        toast("字体已导入；选择作用范围并应用后生效")
                    }
                }
            } catch (error: Exception) { runOnUiThread { pendingFile = null; busy = false; toast("字体导入失败：${error.message}") } }
        }
    }
    private fun prepareNotificationIcon(uri: Uri) {
        work.execute {
            try {
                val name = runCatching { contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) cursor.getString(0).take(120) else "自定义图片"
                } }.getOrDefault("自定义图片")
                val prepared = NotificationIconRepository.prepare(applicationContext, uri)
                runOnUiThread {
                    try {
                        prepared.use {
                            val keys = listOf(NotificationIconArea.MODE, NotificationIconArea.IMAGE_NAME, NotificationIconArea.IMAGE_REVISION)
                            val previous = raw.all
                            prepared.commit({ resumed && canEdit && !destroyed }, {
                                preferences.edit().putString(NotificationIconArea.MODE, "image")
                                    .putString(NotificationIconArea.IMAGE_NAME, name)
                                    .putString(NotificationIconArea.IMAGE_REVISION, prepared.revision).commit()
                            }, {
                                val restore = raw.edit()
                                keys.forEach { key -> if (previous.containsKey(key)) restore.putString(key, previous[key] as String?) else restore.remove(key) }
                                check(restore.commit()) { "原图片配置未能恢复" }
                            })
                            pendingFile = null; busy = false; toast("通知图标图片已导入")
                        }
                    } catch (error: Exception) { pendingFile = null; busy = false; toast("图片导入失败：${error.message}") }
                }
            } catch (error: Exception) { runOnUiThread { pendingFile = null; busy = false; toast("图片导入失败：${error.message}") } }
        }
    }
    /** Open the album to choose this panel's picture; the editor opens on what was chosen. */
    fun pickShadeWallpaper(scene: String) {
        if (!ShadeWallpaperSettings.available()) { toast(ShadeWallpaperSettings.unavailableReason()); return }
        if (!canEdit || busy || !ShadeWallpaperSettings.validScene(scene)) return
        wallpaperImportScene = scene
        pickWallpaperImage.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    /** Cut the panel's current picture again. */
    fun editShadeWallpaper(scene: String) {
        if (!canEdit || busy || !ShadeWallpaperSettings.validScene(scene)) return
        val revision = value(ShadeWallpaperSettings.revisionKey(scene))?.toString().orEmpty()
        if (!ShadeWallpaperSettings.validRevision(revision)) return
        wallpaperEdit = WallpaperEdit(scene, ShadeWallpaperRepository.uri(scene, revision))
    }
    /** The same picture, as it is, for the other panel. */
    fun copyShadeWallpaper(from: String, to: String) {
        if (!canEdit || busy || !ShadeWallpaperSettings.validScene(from) || !ShadeWallpaperSettings.validScene(to)) return
        val revision = value(ShadeWallpaperSettings.revisionKey(from))?.toString().orEmpty()
        if (!ShadeWallpaperSettings.validRevision(revision)) return
        applyShadeWallpaper(to, ShadeWallpaperRepository.uri(from, revision), null)
    }
    /** Make the (cut) picture this panel's: normalized off the UI thread, then published and switched on. */
    fun applyShadeWallpaper(scene: String, uri: Uri, crop: ShadeWallpaperRepository.Crop?) {
        if (!resumed || !canEdit || busy || destroyed || !ShadeWallpaperSettings.validScene(scene)) return
        busy = true
        prepareShadeWallpaper(scene, uri, crop)
    }
    private fun prepareShadeWallpaper(scene: String, uri: Uri, crop: ShadeWallpaperRepository.Crop?) {
        if (!ShadeWallpaperSettings.available()) { pendingFile = null; busy = false; return }
        work.execute {
            try {
                val prepared = ShadeWallpaperRepository.prepare(applicationContext, uri, scene, crop)
                val accepted = synchronized(wallpaperResourcesLock) {
                    !destroyed && preparedWallpapers.add(prepared)
                }
                if (!accepted) { prepared.close(); return@execute }
                val posted = handler.post {
                    if (destroyed) {
                        synchronized(wallpaperResourcesLock) { preparedWallpapers.remove(prepared) }
                        prepared.close(); return@post
                    }
                    try {
                        prepared.use {
                            val key = ShadeWallpaperSettings.revisionKey(scene)
                            val previous = raw.all[key]
                            prepared.commit({ resumed && canEdit && !destroyed }, {
                                // A picture that was just chosen for this scene is meant to be used: the scene and its page go on with it.
                                preferences.edit().putString(key, prepared.revision).putBoolean(ShadeWallpaperSettings.enabledKey(scene), true)
                                    .putBoolean(ShadeWallpaperSettings.masterKey(scene), true).commit()
                            }, {
                                val restore = raw.edit()
                                if (previous is String) restore.putString(key, previous) else restore.remove(key)
                                check(restore.commit()) { "壁纸配置恢复失败" }
                            })
                            pendingFile = null; busy = false; wallpaperEdit = null
                            work.execute { ShadeWallpaperRepository.prune(applicationContext, scene, prepared.revision) }
                            toast("${ShadeWallpaperSettings.title(scene)}壁纸已导入")
                        }
                    } catch (error: Exception) { pendingFile = null; busy = false; toast("壁纸导入失败：${error.message}") }
                    finally { synchronized(wallpaperResourcesLock) { preparedWallpapers.remove(prepared) } }
                }
                if (!posted) {
                    synchronized(wallpaperResourcesLock) { preparedWallpapers.remove(prepared) }
                    prepared.close()
                }
            } catch (error: Exception) {
                handler.post { if (!destroyed) { pendingFile = null; busy = false; toast("壁纸导入失败：${error.message}") } }
            }
        }
    }
    fun deleteShadeWallpaper(scene: String) {
        if (!resumed || !canEdit || busy || !ShadeWallpaperSettings.validScene(scene)) return
        val saved = preferences.edit().putBoolean(ShadeWallpaperSettings.enabledKey(scene), false)
            .putString(ShadeWallpaperSettings.revisionKey(scene), "").commit()
        if (!saved) { toast("壁纸配置保存失败，图片已保留"); return }
        busy = true
        work.execute {
            val result = runCatching { ShadeWallpaperRepository.delete(applicationContext, scene) }
            handler.post { if (!destroyed) { busy = false; toast(if (result.isSuccess) "这张壁纸已删除" else "配置已关闭，图片删除失败，请重试") } }
        }
    }
    fun openNotificationOverrides() {
        notificationOverridesOpen = true
        work.execute {
            val apps = runCatching {
                packageManager.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS).map { info ->
                    ApplicationChoice(info.packageName, runCatching { info.loadLabel(packageManager).toString() }.getOrDefault(info.packageName),
                        info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
            }.getOrDefault(emptyList())
            handler.post { if (!destroyed) applicationChoices = apps }
        }
    }
    fun applicationLabel(packageName: String): String = applicationChoices.firstOrNull { it.packageName == packageName }?.label ?: packageName
    fun clockLaunchLabel(key: String): String {
        val selected = value(key)?.toString().orEmpty()
        val component = BigClockWidgetsSettings.launchAppKey(key)
        if (selected.isBlank()) return if (component) "未选择应用" else "系统默认"
        val unavailable = if (component) "所选应用不可用，请重新选择" else "系统默认（所选应用不可用）"
        return runCatching {
            if (ClockLaunchHelper.customIntent(packageManager, selected) == null) unavailable
            else packageManager.getApplicationLabel(packageManager.getApplicationInfo(selected, 0)).toString()
        }.getOrDefault(unavailable)
    }
    fun openClockLaunchPicker(key: String) {
        if (!canEdit || busy || !clockLaunchEnabled(key)) return
        clockLaunchEditing = key; clockLaunchLoading = true; clockLaunchChoices = emptyList(); clockLaunchDefault = null
        val request = ++clockLaunchRequest
        work.execute {
            val nativeDefault = if (BigClockWidgetsSettings.launchAppKey(key)) null else runCatching {
                val intent = ClockLaunchHelper.systemIntent(this, key == NotificationBigClockSettings.DATE_LAUNCH_APP,
                    System.currentTimeMillis()) ?: return@runCatching null
                val target = intent.component?.packageName ?: return@runCatching null
                val info = packageManager.getApplicationInfo(target, 0)
                ApplicationChoice(target, info.loadLabel(packageManager).toString(),
                    info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
            }.getOrNull()
            val apps = runCatching {
                packageManager.getInstalledApplications(0).mapNotNull { info ->
                    if (ClockLaunchHelper.customIntent(packageManager, info.packageName) == null) null
                    else ApplicationChoice(info.packageName,
                        runCatching { info.loadLabel(packageManager).toString() }.getOrDefault(info.packageName),
                        info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
            }.getOrDefault(emptyList())
            handler.post { if (!destroyed && clockLaunchRequest == request && clockLaunchEditing == key) {
                clockLaunchChoices = apps; clockLaunchDefault = nativeDefault; clockLaunchLoading = false
            } }
        }
    }
    fun dismissClockLaunchPicker() {
        ++clockLaunchRequest
        clockLaunchEditing = null; clockLaunchLoading = false; clockLaunchChoices = emptyList(); clockLaunchDefault = null
    }
    fun clockLaunchEnabled(key: String): Boolean = when (key) {
        NotificationBigClockSettings.CLOCK_LAUNCH_APP -> StatusBarSettings.bool(values, NotificationBigClockSettings.CLOCK_CLICK_ENABLED)
        NotificationBigClockSettings.DATE_LAUNCH_APP -> StatusBarSettings.bool(values, NotificationBigClockSettings.DATE_CLICK_ENABLED)
        else -> if (BigClockWidgetsSettings.launchAppKey(key)) {
            val side = BigClockWidgetsSettings.textSide(key)
            StatusBarSettings.bool(values, BigClockWidgetsSettings.enabledKey(side)) &&
                StatusBarSettings.bool(values, BigClockWidgetsSettings.clickEnabledKey(side))
        } else false
    }
    fun saveClockLaunchApp(key: String, packageName: String): String {
        if (!canEdit || busy) return "请先激活模块或授予 Root，并等待当前操作完成"
        if (!clockLaunchEnabled(key)) return "请先开启对应的点击操作"
        if (BigClockWidgetsSettings.launchAppKey(key)) {
            BigClockWidgetsSettings.launchAppValidationError(packageName)?.let { return it }
        }
        if (packageName.isNotEmpty() && ClockLaunchHelper.customIntent(packageManager, packageName) == null)
            return "所选应用已卸载、被禁用或没有可启动入口"
        return if (saveManagedString(key, packageName)) { dismissClockLaunchPicker(); "" } else "保存失败，请重试"
    }
    private fun saveManagedString(key: String, json: String): Boolean {
        if (!resumed || !canEdit || destroyed) { toast("激活模块或授予 Root 后即可保存配置"); return false }
        val result = runCatching { preferences.edit().putString(key, json).commit() }.getOrDefault(false)
        values = raw.all
        if (!result) toast("配置未能完整保存，请重试")
        return result
    }
    fun saveNotificationOverride(packageName: String, text: String, enabled: Boolean, oldPackage: String? = null): String {
        if (!resumed || !canEdit || busy) return "请先激活模块或授予 Root"
        return try {
            var source = value(NotificationIconOverrides.RULES)?.toString() ?: "[]"
            if (packageName != oldPackage && NotificationIconOverrides.rules(source).any { it.packageName == packageName })
                return "该应用已设置，请编辑现有规则"
            if (!oldPackage.isNullOrBlank() && oldPackage != packageName) source = NotificationIconOverrides.remove(source, oldPackage)
            val updated = NotificationIconOverrides.put(source, packageName, text, enabled)
            if (saveManagedString(NotificationIconOverrides.RULES, updated)) "" else "保存失败，请重试"
        } catch (error: Exception) { error.message ?: "应用规则无效" }
    }
    fun removeNotificationOverride(packageName: String) {
        if (busy) return
        runCatching { NotificationIconOverrides.remove(value(NotificationIconOverrides.RULES)?.toString() ?: "[]", packageName) }
            .onSuccess { saveManagedString(NotificationIconOverrides.RULES, it) }.onFailure { toast(it.message ?: "规则删除失败") }
    }
    fun saveNotificationAppIconOverride(packageName: String, sourcePackage: String, enabled: Boolean, previousPackage: String? = null): String {
        if (!resumed || !canEdit || busy) return "请先激活模块或授予 Root"
        return try {
            var source = value(NotificationIconOverrides.RULES)?.toString() ?: "[]"
            if (packageName != previousPackage && NotificationIconOverrides.rules(source).any { it.packageName == packageName })
                return "该应用已设置，请编辑现有规则"
            if (!previousPackage.isNullOrBlank() && previousPackage != packageName) source = NotificationIconOverrides.remove(source, previousPackage)
            val updated = NotificationIconOverrides.putAppIcon(source, packageName, sourcePackage, enabled)
            if (enabled) packageManager.getApplicationInfo(sourcePackage, 0)
            if (saveManagedString(NotificationIconOverrides.RULES, updated)) "" else "保存失败，请重试"
        } catch (error: Exception) { error.message?.takeIf { it.isNotBlank() } ?: "所选应用图标不可用" }
    }
    fun iconManagementRecords(): String = values[IconManagementSettings.KEY]?.toString() ?: IconManagementSettings.DEFAULT
    fun iconManagementEnabled(): Boolean = values[IconManagementSettings.ENABLED] as? Boolean ?: true
    fun setIconManagementEnabled(enabled: Boolean): String {
        if (!resumed || !canEdit || busy || destroyed) return "请先激活模块或授予 Root"
        return runCatching {
            val previous = raw.all
            if (!preferences.edit().putBoolean(IconManagementSettings.ENABLED, enabled).commit()) {
                val restore = raw.edit()
                if (previous.containsKey(IconManagementSettings.ENABLED)) restore.putBoolean(IconManagementSettings.ENABLED, previous[IconManagementSettings.ENABLED] as? Boolean ?: true)
                else restore.remove(IconManagementSettings.ENABLED)
                check(restore.commit()) { "总开关未能保存，原值未能完整恢复" }
                error("总开关未能保存，已保留原值")
            }
            values = raw.all
            ""
        }.getOrElse { it.message ?: "总开关未能保存" }
    }
    fun iconManagementState(): IconManagementSettings.State = runCatching { IconManagementSettings.parse(iconManagementRecords()) }
        .getOrElse { IconManagementSettings.parse(IconManagementSettings.DEFAULT) }
    fun openIconManagement(section: String = "home", scope: String? = null) {
        iconLibraryTarget = null
        iconManagementDestination = IconManagementDestination(section, scope)
        iconManagementOpen = true; updateNavigation(); refreshIconLibraries()
    }
    fun openIconAssignments(category: String = "hint") = openIconManagement("scope", category.takeIf { IconRoleRegistry.validScope(it) } ?: "hint")
    fun openIconLibrary() = openIconManagement("library")
    fun openIconLibraryForTarget(targetId: String) {
        if (busy || IconRoleRegistry.target(targetId)?.supported != true) return
        openIconManagement("library")
        iconLibraryTarget = targetId
    }
    fun finishIconLibraryForTarget(packId: String) {
        val target = IconRoleRegistry.target(iconLibraryTarget) ?: return
        if (busy || iconPacks.none { it.id == packId }) return
        iconLibraryTarget = null
        iconManagementDestination = IconManagementDestination("target_pick", target.scope, packId, target.id)
    }
    fun finishIconPackApplication() {
        val target = IconRoleRegistry.target(iconLibraryTarget)
        iconLibraryTarget = null
        iconManagementDestination = if (target == null) IconManagementDestination()
            else IconManagementDestination("target", target.scope, targetId = target.id)
        toast((if (iconManagementEnabled()) "套装已应用" else "套装选择已保存，开启总开关后生效") +
            if (target != null) "；当前目标草稿尚未保存" else "")
    }
    fun closeIconManagement() {
        if (busy) return
        cancelIconImport(); iconLibraryTarget = null; iconManagementOpen = false; iconManagementBackRequest = null; updateNavigation()
    }
    fun refreshIconLibraries() {
        if (destroyed || maintenanceRunning) return
        val request = ++iconLibraryRequest
        iconLibrariesLoading = true
        work.execute {
            val result = runCatching {
                val migration = IconManagementMigration.ensure(applicationContext, raw)
                migration to IconManagementRepository.list(applicationContext)
            }
            handler.post { if (!destroyed && request == iconLibraryRequest) {
                iconLibrariesLoading = false
                result.onSuccess { (migration, packs) ->
                    iconPacks = packs
                    iconMigrationCompleted = migration.noticePending
                    iconMigrationNotice = if (migration.noticePending) "图标管理已升级，旧图标库和替换设置已重置，请重新选择 v3 图标包。" +
                        migration.warning.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
                        else migration.warning.takeIf { it.isNotBlank() }
                }.onFailure { toast("图标库读取失败：${it.message}") }
            } }
        }
    }
    fun acknowledgeIconMigration() {
        work.execute {
            val result = runCatching { IconManagementMigration.acknowledge(applicationContext) }
            handler.post { if (!destroyed) {
                if (result.isSuccess) iconMigrationNotice = null else toast("升级提示保存失败，请重试")
            } }
        }
    }
    private fun saveIconRecords(records: String): String {
        if (!resumed || !canEdit || destroyed || busy) return "请先激活模块或授予 Root"
        return runCatching {
            IconManagementSettings.parse(records)
            val previous = raw.all
            if (!preferences.edit().putString(IconManagementSettings.KEY, records).commit()) {
                val restore = raw.edit()
                if (previous.containsKey(IconManagementSettings.KEY)) restore.putString(IconManagementSettings.KEY, previous[IconManagementSettings.KEY] as String?)
                else restore.remove(IconManagementSettings.KEY)
                check(restore.commit()) { "图标设置保存失败，原设置未能完整恢复" }
                error("图标设置未能保存，已保留原设置")
            }
            values = raw.all
            ""
        }.getOrElse { it.message ?: "图标设置未能保存" }
    }
    fun applyIconPack(id: String, scopes: Set<String>, clearOverrides: Boolean, presetId: String? = null): String = runCatching {
        val pack = iconPacks.firstOrNull { it.id == id } ?: error("图标库已不存在，请重新导入")
        var records = IconManagementSettings.applyPack(iconManagementRecords(), id, scopes, clearOverrides)
        if (presetId != null) records = IconManagementSettings.applyColorPreset(records, IconColorPresets.options(pack.manifest)[presetId] ?: error("颜色预设已变化"), scopes)
        saveIconRecords(records)
    }.getOrElse { it.message ?: "套装应用失败" }
    fun applyIconColorPreset(preset: IconColorPresets.Preset, scopes: Set<String>): String = runCatching {
        saveIconRecords(IconManagementSettings.applyColorPreset(iconManagementRecords(), preset, scopes))
    }.getOrElse { it.message ?: "颜色预设未能保存" }
    fun clearIconScopePack(scope: String): String = runCatching {
        saveIconRecords(IconManagementSettings.clearScopePack(iconManagementRecords(), scope))
    }.getOrElse { it.message ?: "范围来源未能恢复" }
    fun applyIconTarget(targetId: String, sources: Map<String, IconManagementSettings.Override>,
        appearance: IconManagementSettings.Appearance?, stateAppearances: Map<String, IconManagementSettings.Appearance>, animationEnabled: Boolean): String = runCatching {
        val target = IconRoleRegistry.target(targetId) ?: error("图标目标已变化，请重新选择")
        check(target.supported) { target.reason.ifBlank { "当前目标保留系统图案" } }
        sources.values.filter { it.mode == "pack" }.forEach { source ->
            check(iconPacks.any { it.id == source.packId && it.manifest.icons.containsKey(source.sourceRole) }) { "图案来源已缺失，请重新选择" }
        }
        saveIconRecords(IconManagementSettings.applyTarget(iconManagementRecords(), targetId, sources, appearance, stateAppearances, animationEnabled))
    }.getOrElse { it.message ?: "图标设置未能保存" }
    fun applyIconAppearance(key: String, appearance: IconManagementSettings.Appearance?): String = runCatching {
        saveIconRecords(IconManagementSettings.applyAppearance(iconManagementRecords(), key, appearance))
    }.getOrElse { it.message ?: "外观设置未能保存" }
    fun resetIconManagement() {
        if (!canEdit || busy) return
        confirmation = UiConfirmation("恢复图标管理默认设置", "清除套装应用、单项图案、颜色和透明度设置，已导入的图标库保留。", "恢复默认") {
            val error = saveIconRecords(IconManagementSettings.reset())
            toast(error.ifEmpty { "图标管理已恢复默认" })
        }
    }
    fun iconLibraryNameError(name: String, excludeId: String? = null): String = runCatching {
        val key = IconManagementRepository.nameKey(name)
        if (iconPacks.any { it.id != excludeId && IconManagementRepository.nameKey(it.name) == key })
            "已有同名图标库，请输入不同名称" else ""
    }.getOrElse { it.message ?: "请输入有效的图标库名称" }
    fun renameIconPack(id: String, name: String) {
        if (busy || !canEdit || !resumed) return
        val invalid = iconLibraryNameError(name, id)
        if (invalid.isNotEmpty()) { toast(invalid); return }
        busy = true
        work.execute {
            val result = runCatching { IconManagementRepository.rename(applicationContext, id, name) }
            handler.post { if (!destroyed) {
                busy = false
                result.onSuccess { pack -> iconPacks = iconPacks.map { if (it.id == id) pack else it }; toast("名称已保存") }
                    .onFailure { toast("重命名失败：${it.message}") }
            } }
        }
    }
    fun deleteIconPack(id: String, name: String) {
        if (busy || !canEdit) return
        val dependencies = runCatching { IconManagementSettings.dependencies(iconManagementRecords(), id) }.getOrDefault(0)
        confirmation = UiConfirmation("删除图标库", "删除“$name”及其资源。${if (dependencies > 0) "引用它的 $dependencies 项套装或单项选择将恢复默认。" else ""}独立颜色和透明度保留。", "删除") {
            if (!resumed || !canEdit || busy) return@UiConfirmation
            val previous = raw.all
            val next = runCatching { IconManagementSettings.removePack(iconManagementRecords(), id) }
            if (next.isFailure) { toast(next.exceptionOrNull()?.message ?: "图标配置无效"); return@UiConfirmation }
            busy = true
            work.execute {
                try {
                    val prepared = IconManagementRepository.prepareDeletion(applicationContext, id)
                    handler.post {
                        val result = runCatching { prepared.use {
                            prepared.commit({ resumed && canEdit && !destroyed && !maintenanceRunning }, {
                                preferences.edit().putString(IconManagementSettings.KEY, next.getOrThrow()).commit()
                            }, {
                                val restore = raw.edit()
                                if (previous.containsKey(IconManagementSettings.KEY)) restore.putString(IconManagementSettings.KEY, previous[IconManagementSettings.KEY] as String?)
                                else restore.remove(IconManagementSettings.KEY)
                                check(restore.commit()) { "原图标配置未能恢复" }
                            })
                        } }
                        busy = false
                        if (!destroyed) {
                            values = raw.all
                            result.onSuccess {
                                iconPacks = iconPacks.filter { it.id != id }
                                iconManagementDestination = IconManagementDestination("library")
                                toast("图标库已删除")
                            }.onFailure { toast("图标库未删除：${it.message}"); refreshIconLibraries() }
                        }
                    }
                } catch (error: Exception) { handler.post {
                    busy = false
                    if (!destroyed) { toast("图标库未删除：${error.message}"); refreshIconLibraries() }
                } }
            }
        }
    }
    fun chooseIconPack() {
        if (canEdit && !busy && iconPendingImport == null) importIconPack.launch(arrayOf("*/*"))
        else if (!canEdit) toast("请先激活模块或授予 Root")
    }
    private fun prepareIconPack(uri: Uri) {
        work.execute {
            try { completeIconPack(IconManagementRepository.prepare(applicationContext, uri, ""), null) }
            catch (error: Exception) { handler.post { if (!destroyed) {
                pendingFile = null; busy = false; toast("图标库导入失败：${error.message}")
            } } }
        }
    }
    fun downloadIconPack(source: String, name: String) {
        if (!resumed || !canEdit || busy || destroyed || iconPendingImport != null) return
        if (source.isBlank()) { toast("请填写 GitHub 仓库或下载网址"); return }
        val token = IconManagementDownload.Cancellation()
        iconPackCancellation = token; iconPackDownloading = true; busy = true
        work.execute {
            try { completeIconPack(IconManagementDownload.prepare(applicationContext, source, name, token), token) }
            catch (error: Exception) { handler.post { if (!destroyed && iconPackCancellation === token) {
                iconPackCancellation = null; iconPackDownloading = false; busy = false
                toast("图标库下载失败：${error.message}")
            } } }
        }
    }
    private fun completeIconPack(prepared: IconManagementRepository.PreparedPack, token: IconManagementDownload.Cancellation?, builtin: String? = null) {
        val status = try { prepared.status() } catch (failure: Exception) { prepared.close(); throw failure }
        val accepted = synchronized(iconResourcesLock) { !destroyed && preparedIconImports.add(prepared) }
        if (!accepted) { prepared.close(); return }
        handler.post {
            if (destroyed || token != null && (iconPackCancellation !== token || token.asBoolean)) {
                synchronized(iconResourcesLock) { preparedIconImports.remove(prepared) }; prepared.close(); return@post
            }
            pendingFile = null; iconPackCancellation = null; iconPackDownloading = false; busy = false
            iconPendingImport = prepared
            iconPendingBuiltin = builtin
            iconImportExisting = status.existing
            iconImportName = status.existing?.name ?: prepared.pack.name
            iconImportError = if (status.nameConflict != null) "已有同名图标库，请输入不同名称" else ""
            iconManagementOpen = true; updateNavigation()
        }
    }
    fun cancelIconImport() {
        val prepared = iconPendingImport ?: return
        iconPendingImport = null
        iconPendingBuiltin = null
        iconImportExisting = null; iconImportName = ""; iconImportError = ""
        synchronized(iconResourcesLock) { preparedIconImports.remove(prepared) }
        work.execute { prepared.close() }
    }
    fun confirmIconImport() = publishPendingIconPack(null, false)
    fun commitIconImportAndApply(scopes: Set<String>, clearOverrides: Boolean, presetId: String? = null) = publishPendingIconPack(scopes, clearOverrides, presetId)
    private fun publishPendingIconPack(scopes: Set<String>?, clearOverrides: Boolean, presetId: String? = null) {
        val prepared = iconPendingImport ?: return
        if (!resumed || !canEdit || busy || destroyed) return
        val existing = iconImportExisting
        val name = iconImportName.trim()
        val invalid = if (existing == null) iconLibraryNameError(name, prepared.pack.id) else ""
        if (invalid.isNotEmpty()) { iconImportError = invalid; return }
        if (scopes != null) {
            val validation = runCatching {
                val proposed = IconManagementSettings.applyPack(iconManagementRecords(), prepared.pack.id, scopes, clearOverrides)
                if (presetId == null) proposed else IconManagementSettings.applyColorPreset(proposed, IconColorPresets.options(prepared.pack.manifest)[presetId] ?: error("颜色预设已变化"), scopes)
            }
            if (validation.isFailure) { iconImportError = validation.exceptionOrNull()?.message ?: "请选择应用范围"; return }
        }
        iconImportError = ""; busy = true
        work.execute {
            val result = runCatching {
                val canCommit = java.util.function.BooleanSupplier { resumed && canEdit && !destroyed && !maintenanceRunning }
                if (existing != null) prepared.useExisting(canCommit) else prepared.commit(name, canCommit)
            }
            // A rejected name keeps the staging area open so the user can correct it without importing again.
            val retryStatus = if (result.isFailure) runCatching { prepared.status() }.getOrNull() else null
            if (result.isSuccess) synchronized(iconResourcesLock) { preparedIconImports.remove(prepared) }
            handler.post { if (!destroyed) {
                busy = false
                result.onSuccess { pack ->
                    if (iconPendingImport === prepared) {
                        iconPendingImport = null; iconPendingBuiltin = null
                        iconImportExisting = null; iconImportName = ""; iconImportError = ""
                    }
                    iconPacks = iconPacks.filter { it.id != pack.id } + pack
                    if (scopes != null) {
                        iconManagementDestination = IconManagementDestination("apply", packId = pack.id)
                        val error = runCatching {
                            var proposed = IconManagementSettings.applyPack(iconManagementRecords(), pack.id, scopes, clearOverrides)
                            if (presetId != null) proposed = IconManagementSettings.applyColorPreset(proposed, IconColorPresets.options(pack.manifest)[presetId] ?: error("颜色预设已变化"), scopes)
                            saveIconRecords(proposed)
                        }
                            .getOrElse { it.message ?: "图标设置未能保存" }
                        if (error.isEmpty()) finishIconPackApplication()
                        else toast("图标库已保存，应用失败：$error")
                    } else if (iconLibraryTarget != null) {
                        finishIconLibraryForTarget(pack.id)
                        toast(if (existing != null) "使用已有图标库，请选择图案" else "请选择该图标使用的图案")
                    } else {
                        iconManagementDestination = IconManagementDestination("pack", packId = pack.id)
                        toast(if (existing != null) "已使用已有图标库，未重复导入" else "图标库已保存，可选择范围应用")
                    }
                }.onFailure { failure ->
                    if (iconPendingImport === prepared) {
                        iconImportExisting = retryStatus?.existing
                        retryStatus?.existing?.let { iconImportName = it.name }
                        iconImportError = failure.message ?: "图标库未能保存，请重试"
                    }
                }
            } }
        }
    }
    fun installOriginalIcons() = prepareBundledIcons("original")
    fun installPuiIcons() = prepareBundledIcons("pui")
    fun prepareBundledIcons(kind: String) {
        if (!resumed || !canEdit || busy || iconPendingImport != null) return
        busy = true
        work.execute {
            try { completeIconPack(IconManagementRepository.prepareBundled(applicationContext, kind), null, kind) }
            catch (error: Exception) { handler.post { if (!destroyed) { busy = false; toast("套装预览失败：${error.message}") } } }
        }
    }
    fun cancelIconPackDownload() {
        iconPackCancellation?.cancel(); iconPackCancellation = null; iconPackDownloading = false; busy = false
    }
    fun pickColor(item: SettingsCatalog.Item) {
        if (!canEdit) { toast("请先激活模块或授予 Root"); return }
        colorEditing = item
    }
    fun saveColor(item: SettingsCatalog.Item, color: Int, customAlpha: Boolean) {
        if (resumed && canEdit) {
            val editor = preferences.edit().putInt(item.key, color).putBoolean(item.alphaKey, customAlpha)
            StatusBarSettings.markColorCustomized(editor, item.key)
            editor.apply()
        }
    }
    fun resetGroup(group: SettingsCatalog.Group, items: List<SettingsCatalog.Item> = group.items, title: String = group.title) {
        confirmation = UiConfirmation("恢复${title}默认设置", "只恢复当前页面的设置，其他配置会保留。", "恢复默认") {
            if (!canEdit) { toast("请先激活模块或授予 Root"); return@UiConfirmation }
            val editor = preferences.edit()
            val before = raw.all
            items.forEach {
                // Resetting a shared page disables sharing; keep the inactive independent label gaps.
                if (StatusBarLayoutSettings.editingKey(it.key, before) != it.key) return@forEach
                editor.remove(it.key)
                if (it.type == "color") {
                    editor.remove(StatusBarSettings.alphaKey(it.key))
                    StatusBarSettings.clearColorCustomization(editor, it.key)
                }
            }
            editor.apply()
        }
    }
    fun toast(message: String) { if (!destroyed) Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}

/** 一次分享、导出或导入在“是否自定义项”这一步的状态：kind 为 export、share 或 import；导入时带着已读好的文件内容。 */
data class ConfigScopeRequest(val kind: String, val prepared: ConfigTransfer.PreparedImport? = null,
    val customizing: Boolean = false, val selected: Set<String> = emptySet())

data class UiConfirmation(val title: String, val summary: String, val button: String, val delaySeconds: Int = 0,
    val expiresAt: Long = 0, val linkLabel: String = "", val linkUrl: String = "",
    val cancelAction: (() -> Unit)? = null, val action: () -> Unit)

data class StatusBarOrderChoice(val title: String, val values: List<String>, val labels: List<String>,
    val selected: String, val description: String = "", val onChoice: (String) -> Unit)

/** The same-app mini stack's settings page id (SettingsCatalog.miniStack). */
const val MINI_STACK_GROUP = "notification_mini_stack"
