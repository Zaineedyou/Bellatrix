package com.bellatrixos.app_explorer.navigation

import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.desktop.interfaces.AllDirectionsDragCallback
import com.bellatrixos.desktop.utils.HoverAwareItemTouchHelper

/** Touch listener synthetic: lepas fokus teks atau pindahkan item saat hover. */
class a(@JvmField val v: Any?, @JvmField val w: View?, @JvmField val u: Int) : View.OnTouchListener {
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (u) {
            0, 1 -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val textView = v as? TextView
                    if (textView?.hasFocus() == true) textView.clearFocus()
                    val editText = w as? EditText
                    if (editText?.hasFocus() == true) {
                        editText.clearFocus()
                        editText.setText("")
                    }
                }
            }
            else -> {
                val helper = v as? HoverAwareItemTouchHelper ?: return false
                if (event.action != MotionEvent.ACTION_MOVE) return false
                val holder = try {
                    helper.C?.get(helper) as? RecyclerView.ViewHolder
                } catch (_: Exception) {
                    null
                } ?: return false
                val recyclerView = w as? RecyclerView ?: return false
                val child = recyclerView.findChildViewUnder(event.x, event.y) ?: return false
                val target = recyclerView.getChildViewHolder(child)
                val callback = try {
                    helper.D?.get(helper) as? AllDirectionsDragCallback
                } catch (_: Exception) {
                    null
                }
                if (callback != null) callback.k(holder, target)
            }
        }
        return false
    }
}
