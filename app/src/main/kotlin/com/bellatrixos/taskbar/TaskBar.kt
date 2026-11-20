package com.bellatrixos.taskbar

import android.content.Context
import android.text.format.DateFormat
import android.view.View
import android.view.animation.Animation
import android.widget.LinearLayout
import android.widget.TextClock
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.taskbar.utils.ObservableList
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.TaskbarBtn

/** Utilitas tampilan taskbar dan sinkronisasi daftar tombolnya. */
object TaskBar {
    @JvmField var f4802a: Animation? = null

    @JvmStatic
    fun a(
        context: Context,
        i2: Int,
        linearLayout: LinearLayout,
        taskbarBtn: TaskbarBtn?,
        mainActivityIn: MainActivityIn
    ) {
        if (taskbarBtn == null) return
        if (i2 == 0) {
            if (Global.o) {
                taskbarBtn.visibility = View.VISIBLE
                mainActivityIn.p0(taskbarBtn)
            } else {
                taskbarBtn.visibility = View.INVISIBLE
                taskbarBtn.setOnBtnClickListener(null)
            }
            for (index in linearLayout.childCount - 1 downTo 0) {
                if (linearLayout.getChildAt(index) is TaskbarBtn) {
                    linearLayout.removeViewAt(index)
                }
            }
            return
        }
        taskbarBtn.visibility = View.GONE
        taskbarBtn.setOnBtnClickListener(null)
        if (Global.o) {
            val button = TaskbarBtn(context)
            button.setIcon(R.drawable.img_app_widget)
            mainActivityIn.p0(button)
            linearLayout.addView(button, 0)
        }
    }

    @JvmStatic
    fun b(
        taskBarAdapter: TaskBarAdapter,
        linearLayout: LinearLayout,
        observableList: ObservableList<TaskBarItem>,
        recyclerView: RecyclerView
    ) {
        observableList.u = object : ObservableList.OnListChangedListener<TaskBarItem> {
            override fun a(obj: Any?) {
                if (Global.f4843n == 0) {
                    TaskBarAdapter.B(linearLayout, recyclerView.canScrollHorizontally(1), 1)
                }
            }

            override fun b() {
                if (Global.f4843n == 0) {
                    TaskBarAdapter.B(linearLayout, recyclerView.canScrollHorizontally(1), 0)
                }
            }
        }
    }

    @JvmStatic
    fun c(textClock: TextClock, str: String?, str2: String?) {
        if (str2 == null && str == null) return
        val date = str ?: "dd-MM-yyyy"
        val time = str2 ?: if (DateFormat.is24HourFormat(textClock.context)) "HH:mm" else "hh:mm a"
        if (str2 == null) {
            val format = "$time\n$date"
            textClock.setFormat12Hour(format)
            textClock.setFormat24Hour(format)
        } else {
            val format = "$time\n$date"
            if (time.contains("a")) {
                textClock.setFormat12Hour(format)
                textClock.setFormat24Hour(null)
            } else {
                textClock.setFormat12Hour(null)
                textClock.setFormat24Hour(format)
            }
        }
        Global.y = str2
        Global.z = str
    }
}
