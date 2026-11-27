package com.bellatrixos.app_browser

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.bellatrixos.R
import com.bellatrixos.widgets.UINotification
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import org.mozilla.geckoview.StorageController

/** The Site settings dialog: remembered permissions, the cache, and the cookies that keep a person signed in. */
object SiteSettings {
    /**
     * [app] is the web app whose cache setting is offered (null in the Browser); [onCacheChanged] gets the new value
     * (true: the cache is used, false: every load and reload goes to the network).
     */
    fun show(
        context: Context,
        app: InstalledApp?,
        onCacheChanged: (Boolean) -> Unit,
    ) {
        val items =
            mutableListOf(
                context.getString(R.string.site_menu_permissions),
                context.getString(R.string.site_menu_clear_cache),
                context.getString(R.string.site_menu_clear_cookies),
            )
        if (app != null) {
            items.add(context.getString(if (app.h()) R.string.site_menu_cache_on else R.string.site_menu_cache_off))
        }
        AlertDialog
            .Builder(context)
            .setTitle(context.getString(R.string.site_settings))
            .setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    0 -> permissions(context)
                    1 -> clearCache(context)
                    2 -> confirmClearCookies(context)
                    else -> app?.let { toggleCache(context, it, onCacheChanged) }
                }
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun permissions(context: Context) {
        val entries = SitePermissions.all(context)
        if (entries.isEmpty()) {
            UINotification.a(context, context.getString(R.string.site_permissions_none), null)
            return
        }
        val labels = entries.map { "${it.origin} - ${SitePermissions.label(context, it.kind)}" }.toTypedArray()
        val allowed = BooleanArray(entries.size) { entries[it].allowed }
        AlertDialog
            .Builder(context)
            .setTitle(context.getString(R.string.site_permissions_title))
            .setMultiChoiceItems(labels, allowed) { _, which, on -> allowed[which] = on }
            .setPositiveButton(context.getString(R.string.site_save)) { _, _ ->
                entries.forEachIndexed { i, e -> SitePermissions.set(context, e.origin, e.kind, allowed[i]) }
            }.setNeutralButton(context.getString(R.string.site_forget_all)) { _, _ -> SitePermissions.clear(context) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun clearCache(context: Context) {
        val flags = StorageController.ClearFlags.NETWORK_CACHE or StorageController.ClearFlags.IMAGE_CACHE
        GeckoEngine.runtime(context).storageController.clearData(flags)
        UINotification.a(context, context.getString(R.string.site_cache_cleared), null)
    }

    private fun confirmClearCookies(context: Context) {
        AlertDialog
            .Builder(context)
            .setTitle(context.getString(R.string.site_cookies_confirm_title))
            .setMessage(context.getString(R.string.site_cookies_confirm_message))
            .setPositiveButton(context.getString(R.string.site_clear)) { _, _ ->
                val flags = StorageController.ClearFlags.COOKIES or StorageController.ClearFlags.DOM_STORAGES
                GeckoEngine.runtime(context).storageController.clearData(flags)
                UINotification.a(context, context.getString(R.string.site_cookies_cleared), null)
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun toggleCache(
        context: Context,
        app: InstalledApp,
        onCacheChanged: (Boolean) -> Unit,
    ) {
        val enabled = !app.h()
        app.o(enabled)
        InstalledAppsDB.e(app.c(), app)
        onCacheChanged(enabled)
        UINotification.a(context, context.getString(if (enabled) R.string.site_cache_turned_on else R.string.site_cache_turned_off), null)
    }
}
