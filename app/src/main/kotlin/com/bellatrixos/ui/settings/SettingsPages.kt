package com.bellatrixos.ui.settings

import com.bellatrixos.widgets.lang.OwlKind
import com.bellatrixos.widgets.checkSecretsCommand
import com.bellatrixos.widgets.publishWidgetCommand
import com.bellatrixos.widgets.WidgetSpec
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
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
import com.bellatrixos.ui.home.PagesScreen
import com.bellatrixos.ui.store.ShareWidgetScreen
import com.bellatrixos.ui.store.StoreScreen

/** The screens of the settings activity; each can also be opened on its own, from home or a widget. */
enum class SettingsPage {
    Settings,
    Style,

    /** Home's pages, to arrange, edit and remove. */
    Pages,

    /** The community themes, over the style screen or from the home menu's theme strip. */
    Themes,
    Agents,
    Store,

    /** The store on its faces, from the empty page left of page 1. */
    FaceStore,
    Packages,

    /** Install, progress, remove; opened by anything that needs Linux. */
    Linux,
    Licenses,
    Gestures,

    /** Every permission, to turn on or off. */
    Permissions,
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
                onPages = { stack.push(SettingsPage.Pages) },
                onAgents = {
                    close()
                    runtime.showCodingAgents()
                },
                onPackages = { stack.push(SettingsPage.Packages) },
                onLinux = { stack.push(SettingsPage.Linux) },
                onLicenses = { stack.push(SettingsPage.Licenses) },
                onGestures = { stack.push(SettingsPage.Gestures) },
                onPermissions = { stack.push(SettingsPage.Permissions) },
            )
        SettingsPage.Style -> StyleScreen(runtime, onDismiss = close, onBrowse = { stack.push(SettingsPage.Themes) }, onAgent = ::agent)
        SettingsPage.Pages -> {
            var sharing by remember { mutableStateOf<WidgetSpec?>(null) }
            PagesScreen(runtime, onDismiss = close, onAgent = ::agent, onShare = { sharing = it })
            sharing?.let { spec ->
                ShareWidgetScreen(
                    spec,
                    // Only your own: one from the store or from someone else isn't yours to submit.
                    onPublish =
                        if (runtime.widgets.isOwnWidget(spec)) {
                            {
                                sharing = null
                                agent("Publish ${spec.name}") { publishWidgetCommand(it, spec) }
                            }
                        } else {
                            null
                        },
                    onCheck = {
                        sharing = null
                        agent("Check ${spec.name}") { checkSecretsCommand(it, spec) }
                    },
                    onDismiss = { sharing = null },
                )
            }
        }
        SettingsPage.Themes -> ThemeCatalogScreen(runtime.themes, onDismiss = close)
        // Kept only for old intents: hand off to the Bellatrix Settings page instead of showing a ported screen.
        SettingsPage.Agents -> {
            close()
            runtime.showCodingAgents()
        }
        SettingsPage.Store -> StoreScreen(runtime.widgets, runtime.agents.installed, homePage, runtime.themes.picked, onDismiss = close, onOpenApp = runtime::openOwlApp)
        SettingsPage.FaceStore -> StoreScreen(runtime.widgets, runtime.agents.installed, homePage, runtime.themes.picked, onDismiss = close, initialKind = OwlKind.Face, onOpenApp = runtime::openOwlApp)
        SettingsPage.Packages -> PackagesScreen(runtime, onDismiss = close)
        SettingsPage.Linux -> LinuxScreen(runtime, onDismiss = close)
        SettingsPage.Licenses -> LicensesScreen(onDismiss = close)
        SettingsPage.Gestures -> GesturesScreen(runtime, onDismiss = close, onAgent = ::agent)
        SettingsPage.Permissions -> PermissionsScreen(runtime, onDismiss = close)
    }
}
