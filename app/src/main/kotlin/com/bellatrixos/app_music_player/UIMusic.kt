package com.bellatrixos.app_music_player

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.interfaces.BackPressHandler
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.UINavigationBar
import com.bellatrixos.widgets.UISplashLayout
import com.bellatrixos.widgets.UITitleBar
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.installed_apps.AppsDatabase
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import java.io.File
import java.io.IOException

/** Pemutar musik yang berjalan sebagai jendela mengambang. */
class UIMusic : UIFragment(), BackPressHandler {
    lateinit var A0: MainActivityIn
    lateinit var B0: TextView
    lateinit var C0: UINavigationBar
    lateinit var D0: RecyclerView
    @JvmField var E0: InstalledApp? = null
    lateinit var F0: UISplashLayout
    @JvmField var G0: TextView? = null
    @JvmField var H0: TextView? = null
    @JvmField var I0: Runnable? = null
    @JvmField var J0: String? = null
    @JvmField var K0: String? = null
    lateinit var L0: ImageButton
    lateinit var M0: ImageButton
    lateinit var N0: ImageButton
    lateinit var O0: ImageButton
    lateinit var Q0: AudioAdapter
    lateinit var d1: ViewStub
    @JvmField var e1: View? = null
    lateinit var u0: Context
    lateinit var v0: FloatingLayout
    lateinit var w0: SeekBar
    lateinit var x0: ImageView
    lateinit var y0: UITitleBar
    lateinit var z0: MediaPlayer
    @JvmField val P0: Window = Window(0)
    @JvmField val R0: ArrayList<Any?> = ArrayList()
    @JvmField val S0: ArrayList<Any?> = ArrayList()
    @JvmField var T0: Boolean = false
    @JvmField var U0: Boolean = true
    @JvmField var V0: Int = 0
    @JvmField var W0: Int = 0
    @JvmField var X0: Int = -1
    @JvmField var Y0: Boolean = false
    @JvmField var Z0: Boolean = false
    @JvmField var a1: Boolean = false
    @JvmField var b1: Boolean = false
    @JvmField var c1: Boolean = false

    /** Fragment arguments; named A in the original obfuscated build. */
    val A: Bundle?
        get() = arguments

    /** Fragment tag; named T in the original obfuscated build. */
    val T: String
        get() = tag.orEmpty()

