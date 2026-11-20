package com.bellatrixos.windows.icons.models

import android.graphics.drawable.Drawable

/** Satu paket ikon pihak ketiga yang terpasang. */
class IconPackItem(
    @JvmField val icon: Drawable?,
    @JvmField val packageName: String,
    @JvmField val label: String
)
