package com.bellatrixos.startmenu

import android.content.Context
import android.os.RemoteException
import android.util.Log
import android.view.View
import com.bellatrixos.AppExecutors
import com.bellatrixos.app_explorer.drives.c
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError

/** Memuat iklan native Start Menu; gagal dimuat dijadwalkan ulang (maks. 3x). */
class l(context: Context, view: View, i2: Int, i3: Int) : Runnable {
    @JvmField val u: Int = i3
    @JvmField val v: Context = context
    @JvmField val w: View = view
    @JvmField val x: Int = i2

    override fun run() {
        if (u != 0) {
            AppExecutors.b().execute(l(v, w, x + 1, 0))
            return
        }
        try {
            val builder = AdLoader.Builder(v, AD_UNIT_ID)
            try {
                builder.forNativeAd(c(w))
            } catch (e: RemoteException) {
                // zzo.g("Failed to add google native ad listener", e) pada bytecode.
                Log.w(TAG, "Failed to add google native ad listener", e)
            }
            builder.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (x >= MAX_RETRIES) return
                    AppExecutors.c().postDelayed(
                        l(v, w, x, 1),
                        (x + 1) * RETRY_DELAY_MS
                    )
                }
            })
            builder.build().loadAd(AdRequest.Builder().build())
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val TAG = "StartMenuRecommend"
        const val AD_UNIT_ID = "ca-app-pub-6770233748632667/1980280201"
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 4000L
    }
}
