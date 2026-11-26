package com.bellatrixos.widgets

import com.bellatrixos.widgets.lang.sha256Hex
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.os.FileObserver
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.bellatrixos.system.AppLanguage
import com.bellatrixos.BuildConfig
import com.bellatrixos.R
import com.bellatrixos.storage.LauncherData
import com.bellatrixos.Runtime
import com.bellatrixos.linux.Agent
import com.bellatrixos.linux.Proot
import com.bellatrixos.linux.Distro
import com.bellatrixos.linux.runJob
import com.bellatrixos.ui.Palette
import com.bellatrixos.theme.Style
import com.bellatrixos.theme.StyleStore
import com.bellatrixos.ui.nativeui.NativeScript
import com.bellatrixos.ui.nativeui.NativeUiSpec
import com.bellatrixos.ui.LauncherStyle
import com.bellatrixos.upgrade.Managed
import com.bellatrixos.upgrade.Policy
import com.bellatrixos.upgrade.writeAtomically
import com.bellatrixos.widgets.lang.CompileError
import com.bellatrixos.widgets.lang.Compiler
import com.bellatrixos.widgets.lang.Json
import com.bellatrixos.widgets.lang.OwlKind
import com.bellatrixos.widgets.lang.Program
import com.bellatrixos.widgets.lang.Source
import com.bellatrixos.widgets.lang.phoneLanguage
import com.bellatrixos.widgets.runtime.WidgetInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** A widget file, compiled. [program] is null when it doesn't compile; [error] says why. */
class WidgetSpec(
    val file: File,
    val name: String,
    val page: Int,
    val order: Int,
    val program: Program?,
    val error: String? = null,
    /** Preferred size in grid cells; used when the widget is first placed. */
    val size: Pair<Int, Int>? = null,
    /** Resizing stops here. */
    val minSize: Pair<Int, Int> = 1 to 1,
    /** The file's modification time when it was compiled; unchanged files aren't recompiled. */
    val stamp: Long = 0,
    /** A home tile or an app in the drawer; known from the first word even when it doesn't compile. */
    val kind: OwlKind = OwlKind.Widget,
) {
    /** A face file: it is its home page; see [WidgetLayout.owner]. Known from the first word, so also while it doesn't compile. */
    val isFace get() = kind == OwlKind.Face

    fun onPage(page: Int) = WidgetSpec(file, name, page, order, program, error, size, minSize, stamp, kind)
}

/** The owl app in file [file], as a pinned shortcut names it; null once it's gone or no longer an app. */
fun List<WidgetSpec>.app(file: String) = firstOrNull { it.file.name == file && it.kind == OwlKind.App }

/**
 * Loads widgets (`*.owl`) from [dir], ~/.config/bellatrix/widgets in the guest,
 * reloads files as they change, and runs each widget's sources while its
 * page is on screen. Commands run inside the guest, never with Android
 * permissions.
 */
