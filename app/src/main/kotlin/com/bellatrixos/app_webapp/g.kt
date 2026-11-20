package com.bellatrixos.app_webapp

import com.bellatrixos.app_browser.utils.Scaling

/** Callback perubahan skala dari menu zoom. */
class g(@JvmField val u: Any) : androidx.core.util.Supplier<Any?>, Scaling.OnScaleChangedListener {
    override fun get(): Any? = null

    override fun k(i2: Int) {
        (u as? UISettingsMenu)?.f4730a = i2
    }
}
