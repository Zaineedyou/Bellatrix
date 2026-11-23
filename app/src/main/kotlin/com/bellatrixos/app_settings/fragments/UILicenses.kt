package com.bellatrixos.app_settings.fragments

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bellatrixos.BuildConfig
import com.bellatrixos.R
import com.bellatrixos.system.openUrl
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.widgets.cards.UISettingsCardCollapse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Third-party notices as a Bellatrix Settings page: one native collapse card per component, with the
 * component's notice, website/source links and license text inside its dropdown. Replaces the old
 * Omalauncher-style licenses screen.
 */
class UILicenses : Fragment() {
    private class Component(
        val name: String,
        @StringRes val use: Int,
        val license: String,
        val notice: String,
        val url: String,
        val source: String? = null,
    )

    private companion object {
        const val APACHE = "apache-2.0"

        // Keep in step with scripts/fetch-proot.sh and patches/proot: these are the pinned versions.
        val COMPONENTS =
            listOf(
                Component(
                    "PRoot",
                    R.string.licenses_use_proot,
                    "gpl-2.0",
                    "Copyright (C) 2015 STMicroelectronics. Built from Termux's 5.1.107.96 with upstream patches; the binary's libtalloc.so.2 dependency is renamed to libtalloc.so.",
                    "https://proot-me.github.io",
                    source = "https://github.com/SimonSchubert/omarchy/tree/main/patches/proot",
                ),
                Component(
                    "talloc",
                    R.string.licenses_use_talloc,
                    "lgpl-3.0",
                    "Copyright (C) Andrew Tridgell 2004, Stefan Metzmacher 2006. Termux build of 2.5.0, renamed to libtalloc.so. The LGPL adds to the GPL 3.0 below it.",
                    "https://talloc.samba.org",
                    source = "https://www.samba.org/ftp/talloc/talloc-2.5.0.tar.gz",
                ),
                Component(
                    "libandroid-shmem",
                    R.string.licenses_use_shmem,
                    "bsd-3-clause",
                    "Copyright (c) 2013, Sergii Pylypenko\nCopyright (c) 2017, Fredrik Fornwall\nAll rights reserved.",
                    "https://github.com/termux/libandroid-shmem",
                    source = "https://github.com/termux/libandroid-shmem/tree/v0.7",
                ),
                Component(
                    "Mesa",
                    R.string.licenses_use_mesa,
                    "mit",
                    "Copyright (C) 1999-2007 Brian Paul and the Mesa contributors. Mesa 26.2.4 with upstream " +
                        "zink patch. Most files are MIT; the rest are under the permissive licenses in Mesa's licenses/ directory.",
                    "https://mesa3d.org",
                    source = "https://github.com/SimonSchubert/omarchy/tree/main/scripts/mesa",
                ),
                Component(
                    "wayland-protocols",
                    R.string.licenses_use_wayland,
                    "mit",
                    "Copyright © 2008-2013 Kristian Høgsberg\nCopyright © 2010-2013 Intel Corporation\n" +
                        "Copyright © 2013 Rafael Antognolli\nCopyright © 2013 Jasper St. Pierre\nCopyright © 2014 Jonas Ådahl\n" +
                        "Copyright © 2014 Jason Ekstrand\nCopyright © 2014-2015 Collabora, Ltd.\nCopyright © 2015 Red Hat Inc.",
                    "https://gitlab.freedesktop.org/wayland/wayland-protocols",
                ),
                Component("Nerd Fonts", R.string.licenses_use_nerd_fonts, "mit", "Copyright (c) 2014 Ryan L McIntyre", "https://www.nerdfonts.com"),
                Component(
                    "Weather Icons",
                    R.string.licenses_use_weather_icons,
                    "ofl-1.1",
                    "Copyright (c) Erik Flowers. Licensed under the SIL Open Font License 1.1.",
                    "https://erikflowers.github.io/weather-icons/",
                ),
                Component(
                    "Material Design Icons",
                    R.string.licenses_use_mdi,
                    APACHE,
                    "Copyright (c) Pictogrammers",
                    "https://pictogrammers.com/library/mdi/",
                ),
                Component(
                    "Compose Multiplatform",
                    R.string.licenses_use_compose,
                    APACHE,
                    "Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers",
                    "https://github.com/JetBrains/compose-multiplatform",
                ),
                Component(
                    "Android Jetpack",
                    R.string.licenses_use_jetpack,
                    APACHE,
                    "Copyright The Android Open Source Project",
                    "https://developer.android.com/jetpack/androidx",
                ),
                Component(
                    "Kotlin and kotlinx.coroutines",
                    R.string.licenses_use_kotlin,
                    APACHE,
                    "Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors",
                    "https://kotlinlang.org",
                ),
                Component(
                    "Apache Commons Compress",
                    R.string.licenses_use_commons_compress,
                    APACHE,
                    "Apache Commons Compress\nCopyright 2002-2026 The Apache Software Foundation\n\n" +
                        "This product includes software developed at The Apache Software Foundation (https://www.apache.org/).",
                    "https://commons.apache.org/proper/commons-compress/",
                ),
                Component("AndroidSVG", R.string.licenses_use_androidsvg, APACHE, "Copyright 2013 Paul LeBeau, Cave Rock Software Ltd.", "https://bigbadaboom.github.io/androidsvg/"),
                Component("JSON-java", R.string.licenses_use_json, "public-domain", "Public Domain.", "https://github.com/stleary/JSON-java"),
                Component("XZ for Java", R.string.licenses_use_xz, "public-domain", "XZ for Java by Lasse Collin and Igor Pavlov. Public Domain.", "https://tukaani.org/xz/java.html"),
            )
    }

