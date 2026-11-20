package com.bellatrixos.app_image_viewer

import android.graphics.PointF
import android.widget.Toast
import com.bellatrixos.R
import kotlin.math.min

/** Tugas penyesuaian tampilan gambar. */
class b(
    @JvmField val v: UIImageViewer,
    @JvmField val u: Int
) : Runnable {

    override fun run() {
        when (u) {
            0 -> {
                val context = v.v0
                Toast.makeText(context, R.string.not_found_file, Toast.LENGTH_SHORT).show()
            }
            else -> {
                val viewer = v
                val image = viewer.H0
                if (image.isReady) {
                    val sWidth = image.sWidth
                    val sHeight = image.sHeight
                    val width = image.width.toFloat()
                    val height = image.height.toFloat()
                    val scale = min(width / sWidth, height / sHeight)
                    val center = PointF(sWidth / 2.0f, sHeight / 2.0f)
                    image.setScaleAndCenter(if (sWidth <= width && sHeight <= height) 1.0f else scale, center)
                }
            }
        }
    }
}
