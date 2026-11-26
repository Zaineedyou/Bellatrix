package com.bellatrixos.app_explorer

import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.app_explorer.SyntaxHighlighter.Kind
import com.bellatrixos.app_explorer.files.Files
import com.bellatrixos.windows.dialogs.UIDialog
import com.bellatrixos.windows.files.FileIntent
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * The editor of the Explorer window. It covers the whole window below the title bar, so Explorer stays the window and
 * the file gets all of it, and gives the window back on close. Code is coloured for its language (by extension, or by
 * its first line for a file nobody named) and shown unwrapped with line numbers; prose is wrapped. Line endings, a
 * UTF-8 byte order mark and the file's own encoding (UTF-8, else a lossless single-byte one) are kept, so saving never
 * rewrites what was not edited. A file that is not text, or too big to edit in a field, goes to the system's apps.
 */
class ExplorerEditor(
    private val explorer: UIExplorer,
    private val host: FrameLayout,
    private val topInset: Int,
    val file: File,
) {
    private class SyntaxSpan(color: Int) : ForegroundColorSpan(color)

    private val context: Context = host.context
    private val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    private val textColor = ContextCompat.getColor(context, R.color.text_default)

    private val root = LinearLayout(context)
    private val title = TextView(context)
    private val languageLabel = TextView(context)
    private val wrapButton = TextView(context)
    private val saveButton = TextView(context)
    private val vScroll = ScrollView(context)
    private val hScroll = HorizontalScrollView(context)
    private val row = LinearLayout(context)
    private val gutter = TextView(context)
    private val edit = EditText(context)

    private var language = FileKinds.languageOf(file.name)
    private var charset: Charset = Charsets.UTF_8
    private var bom = false
    private var crlf = false
    private var wrap = false
    private var loading = true
    private var dirty = false
    private var closed = false
    private var lineCount = 0

    /** A big file or one with very long lines (minified code): no colours, no line numbers, always wrapped. */
    private var lite = false
    private var wrapLocked = false

    /** Counts every edit, so colours computed for older text are thrown away instead of applied. */
    private var textVersion = 0
    private var tokens: List<SyntaxHighlighter.Token> = emptyList()
    private val applied = ArrayList<SyntaxSpan>()
    private val rehighlight = Runnable { highlight() }
    private val repaint = Runnable { paintVisible() }

    private class Loaded(val text: String, val longestLine: Int)

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    fun show() {
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(ContextCompat.getColor(context, R.color.explorer_bg))
        // The folder behind the editor stays out of reach of touches while it is open.
        root.isClickable = true
        root.isFocusable = true

        val bar = LinearLayout(context)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        val back = TextView(context)
        back.text = "‹"
        back.setTextColor(textColor)
        back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        back.gravity = Gravity.CENTER
        back.setPadding(dp(14), 0, dp(14), dp(2))
        back.setOnClickListener { requestClose(null) }
        title.setTextColor(textColor)
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        title.maxLines = 1
        title.ellipsize = TextUtils.TruncateAt.MIDDLE
        languageLabel.setTextColor(withAlpha(textColor, 0x90))
        languageLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        languageLabel.setPadding(dp(8), 0, dp(8), 0)
        wrapButton.text = "Wrap"
        styleButton(wrapButton)
        wrapButton.setOnClickListener { applyWrap(!wrap) }
        saveButton.text = context.getString(R.string.context_menu_save)
        styleButton(saveButton)
        saveButton.setOnClickListener { save(null) }
        bar.addView(back, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        bar.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(languageLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        bar.addView(wrapButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        bar.addView(saveButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)))

        val divider = View(context)
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.app_dialog_border_light))
        root.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))

        gutter.typeface = Typeface.MONOSPACE
        gutter.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
        gutter.setTextColor(withAlpha(textColor, 0x70))
        gutter.gravity = Gravity.END or Gravity.TOP
        gutter.setPadding(dp(10), dp(8), dp(8), dp(8))
        edit.setTextColor(textColor)
        edit.typeface = Typeface.MONOSPACE
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
        edit.background = null
        edit.gravity = Gravity.TOP or Gravity.START
        edit.setPadding(dp(8), dp(8), dp(12), dp(8))
        edit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        edit.isEnabled = false
        edit.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (loading) return
                    textVersion++
                    // Token positions are for the text before this edit; scrolling must not paint with them.
                    tokens = emptyList()
                    if (!dirty) {
                        dirty = true
                        refreshBar()
                    }
                    updateGutter()
                    scheduleHighlight(HIGHLIGHT_DELAY)
                }
            },
        )
        edit.setOnKeyListener { _, _, event -> handleKey(event.keyCode, event) }
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(gutter, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        row.addView(edit, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        vScroll.isFillViewport = true
        hScroll.isFillViewport = true
        // Colours are only put on what is on screen, so scrolling repaints after it settles.
        vScroll.setOnScrollChangeListener { _, _, _, _, _ -> schedulePaint() }
        root.addView(vScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        params.topMargin = topInset
        host.addView(root, params)
        applyWrap(false)
        refreshBar()
        load()
    }

    private fun styleButton(button: TextView) {
        button.setTextColor(textColor)
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        button.setPadding(dp(12), dp(8), dp(12), dp(8))
    }

    private fun withAlpha(color: Int, alpha: Int) = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    /** Wrapped text fills the width and has no line numbers (they would not match); code scrolls sideways. */
    private fun applyWrap(on: Boolean) {
        wrap = on
        (row.parent as? ViewGroup)?.removeView(row)
        hScroll.removeAllViews()
        vScroll.removeAllViews()
        edit.setHorizontallyScrolling(!on)
        gutter.visibility = if (on || lite) View.GONE else View.VISIBLE
        if (on) {
            vScroll.addView(row, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        } else {
            hScroll.addView(row, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
            vScroll.addView(hScroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        wrapButton.alpha = if (on) 1f else 0.5f
    }

    private fun refreshBar() {
        title.text = if (dirty) "• ${file.name}" else file.name
        languageLabel.text = language.label
        saveButton.isEnabled = dirty
        saveButton.alpha = if (dirty) 1f else 0.4f
    }

    private fun updateGutter() {
        if (lite) return
        val text = edit.text
        var lines = 1
        for (i in 0 until text.length) if (text[i] == '\n') lines++
        if (lines == lineCount) return
        lineCount = lines
        val numbers = StringBuilder(lines * 5)
        for (n in 1..lines) {
            if (n > 1) numbers.append('\n')
            numbers.append(n)
        }
        gutter.text = numbers
    }

    private fun load() {
        AppExecutors.b().execute {
            val result = runCatching { read() }
            AppExecutors.c().post {
                if (closed) return@post
                val loaded = result.getOrNull()
                if (loaded == null) {
                    fallBack()
                    return@post
                }
                // An unknown name is told by its first line; the language also decides whether to wrap.
                language = FileKinds.languageOf(file.name, loaded.text.substringBefore('\n'))
                lite = loaded.longestLine > LONG_LINE || loaded.text.length > MAX_FULL
                wrapLocked = loaded.longestLine > LONG_LINE
                wrapButton.visibility = if (wrapLocked) View.GONE else View.VISIBLE
                loading = true
                try {
                    edit.setText(loaded.text)
                } catch (_: OutOfMemoryError) {
                    loading = false
                    fallBack()
                    return@post
                }
                edit.isEnabled = true
                loading = false
                applyWrap(wrapLocked || !language.code)
                refreshBar()
                updateGutter()
                scheduleHighlight(0)
                edit.requestFocus()
            }
        }
    }

    /** The file as text, or null when it is not text or too big to edit here. */
    private fun read(): Loaded? {
        if (!file.isFile || file.length() > MAX_BYTES) return null
        var bytes = file.readBytes()
        for (i in 0 until minOf(bytes.size, PROBE)) if (bytes[i] == 0.toByte()) return null
        bom = bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        if (bom) bytes = bytes.copyOfRange(3, bytes.size)
        val text =
            try {
                charset = Charsets.UTF_8
                Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString()
            } catch (_: CharacterCodingException) {
                // Not UTF-8: ISO-8859-1 maps every byte to one char and back, so saving changes nothing else.
                charset = Charsets.ISO_8859_1
                bom = false
                String(bytes, Charsets.ISO_8859_1)
            }
        crlf = text.contains("\r\n")
        val plain = if (crlf) text.replace("\r\n", "\n") else text
        var longest = 0
        var run = 0
        for (c in plain) {
            if (c == '\n') {
                if (run > longest) longest = run
                run = 0
            } else {
                run++
            }
        }
        return Loaded(plain, maxOf(longest, run))
    }

    private fun fallBack() {
        dismiss()
        FileIntent.a(context, file)
    }

    private fun scheduleHighlight(delay: Long) {
        val handler = AppExecutors.c()
        handler.removeCallbacks(rehighlight)
        handler.postDelayed(rehighlight, delay)
    }

    private fun colorOf(kind: Kind): Int {
        val argb: Long = if (dark) darkColor(kind) else lightColor(kind)
        return argb.toInt()
    }

    private fun darkColor(kind: Kind): Long =
        when (kind) {
            Kind.KEYWORD -> 0xFF569CD6
            Kind.STRING -> 0xFFCE9178
            Kind.COMMENT -> 0xFF6A9955
            Kind.NUMBER -> 0xFFB5CEA8
            Kind.TYPE -> 0xFF4EC9B0
            Kind.KEY -> 0xFF9CDCFE
            Kind.TAG -> 0xFF569CD6
            Kind.ATTRIBUTE -> 0xFF9CDCFE
            Kind.HEADING -> 0xFF569CD6
            Kind.ADDED -> 0xFFB5CEA8
            Kind.REMOVED -> 0xFFF48771
        }

    private fun lightColor(kind: Kind): Long =
        when (kind) {
            Kind.KEYWORD -> 0xFF0000FF
            Kind.STRING -> 0xFFA31515
            Kind.COMMENT -> 0xFF008000
            Kind.NUMBER -> 0xFF098658
            Kind.TYPE -> 0xFF267F99
            Kind.KEY -> 0xFF0451A5
            Kind.TAG -> 0xFF800000
            Kind.ATTRIBUTE -> 0xFFE50000
            Kind.HEADING -> 0xFF800000
            Kind.ADDED -> 0xFF098658
            Kind.REMOVED -> 0xFFA31515
        }

    /** Splits the text into tokens on a worker; what is on screen is then painted from them. */
    private fun highlight() {
        if (closed || loading || lite || language.style == SyntaxStyle.NONE) return
        val editable = edit.text
        if (editable.length > MAX_HIGHLIGHT) {
            tokens = emptyList()
            clearColours(editable)
            return
        }
        val snapshot = editable.toString()
        val version = textVersion
        AppExecutors.b().execute {
            val found = SyntaxHighlighter.tokens(snapshot, language)
            AppExecutors.c().post {
                if (closed || version != textVersion) return@post
                tokens = found
                paintVisible()
            }
        }
    }

    private fun schedulePaint() {
        if (tokens.isEmpty()) return
        val handler = AppExecutors.c()
        handler.removeCallbacks(repaint)
        handler.postDelayed(repaint, PAINT_DELAY)
    }

    /** Puts colours on the lines in view and a screenful either side, so spans stay few however long the file is. */
    private fun paintVisible() {
        if (closed || loading) return
        val layout = edit.layout
        if (layout == null) {
            edit.post { paintVisible() }
            return
        }
        val editable = edit.text
        clearColours(editable)
        if (tokens.isEmpty() || layout.lineCount == 0) return
        val margin = vScroll.height
        val top = maxOf(0, vScroll.scrollY - margin)
        val bottom = vScroll.scrollY + vScroll.height + margin
        val from = layout.getLineStart(layout.getLineForVertical(top))
        val to = layout.getLineEnd(layout.getLineForVertical(bottom).coerceAtMost(layout.lineCount - 1))
        // Tokens never overlap, so their ends are in order too: the first one still reaching [from] is found by halving.
        var low = 0
        var high = tokens.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (tokens[mid].end > from) high = mid else low = mid + 1
        }
        var count = 0
        var i = low
        while (i < tokens.size && count < MAX_SPANS) {
            val token = tokens[i]
            if (token.start >= to) break
            if (token.end <= editable.length && token.start < token.end) {
                val span = SyntaxSpan(colorOf(token.kind))
                editable.setSpan(span, token.start, token.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                applied.add(span)
                count++
            }
            i++
        }
    }

    /** Takes the last span first, which is cheap; removing from the front of thousands would not be. */
    private fun clearColours(editable: Editable) {
        for (i in applied.indices.reversed()) editable.removeSpan(applied[i])
        applied.clear()
    }

    /** Writes the file; [then] runs on the main thread once it is saved. */
    fun save(then: Runnable?) {
        if (loading || !edit.isEnabled) return
        var text = edit.text.toString()
        if (crlf) text = text.replace("\n", "\r\n")
        val body = text.toByteArray(charset)
        val bytes = if (bom) byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + body else body
        AppExecutors.b().execute {
            val ok = runCatching { file.writeBytes(bytes) }.isSuccess
            AppExecutors.c().post {
                if (ok) {
                    dirty = false
                    if (!closed) refreshBar()
                    then?.run()
                } else {
                    Files.a(context, context.getString(R.string.toast_file_failed_save))
                }
            }
        }
    }

    /**
     * Closes the editor. Returns true when it closed at once; false when there are unsaved changes, in which case a
     * dialog asks first and [then] runs after the editor has closed (not at all if the user backs out).
     */
    fun requestClose(then: Runnable?): Boolean {
        if (!dirty) {
            dismiss()
            then?.run()
            return true
        }
        val dialog = UIDialog(context, file.name, context.getString(R.string.dialog_app_unsaved_subtitle))
        dialog.z = context.getString(R.string.context_menu_save)
        dialog.A = context.getString(R.string.dialog_app_unsaved_action_no)
        dialog.C = DialogInterface.OnClickListener { _, _ ->
            save {
                dismiss()
                then?.run()
            }
        }
        dialog.D = DialogInterface.OnClickListener { _, _ ->
            dismiss()
            then?.run()
        }
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        return false
    }

    /** Ctrl+S saves and Esc closes; true when the key was one of those. */
    fun handleKey(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        return when {
            event.isCtrlPressed && keyCode == KeyEvent.KEYCODE_S -> {
                save(null)
                true
            }
            keyCode == KeyEvent.KEYCODE_ESCAPE -> {
                requestClose(null)
                true
            }
            else -> false
        }
    }

    private fun dismiss() {
        if (closed) return
        closed = true
        AppExecutors.c().removeCallbacks(rehighlight)
        AppExecutors.c().removeCallbacks(repaint)
        host.removeView(root)
        explorer.editorClosed(this)
    }

    private companion object {
        /** Past this a file is not edited in a text field. */
        const val MAX_BYTES = 1L * 1024 * 1024

        /** Past this many characters a file is shown plain: no colours, no line numbers, wrapped. */
        const val MAX_FULL = 250_000

        /** A line longer than this (minified code) makes the file plain and wrapped: one very wide line stalls and crashes drawing. */
        const val LONG_LINE = 800

        /** Past this many characters nothing is split into tokens. */
        const val MAX_HIGHLIGHT = 250_000

        /** At most this many colour spans are on the text at once. */
        const val MAX_SPANS = 4000

        const val HIGHLIGHT_DELAY = 350L
        const val PAINT_DELAY = 80L

        /** How much of a file is looked at for a zero byte, which text never has. */
        const val PROBE = 8000

        const val TEXT_SP = 13f
    }
}
