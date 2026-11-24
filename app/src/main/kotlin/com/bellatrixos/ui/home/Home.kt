package com.bellatrixos.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import android.app.WallpaperManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import com.bellatrixos.widgets.OwlBars
import com.bellatrixos.widgets.lang.OwlKind
import com.bellatrixos.widgets.Folder
import com.bellatrixos.widgets.HomeItem
import com.bellatrixos.widgets.Screen
import com.bellatrixos.widgets.Shortcut
import com.bellatrixos.widgets.WidgetItem
import com.bellatrixos.widgets.WidgetSpec
import com.bellatrixos.widgets.app
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBars
import com.bellatrixos.theme.Style
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.linux.LinuxState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import com.bellatrixos.widgets.render.LocalHomeTiles
import com.bellatrixos.widgets.editWidgetCommand
import com.bellatrixos.widgets.fixDependenciesCommand
import com.bellatrixos.widgets.newOwlCommand
import com.bellatrixos.ui.BracketAction
import com.bellatrixos.ui.EdgeGap
import com.bellatrixos.ui.ExtraKeys
import com.bellatrixos.ui.ExtraKeysHeight
import com.bellatrixos.ui.HomeGap
import com.bellatrixos.ui.KeyboardSink
import com.bellatrixos.ui.LauncherStyle
import com.bellatrixos.ui.MenuItems
import com.bellatrixos.ui.Mono
import com.bellatrixos.ui.Palette
import com.bellatrixos.ui.TextSize
import com.bellatrixos.ui.extraKeysWanted
import com.bellatrixos.ui.frame
import com.bellatrixos.ui.passesKeys
import com.bellatrixos.ui.keyActivated

