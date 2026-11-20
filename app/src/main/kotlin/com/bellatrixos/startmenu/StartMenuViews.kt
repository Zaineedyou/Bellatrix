package com.bellatrixos.startmenu

import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R

/** Kumpulan view yang membentuk panel Start Menu. */
class StartMenuViews(@JvmField val f4790a: RelativeLayout) {
    @JvmField val b: TextView = f4790a.findViewById(R.id.startMenuUsername)
    @JvmField val c: ImageView = f4790a.findViewById(R.id.startMenuUserImg)
    @JvmField val f4791d: ImageView = f4790a.findViewById(R.id.startMenuBlur)
    @JvmField val f4792e: RecyclerView = f4790a.findViewById(R.id.startMenuApps)
    @JvmField val f4793f: EditText = f4790a.findViewById(R.id.startMenuSearch)
    @JvmField val g: View = f4790a.findViewById(R.id.recommendedUi)
    @JvmField val f4794h: View = f4790a.findViewById(R.id.startMenuUI)
    @JvmField val f4795i: LinearLayout = f4790a.findViewById(R.id.searchAppUI)
    @JvmField val j: LinearLayout = f4790a.findViewById(R.id.startMenuFilterUI)
    @JvmField val f4796k: FrameLayout = f4790a.findViewById(R.id.startMenuSearchBG)
    @JvmField val l: RadioGroup = f4790a.findViewById(R.id.smFilterGroup)
    @JvmField val m: ImageButton = f4790a.findViewById(R.id.smFilterBack)
}
