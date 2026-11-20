package com.bellatrixos.app_browser

import android.content.DialogInterface
import com.bellatrixos.database.SharedPrefs

/** Callback dialog kebijakan browser. */
class r(@JvmField val u: Int) : DialogInterface.OnClickListener {
    override fun onClick(dialog: DialogInterface, which: Int) {
        if (u == 0) {
            dialog.dismiss()
        } else {
            SharedPrefs.f4755a?.edit()?.putBoolean("is_policy_read", true)?.apply()
        }
    }
}
