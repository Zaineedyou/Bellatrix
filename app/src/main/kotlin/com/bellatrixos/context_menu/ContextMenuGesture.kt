package com.bellatrixos.context_menu

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.util.Supplier
import com.bellatrixos.variables.Global

/** Gestur klik/double-klik untuk menu konteks sesuai preferensi input. */
object ContextMenuGesture {
    @JvmField var f4742a: GestureDetector? = null
    @JvmField var b: Supplier<*>? = null
    @JvmField var c: Supplier<*>? = null

    private val listener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(event: MotionEvent): Boolean {
            if (Global.p == 1) dispatch()
            return true
        }

        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            if (Global.p == 0) dispatch()
            return true
        }

        private fun dispatch() {
            val obj = b?.get() ?: return
            @Suppress("UNCHECKED_CAST")
            val callback = c?.get() as? OnItemClickListener<*> ?: return
            callback.a(obj as Any)
        }
    }

    @JvmStatic
    fun a(context: Context) {
        f4742a = GestureDetector(context.applicationContext, listener)
    }
}
