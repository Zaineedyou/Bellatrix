package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.ui.settings.SettingsPage
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.windows.config.UIPermissions
import com.bellatrixos.windows.installed_apps.IntentSites

/** Halaman privasi. */
class UIPrivacy : Fragment() {
    @JvmField var s0: Context? = null
    @JvmField var t0: UISettingsCard? = null
    @JvmField var u0: UISettingsCard? = null
    @JvmField var v0: UISettingsCard? = null
    @JvmField var permissions: UISettingsCard? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= 30) {
            t0?.setOnCardClickListener(permissionClick(0))
        } else {
            t0?.visibility = View.GONE
        }
        u0?.setOnCardClickListener(permissionClick(1))
        v0?.setOnCardClickListener(permissionClick(2))
        permissions?.setOnCardClickListener {
            (s0?.applicationContext as? App)?.runtime?.openSettings(SettingsPage.Permissions)
        }
    }

    private fun permissionClick(mode: Int) = View.OnClickListener {
        val context = s0 ?: return@OnClickListener
        when (mode) {
            0 -> UIPermissions.b(null, context, context.packageName)
            1 -> IntentSites.a(context, context.packageName)
            else -> IntentSites.b(context, "https://github.com/windows-ui/Bellatrix/blob/main/PrivacyPolicy/README.md")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_privacy, container, false)
        s0 = view.context
        t0 = view.findViewById(R.id.btn_app_storage_permission)
        permissions = view.findViewById(R.id.privacy_permissions)
        u0 = view.findViewById(R.id.btn_device_app_info)
        v0 = view.findViewById(R.id.btn_device_privacy_policy)
        return view
    }
}
