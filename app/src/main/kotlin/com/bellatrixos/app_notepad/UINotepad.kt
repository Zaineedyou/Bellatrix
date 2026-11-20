package com.bellatrixos.app_notepad

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.app_explorer.picker.FilePicker
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.interfaces.BackPressHandler
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.SmoothScrollEditText
import com.bellatrixos.widgets.UITitleBar
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.dialogs.UIDialog
import java.io.BufferedReader
import java.io.File
import java.io.IOException

/**
 * Full manual port of UINotepad.java.
 *
 * The obfuscated member names are kept verbatim because the other migrated
 * classes in this package (a, b, d, e, f, i) and app_explorer's c/FilePicker
 * read and write them directly. Anonymous obfuscated listeners/runners from the
 * dex (j, c, AnonymousClass1/2, media3 helpers) are expressed with Kotlin
 * lambdas and public Android APIs instead.
 */
class UINotepad : UIFragment(), BackPressHandler {
    lateinit var A0: MainActivityIn
    @JvmField var B0: Runnable? = null
    lateinit var C0: TextView
    lateinit var D0: TextView
    lateinit var E0: TextView
    @JvmField var G0: TextWatcher? = null
    @JvmField var H0: ViewTreeObserver.OnScrollChangedListener? = null
    lateinit var I0: BufferedReader
    @JvmField var O0: Boolean = false
    lateinit var u0: Context
    lateinit var v0: File
    @JvmField var w0: String? = null
    lateinit var x0: UITitleBar
    lateinit var y0: SmoothScrollEditText
    lateinit var z0: FloatingLayout

    @JvmField val F0: Window = Window(2)
    @JvmField var J0: Int = 0
    @JvmField var K0: Int = 0
    @JvmField var L0: String? = null
    @JvmField var M0: Boolean = false
    @JvmField var N0: Boolean = false
    @JvmField var P0: Boolean = false
    @JvmField var Q0: Boolean = false
    @JvmField var R0: Boolean = false
    @JvmField @Volatile var S0: Boolean = false
    @JvmField var T0: Boolean = false

    /** Fragment.a0 (bendera "mCalled"). */
    @JvmField var a0: Boolean = false

    // -----------------------------------------------------------------
    // Lifecycle methods; obfuscated names from UINotepad.java kept so the
    // original call sites keep working, plus real Android overrides.
    // -----------------------------------------------------------------

    /** Java: A() - fragment detached. */
    fun A() {
        a0 = true
        // Java clears A0 here. Kotlin keeps it non-null because package
        // helpers dereference it; the framework drops the activity on detach.
    }

    /** Java: C() - view destroyed: stop reading and detach all listeners. */
    fun C() {
        a0 = true
        S0 = true
        try {
            if (::I0.isInitialized) I0.close()
        } catch (_: IOException) {
        }
        // Java sets I0 = null; lateinit instead relies on S0/R0 flags.
        M0 = true
        N0 = false
        if (::y0.isInitialized) {
            H0?.let { listener ->
                val observer = y0.viewTreeObserver
                if (observer.isAlive) observer.removeOnScrollChangedListener(listener)
            }
            G0?.let { y0.removeTextChangedListener(it) }
        }
        B0?.let { AppExecutors.c().removeCallbacks(it) }
        B0 = null
    }

    /** Java: I(Bundle) - persist the "end of file" flag. */
    fun I(bundle: Bundle) {
        bundle.putBoolean("notepad_eof", M0)
    }

