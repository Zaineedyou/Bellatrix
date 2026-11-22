package com.bellatrixos.linux

import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.UITitleBar
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.Windows
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import com.bellatrixos.wayland.ActiveLinuxBackend
import com.bellatrixos.wayland.KeyBarView
import com.bellatrixos.wayland.KeyboardSink
import com.bellatrixos.wayland.LinuxViewBackend
import com.bellatrixos.wayland.LinuxWindowInputView
import com.bellatrixos.wayland.WindowTextureView
import com.bellatrixos.wayland.XdgToplevel

/**
 * A Linux toplevel drawn inside a normal Bellatrix floating window.
 *
 * [LinuxWindowActivity] remains the host for ordinary guest apps, but the
 * terminal is launched as this fragment so it behaves like every other
 * launcher window: draggable, resizable, minimisable and listed in the taskbar.
 * Pixels still come straight from the Wayland compositor into [WindowTextureView],
 * so a raised terminal covers ordinary Views like the Coding Agents page instead
 * of staying behind them on SurfaceView's separate layer. Android only supplies
 * the frame, input, keybar and soft keyboard.
 */
class UILinuxWindow : UIFragment() {
    companion object {
        const val TAG = "UILinuxWindow"
        private const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
    }

    /** 1 = restored/floating; 0 would make [WindowSize] fill the whole container. */
    private val state = Window(1)
    private var installed: InstalledApp? = null
    private var floating: FloatingLayout? = null
    private var titleBar: UITitleBar? = null
    private var activityIn: MainActivityIn? = null

    private var handle: String? = null
    private lateinit var backend: LinuxViewBackend
    private var toplevel: XdgToplevel? = null
    private var closing = false

    private lateinit var content: FrameLayout
    private lateinit var surface: WindowTextureView
    private lateinit var input: LinuxWindowInputView
    private lateinit var sink: KeyboardSink
    private lateinit var keyBar: KeyBarView
    private lateinit var keyBarContainer: FrameLayout

    private var bridgeSetByMe = false
    private var keyboardHeight = 0

    /** `exit` destroys the Wayland toplevel without calling our close button. */
    private val staleCheck =
        object : Runnable {
            override fun run() {
                val w = toplevel
                if (
                    isAdded &&
                    this@UILinuxWindow::backend.isInitialized &&
                    w != null &&
                    !backend.wm.appWindows.contains(w)
                ) {
                    removeSelf()
                    return
                }
                if (isAdded) contentView?.postDelayed(this, 500L)
            }
        }

