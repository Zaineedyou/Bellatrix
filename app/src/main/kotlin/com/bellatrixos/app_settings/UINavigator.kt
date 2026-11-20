package com.bellatrixos.app_settings

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.bellatrixos.app_settings.fragments.UIAbout
import com.bellatrixos.app_settings.fragments.UIAccounts
import com.bellatrixos.app_settings.fragments.UIApps
import com.bellatrixos.app_settings.fragments.UIAppsInstalled
import com.bellatrixos.app_settings.fragments.UIBackground
import com.bellatrixos.app_settings.fragments.UIColors
import com.bellatrixos.app_settings.fragments.UIDevices
import com.bellatrixos.app_settings.fragments.UIDisplay
import com.bellatrixos.app_settings.fragments.UIDisplayScale
import com.bellatrixos.app_settings.fragments.UIFManager
import com.bellatrixos.app_settings.fragments.UIHome
import com.bellatrixos.app_settings.fragments.UILangRegion
import com.bellatrixos.app_settings.fragments.UIMouse
import com.bellatrixos.app_settings.fragments.UIMultitasking
import com.bellatrixos.app_settings.fragments.UIPCUpdate
import com.bellatrixos.app_settings.fragments.UIPersonalize
import com.bellatrixos.app_settings.fragments.UIPrivacy
import com.bellatrixos.app_settings.fragments.UISound
import com.bellatrixos.app_settings.fragments.UIStartMenu
import com.bellatrixos.app_settings.fragments.UIStorage
import com.bellatrixos.app_settings.fragments.UISystem
import com.bellatrixos.app_settings.fragments.UITaskbar
import com.bellatrixos.app_settings.fragments.UITimeLang
import com.bellatrixos.app_settings.fragments.UITyping

/** Navigator untuk halaman-halaman pengaturan. */
class UINavigator {

    interface BreadcrumbCallback

    fun interface ClickAction {
        fun d(i2: Int)
    }

    fun interface OnFragmentDisplayedListener {
        fun a()
    }

    companion object {
        @JvmField var f4706a: ColorStateList? = null
        @JvmField var b: String? = null
        @JvmField var c: String? = null
        @JvmField var f4707d: String? = null
        @JvmField var f4708e: String? = null
        @JvmField var f4709f: String? = null
        @JvmField var g: String? = null
        @JvmField var f4710h: String? = null
        @JvmField var f4711i: String? = null
        @JvmField var j: String? = null
        @JvmField var f4712k: String? = null
        @JvmField var l: String? = null
        @JvmField var m: String? = null
        @JvmField var f4713n: String? = null
        @JvmField var o: String? = null
        @JvmField var p: String? = null
        @JvmField var q: String? = null
        @JvmField var r: String? = null
        @JvmField var s: String? = null
        @JvmField var t: String? = null
        @JvmField var u: String? = null
        @JvmField var v: String? = null
        @JvmField var w: String? = null
        @JvmField var x: String? = null
        @JvmField var y: String? = null

        @JvmStatic
        fun a(i2: Int, relativeLayout: RelativeLayout?): Fragment? {
            if (i2 == 21) return UIMouse()
            if (i2 == 111) return UIDisplayScale()
            if (i2 == 41) return UIAppsInstalled()
            if (i2 == 42) return UIFManager()
            if (i2 == 61) return UILangRegion()
            if (i2 == 62) return UITyping()
            return when (i2) {
                1 -> UISystem()
                2 -> UIDevices()
                3 -> UIPersonalize()
                4 -> UIApps()
                5 -> UIAccounts()
                6 -> UITimeLang()
                7 -> UIPrivacy()
                8 -> UIPCUpdate()
                11 -> UIDisplay()
                12 -> UIStorage()
                13 -> UIMultitasking()
                14 -> UIHome()
                15 -> UIAbout()
                16 -> UISound()
                31 -> UIBackground()
                32 -> UIColors()
                33 -> UITaskbar()
                34 -> {
                    val width = relativeLayout?.width ?: 0
                    val fragment = UIStartMenu()
                    val bundle = Bundle()
                    bundle.putInt("param", width)
                    fragment.arguments = bundle
                    fragment
                }
                else -> null
            }
        }

        @JvmStatic
        fun b(i2: Int): String? {
            val sb: StringBuilder
            val str: String?
            if (i2 == 21) {
                sb = StringBuilder()
                sb.append(c)
                sb.append("  ›  ")
                str = m
            } else if (i2 == 111) {
                sb = StringBuilder()
                sb.append(b)
                sb.append("  ›  ")
                sb.append(j)
                sb.append("  ›  ")
                str = f4712k
            } else if (i2 == 41) {
                sb = StringBuilder()
                sb.append(f4708e)
                sb.append("  ›  ")
                str = v
            } else if (i2 == 42) {
                sb = StringBuilder()
                sb.append(f4708e)
                sb.append("  ›  ")
                str = w
            } else if (i2 == 61) {
                sb = StringBuilder()
                sb.append(g)
                sb.append("  ›  ")
                str = x
            } else if (i2 != 62) {
                when (i2) {
                    2 -> return c
                    3 -> return f4707d
                    4 -> return f4708e
                    5 -> return f4709f
                    6 -> return g
                    7 -> return f4710h
                    8 -> return f4711i
                    11 -> {
                        sb = StringBuilder()
                        sb.append(b)
                        sb.append("  ›  ")
                        str = j
                    }
                    12 -> {
                        sb = StringBuilder()
                        sb.append(b)
                        sb.append("  ›  ")
                        str = f4713n
                    }
                    13 -> {
                        sb = StringBuilder()
                        sb.append(b)
                        sb.append("  ›  ")
                        str = o
                    }
                    14 -> {
                        sb = StringBuilder()
                        sb.append(b)
                        sb.append("  ›  ")
                        str = p
                    }
                    15 -> {
                        sb = StringBuilder()
                        sb.append(b)
                        sb.append("  ›  ")
                        str = q
                    }
                    16 -> {
                        sb = StringBuilder()
                        sb.append(b)
                        sb.append("  ›  ")
                        str = l
                    }
                    31 -> {
                        sb = StringBuilder()
                        sb.append(f4707d)
                        sb.append("  ›  ")
                        str = r
                    }
                    32 -> {
                        sb = StringBuilder()
                        sb.append(f4707d)
                        sb.append("  ›  ")
                        str = s
                    }
                    33 -> {
                        sb = StringBuilder()
                        sb.append(f4707d)
                        sb.append("  ›  ")
                        str = t
                    }
                    34 -> {
                        sb = StringBuilder()
                        sb.append(f4707d)
                        sb.append("  ›  ")
                        str = u
                    }
                    else -> return b
                }
            } else {
                sb = StringBuilder()
                sb.append(g)
                sb.append("  ›  ")
                str = y
            }
            sb.append(str)
            return sb.toString()
        }
    }
}
