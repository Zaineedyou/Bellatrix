package com.bellatrixos.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/** Pembungkus Google UMP untuk persetujuan iklan. */
class ConsentManager {
    @JvmField var f4636a: Boolean = false

    companion object {
        @JvmField var c: ConsentManager? = null

        @JvmStatic
        fun a(context: Context): Boolean =
            UserMessagingPlatform.getConsentInformation(context).consentStatus ==
                ConsentInformation.ConsentStatus.OBTAINED
    }

    fun b(activity: Activity, runnable: Runnable) {
        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false).build()
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            params,
            ConsentInformation.OnConsentInfoUpdateSuccessListener {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    f4636a = true
                    if (info.canRequestAds()) runnable.run()
                }
            },
            ConsentInformation.OnConsentInfoUpdateFailureListener { f4636a = true }
        )
        if (info.canRequestAds()) runnable.run()
    }
}
