package com.bellatrixos.ads

import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.bellatrixos.R
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/** Binding iklan native ke layout kartu "recommended" di Start Menu. */
object AdManager {
    @JvmStatic
    fun a(view: View?, ad: NativeAd?) {
        if (view == null || ad == null) return
        val holder = AdViewHolder(view)
        view.findViewById<View?>(R.id.recommendedDummy)?.visibility = View.GONE

        val nativeAdView: NativeAdView = holder.u
        nativeAdView.visibility = View.VISIBLE

        ad.headline?.let { headline ->
            nativeAdView.headlineView?.visibility = View.VISIBLE
            (nativeAdView.headlineView as? TextView)?.text = headline
        }

        val icon = nativeAdView.iconView as? ImageView
        if (ad.icon != null) {
            icon?.setImageDrawable(ad.icon?.drawable)
            nativeAdView.iconView?.visibility = View.VISIBLE
        } else {
            icon?.setBackgroundResource(R.drawable.ui_startmenu_ic_ad_placeholder)
        }

        val rating = ad.starRating
        if (rating != null) {
            nativeAdView.starRatingView?.visibility = View.VISIBLE
            nativeAdView.advertiserView?.visibility = View.INVISIBLE
            (nativeAdView.starRatingView as? RatingBar)?.rating = rating.toFloat()
        } else {
            nativeAdView.starRatingView?.visibility = View.GONE
            ad.advertiser?.let {
                nativeAdView.advertiserView?.visibility = View.VISIBLE
                (nativeAdView.advertiserView as? TextView)?.text = it
            }
        }

        nativeAdView.setNativeAd(ad)
    }
}
