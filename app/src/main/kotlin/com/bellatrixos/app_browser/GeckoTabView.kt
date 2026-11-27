package com.bellatrixos.app_browser

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.FrameLayout
import com.bellatrixos.windows.Windows
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse

/** The one Gecko runtime (the Firefox engine) every Browser tab shares; it can only be created once per process. */
object GeckoEngine {
    private var runtime: GeckoRuntime? = null

    /** Main thread only. */
    fun runtime(context: Context): GeckoRuntime =
        runtime ?: GeckoRuntime.create(context.applicationContext).also { runtime = it }

    /** The pages' text size in percent. Gecko has one for the whole engine, so every tab follows the last one set. */
    fun zoom(
        context: Context,
        percent: Int,
    ) {
        runtime(context).settings.fontSizeFactor = percent.coerceIn(50, 300) / 100f
    }
}

/**
 * A Browser tab's page: Firefox's engine (GeckoView) in a box, with the calls the Browser app made on its WebView
 * (loadUrl, goBack, reload, title, url, find) and the page's events as plain callbacks, so the rest of the Browser
 * doesn't speak Gecko. Main thread only.
 */
class GeckoTabView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : FrameLayout(context, attrs) {
        private val geckoView = GeckoView(context)

        /** The tab's Gecko session; the Browser's prompt, permission and popup handling is set on it. */
        var session: GeckoSession = newSession()
            private set

        /** The page's title as it loads, null until it has one. */
        var title: String? = null
            private set

        /** The URL showing now, which follows a link the page opened. */
        var url: String? = null
            private set

        private var backAvailable = false
        private var forwardAvailable = false
        private var lastQuery: String? = null

        /** How much of the page has loaded, 0 to 100. */
        var onProgress: ((Int) -> Unit)? = null

        /** A page started loading at the URL. */
        var onPageStart: ((String) -> Unit)? = null

        /** The page finished loading, or failed to. */
        var onPageStop: ((Boolean) -> Unit)? = null

        /** The title changed. */
        var onTitle: ((String) -> Unit)? = null

        /** The URL changed, including a link the page itself followed. */
        var onLocation: ((String) -> Unit)? = null

        /** Back or forward became possible or impossible. */
        var onHistory: (() -> Unit)? = null

        /** A press-and-hold or right click on the page, with where it landed on screen. */
        var onContext: ((GeckoSession.ContentDelegate.ContextElement, Int, Int) -> Unit)? = null

        /** The page asked for, or left, full screen. */
        var onFullScreen: ((Boolean) -> Unit)? = null

        /** A download the page started. */
        var onDownload: ((WebResponse) -> Unit)? = null

        /** The page opened a window (target=_blank, window.open); the result is the session that shows it, or null to refuse. */
        var onNewWindow: ((String) -> GeckoResult<GeckoSession>?)? = null

        /** The page asked to close its own window. */
        var onCloseRequest: (() -> Unit)? = null

        /** The page itself could not be loaded; the argument is a [WebRequestError] code. Gecko still draws its error page. */
        var onLoadError: ((Int) -> Unit)? = null

        /** Every load and reload goes to the network instead of the cache (a web app with its cache turned off). */
        var bypassCache = false

        init {
            addView(geckoView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            // Lets the phone's password manager offer the saved account on a sign-in form.
            geckoView.setAutofillEnabled(true)
            attach(session)
        }

        private fun newSession(): GeckoSession =
            GeckoSession(
                GeckoSessionSettings
                    .Builder()
                    // The Browser has always presented itself as a desktop browser.
                    .usePrivateMode(false)
                    .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
                    .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)
                    .build(),
            )

