package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.net.ConnectivityManager
import com.bellatrixos.R
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.UIToast
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.UpdateAvailability

/** Callback hasil pengecekan pembaruan aplikasi. */
class x(@JvmField val u: UIPCUpdate) : OnSuccessListener<AppUpdateInfo>, OnFailureListener {
    override fun onSuccess(info: AppUpdateInfo) = a(info)

    override fun onFailure(exception: Exception) = d(exception)

    /** OnSuccessListener callback (a(Object)). */
    fun a(appUpdateInfo: AppUpdateInfo) {
        val update = u
        if (!update.isAdded || update.view == null || update.v0 == null) return
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
            update.c0()
            update.a0(UIPCUpdate.STATUS.v)
            update.b0(false)
            update.u0?.text = update.s0?.getString(R.string.settings_update_pending_updates)
            Global.B = 1
        }
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
            update.a0(UIPCUpdate.STATUS.w)
            update.v0?.text = update.s0?.getString(R.string.settings_update_up_to_date_title)
        }
    }

    /** OnFailureListener callback (d(Exception)). */
    fun d(exc: Exception) {
        val update = u
        if (!update.isAdded || update.view == null) return
        update.b0(false)
        val context = update.s0 ?: return
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val info = manager?.activeNetworkInfo
        if (manager == null || info == null || !info.isConnectedOrConnecting) {
            UIToast.a(
                context,
                context.getString(R.string.toast_no_internet),
                R.drawable.menu_ic_internet_20_regular,
                2900
            )
        }
    }
}
