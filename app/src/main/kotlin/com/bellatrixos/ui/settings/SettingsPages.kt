package com.bellatrixos.ui.settings

import com.bellatrixos.widgets.lang.OwlKind
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.bellatrixos.Runtime
import com.bellatrixos.ui.LocalPanelBack
import com.bellatrixos.ui.pane
import com.bellatrixos.ui.home.GesturesScreen
import com.bellatrixos.ui.store.StoreScreen

/** The screens of the settings activity; each can also be opened on its own, from home or a widget. */
enum class SettingsPage {
    Settings,
    Style,
    Agents,
    Store,

    /** The store filtered to faces, kept for old intents; faces are tiles on the one home page. */
    FaceStore,

    /** Install, progress, remove; opened by anything that needs Linux. */
    Linux,
    Gestures,
}

/** Opens [page] over the ones in [stack]; one that's already open comes back to the top, closing what's over it. */
fun SnapshotStateList<SettingsPage>.push(page: SettingsPage) {
    val at = indexOf(page)
    if (at >= 0) removeRange(at + 1, size) else add(page)
}

/** Closes [page] and whatever was opened over it. */
fun SnapshotStateList<SettingsPage>.pop(page: SettingsPage) {
    val at = indexOf(page)
    if (at >= 0) removeRange(at, size)
}

/**
 * The open settings screens, each drawn over the one that opened it, as they were
 * over home. [homePage] is the home page the store puts widgets on. Closing the
 * first one calls [onClose] instead, so it stays drawn until the window goes.
 */
@Composable
fun SettingsPages(
    runtime: Runtime,
    stack: SnapshotStateList<SettingsPage>,
    homePage: Int,
    onClose: () -> Unit,
) {
    val close = { page: SettingsPage -> if (stack.indexOf(page) == 0) onClose() else stack.pop(page) }
    // The screens' own back handlers (a store page, a license) come later, so they go first.
    BackHandler(enabled = stack.isNotEmpty()) { close(stack.last()) }
    Box(Modifier.fillMaxSize().pane()) {
        val pages = stack.toList()
        pages.forEachIndexed { i, page ->
            key(page) {
                // A page under the top one stays composed (its scroll and input survive going back) but is not drawn:
                // the panels are glass, so drawing it would show it through the page over it instead of the wallpaper.
                val covered = i != pages.lastIndex
                Box(Modifier.fillMaxSize().then(if (covered) Modifier.alpha(0f) else Modifier)) {
                    // One opened from another has a chevron back to it instead of the ✕.
                    CompositionLocalProvider(LocalPanelBack provides if (i == 0) null else ({ close(page) })) {
                        SettingsPage(runtime, page, stack, homePage, onDismiss = { close(page) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    runtime: Runtime,
    page: SettingsPage,
    stack: SnapshotStateList<SettingsPage>,
    homePage: Int,
    onDismiss: () -> Unit,
) {
    val close = onDismiss

    // Settings has no home underneath: the agent opens in its own terminal, or Bellatrix Settings opens
    // the standalone Coding Agents page (never the ported screen).
    fun agent(
        title: String,
        command: (String) -> String,
    ) {
        val agent = runtime.agents.default
        if (agent == null) {
            close()
            runtime.showCodingAgents()
            return
        }
        runtime.terminal(command(agent.bin), title, agent)
    }

    when (page) {
        SettingsPage.Settings ->
            SettingsScreen(
                runtime,
                onDismiss = close,
                onStyle = { stack.push(SettingsPage.Style) },
                onAgents = {
                    close()
                    runtime.showCodingAgents()
                },
                // These three are native pages of Bellatrix's Settings window, never Compose screens.
                onPackages = { runtime.showPackages(); close() },
                onLinux = { stack.push(SettingsPage.Linux) },
                onLicenses = { runtime.showLicenses(); close() },
                onGestures = { stack.push(SettingsPage.Gestures) },
                onPermissions = { runtime.showPermissions(); close() },
            )
        SettingsPage.Style -> StyleScreen(runtime, onDismiss = close, onAgent = ::agent)
        // Kept only for old intents: hand off to the Bellatrix Settings page instead of showing a ported screen.
        SettingsPage.Agents -> {
            close()
            runtime.showCodingAgents()
        }
        SettingsPage.Store -> StoreScreen(runtime.widgets, runtime.agents.installed, homePage, runtime.themes.picked, onDismiss = close, onOpenApp = runtime::openOwlApp)
        SettingsPage.FaceStore -> StoreScreen(runtime.widgets, runtime.agents.installed, homePage, runtime.themes.picked, onDismiss = close, initialKind = OwlKind.Face, onOpenApp = runtime::openOwlApp)
        SettingsPage.Linux -> LinuxScreen(runtime, onDismiss = close)
        SettingsPage.Gestures -> GesturesScreen(runtime, onDismiss = close, onAgent = ::agent)
    }
}
