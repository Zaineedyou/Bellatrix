package com.bellatrixos.app_settings

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import com.bellatrixos.app_browser.utils.h
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.app_settings.searchbar.SuggestionAdapter
import com.bellatrixos.app_settings.searchbar.SuggestionItems
import com.bellatrixos.app_settings.utils.SFragment
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.interfaces.BackPressHandler
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.BtnRelativeLayout
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.NoUnderlineClickableSpan
import com.bellatrixos.widgets.UISplashLayout
import com.bellatrixos.widgets.UITitleBarSearch
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.Windows
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import java.util.Stack

/** Pengaturan sistem. */
class UISettings : UIFragment(), UINavigator.BreadcrumbCallback, BackPressHandler {
    @JvmField var A0: InstalledApp? = null
    @JvmField var B0: View? = null
    @JvmField var C0: MainActivityIn? = null
    @JvmField var D0: LinearLayout? = null
    @JvmField var E0: UISplashLayout? = null
    @JvmField var F0: RelativeLayout? = null
    @JvmField var G0: ImageButton? = null
    @JvmField var H0: ImageView? = null
    @JvmField var I0: TextView? = null
    @JvmField var J0: TextView? = null
    @JvmField var N0: ViewStub? = null
    @JvmField var O0: ScrollView? = null
    @JvmField var u0: Context? = null
    @JvmField var v0: Int = 0
    @JvmField var w0: View? = null
    @JvmField var x0: ScrollView? = null
    @JvmField var y0: UITitleBarSearch? = null
    @JvmField var z0: FloatingLayout? = null
    @JvmField val K0: Window = Window(0)
    @JvmField val L0: Stack<Int> = Stack()
    @JvmField val M0: HashMap<Int, Int> = HashMap()
    @JvmField var P0: Boolean = true
    @JvmField var Q0: Int = -1

    override fun onDestroyView() {
        super.onDestroyView()
        L0.clear()
        if (B0 != null) {
            G0?.setOnClickListener(null)
            B0?.setOnClickListener(null)
        }
        K0.f4870e = null
    }

