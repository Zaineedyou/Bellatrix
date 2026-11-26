package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.theme.isVideoBackground
import com.bellatrixos.ui.decodeSampled
import com.bellatrixos.ui.videoPoster
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.UIProgressCircular
import com.bellatrixos.widgets.cards.UISettingsCardSeekBar
import com.bellatrixos.widgets.cards.UISettingsCardSpinner
import com.bellatrixos.widgets.cards.UISettingsCardSpinnerCompact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Halaman wallpaper latar. */
@UnstableApi
class UIBackground : Fragment() {
    @JvmField var A0: ImageView? = null
    @JvmField var B0: ImageView? = null
    @JvmField var C0: ImageView? = null
    @JvmField var D0: UIProgressCircular? = null
    @JvmField var E0: UISettingsCardSpinnerCompact? = null
    @JvmField var A1: UISettingsCardSeekBar? = null
    @JvmField val F0 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        C0059h(this).onActivityResult(result)
    }
    @JvmField var s0: View? = null
    @JvmField var t0: Context? = null
    @JvmField var u0: LinearLayout? = null
    @JvmField var v0: LinearLayout? = null
    @JvmField var w0: TextView? = null
    @JvmField var x0: MainActivityIn? = null
    @JvmField var y0: ImageView? = null
    @JvmField var z0: UISettingsCardSpinner? = null

    /** The row of tiles under Images: the default two, the pictures the user added, then the add button. */
    private var wallpaperRow: LinearLayout? = null
    private val addedTiles = mutableListOf<View>()

    override fun onDestroyView() {
        super.onDestroyView()
        wallpaperRow = null
        addedTiles.clear()
        x0 = null
        A0?.setOnClickListener(null)
        B0?.setOnClickListener(null)
        w0?.setOnClickListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        A0?.clipToOutline = true
        B0?.clipToOutline = true
        y0?.clipToOutline = true
        y0?.setImageBitmap(x0?.G())
        view.postDelayed(i(this, 2), 380L)
        renderAdded()
    }

    private fun px(dp: Int) = (dp * resources.displayMetrics.density + 0.5f).toInt()

    /** A small picture of [file]: a frame of a video, otherwise the image itself, cut down to thumbnail size. */
    private fun thumbnail(file: File): Bitmap? =
        runCatching {
            if (file.isVideoBackground()) {
                videoPoster(file)
            } else {
                decodeSampled(file, 256) ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, _, _ ->
                        decoder.setTargetSize(256, 256)
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    null
                }
            }
        }.getOrNull()

    /**
     * Shows every picture or video the user added to this theme as a tile in the same row as the default two,
     * between them and the add button: tap one to use it, hold it to remove it. Called when the page opens and
     * again after a new one is added.
     */
    fun renderAdded() {
        val context = t0 ?: return
        val runtime = (context.applicationContext as? App)?.runtime ?: return
        val row = wallpaperRow ?: v0?.findViewById<LinearLayout>(R.id.wallpaper_row)?.also { wallpaperRow = it } ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val items =
                withContext<List<Pair<File, Bitmap?>>>(Dispatchers.IO) {
                    val theme = runtime.themes.current ?: return@withContext emptyList()
                    theme.backgrounds().filter(theme::isExtraBackground).map { it to thumbnail(it) }
                }
            if (wallpaperRow !== row) return@launch
            for (tile in addedTiles) row.removeView(tile)
            addedTiles.clear()
            // Right after the second default picture, so the add button stays last.
            var at = row.indexOfChild(B0) + 1
            for ((file, bitmap) in items) {
                val size = px(84)
                val tile =
                    ImageView(context).apply {
                        setBackgroundResource(R.drawable.shape_rounded_corner_8)
                        foreground = context.getDrawable(R.drawable.ui_browser_btn_subtle)
                        clipToOutline = true
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        isClickable = true
                        isFocusable = true
                        contentDescription = file.name
                        setImageBitmap(bitmap)
                        setOnClickListener {
                            y0?.setImageBitmap(bitmap)
                            x0?.v(bitmap)
                            viewLifecycleOwner.lifecycleScope.launch { runtime.themes.setBackground(file) }
                        }
                        setOnLongClickListener {
                            viewLifecycleOwner.lifecycleScope.launch {
                                runtime.themes.removeBackground(file)
                                renderAdded()
                            }
                            true
                        }
                    }
                row.addView(tile, at++, LinearLayout.LayoutParams(size, size).apply { marginStart = px(10) })
                addedTiles.add(tile)
            }
        }
    }

    fun a0() {
        // One picker for every image and video format. The selected type is only a hint; the file
        // itself decides how it is applied, so a .mkv picked while Picture is selected still moves.
        val intent =
            Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
        F0.launch(intent)
    }

    /** The volume card only means something while the wallpaper is a video. */
    fun setVolumeEnabled(video: Boolean) {
        A1?.isEnabled = video
        A1?.alpha = if (video) 1f else 0.5f
    }

    fun b0(z: Boolean) {
        if (z) {
            C0?.visibility = View.GONE
            D0?.a()
            A0?.isEnabled = false
            B0?.isEnabled = false
        } else {
            C0?.visibility = View.VISIBLE
            D0?.b()
            A0?.isEnabled = true
            B0?.isEnabled = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_background, container, false)
        s0 = view
        val context = view.context
        t0 = context
        x0 = context as? MainActivityIn
        y0 = view.findViewById(R.id.desktop_wallpaper)
        v0 = view.findViewById(R.id.view_2)
        u0 = view.findViewById(R.id.view_3)
        A0 = view.findViewById(R.id.img_theme1)
        B0 = view.findViewById(R.id.img_theme2)
        C0 = view.findViewById(R.id.img_select)
        D0 = view.findViewById(R.id.progressBar)
        w0 = view.findViewById(R.id.btnSelectWallpaper)
        z0 = view.findViewById(R.id.spinnerImgType)
        E0 = view.findViewById(R.id.spinnerQuality)
        A1 = view.findViewById(R.id.card_wallpaper_volume)
        return view
    }
}