/** Home pages of JSON widgets on a snap grid, bordered and gapped like Bellatrix windows. */
@Composable
fun HomeScreen(
    runtime: Runtime,
    state: LauncherState,
    modifier: Modifier = Modifier,
) {
    val engine = runtime.widgets
    val linuxState = linuxPhase(runtime)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val arrange = remember { Arrange() }
    // A foldable's cover screen has a layout of its own (its cells and pages in the layout, [cover] in layout.toml), and
    // with home.unfolded-layout so does a Fold's unfolded inner screen ([unfolded]): home shows that one there, and the
    // main one back on the phone-shaped screen. The pages below follow a frame later.
    val screen =
        when {
            coverScreen() -> Screen.COVER
            unfoldedScreen() -> Screen.UNFOLDED
            else -> Screen.MAIN
        }
    SideEffect { engine.show(screen) }
    // Home is hard-coded to one page. Every widget, face, pinned app and folder lands there; no empty feed
    // page, no second tile page, and no two-up page on a foldable. The native grid keeps all its tiles.
    val per = 1
    val pageNumbers = pagerPages(engine.pages, false, false, false)
    // No face takes the page over: face files are tiles on the same native grid as every other widget.
    fun full(page: Int?) = false
    // No pages until the widgets have loaded: pinned apps load first, and pages shown before them would fill in, then
    // shift as the widgets' pages joined, with home turning to page 1 among them.
    val slots = if (engine.loaded) slotsOf(pageNumbers, per, ::full) else emptyList()
    fun slotOf(page: Int) = slots.indexOfFirst { page in it }
    fun homePage() = homePageIn(engine.pages, LauncherStyle.homePage)
    // Home always opens on its one hard-coded page, regardless of home.home-page or an old saved page number.
    val pager = key(engine.loaded) { rememberPagerState(initialPage = slotOf(homePage()).coerceAtLeast(0)) { slots.size } }
    // Kept for drops that ask to show where a tile landed; on one page it is simply page 1. A swipe wins if one
    // arrives before it settles.
    var showPage by remember { mutableStateOf<Int?>(null) }
    val currentSlots by rememberUpdatedState(slots)
    LaunchedEffect(showPage) {
        val page = showPage ?: return@LaunchedEffect
        try {
            withTimeoutOrNull(2_000) { snapshotFlow { currentSlots.indexOfFirst { page in it } }.first { it >= 0 } }?.let { pager.scrollToPage(it) }
        } finally {
            // Not when a newer drop asked meanwhile: that one runs this again.
            if (showPage == page) showPage = null
        }
    }
    var menuFor by remember { mutableStateOf<HomeItem?>(null) }

    val terminals = runtime.terminals
    // A page that has the tile holds the screen still, so its drags stay its own; editing lets go so a page can be moved.
    val editing = state.editing != null || arrange.folder != null
    SideEffect { terminals.editing = editing }
    // Composed last, a BackHandler takes Back first; this one goes before the others to take it only when nothing else on
    // home wants it, so a tile typing (a terminal's control, a page's history) gets it as it does now.
    BackHandler(enabled = terminals.locked) { terminals.unlock() }
    BackHandler(enabled = terminals.typing) { terminals.deactivate() }
    // A focused page takes Back for its own history first, as any web view does, and only then lets go of the tile.
    // Composed after the one above, so it is asked first: a BackHandler takes Back before the ones before it.
    val page = terminals.browser
    BackHandler(enabled = page != null && page.canGoBack) { page?.back() }
    BackHandler(enabled = state.homeMenuOpen) { state.homeMenuOpen = false }
    BackHandler(enabled = arrange.folder != null && !state.homeMenuOpen) { arrange.folder = null }
    // After the folder's, so back puts down an app selected in it before closing it.
    BackHandler(enabled = state.editing != null && !state.homeMenuOpen) { state.editing = null }
    // An app selected in a folder goes with it.
    LaunchedEffect(arrange.folder) {
        if (arrange.folder == null && engine.shortcuts.items.any { it.key == state.editing && it.folder != null }) state.editing = null
    }
    LaunchedEffect(engine) {
        engine.launcherAction = { action ->
            when (action) {
                // "themes" from before the screen was style; widgets still send it.
                "style", "themes" -> runtime.showNativeSettings("3")
                "agents" -> runtime.showCodingAgents()
                "store" -> runtime.showNativeSettings("4")
                "linux" -> runtime.showLinuxSettings()
                "drawer" -> state.open()
                "settings" -> runtime.showNativeSettings()
                // "page:N" is kept for old widgets: the launcher closes what covers home and returns to its one page.
                // A face left of page 1 no longer exists, so every page number means the same Bellatrix home.
                else ->
                    action.removePrefix("page:").takeIf { it != action }?.toIntOrNull()?.let {
                        state.close()
                        state.closePull()
                        state.closeScreens()
                        engine.openScreen = null
                        state.requestedPage = it
                    }
            }
        }
    }
    // Folding or unfolding keeps the page that was showing: unfolded, the pair it's in; folded, the left one of the pair.
    var shownPer by remember { mutableIntStateOf(per) }
    LaunchedEffect(per) {
        if (per == shownPer) return@LaunchedEffect
        val i = slotOf(engine.activePage)
        shownPer = per
        if (i >= 0) pager.scrollToPage(i)
    }
    // Switching screens (a fold or unfold with a home of its own there, or the cover screen) goes back to the page that
    // screen last showed, page 1 the first time: the page numbers are another screen's. A drag the switch cuts short
    // drops nothing, and carries nothing on.
    val lastPage = remember { HashMap<Screen, Int>() }
    var shownScreen by remember { mutableStateOf(screen) }
    LaunchedEffect(screen) {
        if (screen == shownScreen) return@LaunchedEffect
        shownScreen = screen
        state.editing = null
        arrange.folder = null
        arrange.drop()
        showPage = lastPage[screen] ?: homePage()
    }
    // The drawer pins apps to Bellatrix's one home page, wherever a tile was dragged. At start, pinned apps load
    // before widgets, so until widgets have loaded home keeps to that page rather than whatever stands at its index.
    val listed = remember { arrayOf(slots) }
    LaunchedEffect(pager.currentPage, slots, engine.loaded) {
        if (!engine.loaded) return@LaunchedEffect
        if (slots != listed[0]) {
            // The first pages after loading: the pager was made on home.home-page, so that is the active page.
            if (listed[0].isEmpty()) engine.activePage = slots.getOrNull(pager.currentPage)?.first() ?: 1
            listed[0] = slots
            val i = slotOf(engine.activePage)
            if (showPage == null && i >= 0 && i != pager.currentPage) {
                pager.scrollToPage(i)
                return@LaunchedEffect
            }
        }
        engine.activePage = slots.getOrNull(pager.currentPage)?.first() ?: 1
        if (engine.layout.screen == shownScreen && showPage == null) lastPage[shownScreen] = engine.activePage
        state.currentPage = engine.activePage.takeIf { it >= 1 && !full(it) } ?: 1
    }
    val shown = slots.getOrElse(pager.currentPage) { listOf(1) }
    // Faces are ordinary tiles on the one page, so they keep the native keyboard lift and status bar like widgets.
    val faceTyping = false
    val faceShowing = false
    val statusBarAway = false
    // The widgets showing whose lock: holds keep the screen still, read here rather than in composition, so a game's state
    // changing doesn't recompose home. Only those showing: a page turned away from can't hold the one turned to.
    LaunchedEffect(engine, shown) {
        snapshotFlow {
            val showing = shown.flatMap(engine::itemsOn).filterIsInstance<WidgetItem>().mapNotNullTo(HashSet()) { engine.instance(it.spec) }
            showing to showing.filterTo(HashSet()) { it.holdsHome() }
        }.collect { (showing, holding) -> terminals.holdWidgets(showing, holding) }
    }
    // Which pager slots are full screen, for the bar to slide with the others.
    val fullSlots = slots.map { it.all(::full) }
    SideEffect {
        state.faceShowing = faceShowing
        state.statusBarAway = statusBarAway
        state.barShift = { barShift(fullSlots, pager.currentPage + pager.currentPageOffsetFraction) }
    }
    // The background slides with the pages, like stock launchers. Our live wallpaper gets it through Android.
    val view = LocalView.current
    DisposableEffect(engine, view) {
        engine.view = view
        onDispose { if (engine.view === view) engine.view = null }
    }
    LaunchedEffect(pager, view) {
        val wallpapers = WallpaperManager.getInstance(view.context)
        snapshotFlow {
            val last = pager.pageCount - 1
            val offset = if (last <= 0) 0.5f else ((pager.currentPage + pager.currentPageOffsetFraction) / last).coerceIn(0f, 1f)
            // Resent when the wallpaper changes under us (set from the launcher or the phone), which starts out centered.
            offset to runtime.themes.systemWallpaperVersion
        }.collect { (offset, _) ->
            state.wallpaperOffset = offset
            view.windowToken?.let {
                wallpapers.setWallpaperOffsetSteps(if (pager.pageCount > 1) 1f / (pager.pageCount - 1) else 1f, 1f)
                wallpapers.setWallpaperOffsets(it, offset, 0.5f)
            }
        }
    }
    // A terminal tile keeps the keyboard only while its page shows.
    LaunchedEffect(pager.currentPage) { terminals.deactivate() }
    // Its shell exited, or the app lost focus: the soft keyboard goes too.
    val typing = terminals.active != null
    LaunchedEffect(typing) { if (!typing) terminals.keyboard?.hide() }
    DisposableEffect(state) {
        state.onHomeAgain = { if (arrange.folder != null) arrange.folder = null else state.requestedPage = homePage() }
        onDispose { state.onHomeAgain = {} }
    }
    LaunchedEffect(state.requestedPage) {
        state.requestedPage?.let { slotOf(it).takeIf { i -> i >= 0 }?.let { i -> pager.animateScrollToPage(i) } }
        state.requestedPage = null
    }

    fun openMenu(item: HomeItem?) {
        menuFor = item
        state.homeMenuOpen = true
    }

    fun remove(item: HomeItem) {
        state.editing = null
        when (item) {
            is WidgetItem -> engine.removeWidget(item.spec)
            is Shortcut, is Folder -> engine.shortcuts.remove(item.key)
        }
        engine.layout.remove(item.key)
    }

    /** Opens the default agent on the file; with no agent installed, the agents screen instead. */
    fun edit(spec: WidgetSpec) {
        state.editing = null
        state.withAgent(runtime, "Edit ${spec.name}") { editWidgetCommand(it, spec) }
    }

    /** Opens the file itself in Bellatrix's editor: no Linux, no agent, no second launcher screen. */
    fun editFile(spec: WidgetSpec) {
        state.editing = null
        scope.launch { runtime.edit(OwlBars.guestPath(spec.file.name)) }
    }

    // Overlays below are siblings of the pager inside this Box, so they never change the page's size.
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    // Where the keyboard ends up, so a terminal tile resizes once rather than with every frame of its animation.
    @OptIn(ExperimentalLayoutApi::class)
    val imeTarget = WindowInsets.imeAnimationTarget
    val statusBar = WindowInsets.statusBars.union(WindowInsets.displayCutout)
    // Back with the soft keyboard up only reaches the keyboard app, which closes itself. Closed like that
    // (not by the tile's ⌨ button), the tile lets go too, so the bar and recent apps come back. Only once it's gone:
    // hiding it again mid-way, while a back swipe still drives its animation, left home taking no touches.
    // A widget's text field the same: its keyboard closed without it losing focus.
    LaunchedEffect(terminals) {
        snapshotFlow { ime.getBottom(density) > 0 }.collect { shown ->
            if (!shown && (terminals.keyboard?.wanted == true || terminals.field != null)) terminals.deactivate()
        }
    }
    // The bar and recent apps don't wait for that: back as the keyboard starts closing, like the pages.
    LaunchedEffect(terminals) {
        snapshotFlow { imeTarget.getBottom(density) == 0 && (terminals.keyboard?.wanted == true || terminals.field != null) }
            .collect { terminals.keyboardLeaving = it }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val extraKeys = terminals.active != null && extraKeysWanted()
    val side = homeSide()
    val focusManager = LocalFocusManager.current
    val turned = homeTurned()
    val sideRoom = sideWidth()
    val barRoom = with(density) { state.barHeight.takeIf { it > 0 }?.toDp() ?: (PromptBarHeight + HomeGap + EdgeGap) }
    LaunchedEffect(extraKeys) { if (!extraKeys) terminals.extraKeys = null }
    Box(
        modifier
            .onGloballyPositioned { origin = it.positionInWindow() }
            // Sees every press first, without taking it: one outside the active terminal tile ends its turn.
            .pointerInput(terminals) {
                awaitEachGesture { terminals.pressAt(origin + awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).position) }
            },
    ) {
        // Owns the soft keyboard for terminal tiles.
        AndroidView(
            factory = { KeyboardSink(it, runtime.keyboard).also { sink -> terminals.keyboard = sink } },
            onRelease = { if (terminals.keyboard === it) terminals.keyboard = null },
            modifier = Modifier.size(1.dp),
        )
    // A widget's text field on a tile lifts it like a terminal does.
    CompositionLocalProvider(LocalHomeTiles provides terminals) {
    Column(
        Modifier
            .fillMaxSize()
            // Swiping up opens the drawer, like stock launchers, or what gesture settings say; not while editing, in a
            // folder or typing in a tile, nor while a widget holds the screen still. A long-press drag takes its touches
            // first, and the pager only wants sideways ones.
            .homeSwipe(runtime, state, enabled = !terminals.typing && state.editing == null && arrange.folder == null && !terminals.locked)
            // Typing in a tile (its terminal or a widget's text field): lift the pages so the tile stays above the keyboard,
            // but not under the status bar; a tile too tall for both gives up the rest at its bottom.
            .graphicsLayer {
                val keys = if (extraKeys) terminals.extraKeysHeight.takeIf { it > 0f } ?: ExtraKeysHeight.toPx() else 0f
                translationY =
                    -terminals.liftFor(
                        imeHeight = ime.getBottom(density),
                        imeTarget = imeTarget.getBottom(density),
                        windowHeight = view.height,
                        margin = 8.dp.toPx() + keys,
                        top = statusBar.getTop(density) + EdgeGap.toPx(),
                        minHeight = 64.dp.toPx(),
                    )
            }
            // Nothing below where the typing tile ends, above the extra keys, so the gaps around them show the wallpaper, and
            // nothing lifted under the status bar. A face typing is the whole screen: it isn't lifted and keeps drawing
            // under the status bar and down to the keyboard.
            .drawWithContent {
                // Typing first: reading the keyboard's inset would draw this again on each frame it slides, typing or not.
                val keyboard = if (!terminals.typing || faceTyping) 0 else ime.getBottom(density)
                if (keyboard == 0) {
                    drawContent()
                } else {
                    val keys = if (extraKeys) terminals.extraKeysHeight.takeIf { it > 0f } ?: ExtraKeysHeight.toPx() else 0f
                    val lift = terminals.lift - origin.y
                    clipRect(
                        top = statusBar.getTop(density) + lift,
                        bottom = view.height - keyboard - keys - 8.dp.toPx() + lift,
                    ) { this@drawWithContent.drawContent() }
                }
            }
    ) {
        // The pages fill the window, so a face can; tiles keep clear of the system bars (as if shown, so they
        // don't move when a face hides the status bar; with style hiding it they reach the top, as always) and of the
        // bar, which DesktopScreen draws over the pages (so the keyboard lifts it without shrinking the grid). Below the
        // grid the strip, a gap, the bar and the gap to the edge: the bar as measured, since a large font makes it
        // taller. In a phone's landscape they stand beside it instead, on the side portrait's bottom turned to.
        @OptIn(ExperimentalLayoutApi::class)
        val bars = (if (LauncherStyle.style[Style.STATUS_BAR_SHOWN]) WindowInsets.systemBarsIgnoringVisibility else WindowInsets.systemBars).union(WindowInsets.displayCutout)
        val direction = LocalLayoutDirection.current
        val keepClear =
            with(density) {
                GridPad(
                    left = bars.getLeft(density, direction) + if (side == HomeSide.Left) sideRoom.roundToPx() else 0,
                    top = bars.getTop(density),
                    right = bars.getRight(density, direction) + if (side == HomeSide.Right) sideRoom.roundToPx() else 0,
                    bottom = bars.getBottom(density) + if (side != null) 0 else barRoom.roundToPx(),
                )
            }
        HomeGrid(
            pager = pager,
            keepClear = keepClear,
            slots = slots,
            full = ::full,
            itemsOn = engine::itemsOn,
            layout = engine.layout,
            arrange = arrange,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // Down out of the tiles stops at the recent apps first, which Compose's own search passes over.
                    .focusProperties { onExit = { if (requestedFocusDirection == FocusDirection.Down) state.stripEntry.requestFocus() } }
                    // In landscape there's nothing below for that search to find, so it never leaves: Down, or the arrow
                    // toward the rail once there's no page left to turn to that way, goes to the recent apps (the bar's
                    // rail without them) where the arrow finds nothing else.
                    .then(
                        if (side == null) {
                            Modifier
                        } else {
                            Modifier.onKeyEvent { e ->
                                val direction =
                                    when (e.key) {
                                        Key.DirectionDown -> FocusDirection.Down
                                        Key.DirectionLeft -> FocusDirection.Left.takeIf { side == HomeSide.Left }
                                        Key.DirectionRight -> FocusDirection.Right.takeIf { side == HomeSide.Right }
                                        else -> null
                                    } ?: return@onKeyEvent false
                                e.type == KeyEventType.KeyDown && (focusManager.moveFocus(direction) || state.stripEntry.requestFocus())
                            }
                        },
                    )
                    .focusRequester(state.homeEntry)
                    .focusGroup(),
            selected = state.editing,
            locked = terminals.locked,
            onSelect = { state.editing = it },
            actions = { item ->
                // A pinned Android app offers its shortcuts above the bar, as its long-press menu in the drawer does.
                val app = (item as? Shortcut)?.component?.let { runtime.androidApps.pinned(it, item.profile) }
                val (menu, offered) =
                    offeredShortcuts(item.key) {
                        app?.let { shortcutMenu(runtime, it, page = { item.page }, onOpen = { arrange.folder = null }) }
                    }
                // Nothing until they're loaded, so the bar doesn't jump down as they arrive.
                if (offered != null) Column {
                    if (menu != null && offered.isNotEmpty()) {
                        MenuItems(emptyList(), { state.editing = null }, minWidth = 160.dp, itemPadding = 12.dp) {
                            ShortcutRows(offered, menu, { state.editing = null }, TextSize.Item, rule = false)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    EditBar {
                        // A widget's own file, or the owl app a pinned shortcut opens.
                        val spec = if (item is WidgetItem) item.spec else (item as? Shortcut)?.owl?.let(engine.specs::app)
                        if (item is Shortcut) {
                            EditAction(stringResource(R.string.drawer_action_window)) { runtime.openInWindow(item) }
                        }
                        if (spec != null) EditAction(stringResource(R.string.home_edit)) { edit(spec) }
                        if (spec != null) EditAction(stringResource(R.string.home_edit_file)) { editFile(spec) }
                        EditAction(stringResource(R.string.home_remove), Palette.red) { remove(item) }
                    }
                }
            },
            // A function of its own, which skips when its tile didn't change: a lambda's body here would run again for
            // every tile each time home recomposes, as on each page turn.
            render = { item, placed, selected -> HomeTile(item, placed, selected, runtime, state, arrange, turned, linuxState) },
            // The recent apps strip's room: kept while it's empty, so tiles don't reflow when it fills, and still
            // home's to swipe and long-press there until it does.
            room = if (LauncherStyle.recentApps && side == null) RecentStripHeight else 0.dp,
            turned = turned,
            side = side,
            per = per,
            onLongPressEmpty = { openMenu(null) },
            invite = {
                val agent = runtime.agents.default?.name
                PageInvite(
                    stringResource(R.string.home_widget_invite_title),
                    agent?.let { stringResource(R.string.home_widget_invite_text, it) } ?: stringResource(R.string.home_widget_invite_text_no_agent),
                    agent,
                    onBrowse = { runtime.showNativeSettings("4") },
                ) {
                    if (linuxState == LinuxPhase.Installed) {
                        state.withAgent(runtime, "New widget") { newOwlCommand(it, OwlKind.Widget) }
                    } else {
                        // No Linux needed to make a widget: the file opens in Bellatrix's own editor.
                        scope.launch { OwlBars.newWidget(context, runtime) }
                    }
                }
            },
            // Home turns to where a tile the user just added went; a shipped one that doesn't fit, as on a short
            // screen's first home, moves without taking home off page 1.
            onNoRoom = { item ->
                val turn = engine.placedOnPhone(item)
                val page = engine.moveToFreePage(item)
                if (turn) showPage = page
            },
            onMove = { item, page ->
                engine.moveToPage(item, page)
                showPage = page
            },
            onGroup = { app, target -> engine.shortcuts.group(app, target) },
            onReorder = { app, before -> engine.shortcuts.moveBefore(app.key, before) },
            onRenameFolder = { folder, name -> engine.shortcuts.rename(folder.key, name) },
        )
    }
    }

    // A widget holds the home screen still: the border, and the way out of it, over the pages and above them here, so
    // a widget that covers the tile area can't cover the way out.
    if (terminals.locked) {
        LockCue(
            face = faceShowing,
            onUnlock = terminals::unlock,
        )
    }

    if (extraKeys) {
        ExtraKeys(
            runtime,
            Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .onGloballyPositioned {
                    terminals.extraKeys = it.boundsInWindow()
                    terminals.extraKeysHeight = it.size.height.toFloat()
                },
        )
    }

    }
}


/**
 * An invitation on Bellatrix's one empty home page to have [agent] build a widget, or to take one from the store.
 */
@Composable
private fun PageInvite(
    title: String,
    text: String,
    agent: String?,
    onBrowse: () -> Unit,
    onCreate: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(max = 360.dp)
                .frame(1.dp, Palette.border, Palette.card)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText(title, style = Mono.copy(color = Palette.accent))
            // The theme's own text color: muted is too faint on some themes' card.
            BasicText(text, style = Mono)
            // Pulled left by the buttons' own padding, so the first bracket lines up with the text.
            Column(Modifier.offset(x = (-8).dp)) {
                BracketAction(createWithLabel(agent), Palette.accent, onClick = onCreate)
                BracketAction(stringResource(R.string.home_browse_store), Palette.accent, onClick = onBrowse)
            }
        }
    }
}