        private fun attach(s: GeckoSession) {
            s.progressDelegate =
                object : GeckoSession.ProgressDelegate {
                    override fun onPageStart(
                        session: GeckoSession,
                        url: String,
                    ) {
                        onPageStart?.invoke(url)
                    }

                    override fun onPageStop(
                        session: GeckoSession,
                        success: Boolean,
                    ) {
                        onPageStop?.invoke(success)
                    }

                    override fun onProgressChange(
                        session: GeckoSession,
                        progress: Int,
                    ) {
                        onProgress?.invoke(progress)
                    }
                }
            s.navigationDelegate =
                object : GeckoSession.NavigationDelegate {
                    override fun onLocationChange(
                        session: GeckoSession,
                        url: String?,
                        perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                        hasUserGesture: Boolean,
                    ) {
                        val next = url ?: return
                        this@GeckoTabView.url = next
                        onLocation?.invoke(next)
                    }

                    override fun onCanGoBack(
                        session: GeckoSession,
                        canGoBack: Boolean,
                    ) {
                        backAvailable = canGoBack
                        onHistory?.invoke()
                    }

                    override fun onCanGoForward(
                        session: GeckoSession,
                        canGoForward: Boolean,
                    ) {
                        forwardAvailable = canGoForward
                        onHistory?.invoke()
                    }

                    override fun onNewSession(
                        session: GeckoSession,
                        uri: String,
                    ): GeckoResult<GeckoSession>? = onNewWindow?.invoke(uri)

                    override fun onLoadError(
                        session: GeckoSession,
                        uri: String?,
                        error: WebRequestError,
                    ): GeckoResult<String>? {
                        onLoadError?.invoke(error.code)
                        // null keeps Gecko's own error page.
                        return null
                    }
                }
            s.contentDelegate =
                object : GeckoSession.ContentDelegate {
                    override fun onTitleChange(
                        session: GeckoSession,
                        title: String?,
                    ) {
                        this@GeckoTabView.title = title
                        if (!title.isNullOrBlank()) onTitle?.invoke(title)
                    }

                    override fun onContextMenu(
                        session: GeckoSession,
                        screenX: Int,
                        screenY: Int,
                        element: GeckoSession.ContentDelegate.ContextElement,
                    ) {
                        onContext?.invoke(element, screenX, screenY)
                    }

                    override fun onExternalResponse(
                        session: GeckoSession,
                        response: WebResponse,
                    ) {
                        onDownload?.invoke(response)
                    }

                    override fun onFullScreen(
                        session: GeckoSession,
                        fullScreen: Boolean,
                    ) {
                        onFullScreen?.invoke(fullScreen)
                    }

                    override fun onCloseRequest(session: GeckoSession) {
                        onCloseRequest?.invoke()
                    }
                }
            s.open(GeckoEngine.runtime(context))
            geckoView.setSession(s)
        }

        private fun cacheFlags(): Int = if (bypassCache) GeckoSession.LOAD_FLAGS_BYPASS_CACHE else GeckoSession.LOAD_FLAGS_NONE

        fun loadUrl(link: String) {
            session.load(GeckoSession.Loader().uri(link).flags(cacheFlags()))
        }

        fun canGoBack(): Boolean = backAvailable

        fun canGoForward(): Boolean = forwardAvailable

        fun goBack() = session.goBack()

        fun goForward() = session.goForward()

        fun reload() = session.reload(cacheFlags())

        fun stopLoading() = session.stop()

        /** Finds [query] in the page, highlighting every match. An empty one clears the search. */
        fun find(query: String) {
            if (query.isEmpty()) {
                clearMatches()
                return
            }
            lastQuery = query
            session.finder.displayFlags = GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL
            session.finder.find(query, 0)
        }

        /** The next match of the last search, or the one before it. */
        fun findNext(forward: Boolean) {
            val query = lastQuery ?: return
            session.finder.find(query, if (forward) 0 else GeckoSession.FINDER_FIND_BACKWARDS)
        }

        fun clearMatches() {
            lastQuery = null
            session.finder.clear()
        }

        /** An inactive page stops running (timers, media), as a paused WebView did. */
        fun setActive(active: Boolean) = session.setActive(active)

        /** Leaves the full screen the page asked for. */
        fun exitFullScreen() = session.exitFullScreen()

        /** Ends the tab's session; the view can't show a page again. */
        fun destroy() {
            geckoView.releaseSession()
            session.close()
        }

        /** Ctrl+D, Ctrl+P, Ctrl+N and F11 belong to the window, as they did over the WebView. */
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            val isCtrlShortcut =
                event.action == KeyEvent.ACTION_DOWN &&
                    event.isCtrlPressed &&
                    (event.keyCode == 34 || event.keyCode == 44 || event.keyCode == 48)
            val isF11 = event.keyCode == 141
            if (isCtrlShortcut || isF11) {
                val fragment = Windows.f4882e
                if (fragment != null && fragment.f0()) {
                    fragment.h0(event.keyCode, event)
                    return true
                }
            }
            return super.dispatchKeyEvent(event)
        }
    }
