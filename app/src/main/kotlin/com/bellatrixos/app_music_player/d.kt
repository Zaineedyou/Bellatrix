package com.bellatrixos.app_music_player

import android.content.Context
import android.content.DialogInterface
import android.provider.MediaStore
import android.widget.Toast
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.windows.dialogs.UIDialog

/** Tugas dialog dan pemindaian pustaka audio. */
class d(
    @JvmField val u: Int,
    @JvmField val v: Any?
) : Runnable {

    override fun run() {
        when (u) {
            0 -> {
                val music = v as? UIMusic ?: return
                val context = music.u0
                val dialog = UIDialog(
                    context,
                    context.getString(R.string.media_unsupported_title),
                    context.getString(R.string.media_unsupported_subtitle)
                )
                dialog.z = context.getString(R.string.dialog_actions_ok)
                dialog.setCanceledOnTouchOutside(false)
                dialog.C = DialogInterface.OnClickListener { _, _ -> dialog.dismiss() }
                dialog.show()
            }
            1 -> {
                val music = v as? UIMusic ?: return
                val context: Context = music.u0
                val result = ArrayList<AudioModel>()
                try {
                    val cursor = context.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        arrayOf("_data", "_display_name", "duration"),
                        "is_music!=0", null, "date_added DESC"
                    )
                    cursor?.use {
                        while (it.moveToNext()) {
                            val model = AudioModel()
                            model.f4697a = it.getString(0)
                            model.b = it.getString(1)
                            model.c = it.getLong(2)
                            result.add(model)
                        }
                    }
                } catch (_: Exception) {
                }
                AppExecutors.c().post(a(music, result, 3))
            }
            2 -> {
                val context = (v as? UIMusic)?.u0 ?: return
                Toast.makeText(context, R.string.toast_file_failed_open, Toast.LENGTH_SHORT).show()
            }
            else -> {
                val music = v as? UIMusic ?: return
                val more = music.p0()
                val all = music.R0
                val start = all.size
                all.addAll(more)
                music.Q0.notifyItemRangeInserted(start, more.size)
                music.b1 = false
            }
        }
    }
}