    override fun onDestroy() {
        super.onDestroy()
        C0 = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("prev_page_index", Q0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = C0 ?: return
        WindowSize.e(view, z0, K0, y0 ?: view, activity, this)
        A0 = Windows.d(K0, "UISettings")
        if (savedInstanceState != null) {
            E0?.visibility = View.GONE
        }
        L0.push(0)
        I0?.highlightColor = 0

        val context = u0 ?: return
        UINavigator.f4706a = ContextCompat.getColorStateList(context, R.color.ui_settings_heading_color)
        val navItems = context.resources.getStringArray(R.array.settings_nav_items)
        val navSub = context.resources.getStringArray(R.array.settings_nav_sub_items)
        UINavigator.b = navItems[0]
        UINavigator.c = navItems[1]
        UINavigator.f4707d = navItems[2]
        UINavigator.f4708e = navItems[3]
        UINavigator.f4709f = navItems[4]
        UINavigator.g = navItems[5]
        UINavigator.f4710h = navItems[6]
        UINavigator.f4711i = navItems[7]
        UINavigator.linuxTitle = navItems[8]
        UINavigator.j = navSub[0]
        UINavigator.l = context.getString(R.string.settings_system_sound)
        UINavigator.f4712k = navSub[1]
        UINavigator.m = context.getString(R.string.settings_system_mouse)
        UINavigator.f4713n = navSub[2]
        UINavigator.o = navSub[3]
        UINavigator.p = navSub[4]
        UINavigator.q = navSub[5]
        UINavigator.r = navSub[6]
        UINavigator.s = navSub[7]
        UINavigator.t = navSub[8]
        UINavigator.u = navSub[9]
        UINavigator.v = navSub[10]
        UINavigator.w = navSub[11]
        UINavigator.x = navSub[12]
        UINavigator.y = context.getString(R.string.settings_nav_sub_item_typing)

        AppExecutors.c().postDelayed({ delayedSetup(view, savedInstanceState) }, 500L)
        if (K0.c != 0) {
            WindowSize.d(view, z0, K0)
            val layout = z0
            if (layout != null) WindowSize.f(layout, y0, 1)
        }
        z0?.setOnFocusListener(FocusResizeListener(view))
    }

    override fun b(): Boolean {
        if (L0.isEmpty()) return false
        if (L0.size > 1) {
            p0(L0.pop(), false, null)
        } else {
            C0?.A(tag ?: "")
        }
        return true
    }

    override fun f0(): Boolean = P0

    override fun i0(z: Boolean) {
        z0?.setBlurEnabled(z)
    }

    override fun j0(z: Boolean) {
        val layout = z0 ?: return
        layout.setFocusState(z)
        P0 = z
    }

    override fun k0(str: String?) {
        AppExecutors.c().postDelayed({
            val path = str ?: return@postDelayed
            val parts = UINavigation.a(path)
            p0(parts[0], true, displayedListener(parts[1]))
        }, 204L)
    }

    override fun o0() {
        z0?.invalidate()
    }

    /** Navigasi ke halaman pengaturan [i2], menyimpan posisi scroll halaman sebelumnya. */
    fun p0(i2: Int, z: Boolean, onFragmentDisplayedListener: UINavigator.OnFragmentDisplayedListener?) {
        if (Q0 != -1) {
            M0[Q0] = x0?.scrollY ?: 0
        }
        val fragment = UINavigator.a(i2, F0) ?: return
        val manager = childFragmentManager
        val toBack = Q0 > i2
        val delay = if (i2 < 10) 64L else 204L
        AppExecutors.c().postDelayed({
            if (manager.isStateSaved) return@postDelayed
            val tx: FragmentTransaction = manager.beginTransaction()
            if (i2 < 10 || toBack) {
                tx.setCustomAnimations(
                    R.anim.settings_page_enter_from_left,
                    R.anim.settings_page_exit_to_right,
                    R.anim.settings_page_enter_from_right,
                    R.anim.settings_page_exit_to_left
                )
            } else {
                tx.setCustomAnimations(
                    R.anim.settings_page_enter_from_right,
                    R.anim.settings_page_exit_to_left,
                    R.anim.settings_page_enter_from_left,
                    R.anim.settings_page_exit_to_right
                )
            }
            tx.replace(R.id.content_frame, fragment, null)
            tx.addToBackStack(null)
            Windows.i(manager, tx)
            manager.executePendingTransactions()
            val listener = onFragmentDisplayedListener
            if (listener != null && fragment is SFragment) {
                fragment.s0 = Runnable { listener.a() }
            }
        }, delay)
        if (z) {
            L0.push(Q0)
        }
        Q0 = i2
        AppExecutors.c().postDelayed({
            val saved = M0[i2] ?: 0
            x0?.scrollTo(0, saved)
        }, delay)
        UINavigation.b(i2)
    }

    /** Navigasi "cepat" tanpa animasi geser (dipakai nav radio). */
    fun q0(i2: Int) {
        if (i2 < 0) return
        UINavigator.a(i2, F0)?.let { fragment ->
            if (Q0 != -1) L0.push(Q0)
            UINavigation.b(i2)
            Q0 = i2
            val manager = childFragmentManager
            val tx = manager.beginTransaction()
            tx.setCustomAnimations(R.anim.settings_page_change, 0, R.anim.settings_page_change, 0)
            tx.replace(R.id.content_frame, fragment, null)
            tx.addToBackStack(null)
            Windows.i(manager, tx)
        }
        I0?.text = UINavigator.b(i2)
    }

    /** Menjadikan bagian breadcrumb dapat diklik. */
    fun s0(str: String?, i2: Int) {
        val textView = I0 ?: return
        val source = str ?: return
        val parts = source.split("  ›  ")
        val spannable = SpannableString(textView.text)
        var index = 0
        var startFrom = 0
        while (index < parts.size - 1) {
            val prefix = parts[index] + "  ›  "
            val start = textView.text.toString().indexOf(prefix, startFrom)
            if (start < 0) break
            val end = start + prefix.length
            spannable.setSpan(
                NoUnderlineClickableSpan(View.OnClickListener { p0(i2, true, null) }, UINavigator.f4706a!!),
                start,
                end,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            index++
            startFrom = end
        }
        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    /** Menyiapkan navigasi samping (radio, header pengguna, tombol collapse). */
    fun t0() {
        val context = u0 ?: return
        UISettingsManager.b = context.resources.getDimensionPixelSize(R.dimen.settings_collapsible_nav_width)
        val integer = context.resources.getInteger(R.integer.settings_display_mounted_device)
        UISettingsManager.f4714a = integer
        if (v0 == 1 && (K0.c != 0 || integer == 0)) {
            val list = D0
            val scroll = O0
            val button = G0
            if (list != null && scroll != null && button != null) UISettingsManager.a(list, scroll, button)
        }

        val scroll = O0 ?: return
        UINavigation.f4705a = arrayOf(
            scroll.findViewById(R.id.nav_system),
            scroll.findViewById(R.id.nav_bluetooth),
            scroll.findViewById(R.id.nav_personalize),
            scroll.findViewById(R.id.nav_apps),
            scroll.findViewById(R.id.nav_accounts),
            scroll.findViewById(R.id.nav_time_lang),
            scroll.findViewById(R.id.nav_privacy),
            scroll.findViewById(R.id.nav_updates),
            scroll.findViewById(R.id.nav_linux)
        )
        val navItems = context.resources.getStringArray(R.array.settings_nav_items)
        val handler = NavClickHandler()
        for (i in UINavigation.f4705a!!.indices) {
            val button = UINavigation.f4705a!![i]
            button.setText(navItems[i])
            button.setOnClickListener(View.OnClickListener { handler.a(i + 1) })
        }
        (scroll.findViewById<View>(R.id.header_user) as? BtnRelativeLayout)?.setOnClickListener {
            handler.a(5)
        }
        J0 = scroll.findViewById(R.id.device_user_name)
        H0 = scroll.findViewById(R.id.device_user_img)
        if (H0 != null && C0 != null) {
            H0?.clipToOutline = true
            H0?.setImageDrawable(C0?.U())
        }
        J0?.text = SharedPrefs.f4755a?.getString("system_user_name", "This PC")
        G0?.setOnClickListener(navToggle(true))
        B0?.setOnClickListener(navToggle(false))
        z0?.setOnWindowTapListener(WindowTapHandler())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivityIn) C0 = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings, container, false)
        w0 = view
        c0 = view
        val floating = view.findViewById<FloatingLayout>(R.id.main_view_settings)
        z0 = floating
        WindowSize.a(floating, K0.c)
        val context = view.context
        u0 = context
        C0 = context as? MainActivityIn
        v0 = context.resources.configuration.orientation
        y0 = view.findViewById(R.id.titleBar)
        E0 = view.findViewById(R.id.ui_splash)
        I0 = view.findViewById(R.id.page_heading)
        F0 = view.findViewById(R.id.uiTitleTxtLayout)
        D0 = view.findViewById(R.id.settings_content)
        N0 = view.findViewById(R.id.settings_nav_stub)
        B0 = view.findViewById(R.id.collapsibleNavOverlay)
        G0 = view.findViewById(R.id.btn_collapse_nav)
        x0 = view.findViewById(R.id.content_scroll)
        return view
    }

    override fun c0(): String? = "Settings"
    override fun d0(): String? = "com.bellatrixos.app_settings"

    // ------------------------------------------------------------------
    // Helper sintetis (`i.b`–`i.f`, `f.i`, `a.i`, ...)
    // ------------------------------------------------------------------

    private fun displayedListener(viewId: Int) = UINavigator.OnFragmentDisplayedListener {
        if (viewId == -1) return@OnFragmentDisplayedListener
        val root = c0 ?: return@OnFragmentDisplayedListener
        val target = root.findViewById<View>(viewId) ?: return@OnFragmentDisplayedListener
        target.post {
            x0?.smoothScrollTo(0, target.top)
            target.postDelayed({ target.requestFocus() }, 260L)
        }
    }

    private fun navToggle(expand: Boolean) = View.OnClickListener {
        val scroll = O0 ?: return@OnClickListener
        val overlay = B0 ?: return@OnClickListener
        UINavigation.c(expand, scroll, overlay)
    }

    /** i.c — klik item nav, saran pencarian, tap jendela, dan aksi breadcrumb. */
    private inner class NavClickHandler :
        UINavigation.OnNavItemSelected,
        UITitleBarSearch.OnSearchSuggestionClickListener,
        FloatingLayout.OnWindowTapListener,
        UINavigator.ClickAction {
        override fun a(i2: Int) {
            q0(i2)
            val scroll = O0 ?: return
            val overlay = B0 ?: return
            if (overlay.visibility == View.VISIBLE && scroll.visibility == View.VISIBLE) {
                scroll.startAnimation(UINavigation.c)
                AppExecutors.c().postDelayed({
                    scroll.visibility = View.GONE
                    overlay.visibility = View.GONE
                    scroll.translationZ = -1.0f
                }, 120L)
            }
        }

        override fun b(str: String) {
            val parts = UINavigation.a(str)
            p0(parts[0], true, displayedListener(parts[1]))
        }

        override fun c() {
            val search = y0 ?: return
            if (!search.w.isFocused) return
            z0?.requestFocus()
            val imm = u0?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(z0?.windowToken, 0)
        }

        override fun d(i2: Int) {
            p0(i2, true, null)
        }
    }

    /** i.f — fokus jendela dan perubahan ukuran. */
    private inner class FocusResizeListener(private val view: View) :
        FloatingLayout.OnAppFocusListener,
        Window.OnResizeStateChangeListener {
        override fun a() {
            view.bringToFront()
            C0?.d(this@UISettings)
            z0?.setFocusState(true)
            P0 = true
        }

        override fun b(i2: Int) {
            WindowSize.d(view, z0, K0)
            val layout = z0 ?: return
            WindowSize.f(layout, y0, i2)
            WindowSize.g(i2, "UISettings", A0)
            if (UISettingsManager.f4714a != 0 && v0 == 1) {
                if (i2 != 0) {
                    val list = D0
                    val scroll = O0
                    val button = G0
                    if (list != null && scroll != null && button != null) UISettingsManager.a(list, scroll, button)
                } else {
                    val scroll = O0
                    val button = G0
                    scroll?.visibility = View.VISIBLE
                    scroll?.translationZ = 0.0f
                    button?.visibility = View.GONE
                    val list = D0
                    val lp = list?.layoutParams as? ConstraintLayout.LayoutParams
                    if (lp != null) {
                        lp.horizontalWeight = 0.75f
                        list.layoutParams = lp
                    }
                    val sp = scroll?.layoutParams
                    if (sp != null) {
                        sp.width = 0
                        scroll.layoutParams = sp
                    }
                }
            }
        }
    }

    /** i.e — tombol resize / close / minimize pada title bar. */
    private inner class TitleBarButtonHandler(private val mode: Int) : View.OnClickListener {
        override fun onClick(v: View) {
            when (mode) {
                0 -> { val scroll = O0 ?: return; val overlay = B0 ?: return; UINavigation.c(true, scroll, overlay) }
                1 -> { val scroll = O0 ?: return; val overlay = B0 ?: return; UINavigation.c(false, scroll, overlay) }
                2 -> WindowSize.c(K0)
                3 -> C0?.P(this@UISettings)
                else -> C0?.A(tag ?: "")
            }
        }
    }

    /** i.c.c — menutup keyboard saat mengetuk jendela. */
    private inner class WindowTapHandler : FloatingLayout.OnWindowTapListener {
        override fun c() {
            val search = y0 ?: return
            if (!search.w.isFocused) return
            z0?.requestFocus()
            val imm = u0?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(z0?.windowToken, 0)
        }
    }

    /** Setup panel navigasi yang dijalankan 500 ms setelah view dibuat. */
    private fun delayedSetup(view: View, bundle: Bundle?) {
        if (!isAdded) return
        val context = u0 ?: return
        if (UINavigation.b == null) {
            UINavigation.b = AnimationUtils.loadAnimation(context, R.anim.settings_nav_expand)
        }
        if (UINavigation.c == null) {
            UINavigation.c = AnimationUtils.loadAnimation(context, R.anim.settings_nav_collapse)
        }
        E0?.a(bundle)

        val stub = N0
        if (stub != null) {
            stub.setOnInflateListener { _, inflated ->
                O0 = inflated.findViewById(R.id.settings_navigation)
                t0()
            }
            stub.inflate()
        } else {
            O0 = w0?.findViewById(R.id.settings_navigation)
            if (O0 != null) t0()
        }

        if (bundle == null) {
            val args = arguments
            val parts = if (args == null) intArrayOf(1, -1) else UINavigation.a(args.getString("path", "1"))
            if (parts[0] < 10) {
                q0(parts[0])
            } else {
                p0(parts[0], false, displayedListener(parts[1]))
            }
        } else {
            val index = bundle.getInt("prev_page_index", 1)
            Q0 = index
            UINavigation.b(index)
            val title = UINavigator.b(index) ?: ""
            I0?.text = title
            if (title.contains("›")) s0(title, index)
        }

        K0.f4870e = FocusResizeListener(view)
        y0?.v?.post {
            val search = y0 ?: return@post
            if (search.w.width != 0) {
                val padding = Math.round(12 * search.resources.displayMetrics.density) * 2
                search.w.visibility = if (search.y.left - search.u.right < search.w.measuredWidth + padding) View.GONE else View.VISIBLE
            }
        }
        y0?.setResizeBtnListener(TitleBarButtonHandler(2))
        y0?.setCloseBtnListener(TitleBarButtonHandler(3))
        y0?.setMinimizeBtnListener(TitleBarButtonHandler(4))

        val search = y0 ?: return
        val suggestions = SuggestionAdapter(context, SuggestionItems.a(context))
        search.w.dropDownVerticalOffset = context.resources.getDimensionPixelSize(R.dimen.ui_0_4dp)
        search.w.setAdapter(suggestions)
        search.w.onItemClickListener = h(1, search)
        suggestions.u = SuggestionAdapter.OnResultsStateListener { hasResults -> search.w.isActivated = hasResults }
        search.setOnSearchSuggestionClickListener(NavClickHandler())
    }
}
