package com.bellatrixos.windows.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.view.View

/** OnClickListener sintetis untuk tombol dialog. */
class a(@JvmField val v: Dialog, @JvmField val u: Int) : View.OnClickListener {
    override fun onClick(view: View) {
        when (u) {
            0 -> {
                val dialog = v as UIDialog
                val listener = dialog.C
                if (listener != null) {
                    listener.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
                    if (dialog.G) dialog.dismiss()
                }
            }
            1 -> {
                val dialog = v as UIDialog
                dialog.D?.onClick(dialog, DialogInterface.BUTTON_NEGATIVE)
                dialog.dismiss()
            }
            2 -> (v as UIDialog).dismiss()
            else -> (v as UIDialogMouse).dismiss()
        }
    }
}
