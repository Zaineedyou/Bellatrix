package com.bellatrixos.wayland

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.TextAttribute
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bellatrixos.theme.Style
import com.bellatrixos.ui.LauncherStyle
import kotlin.concurrent.thread

/** How a modifier on the keybar is held: for the next key, or until tapped again. */
enum class Latch { Off, Once, Locked }

class StickyModifier(val code: Int) {
    @Volatile
    var latch: Latch = Latch.Off
}

/** Translates Android key events and IME text into Wayland keyboard input. Ported from Bellatrix's KeyboardBridge. */
class KeyboardBridge(
    private val wm: LinuxWindowManager,
    private val images: GuestImages,
) {
    var onShortcut: (Int) -> Boolean = { false }
    var flush: () -> Unit = {}

    val ctrl = StickyModifier(Keys.KEY_LEFTCTRL)
    val alt = StickyModifier(Keys.KEY_LEFTALT)
    val shift = StickyModifier(Keys.KEY_LEFTSHIFT)
    private val sticky = listOf(ctrl, alt, shift)
    val latches = mapOf("ctrl" to ctrl, "alt" to alt, "shift" to shift)

    private var swallowUp: Int? = null

    fun handle(event: KeyEvent): Boolean {
        if (event.isMetaPressed && event.action == KeyEvent.ACTION_DOWN && onShortcut(event.keyCode)) return true
        if (event.keyCode in volumeKeys || event.keyCode == KeyEvent.KEYCODE_BACK || KeyEvent.isGamepadButton(event.keyCode)) return false
        val code = Keys.evdev(event) ?: event.scanCode.takeIf { it > 0 } ?: return false
        if (wm.focused == null) return false
        if (!Keys.isModifier(code)) {
            holdImplied(event.metaState)
        } else if (event.action == KeyEvent.ACTION_DOWN) {
            implied -= code
        }
        forward(event, code)
        if (event.action == KeyEvent.ACTION_UP) liftImplied()
        return true
    }

    private fun forward(
        event: KeyEvent,
        code: Int,
    ) {
        if (event.action == KeyEvent.ACTION_UP && code == swallowUp) {
            swallowUp = null
            return
        }
        if (event.action == KeyEvent.ACTION_UP && code !in down) return
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && latched() && !Keys.isModifier(code)) {
            tap(code)
            swallowUp = code
            return
        }
        if (event.repeatCount > 0) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                key(code, false)
                key(code, true)
            }
            return
        }
        when (event.action) {
            KeyEvent.ACTION_DOWN -> key(code, true)
            KeyEvent.ACTION_UP -> key(code, false)
        }
    }

    fun type(text: CharSequence) {
        liftImplied()
        for (cp in text.codePoints().toArray()) {
            val mapped = if (cp <= 0xffff) KeyNames.forChar(cp.toChar()) else null
            if (mapped == null) {
                if (!Character.isISOControl(cp)) wm.server.postInput { typeCodePoint(cp) }
                continue
            }
            val (code, shifted) = mapped
            withLatched {
                if (shifted) key(Keys.KEY_LEFTSHIFT, true)
                key(code, true)
                key(code, false)
                if (shifted) key(Keys.KEY_LEFTSHIFT, false)
            }
        }
    }

    /**
     * An image the keyboard inserts, like a screenshot from Gboard's clipboard: saved into Linux
     * and pasted as its path, which agents attach. A paste rather than typing, since they only
     * take a path that comes as one. [done] runs once it's read.
     */
    fun insertImage(
        uri: Uri,
        mime: String,
        done: () -> Unit,
    ) = insertImages(listOf(uri), mime, done)

    /** Images picked with [pickImages], pasted as [insertImage] does, their paths apart by spaces. */
    fun insertImages(
        uris: List<Uri>,
        mime: String? = null,
        done: () -> Unit = {},
    ) {
        thread(name = "keyboard-image", isDaemon = true) {
            val paths = uris.mapNotNull { images.save(it, mime)?.let(images::guestPath) }
            done()
            if (paths.isEmpty()) return@thread
            wm.server.insertText(paths.joinToString(" "))
            main.post { tap(Keys.KEY_INSERT, Keys.KEY_LEFTSHIFT) }
        }
    }

    /** Android's photo picker for the activity in front, set as it resumes. */
    @Volatile
    var imagePicker: ImagePicker? = null

    /** Opens Android's photo picker; its picks go into the window that had the keyboard. */
    fun pickImages() {
        imagePicker?.launch()
    }

    /** Implemented by each activity that shows the key bar; see LinuxWindowActivity. */
    fun interface ImagePicker {
        fun launch()
    }

    private val main = Handler(Looper.getMainLooper())

    /** Presses and releases [code], with [held] and any latched modifiers held around it. */
    fun tap(
        code: Int,
        held: Int? = null,
    ) = withLatched {
        held?.let { key(it, true) }
        key(code, true)
        key(code, false)
        held?.let { key(it, false) }
    }

    fun press(combo: KeyNames.Combo) {
        val held = combo.held.filter { code -> sticky.none { it.code == code && it.latch != Latch.Off } }
        withLatched {
            held.forEach { key(it, true) }
            key(combo.code, true)
            key(combo.code, false)
            held.asReversed().forEach { key(it, false) }
        }
    }

    fun press(name: String) {
        KeyNames.combo(name)?.let(::press)
    }

    /** null toggles once/off; otherwise "once", "locked" or "off". */
    fun latch(
        name: String,
        how: String? = null,
    ) {
        val m = latches[name] ?: return
        m.latch =
            when (how) {
                "once" -> Latch.Once
                "locked" -> Latch.Locked
                "off" -> Latch.Off
                else -> if (m.latch == Latch.Off) Latch.Once else Latch.Off
            }
    }

    fun clearLatches() = sticky.forEach { it.latch = Latch.Off }

    private fun latched() = sticky.any { it.latch != Latch.Off }

    private inline fun withLatched(block: () -> Unit) {
        val held = sticky.filter { it.latch != Latch.Off }
        held.forEach { key(it.code, true) }
        block()
        held.forEach { key(it.code, false) }
        held.filter { it.latch == Latch.Once }.forEach { it.latch = Latch.Off }
    }

    private val implied = mutableSetOf<Int>()

    private fun holdImplied(metaState: Int) {
        for ((mask, left, right) in impliedModifiers) {
            if (metaState and mask != 0 && left !in down && right !in down) {
                key(left, true)
                implied += left
            }
        }
    }

    private fun liftImplied() = implied.toList().forEach { key(it, false) }

    fun releaseHeld() = down.toList().forEach { key(it, false) }

    private val down = mutableSetOf<Int>()

    private fun key(
        code: Int,
        pressed: Boolean,
    ) {
        if (pressed) down += code
        else {
            down -= code
            implied -= code
        }
        wm.server.postInput { key(code, pressed) }
    }
}

