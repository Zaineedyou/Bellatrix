package com.bellatrixos.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.PointerIcon
import android.widget.RelativeLayout

/** RelativeLayout yang menyembunyikan pointer icon sistem. */
class CursorRelativeLayout(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    override fun onResolvePointerIcon(event: MotionEvent, pointerIndex: Int): PointerIcon? = null
}
