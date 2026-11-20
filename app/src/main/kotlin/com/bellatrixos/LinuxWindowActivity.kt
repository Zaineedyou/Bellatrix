package com.bellatrixos

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bellatrixos.linux.LinuxRuntime
import com.bellatrixos.wayland.KeyBarView
import com.bellatrixos.wayland.KeyboardBridge
import com.bellatrixos.wayland.KeyboardSink
import com.bellatrixos.wayland.LinuxWindowInputView
import com.bellatrixos.wayland.WindowSurfaceView
import com.bellatrixos.wayland.XdgToplevel

/**
 * Shows one Linux window as its own Android task, with its own Recents card.
 * Pixels come straight from the Wayland compositor into [WindowSurfaceView];
 * Android only supplies input, the keybar and the soft keyboard.
 */
class LinuxWindowActivity : AppCompatActivity() {
    private lateinit var runtime: LinuxRuntime
    private var window: XdgToplevel? = null
    private var keyboardHeight = 0

    private lateinit var root: FrameLayout
    private lateinit var surface: WindowSurfaceView
    private lateinit var input: LinuxWindowInputView
    private lateinit var sink: KeyboardSink
    private lateinit var keyBar: KeyBarView
    private lateinit var keyBarContainer: FrameLayout
    private lateinit var toggle: ImageView

    private val picker =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            window?.let(runtime.wm::focus)
            if (sink.isAttachedToWindow && !sink.showing) sink.show()
            if (uris.isNotEmpty()) runtime.keyboard.insertImages(uris)
        }

    private val imagePicker =
        KeyboardBridge.ImagePicker {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = LinuxRuntime.get(this)
        val window = runtime.wm.windowByHandle(intent.data?.lastPathSegment)
        if (window == null) {
            finishAndRemoveTask()
            return
        }
        this.window = window
        setContentView(R.layout.activity_linux_window)

        root = findViewById(R.id.linux_window_root)
        keyBarContainer = findViewById(R.id.linux_keybar_container)
        toggle = findViewById(R.id.linux_keyboard_toggle)
        toggle.setImageTintList(ContextCompat.getColorStateList(this, R.color.linux_key_foreground))

        surface = WindowSurfaceView(this, runtime, window)
        root.addView(surface, 0, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        input = LinuxWindowInputView(this, runtime.wm, window)
        input.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        input.onCursorTap = { if (!sink.showing) sink.show() }
        root.addView(input, 1, input.layoutParams)

        sink = KeyboardSink(this, runtime.keyboard)
        root.addView(sink, FrameLayout.LayoutParams(1, 1, Gravity.TOP or Gravity.START))

        keyBar = KeyBarView(this)
        KeyBarView.KeyboardBridgeHolder.bridge = runtime.keyboard
        keyBarContainer.addView(keyBar, FrameLayout.LayoutParams(MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        toggle.setOnClickListener { sink.toggle() }

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            keyboardHeight = (ime - nav).coerceAtLeast(0)
            keyBarContainer.visibility = if (keyboardHeight > 0) View.VISIBLE else View.GONE
            relayout()
            insets
        }

        setTitle(runtime.displayName(window.appId, window.title))
    }

    override fun onStart() {
        super.onStart()
        runtime.screenStarted()
    }

    override fun onResume() {
        super.onResume()
        window?.let(runtime.wm::focus)
        runtime.keyboard.imagePicker = imagePicker
        relayout()
    }

    override fun onPause() {
        super.onPause()
        runtime.keyboard.releaseHeld()
        if (runtime.keyboard.imagePicker === imagePicker) runtime.keyboard.imagePicker = null
    }

    override fun onStop() {
        super.onStop()
        runtime.screenStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        KeyBarView.KeyboardBridgeHolder.bridge = null
        val w = window ?: return
        if (isFinishing && runtime.wm.appWindows.contains(w)) runtime.wm.close(w)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_ESCAPE && event.isMetaPressed && event.action == KeyEvent.ACTION_DOWN) {
            if (event.repeatCount == 0) finish()
            return true
        }
        if (runtime.keyboard.handle(event)) return true
        return super.dispatchKeyEvent(event)
    }

    private fun relayout() = root.post {
        val barHeight = if (keyBarContainer.visibility == View.VISIBLE) keyBarContainer.height else 0
        val visible = (root.height - keyboardHeight - barHeight).coerceAtLeast(1)
        input.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, visible)
        input.requestLayout()
        keyBarContainer.translationY = -keyboardHeight.toFloat()
        toggle.translationY = -keyboardHeight.toFloat()
        surface.visibleHeight = visible
    }

    private companion object {
        const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
    }
}
