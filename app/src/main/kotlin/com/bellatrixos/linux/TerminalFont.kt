package com.bellatrixos.linux

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.bellatrixos.R
import com.bellatrixos.wayland.XdgToplevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.WeakHashMap
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The font foot draws text in. Ported from Bellatrix's TerminalFont with Compose
 * state replaced by ordinary fields and Bellatrix SharedPreferences: foot.ini includes
 * [PATH], so every terminal gets the user's family at a size following Android's
 * display density and font size.
 */
class TerminalFont(
    context: Context,
    private val proot: Proot,
) {
    private val prefs = context.getSharedPreferences("bellatrix_linux", Context.MODE_PRIVATE)

    /** The monospace family foot uses; JetBrains Mono is installed by the rootfs setup. */
    @Volatile
    var family: String = prefs.getString(FAMILY, "JetBrains Mono") ?: "JetBrains Mono"
        private set

    /** An em in dp before Android's font size; more means bigger letters, fewer columns. */
    val em: Float get() = TerminalText.of(prefs.getString(TEXT, null)).em

    @Volatile
    var families: List<String> = listOf(family)
        private set

    /** The point size new terminals open at. */
    @Volatile
    var size: Double = DEFAULT_SIZE
        private set

    private var scale = 1
    private var density = 0f
    private var fontScale = 1f
    private var widthPx = 0

    private val ownSize = HashSet<String>()
    private val shown = WeakHashMap<XdgToplevel, Double>()
    private var written: String? = null

    fun setFamily(value: String) {
        family = value
        families = (families + value).distinct().sorted()
        prefs.edit().putString(FAMILY, value).apply()
        write()
    }

    fun setText(value: TerminalText) {
        prefs.edit().putString(TEXT, value.id).apply()
        write()
    }

    /** Applies both style.toml values in one write, avoiding two foot.ini rewrites per style change. */
    fun sync(
        family: String,
        text: TerminalText,
    ) {
        val familyChanged = this.family != family
        if (familyChanged) {
            this.family = family
            families = (families + family).distinct().sorted()
        }
        val textChanged = prefs.getString(TEXT, null) != text.id
        if (familyChanged || textChanged) {
            prefs
                .edit()
                .putString(FAMILY, family)
                .putString(TEXT, text.id)
                .apply()
        }
        write()
    }

    fun update(
        density: Float,
        fontScale: Float,
        scale: Int,
        widthPx: Int,
    ) {
        this.density = density
        this.fontScale = fontScale
        this.scale = scale
        this.widthPx = widthPx
        write()
    }

    suspend fun refreshFamilies() {
        val found =
            withContext(Dispatchers.IO) {
                sortedSetOf<String>().also { found ->
                    runCatching { proot.run(LIST_FAMILIES) { it.trim().takeIf(::isTextFont)?.let(found::add) } }
                        .onFailure { Log.w(TAG, "can't list fonts", it) }
                }
            }
        families = (found + family).sorted()
    }

    fun optionFor(
        appId: String,
        command: String?,
    ): String {
        val fit = if (command == "btop") fitting(BTOP_COLUMNS, widthPx, scale, size) else size
        if (fit >= size) {
            ownSize -= appId
            return ""
        }
        ownSize += appId
        return " -o " + shellQuote("main.font=$family:size=${format(fit)}")
    }

    fun hasOwnSize(appId: String) = appId in ownSize

    fun track(window: XdgToplevel) {
        shown[window] = size
    }

    fun catchUp(window: XdgToplevel): Int {
        val at = shown[window] ?: return 0
        val steps = ((size - at) / STEP).roundToInt()
        shown[window] = at + steps * STEP
        return steps
    }

    fun columns(widthPx: Int) = columns(widthPx, scale, size)

    val emPx get() = (size * PX_PER_PT * scale).toFloat()

    val padPx get() = PAD * scale

    fun write() {
        if (density == 0f) return
        size = pointSize(density, fontScale, scale, em)
        val content = "[main]\nfont=$family:size=${format(size)}\n"
        val file = File(proot.home, PATH.removePrefix("~/"))
        if (content == written && file.isFile) return
        written = content
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(content)
        }.onFailure {
            written = null
            Log.w(TAG, "can't write the terminal font", it)
        }
    }

    companion object {
        const val PATH = "~/.local/state/bellatrix/foot-font.ini"
        private const val LIST_FAMILIES = "fc-list :spacing=100 -f '%{family[0]}\\n'"
        const val STEP = 0.5
        private const val PX_PER_PT = 96f / 72f
        private const val ADVANCE = 0.6f
        private const val PAD = 8
        private const val BTOP_COLUMNS = 80
        private const val DEFAULT_SIZE = 8.0
        private const val MIN_SIZE = 4.0
        private const val MAX_SIZE = 40.0
        private const val TAG = "terminal-font"
        private const val FAMILY = "terminal_font_family"
        private const val TEXT = "terminal_text"

        fun pointSize(
            density: Float,
            fontScale: Float,
            scale: Int,
            em: Float = TerminalText.Default.em,
        ): Double = (halves(em * fontScale * density / (scale * PX_PER_PT))).coerceIn(MIN_SIZE, MAX_SIZE)

        fun columns(
            widthPx: Int,
            scale: Int,
            size: Double,
        ): Int {
            val cell = (ADVANCE * size * PX_PER_PT * scale).roundToInt().coerceAtLeast(1)
            return (widthPx - 2 * PAD * scale) / cell
        }

        fun fitting(
            columns: Int,
            widthPx: Int,
            scale: Int,
            size: Double,
        ): Double {
            val cellPerPt = ADVANCE * PX_PER_PT * scale
            val fit = (widthPx - 2 * PAD * scale) / (cellPerPt * (columns + 1)).toDouble()
            return minOf(size, floor(fit * 2.0) / 2).coerceAtLeast(MIN_SIZE)
        }

        fun isTextFont(family: String) = family.isNotEmpty() && !Regex("emoji|signwriting|bellatrix|symbols", RegexOption.IGNORE_CASE).containsMatchIn(family)

        fun format(size: Double): String = String.format(Locale.ROOT, "%.1f", size)

        private fun halves(pt: Float) = (pt * 2).roundToInt() / 2.0
    }
}

/** The terminal text sizes from Bellatrix's Style menu, as an em in dp. */
enum class TerminalText(
    val id: String,
    @StringRes val label: Int,
    val em: Float,
) {
    Smaller("smaller", R.string.style_text_smaller, 10f),
    Default("default", R.string.style_text_default, 12f),
    Larger("larger", R.string.style_text_larger, 14f),
    ;

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Default
    }
}