    /** Java: L(View, Bundle) - window wiring, menus and editor listener. */
    fun L(view: View, bundle: Bundle?) {
        WindowSize.e(view, z0, F0, x0, A0, this)
        if (bundle != null) T0 = bundle.getBoolean("notepad_eof", false)

        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val path = arguments?.getString("path")

                if (arguments != null) {
                    when {
                        bundle == null -> {
                            if (path != null) {
                                v0 = File(path)
                                y0.isSaveEnabled = false
                                q0(f(this@UINotepad, 0))
                            }
                        }
                        T0 -> {
                            if (path != null) v0 = File(path)
                            val name = v0.name
                            w0 = name
                            x0.setTitle(name)
                            M0 = true
                            y0.isSaveEnabled = true
                        }
                        path != null -> {
                            v0 = File(path)
                            y0.isSaveEnabled = false
                            q0(f(this@UINotepad, 0))
                        }
                    }
                } else {
                    w0 = u0.getString(R.string.notepad_txt_doc_new)
                    E0.text = u0.getString(R.string.cmd_menu_statusbar, 0)
                }

                val watcher = object : TextWatcher {
                    override fun afterTextChanged(editable: Editable) {
                        if (O0) return
                        if (!P0) {
                            x0.setTitle("$w0   \u00a0\u278a")
                            P0 = true
                        }
                        B0?.let { AppExecutors.c().removeCallbacks(it) }
                        val pending = Runnable {
                            E0.text = u0.getString(R.string.cmd_menu_statusbar, editable.length)
                            y0.b()
                        }
                        B0 = pending
                        AppExecutors.c().postDelayed(pending, 200L)
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }
                G0 = watcher
                y0.addTextChangedListener(watcher)
            }
        })

        val resizeStateListener = d(this, view)
        F0.f4870e = resizeStateListener

        // Java: switch case 0 - File menu.
        C0.setOnClickListener {
            val context = u0
            val consumer = e(0, this@UINotepad)
            val menu = A0.l0() as? UIContextMenu ?: return@setOnClickListener
            val items = menu.f4745d
            items.clear()
            val save = UIMenuItem(0, 101, 0, context.getString(R.string.context_menu_save))
            save.f4748d = "Ctrl+S"
            items.add(save)
            items.add(UIMenuItem(0, 102, 0, context.getString(R.string.context_menu_save_as)))
            items.add(UIMenuItem(1, 0, 0, ""))
            items.add(UIMenuItem(0, 5, 0, context.getString(R.string.context_menu_print)))
            items.add(UIMenuItem(0, 22, 0, context.getString(R.string.context_menu_close_window)))
            menu.c = UIContextMenu.OnMenuItemClickListener { _, id -> consumer.accept(id) }
            menu.a(0)
            menu.f(C0)
        }

        // Java: switch case 1 - Edit menu.
        D0.setOnClickListener {
            val context = u0
            val consumer = e(2, this@UINotepad)
            val menu = A0.l0() as? UIContextMenu ?: return@setOnClickListener
            val items = menu.f4745d
            items.clear()
            val undo = UIMenuItem(0, 19, 0, context.getString(R.string.context_menu_undo))
            undo.f4748d = "Ctrl+Z"
            undo.f4751h = false
            items.add(undo)
            val redo = UIMenuItem(0, 20, 0, context.getString(R.string.context_menu_redo))
            redo.f4748d = "Ctrl+Y"
            redo.f4751h = false
            items.add(redo)
            items.add(UIMenuItem(1, 0, 0, ""))
            val paste = UIMenuItem(0, 8, 0, context.getString(R.string.context_menu_paste))
            paste.f4748d = "Ctrl+V"
            items.add(paste)
            val selectAll = UIMenuItem(0, 21, 0, context.getString(R.string.context_menu_select_all))
            selectAll.f4748d = "Ctrl+A"
            items.add(selectAll)
            menu.c = UIContextMenu.OnMenuItemClickListener { _, id -> consumer.accept(id) }
            menu.a(0)
            menu.f(D0)
        }

        // Java: switch case 2 - resize button.
        x0.setResizeBtnListener { WindowSize.c(F0) }

        // Java: switch case 3 - close button.
        x0.setCloseBtnListener { p0() }

        // Java: default - minimize button.
        x0.setMinimizeBtnListener { A0.A(tag ?: "") }

        z0.setOnFocusListener(d(this, view))
        WindowSize.d(view, z0, F0)
        WindowSize.f(z0, x0, 2)
        WindowSize.b(view, z0)
    }

    /** Java: z(LayoutInflater, ViewGroup, Bundle) - inflate the notepad view. */
    fun z(layoutInflater: LayoutInflater, viewGroup: ViewGroup?, bundle: Bundle?): View {
        val view = layoutInflater.inflate(R.layout.app_notepad, viewGroup, false)
        S0 = false
        val context = view.context
        u0 = context
        A0 = context as MainActivityIn
        z0 = view.findViewById(R.id.main_view_img)
        x0 = view.findViewById(R.id.titleBar)
        y0 = view.findViewById(R.id.noteEditText)
        E0 = view.findViewById(R.id.notepadData)
        C0 = view.findViewById(R.id.menuFile)
        D0 = view.findViewById(R.id.menuEdit)
        z0.clipToOutline = true
        contentView = view
        return view
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = z(inflater, container, savedInstanceState)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        L(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        I(outState)
    }

    override fun onDestroyView() {
        C()
        super.onDestroyView()
    }

    override fun onDetach() {
        A()
        super.onDetach()
    }

    /** Obfuscated Fragment.isAdded() called by the i helper. */
    fun q(): Boolean = isAdded

    override fun b(): Boolean {
        A0.A(tag ?: "")
        return true
    }

    override fun c0(): String? = if (::v0.isInitialized) v0.name else null

    override fun d0(): String? = if (::v0.isInitialized) v0.absolutePath else null

    override fun h0(i2: Int, keyEvent: KeyEvent) {
        if (i2 == 47 && keyEvent.isCtrlPressed()) {
            t0(Runnable {
                P0 = false
                x0.setTitle(w0)
            })
        }
    }

    override fun j0(z: Boolean) {
        t0 = z
        if (::z0.isInitialized) z0.setFocusState(z)
    }

    override fun n0(): Boolean = true

    /** Java: p0() - ask before closing a document with unsaved changes. */
    fun p0() {
        if (!P0 || !::v0.isInitialized) {
            A0.P(this)
            return
        }
        val context = u0
        val consumer = e(1, this)
        val dialog = UIDialog(
            context,
            context.getString(R.string.app_notepad_title),
            context.getString(R.string.dialog_app_unsaved_subtitle)
        )
        dialog.z = context.getString(R.string.context_menu_save)
        dialog.A = context.getString(R.string.dialog_app_unsaved_action_no)
        dialog.C = DialogInterface.OnClickListener { _, _ -> consumer.accept(0) }
        dialog.D = DialogInterface.OnClickListener { _, _ -> consumer.accept(1) }
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    /** Java: q0(Runnable) - start loading the file on a background executor. */
    fun q0(runnable: Runnable?) {
        if (N0 || M0) return
        N0 = true
        AppExecutors.b().execute(i(this, 1, runnable))
    }

    /** Java: s0(Runnable) - run after the current load has finished. */
    fun s0(runnable: Runnable?) {
        if (S0 || N0) return
        if (M0) {
            t0(runnable)
        } else {
            q0(i(this, 0, runnable))
        }
    }

    /** Java: t0(Runnable) - save the editor content to the opened file. */
    fun t0(runnable: Runnable?) {
        if (!::v0.isInitialized || !v0.exists()) {
            FilePicker.b = this
            A0.k0("txt/**")
            return
        }
        if (P0) {
            if (M0) {
                AppExecutors.d().execute(i(this, 3, runnable))
                return
            }
            if (v0.length() <= 10485760L) {
                s0(runnable)
                return
            }
            val size = formatFileSize(v0.length())
            val context = u0
            val action = i(this, 2, runnable)
            val dialog = UIDialog(
                context,
                context.getString(R.string.app_notepad_warning_file_lg),
                context.getString(R.string.app_notepad_warning_file_lg_subtitle, size)
            )
            dialog.z = context.getString(R.string.context_menu_save)
            dialog.A = context.getString(R.string.dialog_app_unsaved_action_no)
            dialog.C = DialogInterface.OnClickListener { _, _ -> action.run() }
            dialog.setCanceledOnTouchOutside(true)
            dialog.show()
        }
    }

    /** Java: u0(File) - bind a newly picked document. */
    fun u0(file: File) {
        v0 = file
        val name = file.name
        w0 = name
        x0.setTitle(name)
    }

    /** Local replacement for the unported FileExtensions.l(long). */
    private fun formatFileSize(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1048576L -> {
            val v = bytes * 100L / 1024L
            "${v / 100L}.${v % 100L} KB"
        }
        bytes < 1073741824L -> {
            val v = bytes * 100L / 1048576L
            "${v / 100L}.${v % 100L} MB"
        }
        else -> {
            val v = bytes * 100L / 1073741824L
            "${v / 100L}.${v % 100L} GB"
        }
    }
}
