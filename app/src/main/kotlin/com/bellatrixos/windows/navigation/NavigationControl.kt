package com.bellatrixos.windows.navigation

import android.content.Context
import android.view.View
import androidx.core.util.Supplier
import com.google.android.material.bottomsheet.BottomSheetBehavior

/** Status dan callback kontrol navigasi bawah. */
class NavigationControl(
    @JvmField val f4956a: Context,
    @JvmField val b: View,
    @JvmField val c: BottomSheetBehavior<*>,
    @JvmField val f4957d: Supplier<Any?>
) {
    @JvmField var f4958e: Int = 0

    @JvmField val f4959f: Runnable = Runnable { f4958e = 0 }
}
