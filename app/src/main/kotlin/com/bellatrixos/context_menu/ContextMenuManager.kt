package com.bellatrixos.context_menu

import android.view.MotionEvent
import android.view.View
import androidx.core.util.Supplier
import com.bellatrixos.variables.Global

/** Memasang klik, long-click, dan klik kanan untuk menu konteks. */
object ContextMenuManager {
    fun interface ContextMenuCallback<T> {
        fun d(iArr: IntArray, obj: Any)
    }

    @JvmStatic
    fun a(
        i2: Int,
        view: View,
        supplier: Supplier<*>,
        callback: ContextMenuCallback<*>?,
        supplier2: Supplier<*>,
        /** Runs after a long press has opened the menu, not for a right click: home's icons start their drag here. */
        afterLongClick: ((View) -> Unit)? = null
    ) {
        if (i2 == 0) view.setOnClickListener(d(supplier, 1, supplier2))
        view.setOnTouchListener { _, event ->
            val detector = ContextMenuGesture.f4742a
            if (detector != null && i2 != 0) {
                ContextMenuGesture.c = supplier2
                ContextMenuGesture.b = supplier
                detector.onTouchEvent(event)
            }
            if (event.action == MotionEvent.ACTION_DOWN) {
                Global.A[0] = event.rawX.toInt()
                Global.A[1] = event.rawY.toInt()
            }
            false
        }
        view.setOnLongClickListener {
            if (callback == null) return@setOnLongClickListener false
            callback.d(Global.A, supplier.get() as Any)
            if (Global.p == 1) {
                view.requestFocus()
            } else {
                view.postDelayed({
                    view.isFocusableInTouchMode = true
                    view.requestFocus()
                    view.isFocusableInTouchMode = false
                }, 50L)
            }
            afterLongClick?.invoke(view)
            true
        }
        view.setOnGenericMotionListener { _, event ->
            if (event.buttonState != 2) return@setOnGenericMotionListener false
            Global.A[0] = event.rawX.toInt()
            Global.A[1] = event.rawY.toInt()
            callback?.d(Global.A, supplier.get() as Any)
            if (Global.p == 1) {
                view.requestFocus()
            } else {
                view.postDelayed({
                    view.isFocusableInTouchMode = true
                    view.requestFocus()
                    view.isFocusableInTouchMode = false
                }, 50L)
            }
            true
        }
    }
}
