package com.bellatrixos.app_explorer.drives

import android.view.View
import com.bellatrixos.AppExecutors
import com.bellatrixos.startmenu.StartMenuRecommend
import com.bellatrixos.widgets.FilesRecyclerView
import com.google.android.gms.ads.nativead.NativeAd

/** Listener iklan native sekaligus aksi tekan area kosong. */
class c(@JvmField val u: View) : NativeAd.OnNativeAdLoadedListener,
    FilesRecyclerView.OnEmptyPressListener {
    override fun a() {
        u.requestFocus()
    }

    override fun onNativeAdLoaded(nativeAd: NativeAd) {
        StartMenuRecommend.f4787a?.destroy()
        StartMenuRecommend.f4787a = nativeAd
        StartMenuRecommend.b = true
        // Inflasi native ad view (androidx.core.content.res.a) tidak tersedia di port Kotlin.
        AppExecutors.c().post { u.invalidate() }
    }
}
