package com.bellatrixos.app_explorer.drives

import android.content.Context
import android.view.View
import androidx.core.util.Consumer
import androidx.recyclerview.widget.GridLayoutManager
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.FilesRecyclerView
import com.bellatrixos.widgets.GridAutofillLayoutManager

/** Pasang adapter dan perilaku pilih item pada daftar drive. */
object Drives {
    @JvmStatic
    fun a(
        context: Context,
        view: View,
        driveAdapter: DriveAdapter,
        filesRecyclerView: FilesRecyclerView,
        gridAutofillLayoutManager: GridAutofillLayoutManager,
        list: List<DriveItem>,
        gVar: Consumer<Any>,
        gVar2: Consumer<Any>,
        gVar3: Consumer<Any>
    ) {
        gridAutofillLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (driveAdapter.getItemViewType(position) == 2) gridAutofillLayoutManager.spanCount else 1
        }
        filesRecyclerView.layoutManager = gridAutofillLayoutManager
        filesRecyclerView.adapter = driveAdapter

        filesRecyclerView.setOnEmptyPressListener(c(view))
        driveAdapter.B = d(list, gVar, context, gVar2, gVar3)
        if (Global.p == 1) driveAdapter.C = true
    }
}
