package com.bellatrixos.app_settings.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.fonts.Font
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bellatrixos.App
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.app_settings.UISettings
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.linux.LauncherFont
import com.bellatrixos.theme.Style
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.UIProgressCircular
import com.bellatrixos.widgets.cards.UISettingsCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Halaman personalisasi. */
class UIPersonalize : Fragment() {
    @JvmField var A0: UISettingsCard? = null
    @JvmField var s0: Context? = null
    @JvmField var t0: UISettings? = null
    @JvmField var u0: MainActivityIn? = null
    @JvmField var v0: ImageView? = null
    @JvmField var w0: UIProgressCircular? = null
    @JvmField var x0: UISettingsCard? = null
    @JvmField var y0: UISettingsCard? = null
    @JvmField var z0: UISettingsCard? = null

    /** The Font card: the launcher's font, or a font file the user adds. */
    private var fontCard: UISettingsCard? = null

    private val pickFont = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) addFont(uri) }

    override fun onDestroyView() {
        super.onDestroyView()
        t0 = null
        u0 = null
        x0?.setOnCardClickListener(null)
        y0?.setOnCardClickListener(null)
        z0?.setOnCardClickListener(null)
        A0?.setOnCardClickListener(null)
        fontCard?.setOnCardClickListener(null)
        fontCard = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        t0 = parentFragment as? UISettings
        val context = s0 ?: return
        u0 = context as? MainActivityIn
        v0?.clipToOutline = true
        v0?.setImageBitmap(u0?.G())
        x0?.setOnCardClickListener(z(this, 0))
        y0?.setOnCardClickListener(z(this, 1))
        z0?.setOnCardClickListener(z(this, 2))
        A0?.setOnCardClickListener(z(this, 3))
        fontCard?.setOnCardClickListener { chooseFont() }
        showFont()
    }

    private val runtime get() = (s0?.applicationContext as? App)?.runtime

    /** What the Font card says the launcher draws in now. */
    private fun showFont() {
        val family = runtime?.style?.style?.get(Style.FONT) ?: return
        fontCard?.setSubtitle(
            when {
                family == Style.SYSTEM_FONT -> getString(R.string.settings_font_system)
                LauncherFont.isCustom(family) -> File(family.removePrefix(LauncherFont.CUSTOM)).nameWithoutExtension
                else -> family
            },
        )
    }

    /** A list of the system font and the fonts the user added, with a button to add another. */
    private fun chooseFont() {
        val context = s0 ?: return
        val runtime = runtime ?: return
        val added = LauncherFont.fontsIn(context.filesDir).listFiles { file -> file.isFile }.orEmpty().sortedBy { it.name.lowercase() }
        val values = listOf(Style.SYSTEM_FONT) + added.map { LauncherFont.CUSTOM + it.name }
        val labels = listOf(getString(R.string.settings_font_system)) + added.map { it.nameWithoutExtension }
        // A family named in style.toml that isn't here (a Linux one) shows no choice ticked.
        val current = values.indexOf(runtime.style.style[Style.FONT])
        AlertDialog
            .Builder(context, R.style.UILiquidGlassDialog)
            .setTitle(R.string.settings_personalize_font)
            .setSingleChoiceItems(labels.toTypedArray(), current) { dialog, which ->
                setFont(values[which])
                dialog.dismiss()
            }.setPositiveButton(R.string.settings_font_add) { _, _ -> pickFont.launch(arrayOf("*/*")) }
            .setNegativeButton(R.string.dialog_actions_cancel, null)
            .show()
    }

    /** Draws the launcher in [value] ([Style.FONT]'s kind of text) from now on. */
    private fun setFont(value: String) {
        val runtime = runtime ?: return
        runtime.style.update { it.with(Style.FONT, value) }
        showFont()
        // The same name again, as when its file was just replaced, changes nothing in the setting but the font.
        lifecycleScope.launch { runtime.launcherFont.apply() }
    }

    /** Keeps the font file [uri] points to for the launcher, and uses it. */
    private fun addFont(uri: Uri) {
        val context = s0 ?: return
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) { copyFont(context, uri) }
            if (saved == null) {
                Toast.makeText(context, R.string.settings_font_invalid, Toast.LENGTH_LONG).show()
            } else {
                setFont(LauncherFont.CUSTOM + saved.name)
                Toast.makeText(context, getString(R.string.settings_font_added, saved.nameWithoutExtension), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** [uri] copied into the launcher's fonts folder, or null if it isn't a font Android reads; any thread. */
    private fun copyFont(
        context: Context,
        uri: Uri,
    ): File? {
        val shown =
            runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            }.getOrNull()
        val name = (shown ?: uri.lastPathSegment.orEmpty()).substringAfterLast('/').replace(Regex("[^A-Za-z0-9._ -]"), "_").trim()
        if (!Regex(".+\\.(ttf|otf|ttc)", RegexOption.IGNORE_CASE).matches(name)) return null
        val dir = LauncherFont.fontsIn(context.filesDir).also { it.mkdirs() }
        // Copied under another name first: a file that isn't a font must not replace one that works.
        val part = File(dir, "$name.part")
        val target = File(dir, name)
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                part.outputStream().use { out ->
                    val copied = input.copyTo(out)
                    require(copied in 1..MAX_FONT_BYTES) { "font size $copied" }
                }
            } ?: error("can't open $uri")
            // Android 10 and later check the file as they build the font; the launcher's own loading is that too.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Font.Builder(part).build()
            check(part.renameTo(target)) { "can't keep the font" }
            target
        }.onFailure { part.delete() }.getOrNull()
    }

    fun a0(str: String) {
        val context = s0 ?: return
        val dialog = Dialog(context, R.style.UIDialogDefault)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        u0?.g0()
        w0?.a()
        AppExecutors.d().execute(A(this, str, dialog, 0))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_personalize, container, false)
        s0 = view.context
        v0 = view.findViewById(R.id.desktop_wallpaper)
        x0 = view.findViewById(R.id.btn_settings_taskbar)
        y0 = view.findViewById(R.id.btn_settings_colors)
        A0 = view.findViewById(R.id.btn_settings_startMenu)
        z0 = view.findViewById(R.id.btn_settings_background)
        fontCard = view.findViewById(R.id.btn_settings_font)
        if (Global.f4836a == 2) {
            w0 = view.findViewById(R.id.progressBar)
            val first = view.findViewById<ImageView>(R.id.dkTheme1)
            val second = view.findViewById<ImageView>(R.id.dkTheme2)
            first.clipToOutline = true
            second.clipToOutline = true
            first.setOnClickListener(z(this, 4))
            second.setOnClickListener(z(this, 5))
        }
        return view
    }
}

/** More than any font file there is; a bigger file isn't one. */
private const val MAX_FONT_BYTES = 64L * 1024 * 1024
