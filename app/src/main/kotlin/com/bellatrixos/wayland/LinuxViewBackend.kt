package com.bellatrixos.wayland

import android.content.Context
import android.net.Uri
import android.view.KeyEvent
import com.bellatrixos.App
import com.bellatrixos.Runtime
import com.bellatrixos.ui.KeyboardBridge as OwlKeyboardBridge
import com.bellatrixos.ui.Latch as OwlLatch
import com.bellatrixos.widgets.lang.KeyNames as OwlKeyNames

/** The one Linux backend every Android window view talks to: the launcher's full [Runtime]. */
interface LinuxViewBackend {
    val server: WaylandServer
    val wm: WindowViewManager
    val keyboard: KeyboardViewBridge

    fun screenStarted()
    fun screenStopped()
    fun displayName(
        appId: String,
        title: String,
    ): String
}

private class RuntimeKeyboardBridge(
    private val keyboard: OwlKeyboardBridge,
) : KeyboardViewBridge {
    override var flush: () -> Unit
        get() = keyboard.flush
        set(value) { keyboard.flush = value }

    override var picker: (() -> Unit)? = null

    override val latches get() = keyboard.latches.mapValues { (_, m) -> when (m.latch) {
        OwlLatch.Off -> Latch.Off
        OwlLatch.Once -> Latch.Once
        OwlLatch.Locked -> Latch.Locked
    } }

    override fun handle(event: KeyEvent) = keyboard.handle(event)

    override fun type(text: CharSequence) = keyboard.type(text)

    override fun tap(
        code: Int,
        held: Int?,
    ) = keyboard.tap(code, held)

    override fun insertImage(
        uri: Uri,
        mime: String,
        done: () -> Unit,
    ) = keyboard.insertImage(uri, mime, done)

    override fun insertImages(uris: List<Uri>) = keyboard.insertImages(uris)

    override fun press(combo: String) {
        OwlKeyNames.combo(combo)?.let(keyboard::press)
    }

    override fun latch(
        name: String,
        how: String?,
    ) = keyboard.latch(name, how)

    override fun clearLatches() = keyboard.clearLatches()

    override fun releaseHeld() = keyboard.releaseHeld()
}

private class RuntimeViewBackend(
    private val runtime: Runtime,
) : LinuxViewBackend {
    override val server get() = runtime.server
    override val wm get() = runtime.wm
    override val keyboard = RuntimeKeyboardBridge(runtime.keyboard)

    override fun screenStarted() = runtime.screenStarted()
    override fun screenStopped() = runtime.screenStopped()
    override fun displayName(appId: String, title: String) = runtime.displayName(appId, title)
}

/** Resolves the single runtime that owns Linux for this process. */
object ActiveLinuxBackend {
    fun get(context: Context): LinuxViewBackend = RuntimeViewBackend((context.applicationContext as App).runtime)
}
