package com.bellatrixos.app_settings.fragments

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.app_settings.UISettings
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.ui.settings.SettingsPage
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.widgets.cards.UISettingsCardSpinnerText
import com.bellatrixos.windows.drivers.Display
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Halaman "About" pengaturan. */
class UIAbout : Fragment() {
    @JvmField var A0: TextView? = null
    @JvmField var B0: TextView? = null
    @JvmField var s0: View? = null
    @JvmField var t0: Context? = null
    @JvmField var u0: UISettings? = null
    @JvmField var v0: UISettingsCardSpinnerText? = null
    @JvmField var w0: TextView? = null
    @JvmField var x0: TextView? = null
    @JvmField var y0: TextView? = null
    @JvmField var z0: TextView? = null
    @JvmField var licenses: UISettingsCard? = null

    override fun onDestroyView() {
        super.onDestroyView()
        u0 = null
        v0?.setOnCardClickListener(null)
        licenses?.setOnCardClickListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        licenses?.setOnCardClickListener {
            (t0?.applicationContext as? App)?.runtime?.openSettings(SettingsPage.Licenses)
        }
        s0?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val root = s0 ?: return
                root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                B0?.width = A0?.width ?: 0
                val brand = w0 ?: return
                brand.text = Build.BRAND
                val context = t0 ?: return
                val memoryInfo = ActivityManager.MemoryInfo()
                (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(memoryInfo)
                val total = memoryInfo.totalMem / 1.073741824E9
                x0?.text = context.getString(
                    R.string.settings_about_storage_summary,
                    Math.ceil(total).toLong().toString(),
                    String.format(Locale.US, "%.1f", total)
                )
                y0?.text = Build.ID
                val installed = try {
                    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(
                        Date(context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime)
                    )
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
                z0?.text = installed
                v0?.setTitle(SharedPrefs.f4755a?.getString("system_user_name", "This PC"))
                v0?.setSubtitle(Display.a(context))
                v0?.setOnCardClickListener(ViewOnClickListenerC0052a(0, this))
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_about, container, false)
        s0 = view
        t0 = view.context
        u0 = parentFragment as? UISettings
        w0 = view.findViewById(R.id.about_brand)
        v0 = view.findViewById(R.id.about_name)
        x0 = view.findViewById(R.id.about_ram)
        A0 = view.findViewById(R.id.about_ram_title)
        y0 = view.findViewById(R.id.about_id)
        z0 = view.findViewById(R.id.about_installed_date)
        B0 = view.findViewById(R.id.about_installed_title)
        licenses = view.findViewById(R.id.about_licenses)
        return view
    }
}
