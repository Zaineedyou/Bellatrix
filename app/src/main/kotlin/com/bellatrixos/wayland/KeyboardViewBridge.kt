package com.bellatrixos.wayland

import android.net.Uri
import android.view.KeyEvent

/** The keyboard operations an Android Linux-window view needs, independent of which runtime owns the compositor. */
interface KeyboardViewBridge {
    /** Flushes text the soft keyboard is still composing; set by each [KeyboardSink]. */
    var flush: () -> Unit

    /** Raised by the keybar's image key; set by the activity showing the keybar. */
    var picker: (() -> Unit)?

    fun handle(event: KeyEvent): Boolean

    fun type(text: CharSequence)

    fun tap(
        code: Int,
        held: Int? = null,
    )

    fun insertImage(
        uri: Uri,
        mime: String,
        done: () -> Unit,
    )

    fun insertImages(uris: List<Uri>)

    fun pickImages() = picker?.invoke()

    fun press(combo: String)

    fun latch(
        name: String,
        how: String? = null,
    )

    fun clearLatches()

    fun releaseHeld()
}
