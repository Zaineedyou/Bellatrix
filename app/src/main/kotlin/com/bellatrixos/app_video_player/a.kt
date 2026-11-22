package com.bellatrixos.app_video_player

import android.widget.ImageButton
import android.widget.PopupWindow
import com.bellatrixos.taskbar.center_more_icons.DialogMoreIcons
import com.bellatrixos.taskbar.center_more_icons.MoreIconsManager
import com.bellatrixos.taskbar.center_widgets.WidgetCenter
import com.bellatrixos.widgets.TaskbarBtn

/** Callback penutupan popup terkait pemutar video. */
class a(
    @JvmField val u: Int,
    @JvmField val v: Any?
) : PopupWindow.OnDismissListener {

    override fun onDismiss() {
        when (u) {
            0 -> (v as? Runnable)?.run()
            1 -> {
                @Suppress("UNUSED_VARIABLE")
                val unused = WidgetCenter.u
                (v as? TaskbarBtn)?.isActivated = false
            }
            else -> {
                @Suppress("UNUSED_VARIABLE")
                val unused = DialogMoreIcons.f4815e
                MoreIconsManager.a(false, v as? ImageButton ?: return)
            }
        }
    }
}
