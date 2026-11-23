package com.bellatrixos.app_settings.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bellatrixos.App
import com.bellatrixos.BuildConfig
import com.bellatrixos.adb.AdbBridge
import com.bellatrixos.adb.ShellStatus
import com.bellatrixos.LauncherUpdates
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
    @JvmField var systemUpdates: UISettingsCard? = null
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

    private val launcherUpdates: LauncherUpdates?
        get() = (s0?.applicationContext as? App)?.runtime?.launcherUpdates

    override fun onDestroyView() {
        super.onDestroyView()
        u0?.setOnClickListener(null)
        B0?.setOnClickListener(null)
        A0?.setOnClickListener(null)
        systemUpdates?.setOnCardClickListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        y0?.text = "${getString(R.string.settings_about_version)} ${BuildConfig.VERSION_NAME}"
        u0?.setOnClickListener { checkUpdates() }
        C0?.setSwitchChecked(SharedPrefs.f4755a?.getBoolean("system_notify_updates", false) ?: false)
        A0?.setOnCardClickListener(w(this, 1))
        B0?.setOnCardClickListener(w(this, 2))
        systemUpdates = view.findViewById(R.id.update_system)
        systemUpdates?.setOnCardClickListener {
            (s0?.applicationContext as? App)?.runtime?.showNativeSettings()
        }
        if (Global.B == 1) c0()
        C0?.setOnSwitchCheckedChangeListener(com.bellatrixos.app_settings.fragments.u(2))
    }

    private fun checkUpdates() {
        val launcher = launcherUpdates ?: return
        b0(true)
        a0(STATUS.u)
        launcher.check(force = true) { release ->
            if (!isAdded) return@check
            b0(false)
            a0(if (release != null) STATUS.v else STATUS.w)
            showUpdateDialog(release, launcher)
        }
    }

    private fun showUpdateDialog(release: LauncherUpdates.Info?, launcher: LauncherUpdates) {
        val context = s0 ?: return
        val current = BuildConfig.VERSION_NAME
        val version = release?.version ?: current
        val notes =
            release?.changelog?.takeIf { it.isNotBlank() }
                ?: getString(R.string.settings_update_dialog_no_notes)
        val adb = (context.applicationContext as? App)?.runtime?.adb
        val message =
            buildString {
                append(getString(R.string.settings_update_dialog_current, current)).append('\n')
                append(getString(R.string.settings_update_dialog_version, version)).append("\n\n")
                append(getString(R.string.settings_update_dialog_changelog)).append('\n')
                append(notes.take(4000))
                if (launcher.lastError != null) {
                    append("\n\n").append(launcher.lastError)
                } else if (release == null) {
                    append("\n\n").append(getString(R.string.settings_update_dialog_latest_note))
                }
                if (adb != null) {
                    append("\n\n").append(getString(R.string.adb_shell_title)).append(": ").append(shellStatus(adb))
                    if (!adb.ready) append('\n').append(getString(R.string.adb_shell_setup))
                }
            }
        val builder =
            AlertDialog
                .Builder(context, R.style.UILiquidGlassDialog)
                .setTitle(
                    if (release != null) {
                        getString(R.string.settings_update_dialog_available, version)
                    } else {
                        getString(R.string.settings_update_dialog_latest)
                    },
                ).setMessage(message)
        builder.setPositiveButton(R.string.settings_update_dialog_download_github) { _, _ -> launcher.openRelease(context) }
        if (release != null && adb?.ready == true) {
            builder.setNegativeButton(R.string.adb_shell_install) { _, _ ->
                b0(true)
                launcher.installWithShell(context) { b0(false) }
            }
        } else {
            builder.setNegativeButton(android.R.string.cancel, null)
        }
        if (adb != null && !adb.ready) {
            builder.setNeutralButton(R.string.adb_shell_grant) { _, _ ->
                if (adb.status == ShellStatus.UNAVAILABLE) adb.openManager(context) else adb.requestPermission()
                adb.refresh()
            }
        }
        val dialog = builder.create()
        dialog.show()
        // Frost the content behind the glass on Android 12+, after the window and its
        // decor view exist. Some vendors leave DecorView null until the first frame,
        // so guard it and never take down the launcher over an OEM blur implementation.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialog.window?.decorView?.post {
                val window = dialog.window
                if (window?.decorView != null) runCatching { window.setBackgroundBlurRadius(20) }
            }
        }
    }

    private fun shellStatus(adb: AdbBridge): String =
        getString(
            when (adb.status) {
                ShellStatus.CHECKING -> R.string.adb_shell_status_checking
                ShellStatus.UNAVAILABLE -> R.string.adb_shell_status_unavailable
                ShellStatus.PERMISSION_REQUIRED -> R.string.adb_shell_status_permission
                ShellStatus.READY_ROOT -> R.string.adb_shell_status_root
                ShellStatus.READY_ADB -> R.string.adb_shell_status_adb
            },
        )

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
