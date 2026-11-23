package com.bellatrixos.ui.nativeui

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

/**
 * The data of a Bellatrix native list/grid adapter, as seen from Lua. It reads
 * the adapter that is already installed and restyles its existing rows; it does
 * not replace the list with another screen.
 */
class LuaAdapter(
    private val host: View,
) {
    private val adapterView: AdapterView<*>? = host as? AdapterView<*>
    private val recycler: RecyclerView? = host as? RecyclerView

    init {
        require(adapterView != null || recycler != null) { "view is not a native adapter view" }
    }

    fun count(): Int =
        adapterView?.adapter?.count
            ?: recycler?.adapter?.itemCount
            ?: 0

    fun data(): LuaValue =
        LuaValue.listOf((0 until count()).map { LuaValue.valueOf(textAt(it)) }.toTypedArray())

    fun textAt(index: Int): String {
        val row = viewAt(index) ?: return ""
        return row.findTextView()?.text?.toString() ?: ""
    }

    fun viewAt(index: Int): LuaView? {
        val child =
            when {
                recycler != null -> recycler.layoutManager?.getChildAt(index)
                adapterView != null -> adapterView.getChildAt(index - adapterView.firstVisiblePosition)
                else -> null
            }
        return child?.let { LuaView(it, NativeUiLook.surfaceOfView(it)) }
    }

    /** Only string-backed native adapters accept replacement data; others stay owned by Bellatrix. */
    fun setData(items: LuaTable) {
        val values = mutableListOf<String>()
        var i = 1
        while (true) {
            val value = items.get(i)
            if (value.isnil()) break
            values += value.tojstring()
            i++
        }
        val adapter =
            adapterView?.adapter
                ?: throw IllegalArgumentException("this native adapter keeps its own data; restyle its rows instead")
        @Suppress("UNCHECKED_CAST")
        val strings = adapter as? ArrayAdapter<String>
            ?: throw IllegalArgumentException("this native adapter keeps its own data; restyle its rows instead")
        NativeUiLook.rememberAdapter(strings)
        strings.clear()
        strings.addAll(values)
        strings.notifyDataSetChanged()
    }

    fun notifyChanged() {
        when (val adapter = adapterView?.adapter ?: recycler?.adapter) {
            is android.widget.BaseAdapter -> adapter.notifyDataSetChanged()
            is RecyclerView.Adapter<*> -> adapter.notifyDataSetChanged()
        }
    }

    private fun LuaView.findTextView(): TextView? {
        val raw = raw()
        return (raw as? TextView) ?: findText(raw)
    }

    private fun findText(root: View): TextView? {
        if (root is TextView) return root
        if (root !is android.view.ViewGroup) return null
        for (i in 0 until root.childCount) {
            root.getChildAt(i)?.let { child ->
                findText(child)?.let { return it }
            }
        }
        return null
    }
}
