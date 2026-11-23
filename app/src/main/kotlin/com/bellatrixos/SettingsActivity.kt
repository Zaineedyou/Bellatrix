package com.bellatrixos

import android.app.Activity
import android.os.Bundle

/**
 * Compatibility for old shortcuts and widget links that still start `.SettingsActivity`
 * with a `page` extra. There is no settings screen here: every page name is forwarded to
 * Bellatrix's one native Settings window, then this empty activity goes away.
 */
class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runtime = (application as App).runtime
        when (intent.getStringExtra(EXTRA_PAGE)?.lowercase()) {
            "licenses" -> runtime.showLicenses()
            "packages" -> runtime.showPackages()
            "permissions" -> runtime.showPermissions()
            "agents" -> runtime.showCodingAgents()
            "linux" -> runtime.showLinuxSettings()
            "style" -> runtime.showNativeSettings("3")
            "store" -> runtime.showNativeSettings("4")
            "facestore" -> runtime.showNativeSettings("14")
            "gestures" -> runtime.showNativeSettings("14")
            else -> runtime.showNativeSettings()
        }
        overridePendingTransition(0, 0)
        finish()
    }

    companion object {
        /** Kept exactly as the old port wrote it. */
        private const val EXTRA_PAGE = "page"
    }
}
