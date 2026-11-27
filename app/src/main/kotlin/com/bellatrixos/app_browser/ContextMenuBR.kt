package com.bellatrixos.app_browser

import android.content.Context
import androidx.core.util.Consumer
import com.bellatrixos.R
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.interfaces.MainActivityIn

/** Menu konteks untuk hasil hit-test WebView pada browser. */
class ContextMenuBR {
    companion object {
        /** Opens the menu at ([x], [y]) on screen for a [kind] of target: 7 is a link, 5 an image. */
        @JvmStatic
        fun a(
            context: Context,
            kind: Int,
            x: Int,
            y: Int,
            consumer: Consumer<Any?>
        ) {
            val activity = context as? MainActivityIn ?: return
            val anchor = activity.a0() ?: return
            anchor.x = x.toFloat()
            anchor.y = y.toFloat()
            val menu = activity.l0() as? UIContextMenu ?: return
            menu.f4745d.clear()
            when (kind) {
                0 -> return
                7 -> {
                    menu.f4745d.add(
                        UIMenuItem(0, 1, 0, context.getString(R.string.context_menu_open_link_new_tab))
                    )
                    menu.f4745d.add(
                        UIMenuItem(0, 4, 0, context.getString(R.string.context_menu_copy_link_address))
                    )
                }
                5 -> {
                    menu.f4745d.add(
                        UIMenuItem(0, 5, 0, context.getString(R.string.context_menu_open_img_new_tab))
                    )
                    menu.f4745d.add(
                        UIMenuItem(0, 6, 0, context.getString(R.string.context_menu_save_img_as))
                    )
                    menu.f4745d.add(
                        UIMenuItem(0, 2, 0, context.getString(R.string.context_menu_copy_img))
                    )
                    menu.f4745d.add(
                        UIMenuItem(0, 3, 0, context.getString(R.string.context_menu_copy_img_address))
                    )
                }
                8 -> {
                    menu.f4745d.add(UIMenuItem(0, 5, 0, "Open image in new tab"))
                    menu.f4745d.add(UIMenuItem(0, 6, 0, "Save image as..."))
                    menu.f4745d.add(UIMenuItem(0, 2, 0, "Copy image"))
                    menu.f4745d.add(UIMenuItem(0, 3, 0, "Copy image address"))
                }
            }
            menu.c = com.bellatrixos.app_browser.a(consumer)
            menu.a(0)
            menu.f(anchor)
        }
    }
}
