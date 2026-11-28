package com.bellatrixos.app_browser

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.bellatrixos.R
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension

/** The windows of the add-ons: Firefox's Extensions panel, the add-ons manager, an extension's popup and the permission prompt. */
internal object ExtensionUi {
    private fun dp(
        context: Context,
        value: Int,
    ): Int = (value * context.resources.displayMetrics.density).toInt()

    /** The panel under the toolbar's Extensions button: each enabled extension, which opens its popup when pressed. */
    fun showPanel(
        context: Context,
        anchor: View,
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.app_browser_extensions, null as ViewGroup?)
        val list = view.findViewById<LinearLayout>(R.id.extensions_list)
        val empty = view.findViewById<TextView>(R.id.extensions_empty)
        val width = dp(context, 320)
        val popup = PopupWindow(view, width, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = dp(context, 12).toFloat()
        popup.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.ff_bg_menu))
        popup.windowLayoutType = 1002

        val open: (WebExtension) -> Unit = { extension ->
            val action = Extensions.action(extension, Extensions.currentSession())
            if (action != null && action.enabled != false) {
                popup.dismiss()
                action.click()
            } else if (extension.metaData.optionsPageUrl != null) {
                popup.dismiss()
                Extensions.openOptions(extension)
            }
        }
        val refill: () -> Unit = { fill(context, list, empty, false, open, popup::dismiss) }
        Extensions.addListener(refill)
        popup.setOnDismissListener { Extensions.removeListener(refill) }
        view.findViewById<View>(R.id.extensions_manage).setOnClickListener {
            popup.dismiss()
            showManager(context)
        }
        view.findViewById<View>(R.id.extensions_find).setOnClickListener {
            popup.dismiss()
            Extensions.findMore()
        }
        refill()
        Extensions.refresh()
        popup.showAsDropDown(anchor, anchor.width - width, dp(context, 4))
    }

    /** The add-ons manager: every extension, with a switch to turn it on or off, its options and a way to remove it. */
    fun showManager(context: Context) {
        val view = LayoutInflater.from(context).inflate(R.layout.app_browser_addons, null as ViewGroup?)
        val list = view.findViewById<LinearLayout>(R.id.addons_list)
        val empty = view.findViewById<TextView>(R.id.addons_empty)
        val dialog =
            AlertDialog
                .Builder(context)
                .setTitle(R.string.ext_manager_title)
                .setView(view)
                .setPositiveButton(R.string.context_menu_close, null)
                .setNeutralButton(R.string.ext_find_more) { _, _ -> Extensions.findMore() }
                .create()
        val refill: () -> Unit = { fill(context, list, empty, true, null, dialog::dismiss) }
        Extensions.addListener(refill)
        dialog.setOnDismissListener { Extensions.removeListener(refill) }
        refill()
        Extensions.refresh()
        dialog.show()
    }

    private fun fill(
        context: Context,
        list: LinearLayout,
        empty: TextView,
        manage: Boolean,
        onClick: ((WebExtension) -> Unit)?,
        dismiss: () -> Unit,
    ) {
        list.removeAllViews()
        val extensions = Extensions.all().filter { manage || it.metaData.enabled }
        empty.visibility = if (extensions.isEmpty()) View.VISIBLE else View.GONE
        val inflater = LayoutInflater.from(context)
        for (extension in extensions) {
            val row = inflater.inflate(R.layout.app_browser_extension_item, list, false)
            bind(context, row, extension, manage, dismiss)
            if (onClick != null) row.setOnClickListener { onClick(extension) }
            list.addView(row)
        }
    }

    private fun bind(
        context: Context,
        row: View,
        extension: WebExtension,
        manage: Boolean,
        dismiss: () -> Unit,
    ) {
        val meta = extension.metaData
        val action = Extensions.action(extension, Extensions.currentSession())
        row.findViewById<TextView>(R.id.ext_name).text = meta.name ?: extension.id
        val version = context.getString(R.string.ext_version, meta.version)
        val description = meta.description
        row.findViewById<TextView>(R.id.ext_sub).text =
            when {
                !meta.enabled -> context.getString(R.string.ext_disabled)
                manage && !description.isNullOrBlank() -> version + "\n" + description
                else -> version
            }
        row.alpha = if (meta.enabled) 1f else 0.6f

        val icon = row.findViewById<ImageView>(R.id.ext_icon)
        (action?.icon ?: meta.icon).getBitmap(dp(context, 32)).accept(
            { bitmap -> if (bitmap != null) icon.setImageBitmap(bitmap) },
            { },
        )

        val badge = row.findViewById<TextView>(R.id.ext_badge)
        if (action != null && !manage && !action.badgeText.isNullOrEmpty()) {
            badge.text = action.badgeText
            action.badgeBackgroundColor?.let { (badge.background.mutate() as? GradientDrawable)?.setColor(it) }
            action.badgeTextColor?.let { badge.setTextColor(it) }
            badge.visibility = View.VISIBLE
        }

        val toggle = row.findViewById<SwitchCompat>(R.id.ext_switch)
        if (manage) {
            toggle.visibility = View.VISIBLE
            toggle.isChecked = meta.enabled
            toggle.setOnCheckedChangeListener { _, on -> Extensions.setEnabled(extension, on) }
        }

        row.findViewById<ImageButton>(R.id.ext_more).setOnClickListener { button ->
            val menu = PopupMenu(context, button)
            if (meta.optionsPageUrl != null) menu.menu.add(0, 1, 0, R.string.ext_options)
            menu.menu.add(0, 2, 1, if (meta.enabled) R.string.ext_disable else R.string.ext_enable)
            menu.menu.add(0, 3, 2, R.string.ext_remove)
            menu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        dismiss()
                        Extensions.openOptions(extension)
                    }
                    2 -> Extensions.setEnabled(extension, !meta.enabled)
                    3 -> confirmRemove(context, extension)
                }
                true
            }
            menu.show()
        }
    }

    private fun confirmRemove(
        context: Context,
        extension: WebExtension,
    ) {
        AlertDialog
            .Builder(context)
            .setTitle(context.getString(R.string.ext_remove_title, extension.metaData.name ?: extension.id))
            .setMessage(R.string.ext_remove_message)
            .setPositiveButton(R.string.ext_remove) { _, _ -> Extensions.remove(extension) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** The window an extension's button opens (its popup page), under [anchor]. The returned session shows in it. */
    fun openPopup(
        context: Context,
        anchor: View,
    ): GeckoResult<GeckoSession> {
        val session = GeckoSession(GeckoSessionSettings.Builder().usePrivateMode(false).build())
        session.open(GeckoEngine.runtime(context))
        val geckoView = GeckoView(context)
        val metrics = context.resources.displayMetrics
        val width = minOf(dp(context, 380), metrics.widthPixels - dp(context, 24))
        val height = minOf(dp(context, 560), (metrics.heightPixels * 0.8f).toInt())
        val popup = PopupWindow(geckoView, width, height, true)
        popup.elevation = dp(context, 12).toFloat()
        popup.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.ff_bg_menu))
        popup.windowLayoutType = 1002
        session.contentDelegate =
            object : GeckoSession.ContentDelegate {
                // window.close() in the popup.
                override fun onCloseRequest(session: GeckoSession) {
                    popup.dismiss()
                }
            }
        GeckoPrompts.install(session, { context }) { prompt -> GeckoResult.fromValue(prompt.dismiss()) }
        popup.setOnDismissListener {
            geckoView.releaseSession()
            session.close()
        }
        geckoView.setSession(session)
        popup.showAsDropDown(anchor, anchor.width - width, dp(context, 4))
        return GeckoResult.fromValue(session)
    }

    /** What an extension asks for, in a prompt. [done] hears the answer once. */
    fun confirm(
        context: Context,
        extension: WebExtension,
        title: Int,
        positive: Int,
        permissions: Array<String>,
        origins: Array<String>,
        dataCollection: Array<String>,
        done: (Boolean) -> Unit,
    ) {
        val lines = ArrayList<String>()
        val hosts = ExtensionPermissions.hostAccess(origins)
        if (hosts.allSites) {
            lines.add(context.getString(R.string.ext_prompt_all_sites))
        } else if (hosts.sites.isNotEmpty()) {
            lines.add(context.getString(R.string.ext_prompt_some_sites, hosts.sites.joinToString(", ")))
        }
        lines.addAll(ExtensionPermissions.names(permissions, dataCollection))
        val message =
            if (lines.isEmpty()) {
                context.getString(R.string.ext_prompt_none)
            } else {
                context.getString(R.string.ext_prompt_intro) + "\n" + lines.joinToString("\n") { "• $it" }
            }
        var answered = false

        fun answer(ok: Boolean) {
            if (answered) return
            answered = true
            done(ok)
        }
        AlertDialog
            .Builder(context)
            .setTitle(context.getString(title, extension.metaData.name ?: extension.id))
            .setMessage(message)
            .setPositiveButton(positive) { _, _ -> answer(true) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> answer(false) }
            .setOnCancelListener { answer(false) }
            .show()
    }
}
