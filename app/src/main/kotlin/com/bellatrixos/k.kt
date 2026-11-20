package com.bellatrixos

import android.os.Bundle
import com.bellatrixos.taskbar.TaskBarAdapter
import com.bellatrixos.taskbar.TaskBarDB
import com.bellatrixos.taskbar.TaskBarItem
import com.bellatrixos.taskbar.TaskBarItemPins
import com.bellatrixos.taskbar.utils.ObservableList

/** Runnable synthetic yang memuat daftar aplikasi taskbar dari Bundle/DB. */
class k(
    @JvmField val v: Any,
    @JvmField val w: Bundle?,
    @JvmField val u: Int
) : Runnable {

    private val mainActivity: MainActivity
        get() = MainActivityBridge.outer(v) ?: error("MainActivity tidak ditemukan")

    override fun run() {
        val bundle = w
        val holder = v
        when (u) {
            0 -> {
                AppExecutors.b().execute(k(holder, bundle, 1))
            }
            else -> {
                val context = (mainActivity.W ?: mainActivity) as android.content.Context
                val observableList: ObservableList<TaskBarItem> = mainActivity.C0
                if (bundle != null) {
                    val parcelableArrayList = bundle.getParcelableArrayList<TaskBarItem>("taskbar_items")
                    if (parcelableArrayList != null) {
                        observableList.clear()
                        observableList.addAll(parcelableArrayList)
                    }
                }
                if (observableList.isEmpty()) {
                    if (TaskBarDB.f4804a == null) {
                        TaskBarDB.f4804a = context.getSharedPreferences("TaskbarPrefs", 0)
                    }
                    val listB = TaskBarDB.b()
                    if (listB.isEmpty()) {
                        observableList.add(TaskBarItem("UIExplorer", true))
                        observableList.add(TaskBarItem("UISettings", true))
                        observableList.add(TaskBarItem("UITerminal", true))
                        TaskBarDB.d(observableList)
                    } else {
                        for (item in listB) {
                            observableList.add(TaskBarItem((item as TaskBarItemPins).a(), true))
                        }
                        TaskBarDB.c(observableList)
                    }
                } else if (TaskBarDB.b.isEmpty()) {
                    TaskBarDB.c(observableList)
                }
                mainActivity.t0 = TaskBarAdapter(context, observableList)
                mainActivity.runOnUiThread(l(holder, 1))
            }
        }
    }
}
