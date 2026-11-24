package com.bellatrixos.app_music_player

import android.content.Context
import android.content.DialogInterface
import android.provider.MediaStore
import android.widget.Toast
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.windows.files.MediaPaths
import com.bellatrixos.windows.dialogs.UIDialog
import java.io.File

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
                        arrayOf("_id", "_data", "_display_name", "duration"),
                        null, null, "date_added DESC"
                    )
                    cursor?.use {
                        val id = it.getColumnIndex("_id")
                        while (it.moveToNext()) {
                            val model = AudioModel()
                            // A MediaStore path is often not a file Linux (or even this app) can open.
                            // Keep it when it is readable, otherwise play the same item by its content Uri.
                            val path = it.getString(1)
                            model.f4697a =
                                if (!path.isNullOrEmpty() && File(path).isFile) {
                                    MediaPaths.guest(path) ?: path
                                } else if (id >= 0) {
                                    "content://media/external/audio/media/${it.getLong(id)}"
                                } else {
                                    path.orEmpty()
                                }
                            model.b = it.getString(2)
                            model.c = it.getLong(3)
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
