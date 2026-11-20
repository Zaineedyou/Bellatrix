package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.UIProgressBarLinear
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.widgets.cards.UISettingsCardSwitch
import com.google.android.play.core.appupdate.AppUpdateManager

/** Halaman pembaruan aplikasi. */
class UIPCUpdate : Fragment() {
    /** Status pembaruan; ordinal mengikuti urutan CHECKING/AVAILABLE/UPDATED. */
    class STATUS private constructor(@JvmField val name: String, @JvmField val ordinal: Int) {
        companion object {
            @JvmField val u = STATUS("CHECKING", 0)
            @JvmField val v = STATUS("AVAILABLE", 1)
            @JvmField val w = STATUS("UPDATED", 2)
            @JvmField val x = arrayOf(u, v, w)

            @JvmStatic
            fun valueOf(str: String): STATUS = x.first { it.name == str }

            @JvmStatic
            fun values(): Array<STATUS> = x.copyOf()
        }
    }

    @JvmField var A0: UISettingsCard? = null
    @JvmField var B0: UISettingsCard? = null
    @JvmField var C0: UISettingsCardSwitch? = null
    @JvmField var D0: UIProgressBarLinear? = null
    @JvmField var s0: Context? = null
    @JvmField var t0: ViewStub? = null
    @JvmField var u0: TextView? = null
    @JvmField var v0: TextView? = null
    @JvmField var w0: View? = null
    @JvmField var x0: ImageView? = null
    @JvmField var y0: TextView? = null
    @JvmField var z0: AppUpdateManager? = null

    override fun onDestroyView() {
        super.onDestroyView()
        u0?.setOnClickListener(null)
        B0?.setOnClickListener(null)
        A0?.setOnClickListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        y0?.text = "${getString(R.string.settings_about_version)} ${getString(R.string.app_ui_version)}"
        u0?.setOnClickListener(w(this, 0))
        C0?.setSwitchChecked(SharedPrefs.f4755a?.getBoolean("system_notify_updates", false) ?: false)
        A0?.setOnCardClickListener(w(this, 1))
        B0?.setOnCardClickListener(w(this, 2))
        if (Global.B == 1) c0()
        C0?.setOnSwitchCheckedChangeListener(com.bellatrixos.app_settings.fragments.u(2))
    }

    fun a0(status: STATUS) {
        val context = s0
        val drawable = when (status.ordinal) {
            1 -> context?.let { ContextCompat.getDrawable(it, R.drawable.ui_settings_img_update_alert) }
            2 -> context?.let { ContextCompat.getDrawable(it, R.drawable.ui_settings_img_update_success) }
            else -> null
        }
        x0?.foreground = drawable
    }

    fun b0(z: Boolean) {
        D0?.setAnimating(z)
        D0?.visibility = if (z) View.VISIBLE else View.GONE
        y0?.visibility = if (z) View.GONE else View.VISIBLE
    }

    fun c0() {
        a0(STATUS.v)
        if (w0 == null) {
            val inflated = t0?.inflate() ?: return
            w0 = inflated
            val download = inflated.findViewById<TextView>(R.id.btn_download)
            val subtitle = inflated.findViewById<TextView>(R.id.subtitle_ui_card)
            download.setOnClickListener(w(this, 3))
            subtitle.setOnClickListener(w(this, 4))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_update, container, false)
        s0 = view.context
        u0 = view.findViewById(R.id.btn_update)
        v0 = view.findViewById(R.id.updateTitle)
        A0 = view.findViewById(R.id.btn_review_app)
        B0 = view.findViewById(R.id.btn_share_app)
        C0 = view.findViewById(R.id.switch_get_latest)
        x0 = view.findViewById(R.id.image_update_banner)
        y0 = view.findViewById(R.id.title_update_version)
        D0 = view.findViewById(R.id.title_update_progress)
        t0 = view.findViewById(R.id.update_card_stub)
        return view
    }
}