/** An invisible view that owns the soft keyboard and feeds it into [KeyboardBridge]. */
class KeyboardSink(
    context: Context,
    private val bridge: KeyboardViewBridge,
) : View(context) {
    private var softKeyboard = false
    val wanted get() = softKeyboard
    val showing get() = ViewCompat.getRootWindowInsets(this)?.isVisible(WindowInsetsCompat.Type.ime()) == true

    init {
        isFocusable = false
    }

    private var cjk = false
    private val subtypeObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (softKeyboard && cjkKeyboard() != cjk) imm().restartInput(this@KeyboardSink)
            }
        }

    private var composing: CharSequence = ""

    private val flushComposing: () -> Unit = {
        val text = composing
        if (text.isNotEmpty()) {
            composing = ""
            bridge.type(text)
            imm().restartInput(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bridge.flush = flushComposing
        runCatching {
            val uri = Settings.Secure.getUriFor(Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE)
            context.contentResolver.registerContentObserver(uri, false, subtypeObserver)
        }
    }

    override fun onDetachedFromWindow() {
        if (bridge.flush === flushComposing) bridge.flush = {}
        runCatching { context.contentResolver.unregisterContentObserver(subtypeObserver) }
        super.onDetachedFromWindow()
    }

    override fun onCheckIsTextEditor() = softKeyboard

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        cjk = cjkKeyboard()
        outAttrs.inputType = terminalInputType(cjk || LauncherStyle.style[Style.TERMINAL_SUGGESTIONS])
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        outAttrs.contentMimeTypes = arrayOf("image/*")
        return object : BaseInputConnection(this, false) {
            override fun setComposingText(
                text: CharSequence,
                newCursorPosition: Int,
            ): Boolean {
                composing = text
                return true
            }

            override fun finishComposingText(): Boolean {
                bridge.type(composing)
                composing = ""
                return true
            }

            override fun commitText(
                text: CharSequence,
                newCursorPosition: Int,
            ): Boolean {
                composing = ""
                bridge.type(text)
                return true
            }

            override fun deleteSurroundingText(
                beforeLength: Int,
                afterLength: Int,
            ): Boolean {
                repeat(beforeLength.coerceAtLeast(0)) { bridge.tap(Keys.KEY_BACKSPACE) }
                repeat(afterLength.coerceAtLeast(0)) { bridge.tap(Keys.KEY_DELETE) }
                return true
            }

            /**
             * A suggestion tapped that changes letters already typed: backspace over the old
             * text, then type the replacement, since the terminal owns the real buffer.
             */
            override fun replaceText(
                start: Int,
                end: Int,
                text: CharSequence,
                newCursorPosition: Int,
                textAttribute: TextAttribute?,
            ): Boolean {
                composing = ""
                repeat((end - start).coerceAtLeast(0)) { bridge.tap(Keys.KEY_BACKSPACE) }
                bridge.type(text)
                return true
            }

            /** An image from the keyboard's clipboard, saved into Linux and pasted as its path. */
            override fun commitContent(
                info: InputContentInfo,
                flags: Int,
                opts: Bundle?,
            ): Boolean {
                val description = info.description
                val mime = (0 until description.mimeTypeCount).map(description::getMimeType).firstOrNull { it.startsWith("image/") }
                    ?: return false
                if (flags and InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION != 0) {
                    runCatching { info.requestPermission() }.onFailure { return false }
                }
                bridge.insertImage(info.contentUri, mime) { info.releasePermission() }
                return true
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_MULTIPLE && event.keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                    event.characters?.let(bridge::type)
                } else {
                    bridge.handle(event)
                }
                return true
            }
        }
    }

    private fun imm() = context.getSystemService(InputMethodManager::class.java)

    private fun cjkKeyboard(): Boolean {
        val subtype = imm().currentInputMethodSubtype ?: return false
        @Suppress("DEPRECATION")
        val language = subtype.languageTag.ifEmpty { subtype.locale }.substringBefore('-').substringBefore('_')
        return language in cjkLanguages
    }

    fun toggle() = if (softKeyboard) hide() else show()

    fun show() {
        val imm = imm()
        softKeyboard = true
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        imm.restartInput(this)
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hide() {
        if (!softKeyboard) return
        val imm = imm()
        imm.hideSoftInputFromWindow(windowToken, 0)
        softKeyboard = false
        bridge.clearLatches()
        imm.restartInput(this)
        clearFocus()
        isFocusable = false
    }
}

/** The terminal input's type: plain text, or a password field that hides suggestions and takes raw keys. */
internal fun terminalInputType(plain: Boolean) =
    if (plain) {
        InputType.TYPE_CLASS_TEXT
    } else {
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    }

private val cjkLanguages = setOf("zh", "ja", "ko")

private val impliedModifiers =
    listOf(
        Triple(KeyEvent.META_SHIFT_ON, Keys.KEY_LEFTSHIFT, 54),
        Triple(KeyEvent.META_CTRL_ON, Keys.KEY_LEFTCTRL, 97),
        Triple(KeyEvent.META_ALT_ON, Keys.KEY_LEFTALT, 100),
    )

private val volumeKeys = setOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE)
