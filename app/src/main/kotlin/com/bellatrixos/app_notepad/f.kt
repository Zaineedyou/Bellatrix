package com.bellatrixos.app_notepad

import android.text.Layout
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import com.bellatrixos.R

/** Tugas inisialisasi editor dan aksi tunda notepad. */
class f(
    @JvmField val v: UINotepad,
    @JvmField val u: Int
) : Runnable {

    override fun run() {
        when (u) {
            0 -> {
                val note = v
                val name = note.v0.name
                note.w0 = name
                note.x0.setTitle(name)
                note.y0.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    if (!note.Q0 && scrollY > oldScrollY) note.Q0 = true
                }
                note.y0.setOnTouchListener(
                    com.bellatrixos.app_explorer.navigation.c(1, note)
                )
                note.H0 = ViewTreeObserver.OnScrollChangedListener {
                    val layout: Layout = note.y0.layout ?: return@OnScrollChangedListener
                    if (!note.Q0 || note.N0 || note.M0) return@OnScrollChangedListener
                    val scrollY = note.y0.scrollY
                    val height = note.y0.height
                    val contentHeight = layout.height
                    val movedDown = scrollY > note.J0
                    note.J0 = scrollY
                    if (!movedDown || scrollY + height >= contentHeight - 150) return@OnScrollChangedListener
                    note.Q0 = false
                    note.q0(null)
                }
                note.y0.viewTreeObserver.addOnScrollChangedListener(note.H0)
                note.y0.b()
            }
            1 -> v.A0.P(v)
            2 -> {
                val note = v
                note.P0 = false
                note.x0.setTitle(note.w0)
            }
            3 -> v.y0.selectAll()
            4 -> v.y0.isSaveEnabled = true
            5 -> {
                val context = v.u0
                Toast.makeText(context, R.string.toast_file_failed_open, Toast.LENGTH_SHORT).show()
            }
            else -> {
                val note = v
                note.P0 = false
                note.x0.setTitle(note.w0)
            }
        }
    }
}
