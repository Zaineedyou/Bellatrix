package com.bellatrixos.taskbar.center_widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.taskbar.center_widgets.utils.SavedWidgetItem
import com.bellatrixos.taskbar.center_widgets.utils.WidgetsAdapter
import com.bellatrixos.variables.Global
import com.bellatrixos.wallpaper.models.UIBounds
import com.bellatrixos.windows.blur.UIBlurImage
import com.bellatrixos.windows.drivers.Display

/** Runnable sintetis untuk inisialisasi daftar widget dan efek blur. */
class d(
    @JvmField val v: Any,
    @JvmField val u: Int,
    @JvmField val w: Any
) : Runnable {
    override fun run() {
        when (u) {
            0 -> initialize(v, w as? Context ?: return)
            1 -> {
                val center = v as? WidgetCenter ?: return
                val bounds = w as? UIBounds ?: return
                center.f4821e.post { d(center, 2, UIBlurImage.a(bounds) as Any).run() }
            }
            else -> {
                val center = v as? WidgetCenter ?: return
                val bitmap = w as? Bitmap
                val image = center.f4821e
                image.alpha = 0.0f
                image.animate().alpha(0.09f).setDuration(800L)
                    .withEndAction(a(center, 1)).start()
                image.setImageBitmap(bitmap)
            }
        }
    }

    private fun initialize(listener: Any, context: Context) {
        val center = (listener as? WidgetCenter) ?: (WidgetManager.f4828a as? WidgetCenter) ?: return
        try {
            center.l = WidgetsAdapter(context)
            val scale = SharedPrefs.a(context)
            AppExecutors.c().post {
                center.f4824i.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                center.f4824i.adapter = center.l
                if (scale < 0.9f) {
                    center.f4824i.pivotX = if (Display.f4898a) center.f4824i.width / scale else 0.0f
                    center.f4824i.pivotY = 0.0f
                    center.f4824i.scaleX = scale
                    center.f4824i.scaleY = scale
                }
                val saved = WidgetsDB.a()
                val empty: TextView = center.f4822f
                if (saved.isEmpty()) {
                    empty.visibility = View.VISIBLE
                } else {
                    val sorted = saved.sortedBy { it.b() }
                    for (item in sorted) {
                        val provider = center.s.installedProviders.firstOrNull {
                            it.provider.className == item.c()
                        } ?: continue
                        val hostView = center.r.createView(
                            context.applicationContext,
                            item.a(),
                            provider
                        ) ?: continue
                        center.l.A.add(WidgetItem(hostView, item.d()))
                        center.l.notifyItemInserted(center.l.A.size - 1)
                    }
                    empty.visibility = View.GONE
                }
                center.l.z = object : WidgetsAdapter.OnItemClickListener {
                    override fun a(view: View, i2: Int) {
                        showMenu(context, center, view, i2)
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }

    private fun showMenu(context: Context, center: WidgetCenter, view: View, position: Int) {
        val menu = UIContextMenu(context)
        menu.f4745d.clear()
        val title = UIMenuItem(0, 12, R.drawable.menu_ic_blank_20_regular, context.getString(R.string.dialog_widget_menu_title))
        title.f4751h = false
        menu.f4745d.add(title)
        menu.f4745d.add(UIMenuItem(0, 9, R.drawable.menu_ic_delete_20_regular, context.getString(R.string.dialog_widget_menu_item_remove)))
        val resize = UIMenuItem(0, 18, R.drawable.menu_ic_add_desktop_20_regular, context.getString(R.string.dialog_widget_menu_item_resize))
        resize.g = true
        menu.f4745d.add(resize)
        menu.c = UIContextMenu.OnMenuItemClickListener { _, id ->
            when (id) {
                9 -> {
                    val adapter = center.l
                    if (position in adapter.A.indices) {
                        adapter.A.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        val saved = WidgetsDB.a()
                        if (position in saved.indices) {
                            saved.removeAt(position)
                            WidgetsDB.b(saved)
                        }
                    }
                    if (adapter.A.isEmpty()) center.f4822f.visibility = View.VISIBLE
                }
                18 -> {
                    val sizes = UIContextMenu(context)
                    sizes.f4745d.clear()
                    sizes.f4745d.add(UIMenuItem(0, 41, R.drawable.ui_widgets_ic_normal_20, context.getString(R.string.dialog_widget_menu_item_resize_1)))
                    sizes.f4745d.add(UIMenuItem(1, 0, 0, ""))
                    sizes.f4745d.add(UIMenuItem(0, 43, R.drawable.ui_widgets_ic_fill_20, context.getString(R.string.dialog_widget_menu_item_resize_2)))
                    sizes.c = UIContextMenu.OnMenuItemClickListener { _, sizeId ->
                        if (sizeId == 43) center.l.C(position)
                    }
                    sizes.a(0)
                    sizes.e(view, view.width, -view.height * 2)
                }
            }
        }
        menu.a(android.R.style.Animation_Dialog)
        if (!menu.b(view)) {
            menu.c()
            val x = if (Display.f4898a) 0 else
                Math.round(view.width * Global.f4838e) - menu.contentView.measuredWidth
            menu.showAsDropDown(view, x, -Math.round(view.height * Global.f4838e) / 2)
        }
    }
}
