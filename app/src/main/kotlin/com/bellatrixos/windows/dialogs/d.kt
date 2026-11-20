package com.bellatrixos.windows.dialogs

import android.content.Context
import android.os.CountDownTimer
import com.bellatrixos.R
import com.bellatrixos.app_browser.r

/** Runnable sintetis untuk dialog kebijakan dan mouse. */
class d(@JvmField val u: Int, @JvmField val v: Any) : Runnable {
    override fun run() {
        if (u != 0) {
            (v as? UIDialogMouse)?.dismiss()
            return
        }
        val context = v as Context
        val dialog = UIDialog(
            context,
            context.getString(R.string.app_name),
            context.getString(R.string.app_privacy_policy)
        )
        dialog.z = context.getString(R.string.app_privacy_policy_ok_after)
        dialog.C = r(1)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        dialog.a(false)
        object : CountDownTimer(5000L, 1000L) {
            override fun onFinish() {
                dialog.a(true)
                dialog.B?.text = context.getString(R.string.app_privacy_policy_ok_after)
            }

            override fun onTick(ms: Long) {
                dialog.B?.text = context.getString(R.string.app_privacy_policy_ok_before, (ms / 1000).toString())
            }
        }.start()
    }
}
