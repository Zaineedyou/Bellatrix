package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.bellatrixos.R
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.UIProgressCircular
import com.bellatrixos.widgets.cards.UISettingsCardSpinner
import com.bellatrixos.widgets.cards.UISettingsCardSpinnerCompact

/** Halaman wallpaper latar. */
@UnstableApi
class UIBackground : Fragment() {
    @JvmField var A0: ImageView? = null
    @JvmField var B0: ImageView? = null
    @JvmField var C0: ImageView? = null
    @JvmField var D0: UIProgressCircular? = null
    @JvmField var E0: UISettingsCardSpinnerCompact? = null
    @JvmField val F0 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        C0059h(this).onActivityResult(result)
    }
    @JvmField var s0: View? = null
    @JvmField var t0: Context? = null
    @JvmField var u0: LinearLayout? = null
    @JvmField var v0: RelativeLayout? = null
    @JvmField var w0: TextView? = null
    @JvmField var x0: MainActivityIn? = null
    @JvmField var y0: ImageView? = null
    @JvmField var z0: UISettingsCardSpinner? = null

    override fun onDestroyView() {
        super.onDestroyView()
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
    }

    fun a0() {
        val video = z0?.getSelectedPosition() == 1
        val intent =
            Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_MIME_TYPES, if (video) VIDEO_MIMES else IMAGE_MIMES)
        if (!video) {
            intent.putExtra("aspectX", 1)
            intent.putExtra("aspectY", 1)
            intent.putExtra("scale", true)
        }
        F0.launch(intent)
    }

    companion object {
        private val IMAGE_MIMES =
            arrayOf(
                "image/jpeg", "image/png", "image/webp", "image/gif", "image/heic", "image/heif",
                "image/avif", "image/bmp", "image/x-ms-bmp", "image/tiff", "image/x-icon",
                "image/x-adobe-dng", "image/dng", "image/vnd.adobe.photoshop",
            )

        private val VIDEO_MIMES =
            arrayOf(
                "video/mp4", "video/x-m4v", "video/3gpp", "video/3gpp2", "video/quicktime",
                "video/webm", "video/x-matroska", "video/mpeg", "video/mp2t", "video/x-msvideo",
                "video/x-flv", "video/x-ms-wmv",
            )
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
        return view
    }
}
