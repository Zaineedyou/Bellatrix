package com.bellatrixos.app_video_player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.interfaces.BackPressHandler
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.UIProgressBarLinear
import com.bellatrixos.widgets.UITitleBar
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import java.io.File
import java.util.Locale

/** Media3 based video player window. */
class UIVideo : UIFragment(), BackPressHandler {
    lateinit var A0: FloatingLayout
    lateinit var B0: SeekBar
    @JvmField var C0: Int = 0
    @JvmField var D0: ExoPlayer? = null
    lateinit var E0: d
    lateinit var F0: PlayerView
    lateinit var G0: ImageView
    lateinit var H0: MainActivityIn
    lateinit var I0: RelativeLayout
    lateinit var J0: TextView
    lateinit var K0: TextView
    lateinit var L0: ImageButton
    lateinit var M0: ImageButton
    @JvmField var N0: Runnable? = null
    lateinit var O0: UIProgressBarLinear
    @JvmField var P0: String? = null
    @JvmField var Q0: String? = null
    @JvmField var R0: Animation? = null
    @JvmField var S0: Animation? = null
    lateinit var u0: Context
    lateinit var z0: UITitleBar
    @JvmField var v0: Boolean = false
    @JvmField var w0: Boolean = false
    @JvmField var x0: Boolean = true
    @JvmField var y0: Boolean = false
    @JvmField val T0: Window = Window(0)

    /** Track list belongs to this player instance, not to the class, and is dropped on every media change. */
    @JvmField val trackSelections = TrackSelections()

    /** Whether this player was playing when the fragment was paused, so resume doesn't start a stopped video. */
    private var resumeAfterPause = false

