package com.bellatrixos

import android.os.Bundle
import androidx.compose.runtime.remember
import com.bellatrixos.ui.home.LauncherRoot
import com.bellatrixos.ui.home.LauncherState

/**
 * The OWL home: flex/box/anchor pages from layout.toml, drawn by the same
 * runtime as the widgets and panels. Opt-in from Settings > Home, while the
 * legacy launcher stays the default.
 */
class OwlHomeActivity : InstantActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime.start()
        setInstantContent {
            val state = remember { LauncherState() }
            LauncherRoot(runtime, state)
        }
    }

    override fun onStart() {
        super.onStart()
        runtime.screenStarted()
    }

    override fun onStop() {
        runtime.screenStopped()
        super.onStop()
    }
}
