package com.bellatrixos.app_settings.fragments

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultCallback
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.app_settings.adapters.ColorPickerAdapter
import com.bellatrixos.app_settings.adapters.InstalledAppsAdapter
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.variables.Global
import com.bellatrixos.windows.config.Languages
import com.bellatrixos.windows.drivers.network.Network
import com.bellatrixos.windows.icons.IconEngine
import com.bellatrixos.windows.icons.dialogs.DialogIconPacks
import com.bellatrixos.widgets.cards.UISettingsCardSpinner
import com.bellatrixos.windows.installed_apps.AppsDatabase
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Callback sintetis bersama: menu konteks aplikasi, pemilih warna, spinner,
 * pemilih paket ikon, dan hasil pemilihan gambar pengguna.
 */
class C0054c(@JvmField val u: Int, @JvmField val v: Fragment) :
    ActivityResultCallback<Any?>,
    DialogIconPacks.IconPackSelectionListener,
    InstalledAppsAdapter.OnMoreClickListener,
    ColorPickerAdapter.OnItemClickListener,
    UISettingsCardSpinner.SpinnerCallback {

    override fun a(view: View, i2: Int) {
        val installed = v as? UIAppsInstalled ?: return
        val menu = installed.t0?.l0() as? UIContextMenu ?: return
        val items = menu.f4745d
        items.clear()
        val apps = installed.v0?.y ?: return
        if (i2 < 0 || i2 >= apps.size) return
        val str = apps[i2].c()
        val pinned = installed.t0?.e0()?.let { adapter ->
            @Suppress("UNCHECKED_CAST")
            (adapter as? com.bellatrixos.taskbar.TaskBarAdapter)?.c(str ?: "") ?: false
        } ?: false
        items.add(
            if (pinned) {
                UIMenuItem(0, 17, R.drawable.menu_ic_pin_off_20_regular, installed.getString(R.string.context_menu_unpin_from_taskbar))
            } else {
                UIMenuItem(0, 16, R.drawable.menu_ic_pin_20_regular, installed.getString(R.string.context_menu_pin_to_taskbar))
            }
        )
        if (str != null && !AppsDatabase.b.containsKey(str)) {
            items.add(UIMenuItem(0, 24, R.drawable.menu_ic_delete_20_regular, installed.getString(R.string.context_menu_uninstall)))
        }
        menu.c = C0058g(i2, 0, installed, str)
        menu.a(android.R.style.Animation_Dialog)
        val res = installed.s0?.resources ?: return
        menu.e(view, -res.getDimension(R.dimen.ui_124dp).toInt(), -res.getDimension(R.dimen.ui_24dp).toInt())
    }

    override fun b(str: String) {
        val colors = v as? UIColors ?: return
        val name = when (str) {
            "#0063B1" -> "Blue1"
            "#0078D4" -> "Blue0"
            "#0099BC" -> "Cyan0"
            "#00B294" -> "Green0"
            "#00B7C3" -> "Cyan2"
            "#00CC6A" -> "Green2"
            "#018574" -> "Green1"
            "#038387" -> "Cyan3"
            "#107C10" -> "Green5"
            "#10893E" -> "Green3"
            "#2D7D9A" -> "Cyan1"
            "#486860" -> "Grey5"
            "#498205" -> "Green4"
            "#4A5459" -> "Grey9"
            "#4C4A48" -> "Grey7"
            "#515C6B" -> "Grey3"
            "#525E54" -> "Brown3"
            "#567C73" -> "Grey4"
            "#5D5A58" -> "Grey1"
            "#647C64" -> "Brown2"
            "#68768A" -> "Grey2"
            "#69797E" -> "Grey8"
            "#6B69D6" -> "Blue3"
            "#744DA9" -> "Purple1"
            "#767676" -> "Grey6"
            "#7A7574" -> "Grey0"
            "#7E735F" -> "Brown5"
            "#847545" -> "Brown4"
            "#8764B8" -> "Purple0"
            "#881798" -> "Purple3"
            "#8E8CD8" -> "Blue2"
            "#9A0089" -> "Pink5"
            "#B146C2" -> "Purple2"
            "#BF0077" -> "Pink3"
            "#C239B3" -> "Pink4"
            "#C30052" -> "Pink1"
            "#CA5010" -> "Brown0"
            "#D13438" -> "Red1"
            "#DA3B01" -> "Brown1"
            "#E3008C" -> "Pink2"
            "#E74856" -> "Red3"
            "#E81123" -> "Red4"
            "#EA005E" -> "Pink0"
            "#EF6950" -> "Red0"
            "#F7630C" -> "Orange0"
            "#FF4343" -> "Red2"
            "#FF8C00" -> "Yellow1"
            "#FFB900" -> "Yellow0"
            else -> null
        }
        if (name != null) {
            try {
                SharedPrefs.f4755a?.edit()?.putString("system_primary_color", name)?.apply()
            } catch (_: Exception) {
            } finally {
                colors.x0?.n()
            }
        }
    }

    /** SpinnerCallback.c — bahasa (u=5) atau perilaku klik mouse (default). */
    override fun c(i2: Int) {
        when (u) {
            5 -> {
                val region = v as? UILangRegion ?: return
                if (i2 == 0) return
                val languageCodes = Languages.f4887a
                val str = if (languageCodes != null && i2 >= 0 && i2 < languageCodes.size) languageCodes[i2] else "en"
                try {
                    val configuration = Resources.getSystem().configuration
                    val language = if (Build.VERSION.SDK_INT >= 24) {
                        configuration.locales[0].language
                    } else {
                        @Suppress("DEPRECATION")
                        configuration.locale.language
                    }
                    if (str != language) {
                        region.s0?.let { Languages.a(it, str, com.bellatrixos.app_settings.fragments.v(region, 2)) }
                    } else {
                        region.s0?.let { Languages.a(it, "default", com.bellatrixos.app_settings.fragments.v(region, 1)) }
                    }
                } catch (_: Exception) {
                    return
                }
            }
            else -> {
                val mouse = v as? UIMouse ?: return
                SharedPrefs.f(i2, Runnable {
                    Global.p = i2
                    mouse.u0?.H(i2 != 0)
                })
            }
        }
    }

    override fun d(label: String, packageName: String) {
        val apps = v as? UIApps ?: return
        val current = SharedPrefs.f4755a?.getString("system_icon_pack", "system") ?: "system"
        if (packageName == current) return
        SharedPrefs.f4755a?.edit()?.putString("system_icon_pack", packageName)?.apply()
        apps.s0?.let { IconEngine.b(it).g(packageName) }
        apps.x0?.text = label
        apps.u0?.N()
    }

    override fun onActivityResult(result: Any?) {
        when (u) {
            0 -> {
                val accounts = v as? UIAccounts ?: return
                val uri = result as? Uri ?: return
                val context = accounts.s0 ?: return
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    var bitmap = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    val width = bitmap.width
                    val height = bitmap.height
                    if (width > 512) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, 512, Math.round(512.0 * height / width).toInt(), true)
                    }
                    val output = ByteArrayOutputStream()
                    var quality = 100
                    do {
                        output.reset()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                        quality -= 5
                    } while (output.size() > 51200 && quality > 0)
                    val file = File(context.filesDir, "user_image.jpg")
                    FileOutputStream(file).use { it.write(output.toByteArray()) }
                    val built = Uri.fromFile(file).buildUpon()
                        .appendQueryParameter("timestamp", System.currentTimeMillis().toString())
                        .build()
                    accounts.u0?.setImageURI(built)
                    accounts.v0?.H0?.setImageURI(built)
                    accounts.w0?.a(built)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            else -> {
                val devices = v as? UIDevices ?: return
                devices.w0?.setSwitchChecked(Network.a())
                devices.v0 = false
            }
        }
    }

    // ActivityResultCallback.e(Object)
    fun e(obj: Any?) = onActivityResult(obj)
}
