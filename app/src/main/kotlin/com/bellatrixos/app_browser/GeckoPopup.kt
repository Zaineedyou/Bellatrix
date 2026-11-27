package com.bellatrixos.app_browser

import android.content.Context
import android.net.Uri
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

/**
 * A window a page opened for itself (window.open). Sign-in with Google opens one and talks back to the page that opened
 * it, so it can't replace the page: it gets a dialog of its own that closes when the page closes it.
 */
internal object GeckoPopup {
    private val signInHosts = setOf("accounts.google.com")

    /** Blank windows (filled in by script afterwards) and sign-in pages open as a popup; other links open in the tab. */
    fun wantsPopup(uri: String): Boolean {
        if (uri.isBlank() || uri == "about:blank") return true
        val host = runCatching { Uri.parse(uri).host }.getOrNull()?.lowercase() ?: return false
        return host in signInHosts
    }

    /** The session Gecko loads the popup into, shown in a dialog over [context]'s window. Main thread only. */
    fun open(context: Context): GeckoResult<GeckoSession> {
        val session =
            GeckoSession(
                GeckoSessionSettings
                    .Builder()
                    .usePrivateMode(false)
                    .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
                    .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)
                    .build(),
            )
        val geckoView = GeckoView(context)
        val metrics = context.resources.displayMetrics
        val maxWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 480f, metrics).toInt()
        val width = minOf((metrics.widthPixels * 0.94f).toInt(), maxWidth)
        val height = (metrics.heightPixels * 0.82f).toInt()
        val box = FrameLayout(context)
        box.addView(geckoView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        box.layoutParams = ViewGroup.LayoutParams(width, height)

        val dialog = AlertDialog.Builder(context).setView(box).create()
        session.contentDelegate =
            object : GeckoSession.ContentDelegate {
                override fun onCloseRequest(session: GeckoSession) {
                    dialog.dismiss()
                }
            }
        GeckoPrompts.install(session, { context }) { prompt -> GeckoResult.fromValue(prompt.dismiss()) }
        dialog.setOnDismissListener {
            geckoView.releaseSession()
            session.close()
        }
        // The session is a new one that Gecko opens itself once this returns.
        geckoView.setSession(session)
        dialog.show()
        dialog.window?.setLayout(width, height)
        return GeckoResult.fromValue(session)
    }
}
