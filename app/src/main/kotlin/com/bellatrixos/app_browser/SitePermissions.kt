package com.bellatrixos.app_browser

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.bellatrixos.R
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

/** What a site was told about each thing it can ask for, kept per origin so it is asked once. */
object SitePermissions {
    const val LOCATION = "location"
    const val NOTIFICATIONS = "notifications"
    const val STORAGE = "storage"
    const val CAMERA = "camera"
    const val MICROPHONE = "microphone"

    private const val PREFS = "site_permissions"

    class Entry(
        val origin: String,
        val kind: String,
        val allowed: Boolean,
    )

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** scheme://host[:port] of [uri], or null when it has no host. */
    fun origin(uri: String?): String? {
        val parsed = runCatching { Uri.parse(uri ?: return null) }.getOrNull() ?: return null
        val host = parsed.host ?: return null
        val port = if (parsed.port != -1) ":${parsed.port}" else ""
        return "${parsed.scheme ?: "https"}://$host$port"
    }

    /** True if allowed, false if blocked, null if the site was never answered. */
    fun get(
        context: Context,
        origin: String,
        kind: String,
    ): Boolean? {
        val prefs = prefs(context)
        val key = "$origin|$kind"
        return if (prefs.contains(key)) prefs.getBoolean(key, false) else null
    }

    fun set(
        context: Context,
        origin: String,
        kind: String,
        allowed: Boolean,
    ) {
        prefs(context).edit().putBoolean("$origin|$kind", allowed).apply()
    }

    fun all(context: Context): List<Entry> =
        prefs(context)
            .all
            .mapNotNull { (key, value) ->
                val at = key.lastIndexOf('|')
                if (at <= 0 || value !is Boolean) null else Entry(key.substring(0, at), key.substring(at + 1), value)
            }.sortedWith(compareBy({ it.origin }, { it.kind }))

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun label(
        context: Context,
        kind: String,
    ): String =
        when (kind) {
            LOCATION -> context.getString(R.string.site_kind_location)
            NOTIFICATIONS -> context.getString(R.string.site_kind_notifications)
            STORAGE -> context.getString(R.string.site_kind_storage)
            CAMERA -> context.getString(R.string.site_kind_camera)
            MICROPHONE -> context.getString(R.string.site_kind_microphone)
            else -> kind
        }

    /** What the site wants to do, for the question that follows its address. */
    internal fun askText(
        context: Context,
        kinds: List<String>,
    ): String =
        when {
            kinds.size == 2 && kinds.contains(CAMERA) && kinds.contains(MICROPHONE) ->
                context.getString(R.string.site_ask_camera_microphone)
            else ->
                when (kinds.first()) {
                    LOCATION -> context.getString(R.string.site_ask_location)
                    NOTIFICATIONS -> context.getString(R.string.site_ask_notifications)
                    STORAGE -> context.getString(R.string.site_ask_storage)
                    CAMERA -> context.getString(R.string.site_ask_camera)
                    else -> context.getString(R.string.site_ask_microphone)
                }
        }
}

/**
 * Answers what a page asks for (location, notifications, storage, camera, microphone) from what the person said before,
 * or asks once and remembers. Autoplay and the key system protected video needs are allowed; anything else is refused.
 * [requestAndroid] asks Android for the permissions Gecko needs after an allow (location, camera, microphone).
 */
internal class SitePermissionDelegate(
    private val context: () -> Context?,
    private val requestAndroid: (Array<String>, GeckoSession.PermissionDelegate.Callback) -> Unit,
) : GeckoSession.PermissionDelegate {
    override fun onAndroidPermissionsRequest(
        session: GeckoSession,
        permissions: Array<out String>?,
        callback: GeckoSession.PermissionDelegate.Callback,
    ) {
        requestAndroid(permissions?.let { Array(it.size) { i -> it[i] } } ?: emptyArray(), callback)
    }

    override fun onContentPermissionRequest(
        session: GeckoSession,
        perm: GeckoSession.PermissionDelegate.ContentPermission,
    ): GeckoResult<Int>? {
        val allow = GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
        val deny = GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
        val kind =
            when (perm.permission) {
                GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> SitePermissions.LOCATION
                GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> SitePermissions.NOTIFICATIONS
                GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE -> SitePermissions.STORAGE
                GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS,
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE,
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE,
                -> return GeckoResult.fromValue(allow)
                else -> return GeckoResult.fromValue(deny)
            }
        val ctx = context()
        val origin = SitePermissions.origin(perm.uri)
        if (ctx == null || origin == null) return GeckoResult.fromValue(deny)
        SitePermissions.get(ctx, origin, kind)?.let { return GeckoResult.fromValue(if (it) allow else deny) }

        val result = GeckoResult<Int>()
        AlertDialog
            .Builder(ctx)
            .setTitle(origin)
            .setMessage(SitePermissions.askText(ctx, listOf(kind)))
            .setPositiveButton(ctx.getString(R.string.site_allow)) { _, _ ->
                SitePermissions.set(ctx, origin, kind, true)
                result.complete(allow)
            }.setNegativeButton(ctx.getString(R.string.site_block)) { _, _ ->
                SitePermissions.set(ctx, origin, kind, false)
                result.complete(deny)
            }
            // Closing the question refuses this time without remembering it.
            .setOnCancelListener { result.complete(deny) }
            .show()
        return result
    }

    override fun onMediaPermissionRequest(
        session: GeckoSession,
        uri: String,
        video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
        audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
        callback: GeckoSession.PermissionDelegate.MediaCallback,
    ) {
        val kinds =
            buildList {
                if (!video.isNullOrEmpty()) add(SitePermissions.CAMERA)
                if (!audio.isNullOrEmpty()) add(SitePermissions.MICROPHONE)
            }
        val ctx = context()
        val origin = SitePermissions.origin(uri)
        if (ctx == null || origin == null || kinds.isEmpty()) {
            callback.reject()
            return
        }
        val stored = kinds.map { SitePermissions.get(ctx, origin, it) }
        when {
            stored.any { it == false } -> callback.reject()
            stored.all { it == true } -> callback.grant(video?.firstOrNull(), audio?.firstOrNull())
            else ->
                AlertDialog
                    .Builder(ctx)
                    .setTitle(origin)
                    .setMessage(SitePermissions.askText(ctx, kinds))
                    .setPositiveButton(ctx.getString(R.string.site_allow)) { _, _ ->
                        kinds.forEach { SitePermissions.set(ctx, origin, it, true) }
                        callback.grant(video?.firstOrNull(), audio?.firstOrNull())
                    }.setNegativeButton(ctx.getString(R.string.site_block)) { _, _ ->
                        kinds.forEach { SitePermissions.set(ctx, origin, it, false) }
                        callback.reject()
                    }.setOnCancelListener { callback.reject() }
                    .show()
        }
    }
}
