package com.bellatrixos.taskbar.center_widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.linux.Agent
import com.bellatrixos.taskbar.center_widgets.callbacks.WidgetBindCallback
import com.bellatrixos.taskbar.center_widgets.picker.WidgetPickerAdapter
import com.bellatrixos.taskbar.center_widgets.picker.widgetPicker
import com.bellatrixos.taskbar.center_widgets.utils.SavedWidgetItem
import com.bellatrixos.taskbar.center_widgets.utils.WidgetsAdapter
import com.bellatrixos.taskbar.center_widgets.utils.WidgetsRecyclerView
import com.bellatrixos.variables.Global
import com.bellatrixos.windows.InsetManager
import com.bellatrixos.windows.Windows
import kotlin.math.roundToInt

/**
 * The taskbar's Widget Zone is now the Coding Agents picker. The installed guest agents are listed
 * here, choosing one makes it the default and opens it; the header button and the empty state open
 * the standalone Coding Agents settings page for installing/removing agents. Android AppWidgets are
 * still hosted underneath for existing saved widgets, but this panel no longer presents them.
 */
class WidgetCenter(
    @JvmField val b: Context,
    @JvmField val t: ActivityResultLauncher<Intent>
) : PopupWindow(b), WidgetBindCallback {

    @JvmField val f4819a: View
    @JvmField val c: FrameLayout
    @JvmField val f4821e: ImageView
    @JvmField val f4822f: TextView
    @JvmField val g: RelativeLayout
    @JvmField val f4823h: RecyclerView
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

    private val runtime get() = (b.applicationContext as App).runtime
    private var agentAdapter: AgentAdapter? = null

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
        installAgentPicker()
        f4825k.setOnClickListener { onRearrangeClicked() }
        f4819a.setOnClickListener { dismiss() }
        isFocusable = true
        isOutsideTouchable = true
        setAnimationStyle(R.style.AnimationWidgets)
    }

    private fun dp(dip: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip.toFloat(), b.resources.displayMetrics).roundToInt()

    private fun installAgentPicker() {
        f4824i.visibility = View.GONE
        f4825k.visibility = View.GONE
        f4823h.visibility = View.VISIBLE
        f4823h.layoutManager = LinearLayoutManager(b)
        val adapter = AgentAdapter()
        agentAdapter = adapter
        f4823h.adapter = adapter

        j.setImageResource(R.drawable.ic_settings_20_regular)
        j.contentDescription = b.getString(R.string.dialog_widget_add)
        j.setOnClickListener { manageAgents() }
        f4822f.text = b.getString(R.string.dialog_widget_placeholder)
        f4822f.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.img_widget_zone_coding_agents_48, 0, 0)
        f4822f.isClickable = true
        f4822f.setOnClickListener { manageAgents() }
        refreshAgents()
    }

    private fun manageAgents() {
        runtime.showCodingAgents()
        dismiss()
    }

    private fun refreshAgents() {
        val installed = runtime.agents.installed
        agentAdapter?.update(installed, runtime.agents.default)
        val empty = installed.isEmpty()
        f4822f.visibility = if (empty) View.VISIBLE else View.GONE
        f4823h.visibility = if (empty) View.GONE else View.VISIBLE
    }

    override fun showAtLocation(
        parent: View?,
        gravity: Int,
        x: Int,
        y: Int,
    ) {
        refreshAgents()
        super.showAtLocation(parent, gravity, x, y)
    }

    private inner class AgentAdapter : RecyclerView.Adapter<AgentAdapter.Holder>() {
        private var agents: List<Agent> = emptyList()
        private var picked: Agent? = null

        fun update(
            list: List<Agent>,
            default: Agent?,
        ) {
            agents = list
            picked = default
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): Holder {
            val row =
                LinearLayout(b).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(8), dp(12), dp(8))
                    layoutParams =
                        RecyclerView.LayoutParams(
                            RecyclerView.LayoutParams.MATCH_PARENT,
                            RecyclerView.LayoutParams.WRAP_CONTENT,
                        )
                }
            val icon =
                ImageView(b).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            val texts =
                LinearLayout(b).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(dp(12), 0, dp(8), 0)
                }
            val name =
                TextView(b).apply {
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(b, R.color.text_default))
                }
            val description =
                TextView(b).apply {
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(b, R.color.text_muted))
                }
            texts.addView(name)
            texts.addView(description)
            val check =
                ImageView(b).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
                    setImageResource(R.drawable.ic_checkmark_24_filled)
                    setColorFilter(Color.parseColor("#7AA2F7"))
                }
            row.addView(icon)
            row.addView(texts)
            row.addView(check)
            return Holder(row, icon, name, description, check)
        }

        override fun onBindViewHolder(
            holder: Holder,
            position: Int,
        ) {
            val agent = agents[position]
            holder.name.text = agent.name
            holder.description.text = b.getString(agent.description)
            val iconId = b.resources.getIdentifier("agent_${agent.bin.replace('-', '_')}", "drawable", b.packageName)
            holder.icon.setImageResource(if (iconId != 0) iconId else R.drawable.img_nav_coding_agents_20)
            holder.check.visibility = if (picked?.bin == agent.bin) View.VISIBLE else View.INVISIBLE
            holder.itemView.setOnClickListener {
                runtime.agents.choose(agent)
                refreshAgents()
                runtime.terminal(agent.command, agent.name, agent)
                dismiss()
            }
        }

        override fun getItemCount(): Int = agents.size

        inner class Holder(
            item: View,
            val icon: ImageView,
            val name: TextView,
            val description: TextView,
            val check: ImageView,
        ) : RecyclerView.ViewHolder(item)
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
        manageAgents()
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
        }
        refreshAgents()
    }

    fun b(z: Boolean) {
        f4826n = z
        f4823h.visibility = if (z) View.GONE else View.VISIBLE
        f4824i.visibility = if (z) View.VISIBLE else View.GONE
        if (!z) refreshAgents()
        j.setImageResource(if (z) R.drawable.ic_arrow_left_24_regular else R.drawable.ic_settings_20_regular)
    }

    override fun dismiss() {
        try { if (::r.isInitialized) r.stopListening() } catch (ignored: Exception) {}
        f4821e.animate().alpha(0.0f).setDuration(200L).start()
        super.dismiss()
    }

    companion object {
        @JvmField val u: Int = 0
    }
}
