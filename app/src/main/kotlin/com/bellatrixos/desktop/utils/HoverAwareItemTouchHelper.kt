package com.bellatrixos.desktop.utils

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.app_explorer.navigation.a
import com.bellatrixos.desktop.interfaces.AllDirectionsDragCallback
import java.lang.reflect.Field

/** ItemTouchHelper yang meneruskan drag hover ke callback. */
class HoverAwareItemTouchHelper(allDirectionsDragCallback: AllDirectionsDragCallback) :
    ItemTouchHelper(allDirectionsDragCallback) {

    @JvmField val C: Field?
    @JvmField val D: Field?

    init {
        var fieldC: Field? = null
        var fieldD: Field? = null
        try {
            fieldC = ItemTouchHelper::class.java.getDeclaredField("c")
            try {
                fieldC!!.isAccessible = true
                fieldD = ItemTouchHelper::class.java.getDeclaredField("m")
                fieldD!!.isAccessible = true
            } catch (_: Exception) {
                val field = fieldD
                fieldD = fieldC
                fieldC = fieldD
                fieldD = field
            }
        } catch (_: Exception) {
            fieldC = null
        }
        C = fieldC
        D = fieldD
    }

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        super.attachToRecyclerView(recyclerView)
        if (recyclerView == null) return
        recyclerView.setOnTouchListener(a(this, recyclerView, 2))
    }

    /** Nama attachToRecyclerView, dipakai pemanggil yang belum diubah. */
    fun i(recyclerView: RecyclerView?) {
        attachToRecyclerView(recyclerView)
    }
}
