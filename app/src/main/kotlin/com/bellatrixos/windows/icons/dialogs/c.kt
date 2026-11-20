package com.bellatrixos.windows.icons.dialogs

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.SimpleAdapter
import com.bellatrixos.R

/** ViewBinder yang mengikat Drawable ke ImageView ikon. */
class c(@JvmField val f4951a: Int) : SimpleAdapter.ViewBinder {
    override fun setViewValue(view: View, value: Any?, textRepresentation: String?): Boolean {
        if (view.id != R.id.icon || value !is Drawable) return false
        (view as ImageView).setImageDrawable(value)
        return true
    }
}
