package com.bellatrixos.taskbar

import android.content.Context
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.interfaces.ItemRearrangeInterface
import com.bellatrixos.windows.Windows
import com.bellatrixos.windows.icons.IconEngine
import java.util.Collections

/** Adapter tombol aplikasi pada taskbar. */
class TaskBarAdapter(@JvmField val x: Context, @JvmField val D: MutableList<TaskBarItem>) :
    RecyclerView.Adapter<TaskBarAdapter.ViewHolder>(), ItemRearrangeInterface {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: View = view.findViewById(R.id.indicator)
        @JvmField var v: ImageView = view.findViewById(R.id.icon)
    }

    fun interface OnContextMenuListener {
        fun a(view: View, i2: Int)
    }

    fun interface OnItemClickListener {
        fun a(view: View, i2: Int)
    }

    companion object {
        @JvmField var J: String = "Blank"
        @JvmField val K: Handler = Handler(Looper.getMainLooper())

        @JvmStatic
        fun B(layout: LinearLayout, z: Boolean, i2: Int) {
            if (z) return
            for (i in layout.childCount - 1 downTo 0) {
                val child = layout.getChildAt(i)
                child.translationX = if (i2 == 1) 22.0f else -22.0f
                child.animate().translationX(0.0f).setDuration(400L)
                    .setStartDelay(((layout.childCount - i - 1) * 28).toLong())
                    .setInterpolator(OvershootInterpolator(0.8f)).start()
            }
        }
    }

    @JvmField var B: Int = D.size
    @JvmField var C: Any? = null
    @JvmField var E: OnItemClickListener? = null
    @JvmField var F: OnContextMenuListener? = null
    @JvmField var G: android.view.animation.Animation? = null
    @JvmField var H: android.view.animation.Animation? = null
    @JvmField var I: android.view.animation.Animation? = null
    @JvmField var y: Int = -1
    @JvmField var z: Int = -2
    @JvmField var A: Int = -1

    /**
     * Hapus pin taskbar berdasarkan package name (`E(String)`).
     * Dinamai ulang karena field listener `E` sudah memakai nama itu di Kotlin.
     */
    fun removePinnedApp(str: String) {
        val index = TaskBarDB.b[str] ?: return
        d(index)
    }

    fun a(str: String) {
        val exists = TaskBarDB.b.containsKey(str)
        val pins = TaskBarDB.b().toMutableList()
        if (pins.none { it.a() == str }) {
            pins.add(TaskBarItemPins(str, pins.size))
            TaskBarDB.e(pins)
        }
        if (exists) {
            TaskBarDB.b[str]?.let { D.getOrNull(it)?.o() }
            return
        }
        TaskBarDB.b[str] = TaskBarDB.b.size
        D.add(TaskBarItem(str, true))
        TaskBarDB.b[str] = D.size - 1
        B = D.size - 1
        k(D.size - 1)
    }

    fun c(str: String): Boolean {
        val index = TaskBarDB.b[str] ?: return false
        if (index < 0) return false
        val item = D.getOrNull(index) ?: run {
            TaskBarDB.b.remove(str)
            return false
        }
        return item.h()
    }

    fun d(i2: Int) {
        if (i2 !in D.indices) return
        D.removeAt(i2)
        TaskBarDB.c(D)
        B = D.size
        p(i2)
        val pins = TaskBarDB.b().toMutableList()
        if (i2 in pins.indices) {
            pins.removeAt(i2)
            TaskBarDB.e(pins)
        }
    }

    fun e(str: String) {
        TaskBarDB.b[str]?.let { d(it) }
    }

    fun f(holder: ViewHolder) {
        holder.itemView.isActivated = true
        holder.u.backgroundTintList = null
        G?.let { holder.u.startAnimation(it) }
    }

    fun g(str: String, notify: Boolean, multiple: Boolean) {
        val index = TaskBarDB.b[str] ?: return
        D.getOrNull(index)?.n(multiple)
        if (notify) i(index)
    }

    fun h(holder: ViewHolder) {
        holder.itemView.isActivated = false
        holder.u.backgroundTintList = ColorStateList.valueOf(0)
        I?.let { holder.u.startAnimation(it) }
    }

    fun i(holder: ViewHolder) {
        holder.itemView.isActivated = false
        holder.u.backgroundTintList = ContextCompat.getColorStateList(x, R.color.taskbar_app_inactive_indicator)
        H?.let { holder.u.startAnimation(it) }
    }

    fun j(i2: Int, z: Boolean) {
        if (i2 in D.indices) D[i2].m(z)
    }

    @JvmOverloads
    fun i(index: Int) {
        notifyItemChanged(index)
    }

    @JvmOverloads
    fun k(index: Int) {
        notifyItemInserted(index)
    }

    @JvmOverloads
    fun l(from: Int, to: Int) {
        notifyItemMoved(from, to)
    }

    @JvmOverloads
    fun p(index: Int) {
        notifyItemRemoved(index)
    }

    /** Alias: set minimized state for task at index. */
    fun J(i2: Int, z: Boolean) = j(i2, z)

    /** Alias: update running/multi-instance indicator. */
    fun G(str: String, notify: Boolean, multiple: Boolean) = g(str, notify, multiple)

    override fun b(i2: Int) {
        if (i2 !in D.indices) return
        D.removeAt(i2)
        TaskBarDB.c(D)
        B = D.size
        p(i2)
    }

    override fun c(i2: Int, i3: Int) {
        Collections.swap(D, i2, i3)
        l(i2, i3)
        TaskBarDB.c(D)
    }

    override fun getItemCount(): Int = D.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = D[position]
        val name = item.a()
        holder.v.tag = name
        IconEngine.b(x).c(name, object : IconEngine.Callback {
            override fun i(bitmap: android.graphics.Bitmap?) {
                holder.v.setImageBitmap(bitmap)
            }
        })
        holder.itemView.isSelected = item.d()
        val view = holder.itemView
        if (D.size <= B || z == position || position == Windows.b) {
            when (y) {
                0 -> if (position == Windows.b) {
                    view.isActivated = false
                    holder.u.backgroundTintList = ColorStateList.valueOf(0)
                    K.postDelayed({ h(holder) }, 800L)
                } else if (position == z) {
                    i(holder)
                }
                1 -> if (position == A) {
                    h(holder)
                    A = -1
                }
                2 -> if (position == Windows.b) f(holder) else if (position == z) i(holder)
                3 -> if (position == Windows.b) f(holder) else if (position == z) h(holder)
                4 -> if (position == Windows.b) i(holder)
                5 -> f(holder)
                else -> {
                    if (position != Windows.b || item.c()) {
                        if (!item.c() || item.b()) h(holder) else i(holder)
                    }
                }
            }
        } else {
            h(holder)
        }
        view.setOnClickListener { E?.a(view, holder.bindingAdapterPosition) }
        view.setOnLongClickListener {
            F?.a(view, holder.bindingAdapterPosition) != null
        }
        view.setOnGenericMotionListener { _, event ->
            if (event.buttonState != 2) return@setOnGenericMotionListener false
            F?.a(view, holder.bindingAdapterPosition)
            true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.taskbar_item, parent, false)
        if (G == null || H == null || I == null) {
            G = AnimationUtils.loadAnimation(view.context, R.anim.taskbar_app_indicator_active)
            H = AnimationUtils.loadAnimation(view.context, R.anim.taskbar_app_indicator_inactive)
            I = AnimationUtils.loadAnimation(view.context, R.anim.taskbar_app_indicator_close)
        }
        return ViewHolder(view)
    }
}
