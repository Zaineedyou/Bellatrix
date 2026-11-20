package com.bellatrixos.widgets

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.bellatrixos.R

/** Adapter spinner dengan efek hover. */
class UISpinnerAdapter(context: Context, items: Array<String>) :
    ArrayAdapter<String>(context, R.layout.ui_spinner_btn, items) {

    companion object {
        const val u: Int = 0
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        view.setOnHoverListener { _, _ -> false }
        return view
    }
}
