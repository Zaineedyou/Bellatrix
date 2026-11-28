package com.bellatrixos.app_browser

import android.util.Log
import com.bellatrixos.R
import com.bellatrixos.widgets.UINotification
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/**
 * Firefox add-ons (WebExtensions) for the Browser. Gecko keeps the installed extensions across restarts but hands the
 * app nothing until it asks, so [attach] lists them and puts this app's delegates on each one: install prompts, the
 * toolbar buttons (browser actions) and their popups, tabs an extension opens or closes. Main thread only.
 */
internal object Extensions {
    private const val TAG = "Extensions"

    /** Where "Find more add-ons" goes: addons.mozilla.org, whose "Add to Firefox" button installs through [attach]'s delegates. */
    const val STORE_URL = "https://addons.mozilla.org/firefox/extensions/"

    private var runtime: GeckoRuntime? = null
    private val installed = LinkedHashMap<String, WebExtension>()

    /** Each extension's toolbar button as it is for every tab, and as a tab overrides it (a badge count, say). */
    private val defaultActions = HashMap<String, WebExtension.Action>()
    private val sessionActions = WeakHashMap<GeckoSession, HashMap<String, WebExtension.Action>>()
    private val tabViews = ArrayList<WeakReference<GeckoTabView>>()
    private val listeners = ArrayList<() -> Unit>()

    /** The Browser window that answers prompts and opens tabs for extensions: the one used last. */
    var browser: WeakReference<UIBrowser>? = null

    /** Called once, when the Gecko runtime is created. */
    fun attach(runtime: GeckoRuntime) {
        this.runtime = runtime
        val controller = runtime.webExtensionController
        controller.setPromptDelegate(promptDelegate)
        controller.setAddonManagerDelegate(managerDelegate)
        refresh()
    }

    /** Asks Gecko for the installed extensions. This is also what registers them again after the app restarted. */
    fun refresh() {
        val controller = runtime?.webExtensionController ?: return
        controller.list().accept(
            { list ->
                list?.forEach { register(it) }
                changed()
            },
            { error -> Log.w(TAG, "could not list the extensions", error) },
        )
    }

    /** A new tab's session: every extension may now act on it. */
    fun onSession(view: GeckoTabView) {
        tabViews.removeAll { it.get() == null }
        tabViews.add(WeakReference(view))
        for (extension in installed.values) attachSession(view.session, extension)
    }

    /** Tells the extensions which tab is in front, for their tabs API. */
    fun setTabActive(
        session: GeckoSession,
        active: Boolean,
    ) {
        runtime?.webExtensionController?.setTabActive(session, active)
    }

    /** The tab in front of the active Browser window, if any. */
    fun currentSession(): GeckoSession? = browser?.get()?.z0?.y0?.session

    /** Installed extensions, A to Z. Gecko's own built-in ones are not the person's to manage. */
    fun all(): List<WebExtension> =
        installed.values
            .filter { !it.isBuiltIn }
            .sortedBy { (it.metaData.name ?: it.id).lowercase() }