    // Fragment state used by the window helpers (obfuscated names in the original).
    @JvmField var a0: Boolean = false
    @JvmField var T: String = "UIVideo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tag?.let { T = it }
    }

    companion object {
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun p0(uIVideo: UIVideo, i2: Int): String {
            val totalSeconds = i2 / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
        }
    }

    override fun onDestroy() {
        A()
        super.onDestroy()
    }

    /** Fragment destroy hook; releases the player and clears the tracks button. */
    fun A() {
        a0 = true
        s0()
        L0.setOnClickListener(null)
    }

    override fun onPause() {
        val player = D0
        resumeAfterPause = player?.playWhenReady == true
        if (resumeAfterPause) player?.pause()
        super.onPause()
        G()
    }

    /** Fragment pause hook; stops the delayed control callbacks. */
    fun G() {
        a0 = true
        N0?.let { AppExecutors.c().removeCallbacks(it) }
        AppExecutors.c().removeCallbacks(E0)
    }

    override fun onResume() {
        super.onResume()
        H()
        if (resumeAfterPause) {
            resumeAfterPause = false
            val player = D0 ?: return
            player.playWhenReady = true
            N0?.let { AppExecutors.c().post(it) }
        }
    }

    /** Fragment resume hook; restarts progress updates while playing. */
    fun H() {
        a0 = true
        val exoPlayer = D0 ?: return
        if (!exoPlayer.isPlaying) return
        N0?.let { AppExecutors.c().post(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        L(view, savedInstanceState)
    }

    /** Sets up the Media3 player, listeners and all window controls. */
    fun L(view: View, bundle: Bundle?) {
        E0 = d(this, 1)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(1500, 3000, 500, 1000)
            .build()
        val player = ExoPlayer.Builder(u0)
            .setLoadControl(loadControl)
            .build()
        D0 = player
        F0.player = player
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState != Player.STATE_READY) return
                val currentPlayer = D0 ?: return
                if (currentPlayer.duration <= 0 || w0) return
                val duration = currentPlayer.duration.toInt()
                C0 = duration
                B0.max = duration
                P0 = p0(this@UIVideo, C0)
                G0.setImageResource(R.drawable.img_app_music_pause)
                Q0?.let { J0.text = File(it).name }
                B0.visibility = View.VISIBLE
                O0.setAnimating(false)
                O0.visibility = View.GONE
                if (!v0) {
                    v0 = true
                    N0 = object : Runnable {
                        override fun run() {
                            val exoPlayer = D0
                            if (exoPlayer != null) {
                                val position = exoPlayer.currentPosition.toInt()
                                if (C0 > 0) {
                                    B0.progress = position
                                    K0.text = u0.getString(
                                        R.string.media_duration_format,
                                        p0(this@UIVideo, position),
                                        P0
                                    )
                                }
                                AppExecutors.c().postDelayed(this, 1000L)
                            }
                        }
                    }
                    N0?.let { AppExecutors.c().post(it) }
                    G0.setOnClickListener(c(this@UIVideo, 5))
                    B0.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) D0?.seekTo(progress.toLong())
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })
                    AppExecutors.c().postDelayed(E0, 5000L)
                    F0.setOnClickListener(c(this@UIVideo, 6))
                    L0.setOnClickListener(c(this@UIVideo, 0))
                }
                w0 = true
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                F0.setKeepScreenOn(isPlaying)
            }
        })
        arguments?.getString("path")?.let { q0(it) }
        val resizeListener = b(this, view)
        T0.f4870e = resizeListener
        WindowSize.e(view, A0, T0, z0, H0, this)
        z0.setResizeBtnListener(c(this, 1))
        z0.setCloseBtnListener(c(this, 2))
        z0.setMinimizeBtnListener(c(this, 3))
        M0.setOnClickListener(c(this, 4))
        A0.setOnFocusListener(b(this, view))
    }

    override fun b(): Boolean {
        H0.A(T)
        return true
    }

    override fun f0(): Boolean = x0

    override fun j0(z: Boolean) {
        A0.setFocusState(z)
        x0 = z
    }

    override fun k0(str: String?) {
        super.k0(str)
        w0 = false
        B0.visibility = View.INVISIBLE
        O0.visibility = View.VISIBLE
        O0.setAnimating(true)
        J0.text = u0.getString(R.string.st_menu_loading)
        G0.setImageResource(R.drawable.img_app_music_pause_disabled)
        q0(str)
    }

    override fun c0(): String? = "Video"

    override fun d0(): String? = "com.bellatrixos.app_video_player"

    /** Loads and starts playback of a local file. */
    fun q0(str: String?) {
        if (str == null) return
        Q0 = str
        // A new media has new tracks: the old menu's group indices would point at the wrong stream.
        trackSelections.reset()
        val uri = Uri.fromFile(File(str))
        val mediaItem = MediaItem.Builder().setUri(uri).build()
        D0?.setMediaItem(mediaItem)
        D0?.prepare()
        D0?.playWhenReady = true
    }

    /** Stops and releases the player and resets the cached audio tracks. */
    fun s0() {
        D0?.let { exoPlayer ->
            exoPlayer.stop()
            exoPlayer.release()
        }
        D0 = null
        resumeAfterPause = false
        trackSelections.reset()
        N0?.let { AppExecutors.c().removeCallbacks(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        z(inflater, container, savedInstanceState)

    /** Inflates the player layout and caches every view/animation. */
    fun z(layoutInflater: LayoutInflater, viewGroup: ViewGroup?, bundle: Bundle?): View {
        val viewInflate = layoutInflater.inflate(R.layout.app_video_player, viewGroup, false)
        contentView = viewInflate
        A0 = viewInflate.findViewById(R.id.main_view_img)
        WindowSize.a(A0, T0.c)
        val context = viewInflate.context
        u0 = context
        H0 = context as MainActivityIn
        z0 = viewInflate.findViewById(R.id.titleBar)
        I0 = viewInflate.findViewById(R.id.music_controls)
        J0 = viewInflate.findViewById(R.id.mp3_name)
        K0 = viewInflate.findViewById(R.id.tvDuration)
        B0 = viewInflate.findViewById(R.id.seekBar)
        O0 = viewInflate.findViewById(R.id.seekBarLoading)
        F0 = viewInflate.findViewById(R.id.videoPlayer)
        G0 = viewInflate.findViewById(R.id.playPauseBtn)
        M0 = viewInflate.findViewById(R.id.loopBtn)
        L0 = viewInflate.findViewById(R.id.btnTracks)
        R0 = AnimationUtils.loadAnimation(u0, R.anim.menu_slow_open_up)
        S0 = AnimationUtils.loadAnimation(u0, R.anim.menu_slow_close_down)
        B0.thumbTintMode = null
        B0.visibility = View.INVISIBLE
        return viewInflate
    }
}
