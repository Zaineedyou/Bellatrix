package com.bellatrixos.app_terminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bellatrixos.App
import com.bellatrixos.UIFragment
import com.bellatrixos.interfaces.MainActivityIn

/**
 * A stale taskbar/Recents entry for the old Terminal tile. It draws nothing and
 * immediately opens foot; MainActivity.c0 now bypasses it for every new launch.
 */
class UITerminal : UIFragment() {
    private var launched = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FrameLayout(requireContext()).also { contentView = it }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (launched) return
        launched = true
        view.post {
            val ctx = context ?: return@post
            (ctx.applicationContext as? App)?.runtime?.terminal(null, "Terminal")
            (activity as? MainActivityIn)?.d(this)
        }
    }
}
