package com.bellatrixos.wayland

import android.content.Context
import android.net.Uri
import android.view.KeyEvent
import com.bellatrixos.App
import com.bellatrixos.Runtime
import com.bellatrixos.linux.LinuxRuntime
import com.bellatrixos.storage.LauncherData
import com.bellatrixos.ui.KeyboardBridge as OwlKeyboardBridge
import com.bellatrixos.ui.Latch as OwlLatch
import com.bellatrixos.widgets.lang.KeyNames

/**
 * The active Linux backend for an Android window view. Only one runtime owns the
 * compositor in a process: the legacy one for the classic home, the full OWL one
 * when the flex/box/anchor home is enabled. Views use this instead of reaching
 * for [LinuxRuntime] directly, so a window is always found in the WM that made it.
 */
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

private class LegacyKeyboardBridge(
    private val keyboard: KeyboardBridge,
) : KeyboardViewBridge {
    override var flush: () -> Unit
        get() = keyboard.flush
        set(value) { keyboard.flush = value }

    override var picker: (() -> Unit)? = null

    override val latches get() = keyboard.latches.mapValues { it.value.latch }

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

    override fun press(combo: String) = keyboard.press(combo)

    override fun latch(
        name: String,
        how: String?,
    ) = keyboard.latch(name, how)

    override fun clearLatches() = keyboard.clearLatches()

    override fun releaseHeld() = keyboard.releaseHeld()
}

private class OwlKeyboardBridge(
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
        KeyNames.combo(combo)?.let(keyboard::press)
    }

    override fun latch(
        name: String,
        how: String?,
    ) = keyboard.latch(name, how)

    override fun clearLatches() = keyboard.clearLatches()

    override fun releaseHeld() = keyboard.releaseHeld()
}

private class LegacyViewBackend(
    private val runtime: LinuxRuntime,
) : LinuxViewBackend {
    override val server get() = runtime.server
    override val wm get() = runtime.wm
    override val keyboard = LegacyKeyboardBridge(runtime.keyboard)

    override fun screenStarted() = runtime.screenStarted()
    override fun screenStopped() = runtime.screenStopped()
    override fun displayName(appId: String, title: String) = runtime.displayName(appId, title)
}

private class OwlViewBackend(
    private val runtime: Runtime,
) : LinuxViewBackend {
    override val server get() = runtime.server
    override val wm get() = runtime.wm
    override val keyboard = OwlKeyboardBridge(runtime.keyboard)

    override fun screenStarted() = runtime.screenStarted()
    override fun screenStopped() = runtime.screenStopped()
    override fun displayName(appId: String, title: String) = runtime.displayName(appId, title)
}

/** Resolves the one runtime that owns Linux for this process. */
object ActiveLinuxBackend {
    fun get(context: Context): LinuxViewBackend {
        val app = context.applicationContext as App
        return if (LauncherData.useOwlHome(context)) OwlViewBackend(app.runtime) else LegacyViewBackend(LinuxRuntime.get(context))
    }
}