    private lateinit var list: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.settings_licenses, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        list = view.findViewById(R.id.licenses_list)
        view.findViewById<TextView>(R.id.licenses_header_text).text = getString(R.string.licenses_header, BuildConfig.VERSION_NAME)
        for (component in COMPONENTS) list.addView(componentCard(component))
    }

    private fun componentCard(component: Component): UISettingsCardCollapse {
        val context = requireContext()
        val card = UISettingsCardCollapse(context)
        card.layoutParams = cardParams()
        card.findViewById<ImageView>(R.id.img_ui_card)?.setImageResource(R.drawable.ic_info_26_regular)
        card.findViewById<TextView>(R.id.title_ui_card)?.text = component.name
        card.findViewById<TextView>(R.id.subtitle_ui_card)?.text = getString(component.use)

        val body =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                val pad = (12 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
            }
        body.addView(
            TextView(context).apply {
                text = component.license
                textSize = 11f
                setTextColor(resources.getColor(R.color.text_muted, null))
                layoutParams = cardParams()
            },
        )
        body.addView(
            TextView(context).apply {
                text = component.notice
                textSize = 12f
                setTextColor(resources.getColor(R.color.text_default, null))
                layoutParams = cardParams()
            },
        )
        body.addView(linkCard(R.string.licenses_website) { context.openUrl(component.url) })
        component.source?.let { source -> body.addView(linkCard(R.string.licenses_source) { context.openUrl(source) }) }

        val licenseText = licenseTextView()
        val gplText = licenseTextView()
        body.addView(licenseText)
        body.addView(gplText)
        card.v.addView(body)

        viewLifecycleOwner.lifecycleScope.launch {
            val texts =
                withContext(Dispatchers.IO) {
                    val own =
                        if (component.license == "public-domain") {
                            null
                        } else {
                            runCatching { context.assets.open("licenses/${component.license}.txt").bufferedReader().use { it.readText() } }.getOrNull()
                        }
                    // The LGPL is a set of permissions on top of the GPL 3.0, so it ships with both texts.
                    val gpl =
                        if (component.license == "lgpl-3.0") {
                            runCatching { context.assets.open("licenses/gpl-3.0.txt").bufferedReader().use { it.readText() } }.getOrNull()
                        } else {
                            null
                        }
                    own to gpl
                }
            licenseText.text = texts.first
            licenseText.visibility = if (texts.first == null) View.GONE else View.VISIBLE
            gplText.text = texts.second
            gplText.visibility = if (texts.second == null) View.GONE else View.VISIBLE
        }
        return card
    }

    private fun linkCard(
        @StringRes label: Int,
        click: () -> Unit,
    ): UISettingsCard {
        val card = UISettingsCard(requireContext())
        card.layoutParams = cardParams(2)
        card.setImage(R.drawable.ic_open_20_regular)
        card.setTitle(getString(label))
        card.setSubtitle(null)
        card.setOnCardClickListener { click() }
        return card
    }

    private fun licenseTextView() =
        TextView(requireContext()).apply {
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 11f
            setTextColor(resources.getColor(R.color.text_muted, null))
            movementMethod = ScrollingMovementMethod()
            maxLines = 12
            layoutParams = cardParams()
            visibility = View.GONE
        }

    private fun cardParams(topMarginDp: Int = 0) =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = (topMarginDp * resources.displayMetrics.density).toInt() }
}
