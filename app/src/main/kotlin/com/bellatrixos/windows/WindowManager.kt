package com.bellatrixos.windows

import android.view.View
import android.view.animation.DecelerateInterpolator
import com.bellatrixos.UIFragment
import com.bellatrixos.UIOwlAppWindow
import com.bellatrixos.UIVoice
import com.bellatrixos.app_browser.UIBrowser
import com.bellatrixos.app_explorer.UIExplorer
import com.bellatrixos.app_image_viewer.UIImageViewer
import com.bellatrixos.app_installer.UIInstaller
import com.bellatrixos.app_music_player.UIMusic
import com.bellatrixos.app_notepad.UINotepad
import com.bellatrixos.app_settings.UISettings
import com.bellatrixos.app_terminal.UITerminal
import com.bellatrixos.app_video_player.UIVideo
import com.bellatrixos.app_webapp.UIWebApp
import com.bellatrixos.linux.LINUX_AGENT_TAG_PREFIX
import com.bellatrixos.linux.LINUX_APP_TAG_PREFIX
import com.bellatrixos.linux.UILinuxWindow

/** Registry aplikasi: menerjemahkan tag string menjadi instance [UIFragment]. */
object WindowManager {
    @JvmStatic
    fun a(view: View, z: Boolean) {
        if (z) {
            view.scaleX = 0.95f
            view.scaleY = 0.95f
        } else {
            view.scaleX = 1.05f
            view.scaleY = 1.05f
        }
        view.alpha = 0.95f
        view.animate()
            .scaleX(1.0f).scaleY(1.0f).alpha(1.0f)
            .setDuration(200L)
            .setInterpolator(DecelerateInterpolator(2.0f))
            .start()
    }

    @JvmStatic
    fun b(str: String, str2: String?): UIFragment? = when (str) {
        "UIInstaller" -> UIInstaller()
        "UINotepad" -> UINotepad()
        "UISettings" -> UISettings()
        "UIExplorer" -> UIExplorer()
        "UIMusic" -> UIMusic()
        "UIVideo" -> UIVideo()
        "UIPhotos" -> UIImageViewer()
        "UIBrowser" -> UIBrowser()
        "UITerminal" -> UITerminal()
        "UILinuxWindow" -> UILinuxWindow()
        "UIVoice" -> UIVoice()
        "UIOwlAppWindow" -> UIOwlAppWindow()
        else -> when {
            str.startsWith(LINUX_AGENT_TAG_PREFIX) || str.startsWith(LINUX_APP_TAG_PREFIX) -> UILinuxWindow()
            str2 == null -> null
            str2.startsWith("http") || str2.startsWith("www") -> UIWebApp()
            else -> null
        }
    }
}
