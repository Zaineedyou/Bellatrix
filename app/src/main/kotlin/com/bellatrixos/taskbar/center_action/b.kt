package com.bellatrixos.taskbar.center_action

import android.content.Context
import android.media.AudioManager
import android.widget.Toast
import com.bellatrixos.R
import com.bellatrixos.windows.files.operations.FileOperations

/** Callback volume dan kegagalan operasi berkas pada ActionCenter. */
class b(@JvmField val f4814a: Context) :
    ActionCenterManager.OnStopTrackingTouchCallback,
    FileOperations.TaskFailedListener {

    override fun a(i2: Int) {
        val manager = f4814a.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val stream = AudioManager.STREAM_MUSIC
        manager.setStreamVolume(
            stream,
            Math.round(i2 / 100.0f * manager.getStreamMaxVolume(stream)),
            0
        )
    }

    override fun b() {
        Toast.makeText(f4814a, R.string.toast_file_failed_delete, Toast.LENGTH_SHORT).show()
    }
}
