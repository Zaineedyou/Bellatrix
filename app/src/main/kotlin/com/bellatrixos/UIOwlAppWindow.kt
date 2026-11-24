package com.bellatrixos

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.ui.holdsKeys
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.UITitleBar
import com.bellatrixos.widgets.render.LocalInputFocus
import com.bellatrixos.widgets.render.WidgetScreen
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.Windows
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import kotlinx.coroutines.flow.collect

/**
 * An owl app (`app "name" { }`) inside a normal Bellatrix floating window.
 *
 * The same spec and live instance the full-screen task uses are rendered here, with the launcher's own title bar,
 * drag/resize, taskbar entry and close button. Full-screen stays the default tap; this fragment is the "open in
 * window" action, so an app that relies on screen insets keeps them when opened the old way.
 */
class UIOwlAppWindow : UIFragment() {
    companion object {
        const val TAG = "UIOwlAppWindow"
    }

    private val state = Window(1)
    private var floating: FloatingLayout? = null
    private var titleBar: UITitleBar? = null
    private var host: MainActivityIn? = null
    private lateinit var runtime: Runtime
    private var name: String? = null
    private var shown = false
    private var closing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.owl_app_window, container, false)
        contentView = view
        floating = view.findViewById(R.id.owl_app_floating_root)
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
        val appName =
            arguments?.getString("path") ?: arguments?.getString("packageName").takeIf { !it.isNullOrBlank() }
        if (appName == null || runtime.widgets.appInstance(appName) == null) {
            close()
            return
        }
        name = appName

        val installed: InstalledApp? = Windows.d(state, TAG)
        title.setTitle(runtime.widgets.appInstance(appName)?.first?.name ?: appName)

        WindowSize.e(view, frame, state, title, inHome, this)
        WindowSize.d(view, frame, state)
        WindowSize.f(frame, title, state.c)
        title.setMinimizeBtnListener { inHome.A(tag ?: TAG) }
        title.setCloseBtnListener { close() }
        title.setResizeBtnListener { WindowSize.c(state) }

        frame.setOnFocusListener(
            object : FloatingLayout.OnAppFocusListener, Window.OnResizeStateChangeListener {
                override fun a() {
                    view.bringToFront()
                    setWindowFocused(true)
                    inHome.d(this@UIOwlAppWindow)
                    frame.setFocusState(true)
                }

                override fun b(i2: Int) {
                    WindowSize.d(view, frame, state)
                    WindowSize.f(frame, title, i2)
                    WindowSize.g(i2, TAG, installed)
                }
            },
        )

        val compose = view.findViewById<ComposeView>(R.id.owl_app_compose_host)
        compose.setViewTreeLifecycleOwner(viewLifecycleOwner)
        compose.setViewTreeSavedStateRegistryOwner(viewLifecycleOwner as SavedStateRegistryOwner)
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides requireActivity()) {
                val (spec, instance) =
                    runtime.widgets.appInstance(appName) ?: run {
                        LaunchedEffect(Unit) { close() }
                        return@CompositionLocalProvider
                    }
                LaunchedEffect(spec.name) { title.setTitle(spec.name) }
                LaunchedEffect(appName) {
                    runtime.widgets.appCloses.collect { if (it == appName) close() }
                }
                CompositionLocalProvider(LocalInputFocus provides true) {
                    Box(Modifier.holdsKeys().fillMaxSize()) { WidgetScreen(instance, Modifier.fillMaxSize()) }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startApp()
    }

    override fun onStop() {
        stopApp()
        super.onStop()
    }

    override fun k0(str: String?) {
        view?.bringToFront()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) stopApp() else startApp()
    }

    private fun startApp() {
        val app = name ?: return
        if (shown || closing || !::runtime.isInitialized) return
        shown = true
        runtime.widgets.appStarted(app)
    }

    private fun stopApp() {
        val app = name ?: return
        if (!shown || !::runtime.isInitialized) return
        shown = false
        runtime.widgets.appStopped(app)
    }

    private fun close() {
        if (closing) return
        closing = true
        stopApp()
        val inHome = host
        if (inHome != null) {
            inHome.closeWindow(this)
        } else {
            // No host means we cannot use the shell close path; still drop focus so a
            // hidden window never keeps the rest of the desktop from being touched.
            setWindowFocused(false)
            if (Windows.f4882e === this) Windows.f4882e = null
            runCatching {
                if (!parentFragmentManager.isStateSaved) {
                    parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
                }
            }
        }
    }

    override fun h0(
        i2: Int,
        keyEvent: KeyEvent,
    ) {
        if (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
            val app = name
            if (app == null || !::runtime.isInitialized || runtime.widgets.appInstance(app)?.second?.goUp() != true) close()
        }
    }

    override fun f0(): Boolean = floating?.B ?: t0

    override fun j0(z: Boolean) {
        super.j0(z)
        floating?.setFocusState(z)
    }

    override fun o0() {
        floating?.invalidate()
    }

    override fun a0(): FrameLayout? = null

    override fun b0(): InstalledApp? = null

    override fun c0(): String? = name

    override fun d0(): String? = name

    override fun e0(): Int = state.c

    /** Each app gets its own taskbar entry, like each Linux window does. */
    override fun n0(): Boolean = true
}
