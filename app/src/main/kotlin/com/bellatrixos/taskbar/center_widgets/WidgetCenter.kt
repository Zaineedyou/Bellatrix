package com.bellatrixos.taskbar.center_widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.taskbar.center_widgets.callbacks.WidgetBindCallback
import com.bellatrixos.taskbar.center_widgets.picker.WidgetPickerAdapter
import com.bellatrixos.taskbar.center_widgets.picker.widgetPicker
import com.bellatrixos.taskbar.center_widgets.utils.SavedWidgetItem
import com.bellatrixos.taskbar.center_widgets.utils.WidgetsAdapter
import com.bellatrixos.taskbar.center_widgets.utils.WidgetsRecyclerView
import com.bellatrixos.variables.Global
import com.bellatrixos.windows.InsetManager
import com.bellatrixos.windows.Windows

/** Panel widget center yang menampung widget Android dan pemilihnya. */
class WidgetCenter(
    @JvmField val b: Context,
    @JvmField val t: ActivityResultLauncher<Intent>
) : PopupWindow(b), WidgetBindCallback {

    @JvmField val f4819a: View
    @JvmField val c: FrameLayout
    @JvmField val f4821e: ImageView
    @JvmField val f4822f: TextView
    @JvmField val g: RelativeLayout
    @JvmField val f4823h: androidx.recyclerview.widget.RecyclerView
    @JvmField val f4824i: WidgetsRecyclerView
    @JvmField val j: ImageButton
    @JvmField val f4825k: ImageButton
    @JvmField val s: AppWidgetManager
    @JvmField var l: WidgetsAdapter = WidgetsAdapter(b)
    @JvmField var m: WidgetPickerAdapter? = null
    @JvmField var f4820d: Int
    @JvmField var f4826n: Boolean = false
    @JvmField var o: Boolean = false
    @JvmField var p: Boolean = true
    @JvmField var q: Int = -1
    lateinit var r: AppWidgetHost

    init {
        val windowManager = b.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
        val metrics = DisplayMetrics()
        val width = if (windowManager != null) {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
            (metrics.widthPixels * 0.74).toInt()
        } else -2
        WidgetManager.f4828a = this
        val root = LayoutInflater.from(b).inflate(R.layout.taskbar_dialog_widgets, null as ViewGroup?)
        f4819a = root
        contentView = root
        this.width = width
        height = -1
        isClippingEnabled = false
        setBackgroundDrawable(ColorDrawable(0))
        c = root.findViewById(R.id.mainView)
        f4824i = root.findViewById(R.id.widgetsRecyclerView)
        f4823h = root.findViewById(R.id.widgetPickerRecyclerView)
        j = root.findViewById(R.id.dialog_add_widget)
        f4825k = root.findViewById(R.id.btn_rearrange)
        g = root.findViewById(R.id.heading_view)
        f4822f = root.findViewById(R.id.empty_view)
        f4821e = root.findViewById(R.id.ACBlur)
        root.setPadding(InsetManager.c, InsetManager.f4864a, InsetManager.f4865d, Windows.f4880a + InsetManager.b)
        f4820d = InsetManager.c
        if (SharedPrefs.b()) {
            c.backgroundTintList = ColorStateList.valueOf(Global.c)
        }
        s = AppWidgetManager.getInstance(b.applicationContext)
        if (WidgetsDB.f4829a == null) {
            WidgetsDB.f4829a = b.getSharedPreferences("WidgetPrefs", 0)
        }
        startHost()
        c.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                c.viewTreeObserver.removeOnGlobalLayoutListener(this)
                com.bellatrixos.AppExecutors.d().execute(d(this@WidgetCenter, 0, b))
            }
        })
        j.setOnClickListener { onAddClicked() }
        f4825k.setOnClickListener { onRearrangeClicked() }
        f4819a.setOnClickListener { dismiss() }
        isFocusable = true
        isOutsideTouchable = true
        setAnimationStyle(R.style.AnimationWidgets)
    }

    private fun startHost() {
        try {
            if (!this::r.isInitialized) r = AppWidgetHost(b.applicationContext, 1024)
            r.startListening()
        } catch (ignored: Exception) {
            try { r.stopListening() } catch (ignored2: Exception) {}
            r = AppWidgetHost(b.applicationContext, 1024)
            r.startListening()
        }
    }

    private fun onAddClicked() {
        if (f4826n) {
            b(false)
            return
        }
        b(true)
        if (m == null) {
            val adapter = widgetPicker.a(b, f4823h)
            m = adapter
            adapter.A = WidgetPickerAdapter.OnItemClickListener { position -> addWidget(position) }
        }
    }

    private fun addWidget(position: Int) {
        val info = m?.y?.getOrNull(position) as? AppWidgetProviderInfo
        if (info == null) {
            Toast.makeText(b, R.string.toast_error_occurred, Toast.LENGTH_SHORT).show()
            return
        }
        b(false)
        q = r.allocateAppWidgetId()
        if (s.bindAppWidgetIdIfAllowed(q, info.provider)) {
            r.createView(b.applicationContext, q, info)?.let { l.A(it, q) }
            f4822f.visibility = View.GONE
        } else {
            val intent = Intent("android.appwidget.action.APPWIDGET_BIND").apply {
                putExtra("appWidgetId", q)
                putExtra("appWidgetProvider", info.provider)
            }
            t.launch(intent)
        }
    }

    private fun onRearrangeClicked() {
        if (f4826n) {
            b(false)
            return
        }
        o = !o
        l.B(o, f4824i)
        f4825k.setImageResource(
            if (o) R.drawable.ic_checkmark_24_filled else R.drawable.ic_arrow_sort_24_regular
        )
        if (!o) {
            val saved = ArrayList<SavedWidgetItem>()
            for ((index, item) in l.A.withIndex()) {
                val hostView = item.f4827a
                val info = hostView.appWidgetInfo
                val provider = info?.provider?.className ?: continue
                val entry = SavedWidgetItem(provider, hostView.appWidgetId, index)
                entry.e(item.b)
                saved.add(entry)
            }
            WidgetsDB.b(saved)
        }
    }

    override fun a(i2: Int) {
        if (i2 != -1 || q == -1) {
            Log.d("WidgetCenter", "Widget bind not allowed or cancelled")
        } else {
            val info = s.getAppWidgetInfo(q)
            r.createView(b.applicationContext, q, info)?.let { l.A(it, q) }
            f4822f.visibility = View.GONE
        }
    }

    fun b(z: Boolean) {
        f4826n = z
        f4823h.visibility = if (z) View.VISIBLE else View.GONE
        f4824i.visibility = if (z) View.GONE else View.VISIBLE
        if (z) {
            f4822f.visibility = View.GONE
        } else if (l.A.isEmpty()) {
            f4822f.visibility = View.VISIBLE
        }
        j.setImageResource(if (z) R.drawable.ic_arrow_left_24_regular else R.drawable.ic_add_24_regular)
    }

    override fun dismiss() {
        try { r.stopListening() } catch (ignored: Exception) {}
        f4821e.animate().alpha(0.0f).setDuration(200L).start()
        super.dismiss()
    }

    companion object {
        @JvmField val u: Int = 0
    }
}
