package com.bellatrixos

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.graphics.toArgb
import com.bellatrixos.ui.Palette
import com.bellatrixos.ui.WidgetAsks
import com.bellatrixos.wayland.ActiveLinuxBackend
import com.bellatrixos.wayland.KeyBarView
import com.bellatrixos.wayland.KeyboardSink
import com.bellatrixos.wayland.KeyboardViewBridge
import com.bellatrixos.wayland.LinuxViewBackend
import com.bellatrixos.wayland.LinuxWindowInputView
import com.bellatrixos.wayland.WindowSurfaceView
import com.bellatrixos.wayland.XdgToplevel

/**
 * Shows one Linux window as its own Android task, with its own Recents card.
 * Pixels come straight from the Wayland compositor into [WindowSurfaceView];
 * Android only supplies input, the keybar and the soft keyboard.
 */
class LinuxWindowActivity : AppCompatActivity() {
    private val asks = WidgetAsks(this)
    private lateinit var backend: LinuxViewBackend
    private var window: XdgToplevel? = null
    private var keyboardHeight = 0

    /** Whether this activity put the shared keybar bridge in place, so only it takes it back. */
    private var bridgeSetByMe = false
    /** The bridge that was installed before this activity took over, restored when it leaves. */
    private var priorKeyBarBridge: com.bellatrixos.wayland.KeyboardViewBridge? = null

    private lateinit var root: FrameLayout
    private lateinit var surface: WindowSurfaceView
    private lateinit var input: LinuxWindowInputView
    private lateinit var sink: KeyboardSink
    private lateinit var keyBar: KeyBarView
    private lateinit var keyBarContainer: FrameLayout

    private val picker =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            window?.let(backend.wm::focus)
            if (sink.isAttachedToWindow && !sink.showing) sink.show()
            if (uris.isNotEmpty()) backend.keyboard.insertImages(uris)
        }

    private val imagePicker =
        { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backend = ActiveLinuxBackend.get(this)
        val window = backend.wm.windowByHandle(intent.data?.lastPathSegment)
        if (window == null) {
            finishAndRemoveTask()
            return
        }
        this.window = window
        setContentView(R.layout.activity_linux_window)

        root = findViewById(R.id.linux_window_root)
        keyBarContainer = findViewById(R.id.linux_keybar_container)

        surface = WindowSurfaceView(this, backend.server, backend.wm, window)
        root.addView(surface, 0, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        input = LinuxWindowInputView(this, backend.wm, window)
        input.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        input.onCursorTap = { if (!sink.showing) sink.show() }
        root.addView(input, 1, input.layoutParams)

        sink = KeyboardSink(this, backend.keyboard)
        root.addView(sink, FrameLayout.LayoutParams(1, 1, Gravity.TOP or Gravity.START))

        keyBar = KeyBarView(this)
        keyBarContainer.addView(keyBar, FrameLayout.LayoutParams(MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            keyboardHeight = (ime - nav).coerceAtLeast(0)
            keyBarContainer.visibility = if (keyboardHeight > 0) View.VISIBLE else View.GONE
            relayout()
            insets
        }

        val name = backend.displayName(window.appId, window.title)
        setTitle(name)
        // The manifest alias already names agent windows; this keeps their logo in Recents even
        // when the task was started through the generic fallback instead of that alias.
        val agent = (application as App).runtime.agentOf(window.appId, name)
        if (agent != null) {
            val iconId = resources.getIdentifier("agent_${agent.bin.replace('-', '_')}", "drawable", packageName)
            if (iconId != 0) {
                @Suppress("DEPRECATION")
                setTaskDescription(
                    ActivityManager.TaskDescription(name, BitmapFactory.decodeResource(resources, iconId), Palette.background.toArgb()),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        backend.screenStarted()
    }

    override fun onResume() {
        super.onResume()
        window?.let(backend.wm::focus)
        // The keybar reads this global holder; whichever window is in front must put its bridge back.
        val holder = KeyBarView.KeyboardBridgeHolder
        if (holder.bridge !== backend.keyboard) {
            priorKeyBarBridge = holder.bridge
            holder.bridge = backend.keyboard
            bridgeSetByMe = true
        } else {
            bridgeSetByMe = false
        }
        backend.keyboard.picker = imagePicker
        asks.resumed((application as App).runtime)
        relayout()
    }

    override fun onPause() {
        super.onPause()
        backend.keyboard.releaseHeld()
        if (backend.keyboard.picker === imagePicker) backend.keyboard.picker = null
        clearKeyBarBridge()
        asks.paused((application as App).runtime)
    }

    override fun onStop() {
        super.onStop()
        backend.screenStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearKeyBarBridge()
        val w = window ?: return
        if (isFinishing && backend.wm.appWindows.contains(w)) backend.wm.close(w)
    }

    /** Takes the keybar bridge back only if this activity set it and nobody has replaced it since. */
    private fun clearKeyBarBridge() {
        if (bridgeSetByMe && KeyBarView.KeyboardBridgeHolder.bridge === backend.keyboard) {
            KeyBarView.KeyboardBridgeHolder.bridge = priorKeyBarBridge
            priorKeyBarBridge = null
            bridgeSetByMe = false
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_ESCAPE && event.isMetaPressed && event.action == KeyEvent.ACTION_DOWN) {
            if (event.repeatCount == 0) finish()
            return true
        }
        if (backend.keyboard.handle(event)) return true
        return super.dispatchKeyEvent(event)
    }

    private fun relayout() = root.post {
        val barHeight = if (keyBarContainer.visibility == View.VISIBLE) keyBarContainer.height else 0
        val visible = (root.height - keyboardHeight - barHeight).coerceAtLeast(1)
        input.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, visible)
        input.requestLayout()
        keyBarContainer.translationY = -keyboardHeight.toFloat()
        surface.visibleHeight = visible
    }

    private companion object {
        const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
    }
}

/** Recents card icon for an OWL app or terminal without its own: initials in the theme colors. */
internal fun taskIcon(
    name: String,
    background: Int,
    accent: Int,
): Bitmap {
    val size = 192
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(background)
    val words = name.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
    val initials =
        when {
            words.size >= 2 -> "${words[0][0]}${words[1][0]}".uppercase()
            words.isNotEmpty() -> words[0].take(2).replaceFirstChar(Char::uppercase)
            else -> "◆"
        }
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = size * 0.42f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    val y = size / 2f - (paint.descent() + paint.ascent()) / 2
    canvas.drawText(initials, size / 2f, y, paint)
    return bmp
}
