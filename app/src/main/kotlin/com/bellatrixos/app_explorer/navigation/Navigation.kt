package com.bellatrixos.app_explorer.navigation

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.app_explorer.files.FileAdapter
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.windows.files.FileMeta

/** Pengatur lebar navigasi samping dan filter daftar berkas. */
object Navigation {
    @JvmField val f4688a: Array<Runnable?> = arrayOfNulls(1)

    fun interface OnEnterPressedListener {
        fun f(str: String)
    }

    @JvmStatic
    fun a(context: Context, recyclerView: RecyclerView, vararg viewArr: View) {
        val minWidth = context.resources.getDimensionPixelSize(R.dimen.ui_52dp)
        val maxWidth = context.resources.getDimensionPixelSize(R.dimen.ui_224dp)
        val resizeListener = object : View.OnTouchListener {
            var u: Float = 0f
            var v: Int = 0

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        f4688a[0]?.let { AppExecutors.c().removeCallbacks(it) }
                        u = event.rawX
                        v = recyclerView.width
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val width = maxOf(minWidth, minOf(maxWidth, (v + (event.rawX - u)).toInt()))
                        val params = recyclerView.layoutParams
                        params.width = width
                        recyclerView.layoutParams = params
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        c(*viewArr)
                        SharedPrefs.f4755a?.edit()?.putInt("file_explorer_side_nav_width", recyclerView.width)?.apply()
                        return true
                    }
                    else -> return false
                }
            }
        }
        for (view in viewArr) view.setOnTouchListener(resizeListener)
        recyclerView.setOnTouchListener(c(0, viewArr))
    }

    @JvmStatic
    fun b(editText: EditText, fileAdapter: FileAdapter) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                val query = editable?.toString()
                val filtered = ArrayList<FileMeta>()
                for (fileMeta in fileAdapter.H) {
                    // Kondisi if kosong di sekitar regionMatches; semua item tetap masuk.
                    val name = fileMeta.lowerName
                    if (!query.isNullOrEmpty() && name.isNotEmpty() && query.length <= name.length) {
                        var i = 0
                        val last = name.length - query.length
                        while (i <= last) { i++ }
                    }
                    filtered.add(fileMeta)
                }
                fileAdapter.submitList(fileAdapter.D(filtered))
            }
        })
    }

    @JvmStatic
    fun c(vararg viewArr: View) {
        for (view in viewArr) view.visibility = View.VISIBLE
        f4688a[0]?.let { AppExecutors.c().removeCallbacks(it) }
        f4688a[0] = Runnable {
            for (view in viewArr) view.visibility = View.INVISIBLE
        }
        f4688a[0]?.let { AppExecutors.c().postDelayed(it, 2000L) }
    }
}
