package com.bellatrixos.app_notepad

import android.content.ClipboardManager
import android.content.Context
import androidx.core.util.Consumer
import com.bellatrixos.app_explorer.picker.FilePicker
import com.bellatrixos.windows.drivers.Printer

/** Aksi menu notepad dan pembaruan tata letak jendela. */
class e(
    @JvmField val u: Int,
    @JvmField val v: Any?
) : Consumer<Any?> {

    override fun accept(obj: Any?) {
        when (u) {
            0 -> {
                val note = v as? UINotepad ?: return
                when ((obj as? Number)?.toInt() ?: -1) {
                    5 -> Printer.b(note.A0.f() ?: return, note.y0.text.toString())
                    22 -> note.p0()
                    101 -> note.t0(f(note, 2))
                    102 -> {
                        FilePicker.b = note
                        note.A0.k0("txt/**")
                    }
                }
            }
            1 -> {
                val note = v as? UINotepad ?: return
                if (((obj as? Number)?.toInt() ?: 0) != 0) {
                    note.A0.P(note)
                } else {
                    note.t0(f(note, 1))
                }
            }
            2 -> {
                val which = (obj as? Number)?.toInt() ?: -1
                val note = v as? UINotepad ?: return
                if (which == 21) {
                    note.y0.requestFocus()
                    note.y0.post(f(note, 3))
                }
                if (which == 8) {
                    val manager = note.u0.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = manager?.primaryClip
                    val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text
                    if (text != null) {
                        note.y0.text.insert(note.y0.selectionStart, text)
                    }
                }
            }
            // Cabang default Java meneruskan WindowLayoutInfo ke Channel. androidx.window
            // bukan dependensi proyek ini dan tidak ada pemanggil yang memakai cabang ini.
            else -> Unit
        }
    }
}
