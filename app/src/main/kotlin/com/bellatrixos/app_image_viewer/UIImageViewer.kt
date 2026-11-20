package com.bellatrixos.app_image_viewer

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.interfaces.BackPressHandler
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.UITitleBarImage
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.Windows
import com.bellatrixos.windows.files.FileExtensions
import com.bellatrixos.windows.files.FileMeta
import com.bellatrixos.windows.files.clipboard.ClipboardItem
import com.bellatrixos.windows.files.clipboard.Clipboards
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.io.File
import java.util.ArrayList

/** Penampil gambar dan GIF sebagai jendela mengambang. */
class UIImageViewer : UIFragment(), BackPressHandler {
    lateinit var A0: UITitleBarImage
    lateinit var B0: FloatingLayout
    @JvmField var C0: InstalledApp? = null

    // D0 dinyatakan non-null agar helper paket ini dapat memanggilnya langsung;
    // penunjuk aslinya dikosongkan lagi saat view dihancurkan.
    private var mainActivityRef: MainActivityIn? = null
    var D0: MainActivityIn
        get() = mainActivityRef ?: error("MainActivityIn belum tersedia")
        set(value) { mainActivityRef = value }

    lateinit var E0: RelativeLayout
    lateinit var F0: ImageButton
    lateinit var G0: ImageButton
    lateinit var H0: SubsamplingScaleImageView
    lateinit var I0: ImageButton
    lateinit var J0: ImageButton
    @JvmField var K0: Int = 0
    @JvmField var L0: ArrayList<*>? = null
    @JvmField var M0: Int = 60
    @JvmField var N0: Int = 0
    @JvmField var O0: Float = 0.0f
    @JvmField var P0: Boolean = true
    @JvmField var Q0: Boolean = false
    @JvmField var R0: Boolean = false
    @JvmField val S0: Window = Window(0)
    /** Field berkas gambar (`u0`), dipisah namanya dari method `u0()` agar tidak ambigu. */
    lateinit var u0File: File
    lateinit var v0: Context
    lateinit var w0: SeekBar
    lateinit var x0: ImageView
    lateinit var y0: TextView
    lateinit var z0: ViewStub

    /** Tag fragmen (field T) dipakai MainActivity untuk menutup jendela. */
    val T: String get() = tag ?: ""

    override fun onDestroyView() {
        // field a0 pada Fragment adalah bendera mCalled; tidak dipakai di sini.
        mainActivityRef = null
        if (::H0.isInitialized) H0.recycle()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        WindowSize.e(view, B0, S0, A0, D0, this)
        C0 = Windows.d(S0, "UIPhotos")
        val extras = arguments
        if (extras != null) {
            s0(extras.getString("path"))
        } else {
            A0.C?.visibility = View.GONE
            E0.visibility = View.GONE
            z0.inflate()
        }
        S0.f4870e = e(this, view)
        A0.setResizeBtnListener(d(this, 4))
        A0.setCloseBtnListener(d(this, 5))
        A0.setMinimizeBtnListener(d(this, 6))
        A0.setPrintBtnListener(d(this, 7))
        A0.setShareBtnListener(d(this, 8))
        A0.setRotateBtnListener(d(this, 9))
        A0.setMoreBtnListener(d(this, 10))
        if (S0.c != 0) {
            WindowSize.d(view, B0, S0)
            WindowSize.f(B0, A0, 1)
        }
        B0.setOnFocusListener(e(this, view))
        super.onViewCreated(view, savedInstanceState)
    }

    override fun b(): Boolean {
        D0.A(T)
        return true
    }

    override fun c0(): String? = try {
        u0File.name
    } catch (_: UninitializedPropertyAccessException) {
        null
    }

    override fun d0(): String? = "com.bellatrixos.app_image_viewer"

    override fun f0(): Boolean = P0

    override fun h0(i2: Int, keyEvent: KeyEvent) {
        val list = L0
        if (list == null || list.isEmpty()) return
        if (i2 == KeyEvent.KEYCODE_DPAD_LEFT) {
            v0()
        } else if (i2 == KeyEvent.KEYCODE_DPAD_RIGHT) {
            u0()
        }
        if (keyEvent.isCtrlPressed && i2 == KeyEvent.KEYCODE_O && u0File.exists()) {
            Clipboards.f4922a.add(ClipboardItem(2, u0File.parent ?: "", u0File.name))
        }
    }

    override fun j0(z: Boolean) {
        if (::B0.isInitialized) {
            B0.setFocusState(z)
            P0 = z
        }
    }

    override fun k0(str: String?) {
        if (!u0File.exists() || u0File.absolutePath == str) {
            if (u0File.absolutePath != str || contentView == null) return
            contentView?.bringToFront()
            return
        }
        if (H0.isReady) H0.recycle()
        contentView?.bringToFront()
        s0(str)
        R0 = true
    }

    fun p0(i2: Int): Int {
        val list = L0
        if (list != null && list.isNotEmpty()) {
            var index = i2
            while (true) {
                index++
                if (index >= list.size) break
                val item = list[index]
                if (item is FileMeta && FileExtensions.g(item)) return index
            }
        }
        return -1
    }

    fun q0(i2: Int): Int {
        val list = L0
        if (list != null && list.isNotEmpty()) {
            for (index in i2 - 1 downTo 0) {
                val item = list[index]
                if (item is FileMeta && FileExtensions.g(item)) return index
            }
        }
        return -1
    }

    fun s0(str: String?) {
        if (str == null) return
        AppExecutors.d().execute {
            // androidx.core.content.res.a(this, 18, str): varian case 18 (i4 = 0).
            val file = File(str)
            u0File = file
            if (file.exists()) {
                AppExecutors.c().post(c(this, file.name, str, 0))
            } else {
                AppExecutors.c().post(b(this, 0))
            }
        }
    }

    fun t0(str: String, z: Boolean) {
        if (!z) {
            x0.visibility = View.GONE
            H0.visibility = View.VISIBLE
            H0.recycle()
            H0.setMinimumScaleType(3)
            H0.setImage(ImageSource.uri(str))
            w0.visibility = View.VISIBLE
            return
        }
        H0.visibility = View.GONE
        H0.recycle()
        x0.visibility = View.VISIBLE
        w0.visibility = View.GONE
        Glide.with(v0).asGif().load(u0File).into(x0)
    }

    fun u0() {
        val index = p0(K0)
        if (index != -1) {
            K0 = index
            s0((L0!![index] as FileMeta).file.absolutePath)
        }
    }

    fun v0() {
        val index = q0(K0)
        if (index != -1) {
            K0 = index
            s0((L0!![index] as FileMeta).file.absolutePath)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.app_image_viewer, container, false)
        B0 = view.findViewById(R.id.main_view_img)
        WindowSize.a(B0, S0.c)
        val context = view.context
        v0 = context
        D0 = context as MainActivityIn
        A0 = view.findViewById(R.id.titleBar)
        x0 = view.findViewById(R.id.gif_preview)
        H0 = view.findViewById(R.id.image_preview)
        w0 = view.findViewById(R.id.seekBar)
        F0 = view.findViewById(R.id.btn_prev)
        G0 = view.findViewById(R.id.btn_next)
        E0 = view.findViewById(R.id.img_footer_view)
        z0 = view.findViewById(R.id.img_placeholder)
        I0 = view.findViewById(R.id.btn_zoom_to_fit)
        J0 = view.findViewById(R.id.btn_info)
        y0 = view.findViewById(R.id.txt_dimension)
        w0.thumbTintMode = null
        contentView = view
        return view
    }
}