    private val picker =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (!this::backend.isInitialized) return@registerForActivityResult
            toplevel?.let(backend.wm::focus)
            if (this::sink.isInitialized && sink.isAttachedToWindow && !sink.showing) sink.show()
            if (uris.isNotEmpty()) backend.keyboard.insertImages(uris)
        }

    private val imagePicker =
        { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.linux_floating_window, container, false)
        contentView = view

        val f = view.findViewById<FloatingLayout>(R.id.linux_floating_root)
        floating = f
        titleBar = view.findViewById(R.id.titleBar)

        WindowSize.a(f, state.c)
        WindowSize.b(view, f)
        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        backend = ActiveLinuxBackend.get(requireContext())
        activityIn = requireContext() as? MainActivityIn

        handle =
            arguments?.getString("path")
                ?: arguments?.getString("handle")
                ?: arguments?.getString("packageName")

        val h = handle
        val w = if (h.isNullOrBlank()) null else backend.wm.windowByHandle(h)
        if (w == null) {
            removeSelf()
            return
        }
        toplevel = w

        content = view.findViewById(R.id.linux_content)
        keyBarContainer = view.findViewById(R.id.linux_keybar_container)

        installed = Windows.d(state, TAG)
        // A terminal is always a desktop-style floating window, never a fullscreen pane.
        state.a(1)

        val f = floating ?: return
        val t = titleBar
        val dragHandle: View = t ?: f

        WindowSize.e(view, f, state, dragHandle, activityIn ?: return, this)
        WindowSize.d(view, f, state)
        WindowSize.f(f, t, state.c)
        t?.setTitle(backend.displayName(w.appId, w.title))

        val focusResize =
            object : Window.OnResizeStateChangeListener, FloatingLayout.OnAppFocusListener {
                override fun a() {
                    view.bringToFront()
                    setWindowFocused(true)
                    activityIn?.d(this@UILinuxWindow)
                }

                override fun b(i2: Int) {
                    WindowSize.d(view, f, state)
                    WindowSize.f(f, t, i2)
                    WindowSize.g(i2, TAG, installed)
                }
            }
        state.f4870e = focusResize
        f.setOnFocusListener(focusResize)
        f.setOnWindowTapListener {
            if (!f0()) {
                setWindowFocused(true)
                activityIn?.d(this@UILinuxWindow)
            }
        }

        t?.setResizeBtnListener { WindowSize.c(state) }
        t?.setMinimizeBtnListener { activityIn?.A(tag ?: TAG) }
        t?.setCloseBtnListener { closeWindow() }

        installLinuxViews(w)

        view.isFocusableInTouchMode = true
        view.setOnKeyListener { _, _, event -> handleKey(event) }

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            keyboardHeight = (ime - nav).coerceAtLeast(0)
            keyBarContainer.visibility = if (keyboardHeight > 0) View.VISIBLE else View.GONE
            relayout()
            insets
        }
        relayout()
        view.postDelayed(staleCheck, 500L)
    }

    private fun installLinuxViews(w: XdgToplevel) {
        surface = WindowTextureView(requireContext(), backend.server, backend.wm, w)
        content.addView(surface, 0, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        input = LinuxWindowInputView(requireContext(), backend.wm, w)
        input.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        input.onCursorTap = { if (this::sink.isInitialized && !sink.showing) sink.show() }
        content.addView(input, 1, input.layoutParams)

        sink = KeyboardSink(requireContext(), backend.keyboard)
        content.addView(sink, FrameLayout.LayoutParams(1, 1, Gravity.TOP or Gravity.START))

        keyBar = KeyBarView(requireContext())
        keyBarContainer.addView(
            keyBar,
            FrameLayout.LayoutParams(MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
    }

    override fun onResume() {
        super.onResume()
        if (!this::backend.isInitialized) return
        toplevel?.let(backend.wm::focus)
        backend.keyboard.picker = imagePicker
        if (!bridgeSetByMe) {
            KeyBarView.KeyboardBridgeHolder.bridge = backend.keyboard
            bridgeSetByMe = true
        }
        relayout()
    }

    override fun onPause() {
        if (this::backend.isInitialized) {
            backend.keyboard.releaseHeld()
            if (backend.keyboard.picker === imagePicker) backend.keyboard.picker = null
        }
        releaseBridge()
        super.onPause()
    }

    override fun onDestroyView() {
        contentView?.removeCallbacks(staleCheck)
        state.f4870e = null
        if (this::backend.isInitialized && backend.keyboard.picker === imagePicker) {
            backend.keyboard.picker = null
        }
        releaseBridge()
        super.onDestroyView()
    }

    override fun onDestroy() {
        val w = toplevel
        if (this::backend.isInitialized && !closing && w != null && backend.wm.appWindows.contains(w)) {
            backend.wm.close(w)
        }
        super.onDestroy()
    }

    private fun handleKey(event: KeyEvent): Boolean {
        if (!this::backend.isInitialized) return false
        if (
            event.keyCode == KeyEvent.KEYCODE_ESCAPE &&
            event.isMetaPressed &&
            event.action == KeyEvent.ACTION_DOWN
        ) {
            if (event.repeatCount == 0) closeWindow()
            return true
        }
        return backend.keyboard.handle(event)
    }

    private fun closeWindow() {
        if (closing) return
        closing = true
        val w = toplevel
        if (this::backend.isInitialized && w != null && backend.wm.appWindows.contains(w)) {
            backend.wm.close(w)
        }
        removeSelf()
    }

    /** Take the window out of the fragment registry and the taskbar, even if state was saved. */
    private fun removeSelf() {
        closing = true
        val host = activityIn
        if (host != null) {
            host.closeWindow(this)
            return
        }
        setWindowFocused(false)
        if (Windows.f4882e === this) Windows.f4882e = null
        runCatching {
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        }
    }

    private fun releaseBridge() {
        if (
            bridgeSetByMe &&
            this::backend.isInitialized &&
            KeyBarView.KeyboardBridgeHolder.bridge === backend.keyboard
        ) {
            KeyBarView.KeyboardBridgeHolder.bridge = null
        }
        bridgeSetByMe = false
    }

    private fun relayout() = view?.post {
        if (!::content.isInitialized) return@post
        if (!::input.isInitialized) return@post
        if (!::surface.isInitialized) return@post
        if (!::keyBarContainer.isInitialized) return@post

        val barHeight = if (keyBarContainer.visibility == View.VISIBLE) keyBarContainer.height else 0
        val visible = (content.height - keyboardHeight - barHeight).coerceAtLeast(1)
        input.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, visible)
        input.requestLayout()
        keyBarContainer.translationY = -keyboardHeight.toFloat()
        surface.visibleHeight = visible
    }

    override fun f0(): Boolean = floating?.B ?: t0

    override fun j0(z: Boolean) {
        super.j0(z)
        floating?.setFocusState(z)
    }

    override fun o0() {
        floating?.invalidate()
    }

    override fun a0(): FrameLayout? = if (::content.isInitialized) content else null

    override fun b0(): InstalledApp? = installed

    override fun c0(): String? = handle

    override fun d0(): String? = handle

    override fun e0(): Int = state.c

    /** Each handle gets its own terminal window instead of reusing the first one. */
    override fun n0(): Boolean = true

    override fun h0(
        i2: Int,
        keyEvent: KeyEvent,
    ) {
        handleKey(keyEvent)
    }
}
