package com.bellatrixos.windows.navigation

import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

/** Kontrol tampilan system bar (navigation/status). */
object Navigation {
    @JvmStatic
    fun a(i2: Int, view: View, z: Boolean) {
        if (Build.VERSION.SDK_INT < 30) {
            @Suppress("DEPRECATION")
            val flags = when {
                z && i2 == 0 -> 4102
                z && i2 == 1 -> 5122
                !z && i2 == 1 -> 0
                else -> 4100
            }
            @Suppress("DEPRECATION")
            view.systemUiVisibility = flags
            return
        }
        val controller: WindowInsetsController? = view.windowInsetsController
        if (controller != null) {
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
            when {
                z && i2 == 0 -> {
                    controller.hide(WindowInsets.Type.systemBars())
                    return
                }
                z && i2 == 1 -> {
                    controller.hide(WindowInsets.Type.navigationBars())
                    controller.show(WindowInsets.Type.statusBars())
                }
                else -> controller.show(WindowInsets.Type.systemBars())
            }
        }
    }
}
