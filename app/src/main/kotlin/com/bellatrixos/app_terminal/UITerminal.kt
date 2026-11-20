package com.bellatrixos.app_terminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.linux.LinuxRuntime

/** Bellatrix's Terminal entry: opens a foot window owned by the Linux compositor. */
class UITerminal : UIFragment() {
    private var launched = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.app_terminal, container, false)
        contentView = view
        view.findViewById<Button>(R.id.terminal_open).setOnClickListener { open() }
        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val runtime = LinuxRuntime.get(requireContext())
        if (runtime.isLinuxReady && !launched) view.post { open() }
    }

    private fun open() {
        if (launched) return
        launched = true
        LinuxRuntime.get(requireContext()).terminal(null, "Terminal")
    }
}