class WidgetEngine(
    private val context: Context,
    private val proot: Proot,
    private val runtime: Runtime,
) {
    /** Outside the rootfs, so widgets work without Linux; the guest sees it at ~/[GUEST_DIR]. */
    val dir = LauncherData.widgetsDir(context)
    private val errorDir get() = File(dir, ".errors")

    /** What `owl-check` reads: per file, the SHA-256 the launcher last compiled, the result and the preview. */
    private val checkDir get() = File(dir, ".check")

    /** Where `owl-send` drops requests (<id>.req) and reads the answers (<id>.done); see [receive]. */
    private val inboxDir get() = File(dir, ".inbox")

    /** Tiles rendered off screen for agents, who can't see the phone; see [Previews]. */
    private val previewDir get() = File(dir, ".preview")

    /** SHA-256 of each widget file as last compiled. */
    private val hashes = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** `persist` values, launcher-side like the grid layout, so widget files stay the agent's. */
    private val stateDir = File(context.filesDir, "widget-state")
    val specs = mutableStateListOf<WidgetSpec>()
    /**
     * Observed: Linux coming up after the first load recomposes a tile that needs it before the reload starts its
     * instance, and when that reload changes no spec nothing else would draw the tile again.
     */
    private val instances = mutableStateMapOf<String, WidgetInstance>()
    val layout = WidgetLayout(context)
    val shortcuts = Shortcuts(context, layout)

    /** layout.toml, which [layout] and [shortcuts] write to and an agent's edits come in through. */
    val home =
        HomeStore(context, this, runtime).also { home ->
            layout.onSave = home::changed
            shortcuts.onSave = home::changed
        }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val services = WidgetServices(context, proot, runtime, scope)

    /** Opens launcher screens ("style" or "themes", "agents", "drawer"); set by the UI. */
    var launcherAction by services::launcherAction

    /** What the drawer's long-press menu does, for appAction(...); set by the UI. */
    internal var appHandlers by services::appHandlers

    /** The home screen's view, for haptic(); set by the UI. */
    var view by services::view

    /** android.airpods and airpods(...), which ask the activity in front for Bluetooth. */
    val airpods get() = services.airpods

    /** android.media, and android.modes' Do Not Disturb switch: both need notification access. */
    val media get() = services.media

    /** The store's widgets, and its apps. */
    val market = Market(this, OwlKind.Widget)
    val faceMarket = Market(this, OwlKind.Face)
    val appMarket = Market(this, OwlKind.App)

    /** All three, in the store's order. */
    val markets get() = listOf(market, faceMarket, appMarket)

    /** The store for [kind]; panels have none, so they get the widgets'. */
    fun market(kind: OwlKind) = markets.firstOrNull { it.kind == kind } ?: market

    /** Settings' "update all" for widgets and apps is running. */
    var updating by mutableStateOf(false)
        private set

    /** Why the last "update all" stopped short, or null. */
    var updateError by mutableStateOf<String?>(null)
        private set

    /** Reloads every catalog now, on the engine's scope: leaving settings doesn't cancel it. */
    fun checkUpdates() {
        scope.launch { for (m in markets) m.refresh() }
    }

    /** Installs every store update but edited ones, which only the store replaces, on the engine's scope: leaving settings doesn't stop it. */
    fun updateAll() {
        if (updating) return
        updating = true
        updateError = null
        scope.launch {
            try {
                for (m in markets) {
                    for (e in m.updates.filterNot(m::edited)) {
                        m.install(e, page = 0)?.let { updateError = "${e.name}: $it" }
                    }
                }
            } finally {
                updating = false
            }
        }
    }

    /** Widgets whose packages the user already agreed to (store installs): install them without asking again. */
    private val autoInstall = HashSet<String>()

    /** Declared packages that aren't installed, per widget file. Such widgets wait instead of running. */
    val missing = mutableStateMapOf<String, List<String>>()

    /** Package installs in progress or failed, per widget file. */
    val installs = mutableStateMapOf<String, Install>()

    sealed interface Install {
        data object Running : Install

        data class Failed(
            val log: String,
        ) : Install
    }

    /**
     * Home screens currently started. A count, not a flag: when Android replaces
     * the home activity, the old one's onStop can arrive after the new one's
     * onStart, and a flag would then switch widgets off while they're on screen.
     */
    private var started by mutableIntStateOf(0)

    /** Sources pause while no home screen is showing. */
    val visible get() = started > 0

    fun homeStarted() {
        started++
    }

    fun homeStopped() {
        started = (started - 1).coerceAtLeast(0)
    }

    /**
     * Linux is installed and running. Without it, widgets that need it
     * ([Program.needsLinux]) don't run and show as placeholders in their cells.
     */
    var linux by mutableStateOf(false)
        private set

    /** Linux was installed or removed: starts or stops the widgets that need it; their cells stay theirs either way. */
    fun linuxChanged(on: Boolean) {
        if (linux == on) return
        linux = on
        if (!on) {
            missing.clear()
            installs.clear()
        }
        // Through the reload loop, so it never runs alongside a reload for a file change.
        changes.trySend(false)
    }

    /** [spec] needs Linux, which isn't installed: it shows as a placeholder. */
    fun waitsForLinux(spec: WidgetSpec) = !linux && spec.program?.needsLinux == true

    /** File name of the widget whose screen { } is open, full screen over home; see [screenInstance]. */
    var openScreen by mutableStateOf<String?>(null)

    /** [name] is a running app, which opens in its own task (OwlAppActivity). */
    fun isApp(name: String) = instances[name]?.program?.kind == OwlKind.App

    /** A running app with its spec, for its activity; null once it's removed or stops compiling. */
    fun appInstance(name: String): Pair<WidgetSpec, WidgetInstance>? {
        val spec = specs.app(name) ?: return null
        return instances[name]?.takeIf { it.program.kind == OwlKind.App }?.let { spec to it }
    }

    /** Apps whose activity is started, by file name: a count, like [started]. They stay live while the home screen isn't showing. */
    private val shownApps = mutableStateMapOf<String, Int>()

    fun appStarted(name: String) {
        shownApps[name] = (shownApps[name] ?: 0) + 1
    }

    fun appStopped(name: String) {
        val n = (shownApps[name] ?: 0) - 1
        if (n > 0) shownApps[name] = n else shownApps.remove(name)
    }

    /** Apps that ran close(): their activity closes. */
    val appCloses = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** App files that compile and can run here, for the app drawer. */
    fun apps(): List<WidgetSpec> = specs.filter { it.kind == OwlKind.App && it.program != null && !waitsForLinux(it) }

    /**
     * Panel files, which gesture settings offer for swiping up and down; ones that don't compile too, to say so.
     * The stock drawer and the two declarative surface files aren't swipe panels: [HOME_BAR] and [TASKBAR_BAR]
     * draw over Bellatrix's own home and taskbar, so offering them as a gesture target would open them as a
     * separate full screen.
     */
    fun panels(): List<WidgetSpec> = specs.filter { spec ->
        spec.kind == OwlKind.Panel && spec.file.name !in setOf(STOCK_DRAWER, HOME_BAR, TASKBAR_BAR)
    }

    /**
     * A declarative surface file: a panel-kind OWL file drawn over a native Bellatrix view rather than swiped open.
     * [HOME_BAR] fills the home area above the taskbar; [TASKBAR_BAR] fills the taskbar itself. Both use
     * Flex/Box/Anchor layout (owl 24), so they can tint or cover the native look without a second launcher screen
     * or a second engine.
     */
    fun barInstance(name: String): WidgetInstance? {
        val spec = specs.firstOrNull { it.file.name == name && it.kind == OwlKind.Panel } ?: return null
        if (waitsForLinux(spec)) return null
        return instances[name]?.takeIf { it.program.kind == OwlKind.Panel }
    }

    /**
     * The panel the drawer shows above the prompt bar: the one picked for swiping up, else drawer.owl, else the stock
     * drawer, so a drawer that's gone or broken never leaves the apps out of reach. Null until one runs.
     */
    fun drawerPanel(): String? = listOf(LauncherStyle.style[Style.SWIPE_UP], DRAWER, STOCK_DRAWER).firstOrNull { panelInstance(it) != null }

    /** The running panel [name] with its spec, for home to show; null if it's gone, doesn't compile or can't run here. */
    fun panelInstance(name: String): Pair<WidgetSpec, WidgetInstance>? {
        // Surfaces belong over the native home/taskbar views, never in a swipe panel, even if style.toml names one by hand.
        if (name in BAR_FILES) return null
        val spec = specs.firstOrNull { it.file.name == name && it.kind == OwlKind.Panel } ?: return null
        if (waitsForLinux(spec)) return null
        return instances[name]?.takeIf { it.program.kind == OwlKind.Panel }?.let { spec to it }
    }

    /** Panels on screen, by file name; set by home as they slide in and out. */
    var shownPanels by mutableStateOf(emptySet<String>())

    /** Panels that ran close(): home slides them away. */
    val panelCloses = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** The panels gestures open: kept live while home shows, so one is ready the moment a swipe starts. */
    private fun gesturePanels() = setOfNotNull(drawerPanel(), LauncherStyle.style[Style.SWIPE_DOWN])

    /**
     * The extra keys over the soft keyboard: keybar.owl, else the stock ones, so keys that are gone or broken never leave a
     * terminal without esc or the arrows. Null until one runs.
     */
    fun keybarInstance(): WidgetInstance? =
        // Through specs, which are snapshot state, so the keys on screen follow a reload.
        listOf(KEYBAR, STOCK_KEYBAR).firstNotNullOfOrNull { name -> specs.firstOrNull { it.file.name == name && it.kind == OwlKind.Keybar }?.let(::instance) }

    /** How many places show the extra keys now (home, a Linux window); they're live only then. */
    private var keybarShown by mutableIntStateOf(0)

    fun keybarShown(shown: Boolean) {
        keybarShown += if (shown) 1 else -1
    }

    /** On a home page: a widget's tile, or a face; not an app, a panel or the extra keys. */
    fun onHome(spec: WidgetSpec) = spec.kind == OwlKind.Widget || spec.kind == OwlKind.Face

    /** The running widget whose screen is open, with its spec (for the title fallback). */
    fun screenInstance(): Pair<WidgetSpec, WidgetInstance>? {
        val name = openScreen ?: return null
        val spec = specs.firstOrNull { it.file.name == name } ?: return null
        return instance(spec)?.takeIf { it.program.screen != null }?.let { spec to it }
    }

    /** The page on screen; widgets on it and its neighbours stay live, the rest pause. */
    var activePage by mutableIntStateOf(1)

    /** Saves the page a file or a drag chose for [fileName]. */
    fun setPage(
        fileName: String,
        page: Int,
    ) = layout.setPage(fileName, page)

    /**
     * Home moves to [screen] (a foldable's cover screen or unfolded inner screen, or back): the layout's cells are that
     * screen's from here on, and every widget stays on Bellatrix's one page. No file is compiled again. The unfolded
     * screen, the first time it shows once the widgets have loaded, rescales the main page's cells onto its wider grid
     * (see [seedUnfolded]).
     */
    fun show(screen: Screen) {
        val seeding = screen == Screen.UNFOLDED && loaded && !layout.has(screen)
        if (seeding) seedUnfoldedScreen()
        if (layout.screen == screen) {
            if (seeding) repage()
            return
        }
        layout.show(screen)
        repage()
    }

    private fun seedUnfoldedScreen() {
        val main = itemsByPageOn(Screen.MAIN).values.flatten()
        val minWidth = main.associate { it.key to it.minSize.first }
        val seed =
            seedUnfolded(
                main.map { Triple(it.key, it.page, layout.cell(it.key, Screen.MAIN)) },
                layout.columnsOf(Screen.MAIN),
                layout.columnsOf(Screen.UNFOLDED),
            ) { minWidth[it] ?: 1 }
        layout.seed(Screen.UNFOLDED, seed)
    }

    /** Every widget onto the page the layout has for it on the screen shown, else its file's. */
    private fun repage() {
        for (i in specs.indices) {
            val spec = specs[i]
            val page = layout.pageOn(spec.file.name, spec.page)
            if (spec.page != page) specs[i] = spec.onPage(page)
        }
    }

    /**
     * The user put [item] on home: an app they pinned, or a widget given a cell on the phone (a store add, a share, a
     * drag). Its page is saved when it differs from the one its file asks for.
     */
    fun placedOnPhone(item: HomeItem) = item !is WidgetItem || layout.chosenPage(item.key) != null

    /** Keeps [item] on its page, placing it at the least crowded spot there when the page is full. */
    fun moveToFreePage(item: HomeItem): Int {
        val page = item.page.coerceAtLeast(1)
        moveToPage(item, page)
        return page
    }

    /** A new face starts on the page after the last one in use. */
    fun newFacePage() = (pages.maxOrNull() ?: 0) + 1

    /** Pages stay in numeric order; the file's `[[page]]` order is what lays them out. */
    fun arrangePages(order: List<Int>) = Unit

    /** Removing a page moves its items to the one before it, if any. */
    fun removePage(page: Int) {
        if (pages.size <= 1 || page !in pages) return
        val target = pages.filter { it < page }.maxOrNull() ?: pages.first { it != page }
        for (item in itemsOn(page)) moveToPage(item, target)
        if (activePage == page) activePage = target
    }

    /** Puts [item] on [page] of [screen] (the one shown unless said). */
    fun moveToPage(
        item: HomeItem,
        page: Int,
        screen: Screen = layout.screen,
    ) {
        val target = page.coerceAtLeast(1)
        when (item) {
            is WidgetItem -> if (screen == layout.screen) placeOnPage(item.key, target) else layout.setPage(item.key, target, screen)
            is Shortcut, is Folder -> shortcuts.move(item.key, target, screen)
        }
    }

    /** Puts the widget or face in [fileName] on [page], loaded already or not. */
    fun placeOnPage(
        fileName: String,
        page: Int,
    ) {
        val target = page.coerceAtLeast(1)
        setPage(fileName, target)
        val i = specs.indexOfFirst { it.file.name == fileName }
        if (i >= 0 && specs[i].page != target) specs[i] = specs[i].onPage(target)
    }

    private fun agentWidgetFile(agent: Agent) = "$AGENT_PREFIX${agent.bin}.owl"

    /** An agent's terminal tile, which [addAgentWidget] made. */
    fun isAgentWidget(spec: WidgetSpec) = spec.file.name.startsWith(AGENT_PREFIX)

    /** [agent]'s terminal tile is on the home screen. */
    fun hasAgentWidget(agent: Agent) = specs.any { it.file.name == agentWidgetFile(agent) }

    /** Adds an agent's terminal tile to [page]. */
    suspend fun addAgentWidget(
        agent: Agent,
        page: Int,
    ) = withContext(Dispatchers.IO) {
        val name = agentWidgetFile(agent)
        setPage(name, page)
        File(dir, name).writeText(agentWidget(agent))
    }

    /**
     * Adds another plain terminal tile to [page], a copy of the default one. Each is a file of its own, so there can
     * be any number, and one removed (the default included) is always a tap away in the store.
     */
    suspend fun addTerminalWidget(page: Int) =
        withContext(Dispatchers.IO) {
            val name = TERMINAL_PREFIX + System.currentTimeMillis().toString(36) + ".owl"
            setPage(name, page)
            File(dir, name).writeBytes(context.assets.open("widgets/$TERMINAL_ASSET").use { it.readBytes() })
        }

    fun installDependenciesWhenLoaded(fileName: String) {
        autoInstall += fileName
    }

    /**
     * Takes a widget off the home screen, or an app out of the drawer (and its icon off home). Its file moves to
     * .removed/ rather than being deleted: it may be an agent's work, and this is easy to undo by hand.
     */
    fun removeWidget(spec: WidgetSpec) {
        val trash = File(dir, ".removed").apply { mkdirs() }
        spec.file.renameTo(File(trash, spec.file.name))
        instances.remove(spec.file.name)?.stop()
        if (openScreen == spec.file.name) openScreen = null
        shortcuts.removeOwl(spec)
        specs.remove(spec)
        forget(spec.file.name)
    }

    /** A widget file that's gone takes its place, page and saved values along, so a new file of that name starts fresh. */
    private fun forget(fileName: String) {
        layout.remove(fileName)
        File(stateDir, "$fileName.json").delete()
        File(stateDir, "$fileName.secret").delete()
        hashes.remove(fileName)
        File(checkDir, "$fileName.txt").delete()
        File(previewDir, "$fileName.png").delete()
        File(previewDir, "$fileName.screen.png").delete()
    }

    /** Widgets, pinned apps and folders on [page]. Wrappers compare by spec, so unchanged pages skip recomposition. */
    fun itemsOn(page: Int): List<HomeItem> = itemsByPage[page].orEmpty()

    /**
     * Home's items by page as [screen] has them, shown or not. For layout.toml, which holds every screen; home
     * draws from [itemsOn]. Widgets keep the spec object they were built with, so removal and recomposition
     * still compare by identity.
     */
    fun itemsByPageOn(screen: Screen): Map<Int, List<HomeItem>> {
        if (screen == layout.screen) return itemsByPage
        val widgets = specs.filter(::onHome).map(::WidgetItem)
        return (widgets + shortcuts.onGrid(screen)).groupBy { it.page }
    }

    /** Built once per change rather than per call: home asks for every page on each recomposition, and drags ask often. */
    private val itemsByPage by derivedStateOf {
        (specs.filter(::onHome).map(::WidgetItem) + shortcuts.onGrid()).groupBy { it.page }
    }

    val pages: List<Int> get() = itemsByPage.keys.sorted()

    fun instance(spec: WidgetSpec): WidgetInstance? = instances[spec.file.name]?.takeIf { it.program === spec.program }

    /** A tap on the tile itself, rather than on a node with its own onTap. */
    fun tap(spec: WidgetSpec) {
        val inst = instance(spec) ?: return
        spec.program?.onTap?.run(inst.root, emptyList())
    }

    /** Signals a change in [dir], true for a file written there; conflated, so a burst of writes reloads once. */
    private val changes = Channel<Boolean>(Channel.CONFLATED)

    /** The language the widgets were compiled for, which strings { } picks: the phone's at the last reload. */
    private var language = phoneLanguage()

    @Suppress("unused") // Held so it isn't collected, which would stop the events.
    private var observer: FileObserver? = null

    @Suppress("unused") // The same, for preview requests.
    private var checkObserver: FileObserver? = null

    @Suppress("unused") // The same, for owl-send.
    private var inboxObserver: FileObserver? = null

    fun start() {
        scope.launch {
            withContext(Dispatchers.IO) {
                dir.mkdirs()
                checkDir.mkdirs()
                // Requests from before this start have been given up on: owl-send waits a few seconds at most.
                // The guest can write here: a link in the inbox's place must not take what it points to with it.
                runCatching { LauncherData.deleteTree(inboxDir) }
                inboxDir.mkdirs()
                markets.forEach { it.load() }
            }
            // owl-check asks for a preview the launcher hasn't made yet by writing the file's hash to <file>.want.
            checkObserver =
                object : FileObserver(checkDir, CLOSE_WRITE or MOVED_TO) {
                    override fun onEvent(
                        event: Int,
                        path: String?,
                    ) {
                        if (path != null && path.endsWith(".themes") && BuildConfig.DEBUG) {
                            val file = File(checkDir, path)
                            val screens = runCatching { file.readLines() }.getOrDefault(emptyList()).map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }
                            file.delete()
                            scope.launch { previewThemes(path.removeSuffix(".themes"), screens) }
                            return
                        }
                        if (path == null || !path.endsWith(".want")) return
                        val want = File(checkDir, path)
                        val hash = runCatching { want.readText().trim() }.getOrNull()
                        want.delete()
                        val name = path.removeSuffix(".want")
                        scope.launch { if (hash != null && hashes[name] == hash) requestPreview(name) }
                    }
                }.also { it.startWatching() }
            launch { for (name in previewQueue) preview(name) }
            inboxObserver =
                object : FileObserver(inboxDir, CLOSE_WRITE or MOVED_TO) {
                    override fun onEvent(
                        event: Int,
                        path: String?,
                    ) {
                        if (path != null && path.endsWith(".req")) scope.launch { receive(path.removeSuffix(".req")) }
                    }
                }.also { it.startWatching() }
            observer =
                object : FileObserver(dir, CLOSE_WRITE or MOVED_TO or MOVED_FROM or DELETE or CREATE) {
                    override fun onEvent(
                        event: Int,
                        path: String?,
                    ) {
                        if (path != null && path.endsWith(".owl")) changes.trySend(true)
                    }
                }.also { it.startWatching() }
            launch {
                snapshotFlow { loaded }.first { it }
                snapshotFlow { runtime.style.style[Style.COLUMNS] }.collect { layout.regrid(it, itemsByPageOn(Screen.MAIN).values.toList()) }
            }
            launch {
                snapshotFlow { loaded }.first { it }
                snapshotFlow { runtime.style.style[Style.UNFOLDED_COLUMNS] }.collect { layout.regrid(it, itemsByPageOn(Screen.UNFOLDED).values.toList(), Screen.UNFOLDED) }
            }
            // A new phone language, or one picked in settings: files with strings { } or lang() compile anew for it.
            launch { AppLanguage.language.drop(1).collect { changes.trySend(false) } }
            context.registerComponentCallbacks(
                object : ComponentCallbacks {
                    override fun onConfigurationChanged(newConfig: Configuration) {
                        if (phoneLanguage() != language) changes.trySend(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onLowMemory() = Unit
                },
            )
            changes.trySend(false)
            for (written in changes) {
                // Editors write in several steps (truncate, write, rename); let them finish. Only then: home waits on the
                // first load, and on the one that starts the widgets that need Linux.
                if (written) delay(200)
                changes.tryReceive()
                reload(withContext(Dispatchers.IO) { widgetFiles() })
            }
        }
    }

    private fun widgetFiles() = dir.listFiles { f -> (!f.name.startsWith(".") || f.name == STOCK_DRAWER || f.name == STOCK_KEYBAR) && f.extension == "owl" }.orEmpty().sortedBy { it.name }

    /** New files that haven't compiled yet: home placed them at the default size. Main thread. */
    private val unsized = HashSet<String>()

    private suspend fun reload(files: List<File>) {
        // Compiled for another language, a file's texts would stay in it.
        val relanguaged = phoneLanguage() != language
        language = phoneLanguage()
        val old = specs.associateBy { it.file.name }
        // Each file on its own thread: at startup every one compiles, and home waits for them all.
        val parsed =
            withContext(Dispatchers.IO) {
                files.map { f -> async { old[f.name]?.takeIf { !relanguaged && it.stamp == f.lastModified() } ?: compile(f, old[f.name]) } }.awaitAll()
            }
        // A new file that didn't compile was placed with no size: to go by; once it compiles, its page places it again, at its size.
        unsized.retainAll(parsed.mapTo(HashSet()) { it.file.name })
        for (spec in parsed) {
            val name = spec.file.name
            if (spec.program == null) {
                if (old[name] == null && Screen.entries.none { layout.cell(name, it) != null }) unsized += name
            } else if (unsized.remove(name) && spec.size != null) {
                for (screen in Screen.entries) layout.clearCell(name, screen)
            }
        }
        val changed = parsed.filter { old[it.file.name] !== it }.map { it.file.name }
        // A file's `page:` is where the tile starts; a page saved on the phone (or on the cover screen) wins.
        // Resolve it here, before sorting and before home wraps specs in tiles, so removal keeps the same object.
        val normalized =
            parsed.map { spec ->
                val page = layout.pageOn(spec.file.name, spec.page)
                if (page == spec.page) spec else spec.onPage(page)
            }
        specs.clear()
        specs += normalized.sortedWith(compareBy({ it.page }, { it.order }, { it.file.name }))
        // Unchanged files keep their running instance; changed ones start fresh but keep their values.
        val previous = HashMap(instances)
        instances.clear()
        for (spec in normalized) {
            val program = spec.program ?: continue
            // Stopped below with the other leftovers.
            if (program.needsLinux && !linux) continue
            val prev = previous.remove(spec.file.name)
            if (prev?.program === program) {
                instances[spec.file.name] = prev
                continue
            }
            prev?.stop()
            val name = spec.file.name
            val inst =
                WidgetInstance(
                    program,
                    services,
                    { message -> writeError(spec.file, message, append = true) },
                    File(stateDir, "$name.json"),
                    File(stateDir, "$name.secret"),
                    onScreen = { open ->
                        when {
                            program.kind == OwlKind.App -> if (!open) appCloses.tryEmit(name)
                            program.kind == OwlKind.Panel -> if (!open) panelCloses.tryEmit(name)
                            open -> openScreen = name
                            openScreen == name -> openScreen = null
                        }
                    },
                    linux = { linux },
                    // Surfaces are drawn over home whenever home shows, so their triggers and sounds count as foreground too.
                    foreground = { (shownApps[name] ?: 0) > 0 || openScreen == name || name in shownPanels || name in BAR_FILES },
                )
            prev?.let(inst::adopt)
            instances[spec.file.name] = inst
            // The spec as it is now, not as loaded: placing it again replaces it without a reload.
            inst.start(scope) { isLive(specs.firstOrNull { it.file.name == name } ?: spec) }
        }
        // A screen whose widget went away, stopped compiling or lost its screen { } closes.
        if (openScreen?.let { instances[it]?.program?.screen } == null) openScreen = null
        // Stopped first: stopping saves persisted values, which forget() then deletes.
        previous.values.forEach { it.stop() }
        // A file deleted outside the launcher (an agent, rm) frees its spot, so it doesn't come back on top of
        // whatever moved in. Only while running: an empty listing at startup must not wipe the layout.
        if (old.isNotEmpty() && files.isNotEmpty()) {
            for (name in old.keys - parsed.map { it.file.name }.toSet()) forget(name)
        }
        // Not at startup, when every file compiles: only what changed while the launcher runs, which is an agent at work.
        if (loaded) changed.forEach(::requestPreview)
        if (!loaded) {
            runtime.drawOwlIcons(parsed.filter { it.kind == OwlKind.App })
            loaded = true
            // Before faces, page 1 could be emptied by moving its last tile away, and home opens on page 1: the first page
            // of tiles becomes it again.
            val first = pages.firstOrNull { p -> itemsOn(p).any { !it.isFace } }
            if (first != null && first != 1) arrangePages(pages)
            home.loaded()
        }
        // A guest shell call: after home has its widgets rather than before.
        checkDependencies(parsed)
    }

    /**
     * Answers an `owl-send` request: lines of the widget's file name, the
     * command (set or refresh), and for set the state's name, then its value
     * to the end. The answer, `ok` or `error: ...`, goes to <id>.done.
     */
    private suspend fun receive(id: String) {
        val request = File(inboxDir, "$id.req")
        val text = withContext(Dispatchers.IO) { runCatching { request.readText() }.getOrNull().also { request.delete() } } ?: return
        val parts = text.split("\n", limit = 4)
        val name = parts[0]
        val spec = specs.firstOrNull { it.file.name == name }
        val inst = spec?.let(::instance)
        val reply =
            when {
                spec == null -> "error: the launcher hasn't loaded it"
                spec.program == null -> "error: it doesn't compile; run owl-check $name"
                inst == null -> "error: it isn't running" + if (waitsForLinux(spec)) ", because it needs Linux" else ""
                parts.getOrNull(1) == "refresh" -> "ok".also { inst.refreshNow() }
                parts.getOrNull(1) == "set" -> inst.setState(parts.getOrNull(2).orEmpty(), Json.parseOrText(parts.getOrNull(3).orEmpty()))?.let { "error: $it" } ?: "ok"
                else -> "error: unknown command '${parts.getOrNull(1).orEmpty()}'"
            }
        // The picture owl-check has is of the widget before this: without its preview line, the next check asks for a new one.
        if (reply == "ok") hashes[name]?.let { writeCheck(spec!!.file, it, "ok").join() }
        withContext(Dispatchers.IO) {
            runCatching { writeAtomically(File(inboxDir, "$id.done"), (reply + "\n").toByteArray()) }
        }
    }

    /** The first load is done; see [reload]. Until then home's pages are still filling in. */
    var loaded by mutableStateOf(false)
        private set

    private val previews = Previews(context)
    private val previewQueue = Channel<String>(Channel.UNLIMITED)
    private val queued = HashSet<String>()

    /** Widgets being previewed; their sources run while the home screen is hidden. */
    private val previewing = mutableStateListOf<String>()

    /** Main thread. */
    private fun requestPreview(name: String) {
        if (queued.add(name)) previewQueue.trySend(name)
    }

    /**
     * Renders [name]'s tile to .preview/<file>.png once its sources have
     * loaded, at its size on the home screen, and notes it in its check file.
     */
    private suspend fun preview(name: String) {
        queued.remove(name)
        val spec = specs.firstOrNull { it.file.name == name } ?: return
        val program = spec.program ?: return
        val hash = hashes[name] ?: return
        val skip =
            when {
                program.terminal -> "Terminal tiles aren't previewed"
                program.browser -> "Browser tiles aren't previewed"
                waitsForLinux(spec) -> "it needs Linux, which isn't installed"
                !missing[name].isNullOrEmpty() -> "it needs ${missing[name]!!.joinToString(" ")}; install with ${Distro.active.installHint}"
                instance(spec) == null -> "it isn't running"
                else -> null
            }
        if (skip != null) {
            writeCheck(spec.file, hash, "ok", "preview-skipped $hash $skip")
            return
        }
        val inst = instance(spec)!!
        if (program.kind == OwlKind.App || program.kind == OwlKind.Panel || program.kind == OwlKind.Keybar) return previewView(spec, inst, hash)
        val cells = tileCells(spec)
        val (w, h) = if (spec.isFace) layout.pagePx(context.resources.displayMetrics) else layout.tilePx(cells, context.resources.displayMetrics)
        previewing += name
        val loaded = loadedOf(inst)
        var screen: String? = null
        val ok =
            try {
                previews.render(spec, inst, shownCells(cells), w, h, File(previewDir, "$name.png"), loaded).also {
                    // The screen { } as it opens over home, at the phone's size.
                    if (it && program.screen != null) {
                        val (pw, ph) = screenPreviewSize(spec)
                        if (previews.renderScreen(spec, inst, pw, ph, File(previewDir, "$name.screen.png"), loaded)) {
                            screen = "screen $hash ${pw}x$ph"
                        }
                    }
                }
            } finally {
                previewing.remove(name)
            }
        // The file may have changed while this rendered; its own preview follows.
        if (hashes[name] != hash) return
        val line = if (ok) shownCells(cells).let { (c, r) -> "preview $hash ${c}x$r ${w}x$h" } else "preview-skipped $hash rendering failed"
        // Where home put the tile, which the file's size: only decides the first time.
        val home = if (spec.isFace) null else layout.cell(name)?.let { "home $hash ${it.x} ${it.y} ${it.w} ${it.h}" }
        writeCheck(spec.file, hash, "ok", listOfNotNull(line, screen, home).joinToString("\n"))
    }

    /** The tile's cells as it's laid out on home, else as the file sizes it; a face's are the whole grid. */
    private fun tileCells(spec: WidgetSpec) = (if (spec.isFace) layout.grid ?: (layout.columns to 12) else null) ?: layout.cell(spec.file.name)?.let { it.w to it.h } ?: spec.size?.let { (w, h) -> w.coerceIn(1, layout.columns) to h.coerceAtLeast(1) } ?: (3 to 3)

    /**
     * The size `owl-check` should use for a screen preview. The two surface files are not full swipe
     * panels: `home.owl` fills the desktop area above the native taskbar and `taskbar.owl` fills that
     * taskbar, so their PNGs show the size they actually render at on the phone.
     */
    private fun screenPreviewSize(spec: WidgetSpec): Pair<Int, Int> {
        val res = context.resources
        val dm = res.displayMetrics
        return when (spec.file.name) {
            HOME_BAR -> {
                val bar = res.getDimensionPixelSize(R.dimen.taskbar_height)
                val bottom = res.getDimensionPixelSize(R.dimen.taskbar_margin_bottom)
                dm.widthPixels to (dm.heightPixels - bar - bottom).coerceAtLeast(1)
            }
            TASKBAR_BAR -> {
                val side = res.getDimensionPixelSize(R.dimen.taskbar_margin_horizontal)
                val bar = res.getDimensionPixelSize(R.dimen.taskbar_height)
                (dm.widthPixels - 2 * side).coerceAtLeast(1) to bar
            }
            else -> dm.widthPixels to dm.heightPixels
        }
    }

    /**
     * [cells] as home shows them, for `cols` and `rows`: on its side in landscape, like the tile on home. The preview
     * shares the tile's instance, so drawing it with the file's cells would put them in the tile on home too.
     */
    private fun shownCells(cells: Pair<Int, Int>) = if (layout.turned) cells.second to cells.first else cells

    /** Waits until every source of [inst] has a value; what a preview draws after. */
    private fun loadedOf(inst: WidgetInstance): suspend () -> Unit = { snapshotFlow { inst.loaded() }.first { it } }

    /** An app has no tile: its view is drawn at the phone's size, as it opens, and noted as its screen. */
    private suspend fun previewView(
        spec: WidgetSpec,
        inst: WidgetInstance,
        hash: String,
    ) {
        val name = spec.file.name
        val (pw, ph) = screenPreviewSize(spec)
        previewing += name
        val ok =
            try {
                previews.renderScreen(spec, inst, pw, ph, File(previewDir, "$name.screen.png"), loadedOf(inst))
            } finally {
                previewing.remove(name)
            }
        if (hashes[name] != hash) return
        val tile =
            "preview-skipped $hash " +
                when (spec.kind) {
                    OwlKind.Panel -> "a panel has no tile"
                    OwlKind.Keybar -> "the terminal key bar has no tile"
                    else -> "an app has no tile"
                }
        writeCheck(spec.file, hash, "ok", if (ok) "$tile\nscreen $hash ${pw}x$ph" else "$tile; rendering its screen failed")
    }

    /**
     * Debug builds, for the widget store's per-theme previews (scripts/store-previews.sh): once
     * [name]'s sources have loaded, renders its tile in every bundled Bellatrix theme to
     * .preview/themes/<theme>/<file>.png (an app: its view at the phone's size), then writes .preview/themes/<file>.done. Asked for by
     * writing .check/<file>.themes. The whole launcher takes each theme's colors while it runs.
     *
     * An app can be shown on several [screens], one per line of the .themes file: the states to set before it's drawn,
     * as `view = "watch"; openId = ""`, in order; `wait = 60` pauses that many seconds there. Each starts from the screen
     * before it. The first is <file>.png, the rest <file>.<n>.png.
     */
    private suspend fun previewThemes(
        name: String,
        screens: List<String> = emptyList(),
    ) {
        val spec = specs.firstOrNull { it.file.name == name } ?: return
        val inst = instance(spec) ?: return
        val cells = tileCells(spec)
        val (w, h) = if (spec.isFace) layout.pagePx(context.resources.displayMetrics) else layout.tilePx(cells, context.resources.displayMetrics)
        val themes = withContext(Dispatchers.IO) { runtime.themes.bundled }
        val before = Palette.current
        val dir = File(previewDir, "themes")
        previewing += name
        try {
            val loaded = loadedOf(inst)
            withTimeoutOrNull(30_000) { loaded() }
            val (pw, ph) = screenPreviewSize(spec)
            for ((n, screen) in screens.ifEmpty { listOf("") }.withIndex()) {
                for ((state, value) in assignments(screen)) {
                    // wait = <seconds> pauses first, for an API that limits calls a minute.
                    if (state == "wait") {
                        delay((value.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 300.0).times(1000).toLong())
                        continue
                    }
                    inst.setState(state, Json.parseOrText(value))?.let { Log.w("widgets", "preview screen ${n + 1} of $name: $it") }
                }
                // What the new screen fetches starts now; give it time before the first theme is drawn.
                if (screen.isNotEmpty()) {
                    delay(SCREEN_SETTLE_MS)
                    withTimeoutOrNull(30_000) { loaded() }
                }
                // Waited for once a screen: a source that never loads would otherwise hold up every theme.
                val ready: suspend () -> Unit = {}
                for (theme in themes) {
                    Palette.apply(theme.colors)
                    val out = File(dir, "${theme.name}/$name${if (n > 0) ".${n + 1}" else ""}.png")
                    if (spec.kind == OwlKind.App || spec.kind == OwlKind.Panel || spec.kind == OwlKind.Keybar) {
                        previews.renderScreen(spec, inst, pw, ph, out, ready)
                    } else {
                        previews.render(spec, inst, shownCells(cells), w, h, out, ready)
                    }
                }
            }
        } finally {
            Palette.restore(before)
            previewing.remove(name)
        }
        withContext(Dispatchers.IO) { File(dir, "$name.done").writeText("${cells.first}x${cells.second} ${w}x$h\n") }
    }

    /** A preview screen's `name = value; name = value` pairs; a value is JSON (a quoted string, a number, true) or plain text. */
    private fun assignments(screen: String) = SCREEN_STATE.findAll(screen).map { it.groupValues[1] to it.groupValues[2].trim() }.toList()

    private fun isLive(spec: WidgetSpec): Boolean {
        if (!missing[spec.file.name].isNullOrEmpty()) return false
        // A preview needs real values, even while the home screen is hidden behind the agent's terminal.
        if (spec.file.name in previewing) return true
        // An app in its own task: live while that's showing, whatever home does.
        if ((shownApps[spec.file.name] ?: 0) > 0) return true
        // Over a Linux window too, while home is hidden.
        if (spec.kind == OwlKind.Keybar) return keybarShown > 0
        if (!visible) return false
        // Declarative surfaces live for as long as home shows: they ARE drawn on home, not a page or a swipe panel.
        if (spec.file.name in BAR_FILES) return true
        // Its screen is open: it's on screen wherever the pages are.
        if (spec.file.name == openScreen) return true
        if (spec.kind == OwlKind.Panel) return spec.file.name in shownPanels || spec.file.name in gesturePanels()
        // Apps have no page to be near.
        if (!onHome(spec)) return false
        val pages = pages
        val here = pages.indexOf(activePage)
        val there = pages.indexOf(spec.page)
        return here < 0 || there < 0 || kotlin.math.abs(here - there) <= 1
    }

    private fun compile(
        file: File,
        previous: WidgetSpec?,
    ): WidgetSpec {
        val stamp = file.lastModified()
        val bytes = runCatching { file.readBytes() }.getOrElse { return broken(file, previous, stamp, it.message ?: "can't read", "") }
        val text = String(bytes)
        val hash = sha256Hex(bytes)
        hashes[file.name] = hash
        return try {
            val p = Compiler.compile(text)
            writeError(file, null)
            // Advice, after the clear and before the check result, so owl-check prints it with the rest.
            for (w in p.warnings) writeError(file, "warning: $w", append = true)
            writeCheck(file, hash, "ok")
            WidgetSpec(file, p.name, layout.pageOn(file.name, p.page), p.order, p, null, p.size, p.minSize, stamp, p.kind)
        } catch (e: CompileError) {
            broken(file, previous, stamp, Source(text).format(e), hash, OwlKind.of(text))
        } catch (e: RuntimeException) {
            broken(file, previous, stamp, e.toString(), hash, OwlKind.of(text))
        }
    }

    /** A widget that doesn't compile stays where it was, so fixing it doesn't reshuffle the page. */
    private fun broken(
        file: File,
        previous: WidgetSpec?,
        stamp: Long,
        error: String,
        hash: String,
        kind: OwlKind = previous?.kind ?: OwlKind.Widget,
    ): WidgetSpec {
        writeError(file, error)
        writeCheck(file, hash, "error")
        return WidgetSpec(file, file.nameWithoutExtension, layout.pageOn(file.name, 1), previous?.order ?: 999, null, error, previous?.size, previous?.minSize ?: (1 to 1), stamp, kind)
    }

    /** Error files are written in order: a clear must never land after the error that follows it. */
    private val errorWriter = Dispatchers.IO.limitedParallelism(1)

    /**
     * Errors also go to .errors/<file>.txt, so an agent editing widgets in the
     * guest can read what went wrong without seeing the phone.
     */
    private fun writeError(
        file: File,
        error: String?,
        append: Boolean = false,
    ) {
        val target = File(errorDir, file.name + ".txt")
        scope.launch(errorWriter) {
            runCatching {
                when {
                    error == null -> target.delete()
                    append -> {
                        errorDir.mkdirs()
                        if (target.length() < 64 shl 10) target.appendText("runtime: $error\n")
                    }
                    else -> {
                        errorDir.mkdirs()
                        target.writeText(error + "\n")
                    }
                }
            }
        }
    }

    /**
     * Writes .check/<file>.txt for `owl-check`: `sha256 <hash>`, `result ok` or
     * `result error`, and once rendered, `preview <hash> <cells> <pixels>` or
     * `preview-skipped <hash> <why>`, then `screen <hash> <pixels>` for a screen { } and `home <hash> <x> <y> <w> <h>`
     * for a tile's cell on home. After the error file, in the same order,
     * so a check that sees the hash also sees that version's errors.
     */
    private fun writeCheck(
        file: File,
        hash: String,
        result: String,
        preview: String? = null,
    ): Job {
        val text = "sha256 $hash\nresult $result\n" + (preview?.let { "$it\n" } ?: "")
        return scope.launch(errorWriter) {
            runCatching { writeAtomically(File(checkDir, "${file.name}.txt"), text.toByteArray()) }
        }
    }

    /**
     * Checks, in one guest shell call, which declared pacman packages are
     * missing and which programs the commands use that aren't installed. A
     * program that's missing and undeclared is reported, so the author adds it
     * to `requires`.
     */
    private suspend fun checkDependencies(specs: List<WidgetSpec>) {
        if (!linux) return
        val programs = specs.mapNotNull { it.program }
        val packages = programs.flatMap { DependencyCheck.declared(it.requires) }.distinct()
        val bins = programs.flatMap { it.programs }.distinct()
        if (packages.isEmpty() && bins.isEmpty()) {
            missing.clear()
            return
        }
        val out = runCatching { services.shells.run(DependencyCheck.script(packages, bins), 20).out }.getOrElse { return }
        val found = DependencyCheck.parse(out)
        val missingPkgs = found.packages
        val missingBins = found.programs
        for (spec in specs) {
            val p = spec.program ?: continue
            val need = DependencyCheck.declared(p.requires).filter { it in missingPkgs }
            if (need.isEmpty()) missing.remove(spec.file.name) else missing[spec.file.name] = need
            if (need.isNotEmpty() && autoInstall.remove(spec.file.name)) installDependencies(spec)
            // With declared packages still missing, their programs are expected to be absent.
            val undeclared = if (need.isEmpty()) p.programs.filter { it in missingBins } else emptyList()
            for (bin in undeclared) {
                if (warned.add("${spec.file.name}:${spec.stamp}:$bin")) {
                    writeError(spec.file, "warning: runs `$bin`, which isn't installed; add its package to requires { ${Distro.active.manager}: [...] }", append = true)
                }
            }
        }
    }

    /** Warnings already written, per file version, so reloads don't repeat them. */
    private val warned = HashSet<String>()

    /** Installs a widget's missing packages the way the bootstrap does: a full sync, so no partial upgrades. */
    fun installDependencies(spec: WidgetSpec) {
        val name = spec.file.name
        val packages = missing[name].orEmpty()
        if (packages.isEmpty() || installs[name] == Install.Running) return
        installs[name] = Install.Running
        scope.launch {
            val (ok, tail) = runtime.packages.lock.withLock { withContext(Dispatchers.IO) { proot.runJob(Distro.active.installCommand(packages)) } }
            if (ok) installs.remove(name) else installs[name] = Install.Failed(tail.toString())
            // Other widgets may have been waiting on the same packages.
            checkDependencies(specs.toList())
        }
    }

    /**
     * The widget docs, examples and default widgets. Defaults are the user's
     * once edited or removed; the docs always match this app's language, as
     * reference for agents. Examples aren't loaded until moved up into [dir].
     */
    fun managed(): List<Managed> {
        fun asset(
            path: String,
            target: File,
            policy: Policy,
        ) = Managed.asset(context, path, target, policy)

        val assets = context.assets
        return buildList {
            add(asset("widgets/README.md", File(dir, "README.md"), Policy.Owned))
            for (name in assets.list("widgets/docs").orEmpty()) add(asset("widgets/docs/$name", File(dir, "docs/$name"), Policy.Owned))
            for (name in assets.list("widgets/examples").orEmpty()) add(asset("widgets/examples/$name", File(dir, "examples/$name"), Policy.Seeded))
            for (name in assets.list("widgets").orEmpty().filter { it.endsWith(".owl") }) add(asset("widgets/$name", File(dir, name), Policy.Seeded))
            // The Flex/Box/Anchor examples on Bellatrix's native bar, there from the first install so they can be tested.
            add(Managed.text(File(dir, HOME_BAR), Policy.Seeded) { OwlBars.homeTemplate })
            add(Managed.text(File(dir, TASKBAR_BAR), Policy.Seeded) { OwlBars.taskbarTemplate })
            // Always as this launcher ships them, whatever became of drawer.owl and keybar.owl.
            add(asset("widgets/$DRAWER", File(dir, STOCK_DRAWER), Policy.Owned))
            add(asset("widgets/$KEYBAR", File(dir, STOCK_KEYBAR), Policy.Owned))
        }
    }

    /** Agents look for AGENTS.md / CLAUDE.md in the guest home; these point them at the widget docs. */
    fun guestManaged(): List<Managed> = guestManaged(context, proot)

    companion object {
        /**
         * The guest files the launcher seeds without waiting for the widget engine (used by the
         * startup hook in [com.bellatrixos.BellatrixApplication]). Same files, same policies as
         * [WidgetEngine.guestManaged].
         */
        fun guestManaged(
            context: Context,
            proot: Proot,
        ): List<Managed> =
            listOf("AGENTS.md", "CLAUDE.md").map { Managed.text(File(proot.home, it), Policy.Seeded) { AGENT_POINTER } } +
                // The map of every editable file and how not to break one; updated while the user hasn't changed it.
                Managed.asset(context, "ricing.md", File(proot.home, "RICING.md"), Policy.Seeded) +
                listOf("owl-check", "owl-send").map { bin ->
                    Managed.asset(context, "$bin.sh", File(proot.rootfs, "usr/local/bin/$bin"), Policy.Owned, executable = true)
                }

        /** A store preview screen's `name = value`, up to the next `;` outside quotes. */
        private val SCREEN_STATE = Regex("""(\w+)\s*=\s*("(?:[^"\\]|\\.)*"|[^;]+)""")

        /** How long a store preview screen gets to fetch what it shows before its sources count as loaded. */
        private const val SCREEN_SETTLE_MS = 3_000L

        /** The default terminal tile, which [addTerminalWidget] copies. */
        private const val TERMINAL_ASSET = "45-terminal.owl"

        /** The app drawer, a panel: the user's to change, from the stock one the launcher ships. */
        const val DRAWER = "drawer.owl"

        /**
         * The drawer as this launcher ships it, kept that way: what the drawer shows while drawer.owl is gone or broken,
         * and what an agent can compare an edited drawer.owl with. Hidden, so it isn't offered as a panel of its own.
         */
        const val STOCK_DRAWER = ".stock-drawer.owl"

        /** The extra keys over the soft keyboard while typing in a terminal: the user's to change, like the drawer. */
        const val KEYBAR = "keybar.owl"

        /** The extra keys as this launcher ships them, kept that way: the fallback, and what an edited keybar.owl compares with. */
        const val STOCK_KEYBAR = ".stock-keybar.owl"

        /**
         * Declarative surface files: panel-kind OWL files drawn over Bellatrix's own home and taskbar views
         * (not swiped open). Edit these to skin the native surfaces with Flex/Box/Anchor. Both are user files,
         * created on first edit from the templates in [OwlBars], and watched by the same file observer as every .owl.
         */
        const val HOME_BAR = "home.owl"
        const val TASKBAR_BAR = "taskbar.owl"
        val BAR_FILES = setOf(HOME_BAR, TASKBAR_BAR)

        private const val TERMINAL_PREFIX = "terminal-"
        private const val AGENT_PREFIX = "agent-"

        /** The widgets dir, relative to the guest home. */
        const val GUEST_DIR = ".config/bellatrix/widgets"

        private const val AGENT_POINTER =
            "# bellatrix\n\nThis Void Linux system runs inside an Android launcher, bellatrix; its source is at " +
                "https://github.com/Zaineedyou/Bellatrix, should you need to understand how the launcher works. RICING.md next to this file " +
                "maps every file you can edit to change the launcher's look and what keeps an edit from breaking it: read it first.\n\n" +
                "It's Void Linux (glibc, aarch64) under proot, not a booted system: you are root already (no sudo needed), and there is " +
                "no systemd (systemctl, services and timers don't work), no cron, no Docker and no kernel modules. Install " +
                "packages with xbps (xbps-install -S <name>, xbps-query -Rs <word> to search), command-line tools with mise. /sdcard is the phone's own storage, but it shows only " +
                "the files Linux put there itself: save files for the user's other apps in /sdcard/Download or /sdcard/Documents " +
                "(pictures in /sdcard/Pictures). The user's other files are in the phone folders they let Linux reach, which have " +
                "no path: `bellatrix folders` lists them, `bellatrix ls`, `get` and `put` list and copy files in and out, and " +
                "`bellatrix pick` lets the user choose any file on the phone. xdg-open opens http(s) links in Android's browser; the editor is nano (`\$EDITOR`) and " +
                "`bellatrix-files [folder]` opens the launcher's file manager; GUI apps (Wayland) open as windows on the launcher.\n\n" +
                "Home-screen widgets are `.owl` files in " +
                "~/$GUEST_DIR, written in a small declarative language documented in README.md there (and docs/ next to it); so are apps for the " +
                "app drawer (`app \"name\" { }`) and panels that swiping up or down on home opens (`panel \"name\" { }`, picked with " +
                "gestures.swipe-up and gestures.swipe-down in ~/${StyleStore.GUEST_DIR}/style.toml). The app drawer is the panel " +
                "drawer.owl there: to change the drawer, edit it; .stock-drawer.owl is the drawer as this launcher ships it. " +
                "Likewise keybar.owl (`keybar \"name\" { }`) is the terminal key bar over the soft keyboard while typing in a terminal, " +
                "and .stock-keybar.owl the bar as shipped. " +
                "The home and taskbar are skinned with two more declarative files here: home.owl and taskbar.owl, both `panel \"name\" { }` files. " +
                "They draw over Bellatrix's real home and taskbar views rather than swiping open: they start transparent, so the native look stays, " +
                "and an opaque background or anchored nodes change those surfaces. Lay modules out with Flex, Box and Anchor and the `anchor` property " +
                "(owl 24) like a Waybar. Saved files show up on the phone within seconds. After saving, run `owl-check <file>`: it prints the errors " +
                "and the path of a PNG of the tile or view, drawn with live data; look at it to check the layout. " +
                "examples/ holds more widgets to learn from or move up to use. Scripts and hooks can set a running " +
                "widget's state or refresh it with `owl-send <file> set <state> <value>` and `owl-send <file> refresh`.\n\n" +
                "The launcher's look (theme, background, tile spacing and borders, corners, the system bars' colors, the " +
                "terminals' font) is ~/${StyleStore.GUEST_DIR}/${StyleStore.FILE}; ${StyleStore.DEFAULTS} next to it lists every " +
                "setting with its default and range. Changes show within a second; ${StyleStore.ERRORS} there says what was ignored and why.\n\n" +
                "To change the native Bellatrix views themselves — taskbar, start menu, action center, Settings cards, " +
                "window toolbars and home tiles — edit ~/${NativeUiSpec.GUEST_DIR}/${NativeUiSpec.FILE} for declarative style and " +
                "~/${NativeUiSpec.GUEST_DIR}/${NativeScript.FILE} for behaviour. look.toml contains `[[rule]]` blocks that find real " +
                "native views by resource id or class and change their size, padding, background, border, text and icons in place. " +
                "ui.lua runs inside the one App.runtime and exposes `nativeui` to hook clicks, long-presses and gestures, edit " +
                "adapter data, pin taskbar apps, open the existing Settings page and change window decorations. There is no second " +
                "launcher or replacement screen: the views you change are the ones already on screen. " +
                "${NativeUiSpec.README} next to them lists every surface, selector, property and API call; examples/ has active " +
                "taskbar and Settings scripts. Both files are watched: a save reaches the open views within a second, and " +
                "errors.txt there names anything ignored. Do not add a theme picker or a parallel UI to do this.\n\n" +
                "Everything on Bellatrix's one hard-coded home page (faces as tiles, widgets, pinned apps and folders, each tile's cell) is " +
                "~/${HomeStore.GUEST_DIR}/${HomeStore.FILE}; edit it to move, resize or pin things. There are no extra pages to reorder; " +
                "a `page:` in an owl file is ignored and the tile lands on that one page. docs/home.md in the widgets folder explains it.\n\n" +
                "The phone's approximate location, its calendar, the AirPods connected to it and its battery are `bellatrix location`, " +
                "`bellatrix calendar [days]`, `bellatrix airpods` and `bellatrix battery` (JSON; `bellatrix --help` says more), where the " +
                "launcher was allowed them. `bellatrix apps` lists what is on home: the icons on the desktop and the apps pinned to the taskbar, with their " +
                "names and packages, and the file of home's tiles and widgets (JSON). `bellatrix notify <title> [text]` puts a notification on the phone, to tell the user something " +
                "they should know while away, such as a long task being done; `bellatrix share <file>...` opens Android's share sheet for " +
                "files, to send them on with an app the user picks.\n"
    }
}
