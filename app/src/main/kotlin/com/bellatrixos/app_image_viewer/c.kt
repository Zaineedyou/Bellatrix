package com.bellatrixos.app_image_viewer

import android.graphics.PointF
import android.widget.SeekBar
import com.bellatrixos.R
import com.bellatrixos.app_explorer.UIExplorer
import com.bellatrixos.windows.Windows
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.min

/** Muat gambar, pasang listener, dan jalankan transisi alpha. */
class c(
    @JvmField val v: UIImageViewer,
    @JvmField val w: String,
    @JvmField val x: String,
    @JvmField val u: Int
) : Runnable {

    override fun run() {
        when (u) {
            0 -> {
                val viewer = v
                val title = w
                val path = x
                viewer.A0.setTitle(title)
                if (viewer.R0) {
                    viewer.H0.animate().alpha(0.0f).setDuration(140L)
                        .withEndAction(c(viewer, title, path, 1)).start()
                } else {
                    viewer.t0(path, title.endsWith(".gif"))
                }
                if (!viewer.Q0) {
                    viewer.H0.setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
                        override fun onReady() {}

                        override fun onImageLoaded() {
                            val image = viewer.H0
                            val sWidth = image.sWidth
                            val sHeight = image.sHeight
                            val width = image.width.toFloat()
                            val height = image.height.toFloat()
                            val scale: Float
                            if (sWidth <= width) {
                                if (sHeight <= height) {
                                    image.minScale = 1.0f
                                    image.maxScale = 2.5f
                                    image.setScaleAndCenter(1.0f, PointF(sWidth / 2.0f, sHeight / 2.0f))
                                    viewer.N0 = 100
                                    scale = 75.0f
                                } else {
                                    val fit = min(width / sWidth, height / sHeight)
                                    image.minScale = fit
                                    image.maxScale = 2.5f
                                    image.setScaleAndCenter(fit, PointF(sWidth / 2.0f, sHeight / 2.0f))
                                    scale = fit
                                    viewer.N0 = (fit * 100.0f).toInt()
                                }
                            } else {
                                val fit = min(width / sWidth, height / sHeight)
                                image.minScale = fit
                                image.maxScale = 2.5f
                                image.setScaleAndCenter(fit, PointF(sWidth / 2.0f, sHeight / 2.0f))
                                scale = fit
                                viewer.N0 = (fit * 100.0f).toInt()
                            }
                            viewer.M0 = scale.toInt()
                            viewer.w0.max = 250
                            viewer.w0.progress = viewer.N0
                            viewer.y0.text = "$sWidth x $sHeight"
                            viewer.G0.visibility = if (viewer.p0(viewer.K0) != -1) 0 else 4
                            viewer.F0.visibility = if (viewer.q0(viewer.K0) != -1) 0 else 4
                        }

                        override fun onPreviewLoadError(e: Exception?) {}
                        override fun onImageLoadError(e: Exception?) {
                            viewer.H0.setImage(ImageSource.resource(R.drawable.img_placeholder))
                        }
                        override fun onTileLoadError(e: Exception?) {}
                        override fun onPreviewReleased() {}
                    })
                    viewer.w0.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (!fromUser) return
                            val image = viewer.H0
                            if (!image.isReady) return
                            val value = max(progress, viewer.M0)
                            if (progress != value) {
                                seekBar?.progress = value
                                return
                            }
                            image.setScaleAndCenter(value / 100.0f, image.center)
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })
                    viewer.I0.setOnClickListener(d(viewer, 0))
                    viewer.J0.setOnClickListener(d(viewer, 1))
                    val explorer: UIExplorer? = Windows.f4883f
                    if (explorer is UIExplorer) {
                        val fileAdapter = explorer.E0
                        if (fileAdapter != null) {
                            viewer.L0 = ArrayList(fileAdapter.currentList)
                            viewer.K0 = fileAdapter.G(viewer.u0File)
                        }
                        viewer.F0.setOnClickListener(d(viewer, 2))
                        viewer.G0.setOnClickListener(d(viewer, 3))
                    }
                    viewer.Q0 = true
                }
            }
            else -> {
                val viewer = v
                viewer.t0(x, w.endsWith(".gif"))
                viewer.H0.animate().alpha(1.0f).setDuration(140L).start()
            }
        }
    }
}