    /** The toolbar button of [extension] as it is for [session] (null: for every tab), or null when it has none. */
    fun action(
        extension: WebExtension,
        session: GeckoSession?,
    ): WebExtension.Action? {
        val base = defaultActions[extension.id]
        val own = session?.let { sessionActions[it]?.get(extension.id) }
        return if (own != null && base != null) own.withDefault(base) else own ?: base
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun setEnabled(
        extension: WebExtension,
        enabled: Boolean,
    ) {
        val controller = runtime?.webExtensionController ?: return
        val result =
            if (enabled) {
                controller.enable(extension, WebExtensionController.EnableSource.USER)
            } else {
                controller.disable(extension, WebExtensionController.EnableSource.USER)
            }
        result.accept(
            { updated ->
                updated?.let { register(it) }
                changed()
            },
            { error -> Log.w(TAG, "could not change ${extension.id}", error) },
        )
    }

    fun remove(extension: WebExtension) {
        val controller = runtime?.webExtensionController ?: return
        controller.uninstall(extension).accept(
            { changed() },
            { error -> Log.w(TAG, "could not remove ${extension.id}", error) },
        )
    }

    /** Opens the extension's options page in a tab. */
    fun openOptions(extension: WebExtension) {
        val url = extension.metaData.optionsPageUrl ?: return
        browser?.get()?.k0(url)
    }

    fun findMore() {
        browser?.get()?.k0(STORE_URL)
    }

    private fun register(extension: WebExtension) {
        installed[extension.id] = extension
        extension.setActionDelegate(actionDelegate)
        extension.tabDelegate = tabDelegate
        for (ref in tabViews) ref.get()?.let { attachSession(it.session, extension) }
    }

    private fun attachSession(
        session: GeckoSession,
        extension: WebExtension,
    ) {
        val controller = session.webExtensionController
        controller.setActionDelegate(extension, actionDelegate)
        controller.setTabDelegate(extension, sessionTabDelegate)
    }

    private fun changed() {
        for (listener in listeners.toList()) listener()
    }

    private fun toast(
        resource: Int,
        argument: Any,
    ) {
        val context = browser?.get()?.u0 ?: return
        UINotification.a(context, context.getString(resource, argument), null)
    }

    private fun nameOf(extension: WebExtension): String = extension.metaData.name ?: extension.id

    /** Asks the person (the extension's install, update or optional-permission prompt) and answers [done] once. */
    private fun ask(
        extension: WebExtension,
        title: Int,
        positive: Int,
        permissions: Array<String>,
        origins: Array<String>,
        dataCollection: Array<String>,
        done: (Boolean) -> Unit,
    ) {
        val context = browser?.get()?.u0
        if (context == null) {
            done(false)
            return
        }
        ExtensionUi.confirm(context, extension, title, positive, permissions, origins, dataCollection, done)
    }

    private fun answer(
        extension: WebExtension,
        title: Int,
        positive: Int,
        permissions: Array<String>,
        origins: Array<String>,
        dataCollection: Array<String>,
    ): GeckoResult<AllowOrDeny> {
        val result = GeckoResult<AllowOrDeny>()
        ask(extension, title, positive, permissions, origins, dataCollection) { ok ->
            result.complete(if (ok) AllowOrDeny.ALLOW else AllowOrDeny.DENY)
        }
        return result
    }

    private val promptDelegate =
        object : WebExtensionController.PromptDelegate {
            override fun onInstallPromptRequest(
                extension: WebExtension,
                permissions: Array<String>,
                origins: Array<String>,
                dataCollectionPermissions: Array<String>,
            ): GeckoResult<WebExtension.PermissionPromptResponse>? {
                val result = GeckoResult<WebExtension.PermissionPromptResponse>()
                ask(extension, R.string.ext_add_title, R.string.ext_add, permissions, origins, dataCollectionPermissions) { ok ->
                    // The Browser has no private windows, and the optional technical-data sharing is left off.
                    result.complete(WebExtension.PermissionPromptResponse(ok, false, false))
                }
                return result
            }

            override fun onUpdatePrompt(
                extension: WebExtension,
                newPermissions: Array<String>,
                newOrigins: Array<String>,
                newDataCollectionPermissions: Array<String>,
            ): GeckoResult<AllowOrDeny>? =
                answer(extension, R.string.ext_update_title, R.string.ext_update, newPermissions, newOrigins, newDataCollectionPermissions)

            override fun onOptionalPrompt(
                extension: WebExtension,
                permissions: Array<String>,
                origins: Array<String>,
                dataCollectionPermissions: Array<String>,
            ): GeckoResult<AllowOrDeny>? =
                answer(extension, R.string.ext_optional_title, R.string.ext_allow, permissions, origins, dataCollectionPermissions)
        }

    private val managerDelegate =
        object : WebExtensionController.AddonManagerDelegate {
            override fun onInstalled(extension: WebExtension) {
                register(extension)
                changed()
                toast(R.string.ext_installed, nameOf(extension))
            }

            override fun onEnabled(extension: WebExtension) {
                register(extension)
                changed()
            }

            override fun onDisabled(extension: WebExtension) {
                register(extension)
                changed()
            }

            override fun onReady(extension: WebExtension) {
                register(extension)
                changed()
            }

            override fun onUninstalled(extension: WebExtension) {
                installed.remove(extension.id)
                defaultActions.remove(extension.id)
                changed()
                toast(R.string.ext_removed, nameOf(extension))
            }

            override fun onInstallationFailed(
                extension: WebExtension?,
                installException: WebExtension.InstallException,
            ) {
                // Saying no to the prompt is a failed install too, and not worth a message.
                if (installException.code == WebExtension.InstallException.ErrorCodes.ERROR_USER_CANCELED) return
                toast(R.string.ext_install_failed, installException.code)
            }
        }

    private val actionDelegate =
        object : WebExtension.ActionDelegate {
            override fun onBrowserAction(
                extension: WebExtension,
                session: GeckoSession?,
                action: WebExtension.Action,
            ) {
                if (session == null) {
                    defaultActions[extension.id] = action
                } else {
                    sessionActions.getOrPut(session) { HashMap() }[extension.id] = action
                }
                changed()
            }

            override fun onTogglePopup(
                extension: WebExtension,
                action: WebExtension.Action,
            ): GeckoResult<GeckoSession>? = popup()

            override fun onOpenPopup(
                extension: WebExtension,
                action: WebExtension.Action,
            ): GeckoResult<GeckoSession>? = popup()
        }

    /** The popup an extension's button opens, shown under the Extensions button; null when no Browser window can show it. */
    private fun popup(): GeckoResult<GeckoSession>? {
        val window = browser?.get() ?: return null
        val context = window.u0 ?: return null
        val anchor = window.extensionsButton ?: return null
        return ExtensionUi.openPopup(context, anchor)
    }

    private val tabDelegate =
        object : WebExtension.TabDelegate {
            override fun onNewTab(
                source: WebExtension,
                createDetails: WebExtension.CreateTabDetails,
            ): GeckoResult<GeckoSession>? {
                val window = browser?.get() ?: return null
                val result = GeckoResult<GeckoSession>()
                // Gecko loads the page itself into the session of the new tab.
                window.openExtensionTab { session -> result.complete(session) }
                return result
            }

            override fun onOpenOptionsPage(source: WebExtension) {
                openOptions(source)
            }
        }

    private val sessionTabDelegate =
        object : WebExtension.SessionTabDelegate {
            override fun onCloseTab(
                source: WebExtension?,
                session: GeckoSession,
            ): GeckoResult<AllowOrDeny> {
                val view = tabViews.firstNotNullOfOrNull { ref -> ref.get()?.takeIf { it.session === session } }
                val closed = view?.closeFromExtension() == true
                return GeckoResult.fromValue(if (closed) AllowOrDeny.ALLOW else AllowOrDeny.DENY)
            }

            override fun onUpdateTab(
                extension: WebExtension,
                session: GeckoSession,
                details: WebExtension.UpdateTabDetails,
            ): GeckoResult<AllowOrDeny> = GeckoResult.fromValue(AllowOrDeny.ALLOW)
        }
}
