package com.bellatrixos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.ui.voice.VoiceScreen
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.UITitleBar
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.Windows

/**
 * Voice as a Bellatrix window, not another Android task. The content is the same
 * [VoiceScreen], hosted in the launcher's own floating frame and taskbar entry.
 */
class UIVoice : UIFragment() {
    companion object {
        private const val TAG = "UIVoice"
    }

    private val state = Window(1)
    private var floating: FloatingLayout? = null
    private var titleBar: UITitleBar? = null
    private var host: MainActivityIn? = null
    private lateinit var runtime: Runtime

    /** Each explicit reopen asks to talk again; a return from minimize only makes it visible. */
    private var opens by mutableIntStateOf(0)
    private var shown by mutableStateOf(false)
    private var closing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.voice_window, container, false)
        contentView = view
        floating = view.findViewById(R.id.voice_floating_root)
        titleBar = view.findViewById(R.id.titleBar)
        floating?.let { WindowSize.a(it, state.c) }
        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        runtime = (requireContext().applicationContext as App).runtime
        host = requireContext() as? MainActivityIn
        val inHome = host ?: return
        val frame = floating ?: return
        val title = titleBar ?: return

        val installed = Windows.d(state, TAG)
        WindowSize.e(view, frame, state, title, inHome, this)
        WindowSize.d(view, frame, state)
        WindowSize.f(frame, title, state.c)
        title.setMinimizeBtnListener { host?.A(tag ?: TAG) }
        title.setCloseBtnListener { close() }

        frame.setOnFocusListener(
            object : FloatingLayout.OnAppFocusListener, Window.OnResizeStateChangeListener {
                override fun a() {
                    view.bringToFront()
                    j0(true)
                    host?.d(this@UIVoice)
                    frame.setFocusState(true)
                }

                override fun b(i2: Int) {
                    WindowSize.d(view, frame, state)
                    WindowSize.f(frame, title, i2)
                    WindowSize.g(i2, TAG, installed)
                }
            },
        )

        opening()

        val compose = view.findViewById<ComposeView>(R.id.voice_compose_host)
        compose.setViewTreeLifecycleOwner(viewLifecycleOwner)
        compose.setViewTreeSavedStateRegistryOwner(viewLifecycleOwner as SavedStateRegistryOwner)
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides requireActivity()) {
                VoiceScreen(runtime, opens = opens, shown = shown, onExit = ::close)
            }
        }
    }

    /** A second tap on the mic (or the taskbar) is a new conversation; minimizing only hides it. */
    override fun k0(str: String?) {
        if (::runtime.isInitialized) opening()
        view?.bringToFront()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!::runtime.isInitialized || closing) return
        if (hidden) {
            if (shown) {
                shown = false
                runtime.voice.closed()
            }
        } else if (!shown) {
            shown = true
            runtime.voice.opened()
        }
    }

    override fun onDestroy() {
        if (shown) {
            shown = false
            if (::runtime.isInitialized) runtime.voice.closed()
        }
        super.onDestroy()
    }

    private fun opening() {
        closing = false
        opens++
        runtime.voice.openedAnew()
        if (!shown) {
            shown = true
            runtime.voice.opened()
        }
    }

    private fun close() {
        if (closing) return
        closing = true
        if (shown) {
            shown = false
            runtime.voice.closed()
        }
        val inHome = host
        if (inHome != null) {
            inHome.P(this)
        } else {
            runCatching {
                if (!parentFragmentManager.isStateSaved) {
                    parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
                }
            }
        }
    }
}