    /** Fragment.isAdded(); named q() in the original obfuscated build. */
    fun q(): Boolean = isAdded

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.app_music_player, container, false)
        contentView = view
        v0 = view.findViewById(R.id.main_view_img)
        WindowSize.a(v0, P0.c)
        val context = view.context
        u0 = context
        A0 = context as MainActivityIn
        y0 = view.findViewById(R.id.titleBar)
        G0 = view.findViewById(R.id.mp3_name)
        H0 = view.findViewById(R.id.tvDuration)
        w0 = view.findViewById(R.id.seekBar)
        x0 = view.findViewById(R.id.albumArt)
        L0 = view.findViewById(R.id.playPauseBtn)
        N0 = view.findViewById(R.id.btn_nxt)
        O0 = view.findViewById(R.id.btn_prev)
        F0 = view.findViewById(R.id.ui_splash)
        d1 = view.findViewById(R.id.empty_stub)
        M0 = view.findViewById(R.id.loopBtn)
        D0 = view.findViewById(R.id.music_list)
        B0 = view.findViewById(R.id.title_txt)
        C0 = view.findViewById(R.id.navBar)
        v0.clipToOutline = true
        x0.clipToOutline = true
        w0.thumbTintMode = null
        N0.isEnabled = false
        O0.isEnabled = false
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        WindowSize.e(view, v0, P0, y0, A0, this)
        E0 = resolveApp(P0, "UIMusic")
        if (savedInstanceState != null) {
            F0.visibility = View.GONE
        }
        if (savedInstanceState != null) {
            W0 = savedInstanceState.getInt("playback_position", 0)
            Y0 = savedInstanceState.getBoolean("was_playing", false)
            T0 = savedInstanceState.getBoolean("looping", false)
        }
        AppExecutors.c().postDelayed(a(this, savedInstanceState, 0), 400L)

        z0 = MediaPlayer()
        z0.isLooping = T0
        z0.setOnErrorListener { mp, _, _ ->
            try {
                if (K0 == null) {
                    mp.reset()
                } else {
                    AppExecutors.c().post(d(0, this))
                    mp.reset()
                }
                true
            } catch (_: Exception) {
                true
            }
        }
        z0.setOnPreparedListener { mp ->
            if (W0 > 0) mp.seekTo(W0)
            if (Y0) {
                mp.start()
                L0.setImageResource(R.drawable.img_app_music_pause)
            } else {
                L0.setImageResource(R.drawable.img_app_music_play)
            }
            J0 = AudioManager.a(mp.duration)
            w0.max = mp.duration
            L0.alpha = 1.0f
            t0()
            K0?.let { G0?.text = File(it).name }
            if (Z0) return@setOnPreparedListener
            Z0 = true
            I0 = object : Runnable {
                override fun run() {
                    val current = z0.currentPosition
                    w0.progress = current
                    H0?.text = u0.getString(
                        R.string.media_duration_format,
                        AudioManager.a(current),
                        J0
                    )
                    AppExecutors.c().postDelayed(this, 1000L)
                }
            }
            I0?.let { AppExecutors.c().post(it) }
            L0.setOnClickListener(b(this, 3))
            w0.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) z0.seekTo(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        z0.setOnCompletionListener { mp ->
            if (X0 >= 0 && X0 < R0.size - 1) {
                X0++
                s0()
                return@setOnCompletionListener
            }
            w0.progress = 0
            mp.seekTo(0)
            L0.setImageResource(R.drawable.img_app_music_play)
            Y0 = false
            t0()
        }

        P0.f4870e = c(this, view)
        y0.setResizeBtnListener(b(this, 6))
        y0.setCloseBtnListener(b(this, 0))
        y0.setMinimizeBtnListener(b(this, 1))
        M0.setOnClickListener(b(this, 2))
        if (P0.c != 0) {
            WindowSize.d(view, v0, P0)
            WindowSize.f(v0, y0, 1)
        }
        v0.setOnFocusListener(c(this, view))
        view.post(a(this, view, 2))
        C0.setOnItemSelectedListener(RadioGroup.OnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.nav_home -> {
                    D0.visibility = View.VISIBLE
                    x0.visibility = View.GONE
                    e1?.visibility = View.VISIBLE
                    B0.setText(R.string.app_music_title_home)
                    if (!c1 && !a1) {
                        a1 = true
                        AppExecutors.d().execute(d(1, this))
                    }
                }

                R.id.nav_music -> {
                    D0.visibility = View.GONE
                    x0.visibility = View.VISIBLE
                    e1?.visibility = View.INVISIBLE
                    B0.setText(R.string.app_music_title)
                }

                else -> {
                    // Tidak ada aksi untuk item navigasi lainnya.
                }
            }
        })
        O0.setOnClickListener(b(this, 4))
        N0.setOnClickListener(b(this, 5))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::z0.isInitialized) {
            outState.putInt("playback_position", z0.currentPosition)
            outState.putBoolean("was_playing", z0.isPlaying)
        }
        outState.putBoolean("looping", T0)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        if (::z0.isInitialized) {
            z0.release()
        }
        I0?.let { AppExecutors.c().removeCallbacks(it) }
        contentView = null
        super.onDestroyView()
    }

    override fun b(): Boolean {
        A0.A(T)
        return true
    }

    override fun c0(): String? = "Music"
    override fun d0(): String? = "com.bellatrixos.app_music_player"

    override fun f0(): Boolean = U0

    override fun j0(z: Boolean) {
        if (::v0.isInitialized) {
            v0.setFocusState(z)
            U0 = z
        }
    }

    /** Implementasi k0(String) dari Java: bawa jendela ke depan lalu ganti trek. */
    override fun k0(str: String?) {
        super.k0(str)
        if (::z0.isInitialized) {
            z0.reset()
            q0(str)
        }
        w0.progress = 0
        L0.setImageResource(R.drawable.img_app_music_pause)
        t0()
    }

    /** Ambil maksimal 10 lagu berikutnya dari daftar yang sudah dipindai. */
    fun p0(): ArrayList<Any?> {
        val end = minOf(V0 + 10, S0.size)
        val page = ArrayList<Any?>(S0.subList(V0, end))
        V0 = end
        return page
    }

    /** Siapkan dan mulai memuat sumber audio. */
    fun q0(str: String?) {
        if (str == null) return
        K0 = str
        try {
            if (::z0.isInitialized) {
                z0.setDataSource(str)
                z0.prepareAsync()
            }
        } catch (_: IOException) {
            AppExecutors.c().post(d(2, this))
        }
        AppExecutors.d().execute(a(this, str, 1))
    }

    /** Putar lagu pada indeks [X0]. */
    fun s0() {
        val index = X0
        if (index >= 0 && index < R0.size) {
            k0((R0[index] as AudioModel).f4697a)
            Y0 = true
            t0()
            if (::Q0.isInitialized) {
                val adapter = Q0
                val previous = adapter.x
                adapter.x = index
                if (previous != -1) adapter.notifyItemChanged(previous)
                adapter.notifyItemChanged(index)
            }
        }
    }

    /** Perbarui status tombol sebelumnya/berikutnya. */
    fun t0() {
        if (::z0.isInitialized) {
            O0.isEnabled = K0 != null
        }
        N0.isEnabled = X0 >= 0 && X0 < R0.size - 1
    }

    /**
     * Padanan Windows.d(Window, String) dari Java. Fungsi tersebut belum tersedia
     * pada port Kotlin saat ini, jadi logikanya ditulis ulang dengan API publik.
     */
    private fun resolveApp(window: Window, tag: String): InstalledApp? {
        InstalledAppsDB.c(tag)?.let { existing ->
            window.a(existing.d())
            return existing
        }
        if (InstalledAppsDB.f4955a == null) return null
        val app = InstalledApp(AppsDatabase.b[tag], tag, tag, window.c, false)
        val list = InstalledAppsDB.b().toMutableList()
        list.add(app)
        InstalledAppsDB.b[tag] = list.size - 1
        InstalledAppsDB.f(list, false)
        return app
    }
}
