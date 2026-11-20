package com.bellatrixos.windows.switcher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.windows.WindowSize
import kotlin.math.min

/** Popup penukar jendela aplikasi yang sedang berjalan. */
@SuppressLint("InflateParams")
class UIInstanceSwitcher(context: Context) : PopupWindow(context) {

    @JvmField val f4960a: View
    @JvmField val b: InstanceAdapter

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.taskbar_task_switcher_dialog, null)
        f4960a = root
        val recycler = root.findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val adapter = InstanceAdapter(
            context as? MainActivityIn,
            InstanceAdapter.OnInstancesChanged { a() }
        )
        b = adapter
        recycler.adapter = adapter
        recycler.itemAnimator = null
        contentView = root
        width = -2
        height = -2
        isFocusable = true
        isOutsideTouchable = true
        animationStyle = R.style.AnimationDialogSwitcher
        setBackgroundDrawable(ColorDrawable(0))
        c = this
    }

    fun a() {
        val spec = View.MeasureSpec.makeMeasureSpec(0, 0)
        f4960a.measure(spec, spec)
        width = min(f4960a.measuredWidth, WindowSize.f4871a)
        height = f4960a.measuredHeight
    }

    override fun dismiss() {
        super.dismiss()
        if (c === this) c = null
    }

    companion object {
        @JvmField var c: UIInstanceSwitcher? = null
    }
}
