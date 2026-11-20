package com.bellatrixos.app_browser.utils

import android.content.ClipboardManager
import com.bellatrixos.AppExecutors

/** Menandai perubahan clipboard lalu membersihkan flag setelah 2 detik. */
class b : ClipboardManager.OnPrimaryClipChangedListener {
    override fun onPrimaryClipChanged() {
        Clipboard.f4649a = true
        AppExecutors.c().postDelayed({ Clipboard.f4649a = false }, 2000L)
    }
}