/** "create with claude code", or with an agent while none is the default. */
@Composable
internal fun createWithLabel(agent: String?): String =
    agent?.let { stringResource(R.string.home_create_with, it.lowercase()) } ?: stringResource(R.string.home_create_with_an_agent)

/** Bellatrix home is hard-coded to one page: no empty feed page, no second tile page, no face page. */
@Suppress("UNUSED_PARAMETER")
internal fun pagerPages(
    pages: List<Int>,
    lastEmpty: Boolean,
    emptyPageLeft: Boolean,
    emptyPageRight: Boolean,
): List<Int> = listOf(1)

/** Home always opens on its one page, whatever an old style or layout says. */
@Suppress("UNUSED_PARAMETER")
internal fun homePageIn(
    pages: List<Int>,
    wanted: Int,
): Int = 1

/**
 * The pager's slots. Bellatrix always passes one page and [per] 1, so this is the one home page; the old paired and
 * full-page branches stay only for callers from before the single hard-coded page.
 */
internal fun slotsOf(
    pages: List<Int>,
    per: Int,
    full: (Int) -> Boolean,
): List<List<Int>> {
    if (per == 1) return pages.map { listOf(it) }
    val slots = ArrayList<List<Int>>()
    var i = 0
    while (i < pages.size) {
        val page = pages[i]
        val next = pages.getOrNull(i + 1)
        slots +=
            when {
                full(page) -> listOf(page)
                next == null -> listOf(page, page + 1)
                full(next) -> listOf(page)
                else -> listOf(page, next).also { i++ }
            }
        i++
    }
    return slots
}

