package com.bellatrixos.windows.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bellatrixos.R
import com.bellatrixos.windows.files.FileExtensions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Dialog detail sebuah berkas atau folder. */
class UIDialogProperties(context: Context, @JvmField val x: File) :
    Dialog(context, R.style.UIDialogTransparent) {

    @JvmField var u: Int = 0
    @JvmField var v: Int = 0
    @JvmField var w: Int = 0

    companion object {
        const val y: Int = 0

        private fun formatSize(bytes: Long): String {
            if (bytes < 1024L) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024.0) return String.format(Locale.getDefault(), "%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024.0) return String.format(Locale.getDefault(), "%.1f MB", mb)
            return String.format(Locale.getDefault(), "%.1f GB", mb / 1024.0)
        }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.ui_dialog_properties)
        val titleBar = findViewById<LinearLayout>(R.id.dialog_title)
        val close = findViewById<ImageView>(R.id.btn_close)
        val image = findViewById<ImageView>(R.id.file_img)
        val nameView = findViewById<TextView>(R.id.file_name)
        val pathView = findViewById<TextView>(R.id.file_path)
        val sizeView = findViewById<TextView>(R.id.file_size)
        val extView = findViewById<TextView>(R.id.file_extension)
        val dateView = findViewById<TextView>(R.id.file_last_modified)
        val name = x.name
        nameView.text = name
        pathView.text = x.absolutePath
        extView.text = if (x.isDirectory) {
            context.getString(R.string.dialog_properties_file_folder)
        } else {
            ".${FileExtensions.c(name)}"
        }
        image.setImageResource(if (x.isDirectory) R.drawable.img_folder_default else FileExtensions.e(name))
        try {
            dateView.text = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
                .format(Date(x.lastModified()))
        } catch (ignored: Exception) {
        }
        if (x.exists()) sizeView.text = formatSize(x.length())
        findViewById<TextView>(R.id.btn_positive).setOnClickListener { dismiss() }
        findViewById<TextView>(R.id.btn_negative).setOnClickListener { dismiss() }
        close.setOnClickListener { dismiss() }
        val window = window ?: return
        val decor = window.decorView
        decor.post { decor.setFitsSystemWindows(true) }
        val params = window.attributes
        var startX = 0.0f
        var startY = 0.0f
        var touchX = 0.0f
        var touchY = 0.0f
        titleBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val current = window.attributes
                    startX = current.x.toFloat()
                    startY = current.y.toFloat()
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (event.rawX - touchX + startX).toInt()
                    val nextY = (event.rawY - touchY + startY).toInt()
                    params.y = max(v, min(w, nextY))
                    window.attributes = params
                    true
                }
                else -> false
            }
        }
    }
}
