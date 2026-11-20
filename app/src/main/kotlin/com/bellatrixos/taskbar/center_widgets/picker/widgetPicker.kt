package com.bellatrixos.taskbar.center_widgets.picker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Menyusun daftar provider widget yang terpasang, dikelompokkan per aplikasi. */
object widgetPicker {
    @JvmStatic
    fun a(context: Context, recyclerView: RecyclerView): WidgetPickerAdapter {
        val providers = AppWidgetManager.getInstance(context).installedProviders
        val items = ArrayList<Any>()
        val groups = LinkedHashMap<String, MutableList<AppWidgetProviderInfo>>()
        val packageManager = context.packageManager
        for (info in providers) {
            val label = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(info.provider.packageName, 0)
                ).toString()
            } catch (ignored: Exception) {
                info.provider.packageName
            }
            val group = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                groups.computeIfAbsent(label) { ArrayList() }
            } else {
                groups.getOrPut(label) { ArrayList() }
            }
            group.add(info)
        }
        for (entry in groups) {
            items.add(entry.key)
            items.addAll(entry.value)
        }
        val adapter = WidgetPickerAdapter(context, items, packageManager)
        val layoutManager = GridLayoutManager(recyclerView.context, 4)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (adapter.getItemViewType(position) == 0) 4 else 1
        }
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        return adapter
    }
}
