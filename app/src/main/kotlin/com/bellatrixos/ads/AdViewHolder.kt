package com.bellatrixos.ads

import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.google.android.gms.ads.nativead.NativeAdView

class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    @JvmField val u: NativeAdView = view.findViewById(R.id.native_ad_view)

    init {
        u.setIconView(view.findViewById<ImageView>(R.id.icon))
        u.setHeadlineView(view.findViewById<TextView>(R.id.primary))
        u.setAdvertiserView(view.findViewById<TextView>(R.id.advertiser))
        u.setStarRatingView(view.findViewById<RatingBar>(R.id.rating_bar))
    }
}
