package com.bellatrixos.app_explorer.navigation

import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import com.bellatrixos.R
import com.bellatrixos.app_notepad.UINotepad
import com.bellatrixos.widgets.BtnMouseBehavior

/** Touch listener synthetic: tampilkan navigasi, blokir drag teks, atau perilaku mouse. */
class c(@JvmField val u: Int, @JvmField val v: Any?) : View.OnTouchListener {
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (u) {
            0 -> {
                if (event.action != MotionEvent.ACTION_DOWN) return false
                @Suppress("UNCHECKED_CAST")
                Navigation.c(*(v as Array<View>))
                return false
            }
            1 -> {
                val notepad = v as? UINotepad ?: return false
                if (event.source and 8194 != 8194 || event.buttonState and 1 == 0 ||
                    event.actionMasked != MotionEvent.ACTION_MOVE
                ) return false
                val editor = notepad.view?.findViewById<EditText>(R.id.noteEditText)
                    ?: view.rootView.findViewById(R.id.noteEditText) ?: return false
                if (!editor.hasSelection()) return false
                val start = editor.selectionStart
                val end = editor.selectionEnd
                if (start >= 0 && end > start) {
                    return editor.text.subSequence(start, end).toString().trim().isEmpty()
                }
                return false
            }
            else -> {
                BtnMouseBehavior.b(view, event)
                return false
            }
        }
    }
}