/**
 * Where the strip and the bar stand across at pager [position], in screen widths. With one home page [full] is all
 * false and this stays 0, so the native strip and bar never slide away; the old face-page branches remain for old callers.
 */
internal fun barShift(
    full: List<Boolean>,
    position: Float,
): Float {
    val left = kotlin.math.floor(position).toInt()
    val f = position - left
    val leftFull = full.getOrElse(left) { false }
    val rightFull = full.getOrElse(left + 1) { leftFull }
    return when {
        !leftFull && !rightFull -> 0f
        // Going right from a tile page: the bar leaves to the left with it.
        !leftFull -> -f
        // Coming from a face on the left: the bar comes in from the right with the tile page.
        !rightFull -> 1f - f
        else -> 1f
    }
}

/** A tile on home: a widget, a pinned app or a folder, [selected] for editing, at [placed]. */
@Composable
private fun HomeTile(
    item: HomeItem,
    placed: Modifier,
    selected: Boolean,
    runtime: Runtime,
    state: LauncherState,
    arrange: Arrange,
    /** As homeTurned(): cells are shown on their side. */
    turned: Boolean,
    linuxState: LinuxPhase,
) {
    val engine = runtime.widgets
    val terminals = runtime.terminals
    val density = LocalDensity.current
    val open: () -> Unit =
        when (item) {
            is WidgetItem -> { -> engine.tap(item.spec) }
            // Opening an app from a folder closes the folder, like Android.
            is Shortcut -> { ->
                arrange.folder = null
                runtime.open(item)
            }
            is Folder -> { -> arrange.folder = item.key }
        }
    // Keys reach a tile as a finger does: Enter opens it, and Menu or holding Enter selects it, as a long press.
    // What takes taps in a widget is inside it, Right away; Enter goes there too on one with no tap of its own.
    // It shows as the tile's own focus.
    var keyed by remember(item.key) { mutableStateOf(false) }
    val inside = remember(item.key) { FocusRequester() }
    val keyOpen: () -> Unit = if (item is WidgetItem && item.spec.program?.onTap == null) { { inside.requestFocus() } } else open
    // Keys on a tile as it's removed, or as its page goes, go to the first tile of the page that shows.
    val tile = placed.passesKeys(state.homeEntry).keyActivated(keyOpen, { state.editing = item.key }, inside) { keyed = it.hasFocus }
    val highlighted = selected || keyed
    when (item) {
        is WidgetItem -> {
            // Whose terminal has the keyboard: the tile gives up what the keyboard covers once the lift is spent.
            val instance = engine.instance(item.spec)
            val typing = instance != null && terminals.typingIn(instance)
            WidgetTile(
                spec = item.spec,
                instance = engine.instance(item.spec),
                // Faces are ordinary tiles on the one page, so they use their own cell like any widget.
                cells = engine.layout.cell(item.key)?.let { it.w to it.h }?.let { (w, h) -> if (turned) h to w else w to h },
                // Accent like Hyprland's active border: only while one of its terminals has the keyboard.
                focused = highlighted || typing,
                keyboard = 0f,
                modifier =
                    if (typing) {
                        tile
                            .onGloballyPositioned { terminals.tile = it.boundsInWindow().translate(0f, terminals.lift) }
                            .padding(bottom = with(density) { terminals.cut.toDp() })
                    } else {
                        tile
                    },
                onTap = open,
                needs = engine.missing[item.spec.file.name].orEmpty(),
                install = engine.installs[item.spec.file.name],
                onInstall = { engine.installDependencies(item.spec) },
                waitsForLinux = if (engine.waitsForLinux(item.spec)) linuxState else null,
                onInstallLinux = { runtime.showLinuxSettings() },
                onFix = { log -> state.withAgent(runtime, "Fix ${item.spec.name}") { fixDependenciesCommand(it, item.spec, log) } },
            )
        }
        is Shortcut ->
            ShortcutTile(
                shortcut = item,
                label = runtime.shortcutLabel(item),
                icon = runtime.shortcutIcon(item, LauncherStyle.monochromeIcons),
                focused = highlighted,
                modifier = tile,
                onTap = open,
                paused = runtime.androidApps.paused(item.profile),
                clone = runtime.androidApps.cloned(item.profile),
                dot = runtime.hasDot(item),
            )
        is Folder ->
            FolderTile(
                folder = item,
                icons = item.apps.take(4).map { runtime.shortcutIcon(it, LauncherStyle.monochromeIcons) },
                labels = item.apps.take(4).map { runtime.shortcutLabel(it) },
                focused = highlighted,
                modifier = tile,
                onTap = open,
                dot = item.apps.any(runtime::hasDot),
            )
    }
}

/** What home and its bar show of Linux: [LinuxState] without an install's progress and log, which change with every line it logs. */
enum class LinuxPhase {
    Checking,
    Installing,
    Installed,

    /** Not installed, failed or being removed. */
    Absent,
}

/** Linux's [LinuxPhase], recomposing the caller only when that changes. */
@Composable
fun linuxPhase(runtime: Runtime): LinuxPhase {
    val bootstrap = runtime.bootstrap
    val phases = remember(bootstrap) { bootstrap.state.phases() }
    val initial = remember(bootstrap) { linuxPhase(bootstrap.state.value) }
    return phases.collectAsState(initial).value
}

/** These states' phases, each once until it changes. */
internal fun Flow<LinuxState>.phases(): Flow<LinuxPhase> = map(::linuxPhase).distinctUntilChanged()

private fun linuxPhase(state: LinuxState) =
    when (state) {
        LinuxState.Checking -> LinuxPhase.Checking
        is LinuxState.Installing -> LinuxPhase.Installing
        LinuxState.Installed -> LinuxPhase.Installed
        LinuxState.NotInstalled, is LinuxState.Failed, LinuxState.Removing -> LinuxPhase.Absent
    }
