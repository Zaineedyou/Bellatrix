package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.app_settings.adapters.InstalledAppsAdapter
import com.bellatrixos.interfaces.MainActivityIn

/** Daftar aplikasi yang terpasang. */
class UIAppsInstalled : Fragment() {
    @JvmField var s0: Context? = null
    @JvmField var t0: MainActivityIn? = null
    @JvmField var u0: RecyclerView? = null
    @JvmField var v0: InstalledAppsAdapter? = null
    @JvmField var w0: List<Any>? = null

    override fun onDestroyView() {
        super.onDestroyView()
        t0 = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.postDelayed(RunnableC0057f(this, 0), 380L)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_apps_installed, container, false)
        val context = view.context
        s0 = context
        t0 = context as? MainActivityIn
        u0 = view.findViewById(R.id.installed_apps)
        return view
    }
}
