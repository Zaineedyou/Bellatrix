package com.bellatrixos.app_notepad

import android.text.Editable
import android.widget.Toast
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/** Tugas baca/tulis file teks notepad secara bertahap. */
class i(
    @JvmField val v: Any?,
    @JvmField val u: Int,
    @JvmField val w: Any?
) : Runnable {

    override fun run() {
        when (u) {
            0, 2 -> (v as? UINotepad)?.s0(w as? Runnable)
            1 -> readFile()
            3 -> saveFile()
            else -> {
                val note = v as? UINotepad ?: return
                val length = (w as? Editable)?.length ?: 0
                note.E0.text = note.u0.getString(R.string.cmd_menu_statusbar, length)
                note.y0.b()
            }
        }
    }

    private fun readFile() {
        val note = v as? UINotepad ?: return
        val callback = w as? Runnable
        try {
            if (!note.R0) {
                Thread.sleep(220L)
                note.I0 = BufferedReader(InputStreamReader(FileInputStream(note.v0)))
            }
            val builder = StringBuilder()
            var chunks = 0
            while (chunks < 300) {
                val pending = note.L0
                if (pending != null) {
                    val count = Math.min(pending.length - note.K0, 8192)
                    builder.append(pending, note.K0, note.K0 + count)
                    note.K0 += count
                    if (note.K0 >= pending.length) {
                        builder.append('\n')
                        note.L0 = null
                        note.K0 = 0
                    }
                } else {
                    val line = note.I0.readLine()
                    if (line == null) {
                        note.M0 = true
                        note.I0.close()
                        AppExecutors.c().post(f(note, 4))
                    } else if (line.length > 8192) {
                        note.L0 = line
                        note.K0 = 0
                    } else {
                        builder.append(line)
                        builder.append('\n')
                        chunks++
                    }
                }
                postChunk(note, builder.toString(), callback)
                return
            }
            postChunk(note, builder.toString(), callback)
        } catch (_: java.io.IOException) {
            note.N0 = false
            if (!note.S0) AppExecutors.c().post(f(note, 5))
        } catch (_: InterruptedException) {
            note.N0 = false
        }
    }

    private fun postChunk(note: UINotepad, text: String, callback: Runnable?) {
        AppExecutors.c().post {
            if (!note.S0 && note.q()) {
                note.O0 = true
                val scrollY = note.y0.scrollY
                note.y0.append(text)
                note.y0.post { note.y0.scrollTo(0, scrollY) }
                note.O0 = false
                if (!note.R0) {
                    note.R0 = true
                    note.y0.setSelection(0)
                    note.y0.scrollTo(0, 0)
                }
                note.E0.text = note.u0.getString(R.string.cmd_menu_statusbar, note.y0.length())
                note.N0 = false
                callback?.run()
            }
        }
    }

    private fun saveFile() {
        val note = v as? UINotepad ?: return
        val context = note.u0
        val text: Editable = note.y0.text
        var ok = true
        try {
            OutputStreamWriter(FileOutputStream(note.v0), StandardCharsets.UTF_8).use { writer ->
                val buffer = CharArray(4096)
                val length = text.length
                var offset = 0
                while (offset < length) {
                    val count = Math.min(4096, length - offset)
                    val end = offset + count
                    text.getChars(offset, end, buffer, 0)
                    writer.write(buffer, 0, count)
                    offset = end
                }
                writer.flush()
            }
        } catch (_: Exception) {
            Toast.makeText(context, R.string.toast_file_failed_save, Toast.LENGTH_SHORT).show()
            ok = false
        }
        AppExecutors.c().post { (w as? Runnable)?.run() }
        if (!ok) return
    }
}
