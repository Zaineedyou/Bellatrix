package com.bellatrixos.widgets

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.context_menu.UIContextMenu
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t

/** Efek tekan ringan yang dipakai semua tombol kustom. */
internal fun View.applyPressEffect() {
    setOnTouchListener { v, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start(); false }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.animate().scaleX(1f).scaleY(1f).setDuration(80).start(); false }
            else -> false
        }
    }
}

internal fun View.btnTouch(event: MotionEvent): Boolean {
    BtnMouseBehavior.b(this, event)
    return false
}

open class BtnImageButton @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    AppCompatImageButton(c, a) {
    init { BtnMouseBehavior.f4849a.a(this) }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        BtnMouseBehavior.b(this, event)
        return super.onTouchEvent(event)
    }
}

open class BtnLinearLayout @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    LinearLayout(c, a) {
    init { BtnMouseBehavior.f4849a.a(this) }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        BtnMouseBehavior.b(this, event)
        return super.onTouchEvent(event)
    }
}

open class BtnRelativeLayout @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    RelativeLayout(c, a) {
    init { BtnMouseBehavior.f4849a.a(this) }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        BtnMouseBehavior.b(this, event)
        return super.onTouchEvent(event)
    }
}

open class BtnConstraintLayout @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    ConstraintLayout(c, a) {
    init { BtnMouseBehavior.f4849a.a(this) }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        BtnMouseBehavior.b(this, event)
        return super.onTouchEvent(event)
    }
}

open class BtnTextView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    AppCompatTextView(c, a) {
    init { BtnMouseBehavior.f4849a.a(this) }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        BtnMouseBehavior.b(this, event)
        return super.onTouchEvent(event)
    }
}

open class BtnRadioButton @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    AppCompatRadioButton(c, a) {
    init { BtnMouseBehavior.f4849a.a(this) }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        BtnMouseBehavior.b(this, event)
        return super.onTouchEvent(event)
    }
}

open class BtnSpinner @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    AppCompatSpinner(c, a) {
    init { BtnMouseBehavior.f4849a.a(this) }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        BtnMouseBehavior.b(this, event)
        return super.onTouchEvent(event)
    }
}

/** Header panel file explorer: jumlah item dan tombol ganti tampilan. */
class ExplorerFilesInfoView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    LinearLayout(c, a) {
    fun interface OnViewChangeListener {
        fun a()
    }

    @JvmField var u: TextView? = null
    @JvmField var v: ImageButton? = null
    @JvmField var w: ImageButton? = null
    @JvmField var x: OnViewChangeListener? = null

    fun a(i2: Int, i3: Int) {
        v?.isSelected = i2 == 1
        w?.isSelected = i2 == 0
        u?.text = context.resources.getQuantityString(
            com.bellatrixos.R.plurals.explorer_files_info, i3, i3
        )
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        u = findViewById(com.bellatrixos.R.id.filesInfoText)
        v = findViewById(com.bellatrixos.R.id.listViewButton)
        w = findViewById(com.bellatrixos.R.id.gridViewButton)
        v?.setOnClickListener { x?.a() }
        w?.setOnClickListener { x?.a() }
    }

    fun setCurrentViewStyle(i2: Int) {
        v?.isSelected = i2 == 1
        w?.isSelected = i2 == 0
    }

    fun setInfoText(text: CharSequence?) { u?.text = text }

    fun setItemsCount(count: Int) {
        u?.text = context.resources.getQuantityString(
            com.bellatrixos.R.plurals.explorer_files_info, count, count
        )
    }

    fun setOnViewChangeListener(listener: OnViewChangeListener?) { x = listener }
}

/** GridLayoutManager yang menyesuaikan jumlah kolom dengan lebar item. */
class GridAutofillLayoutManager(context: Context) : GridLayoutManager(context, 1) {
    @JvmField var M: Context = context
    @JvmField var N: Float = 0.0f
    @JvmField var O: Float = -1.0f
    @JvmField var P: Int = -1
    @JvmField var Q: Int = -1

    init { r1(-1) }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        val widthSize = width
        val heightSize = height
        if (widthSize > 0 && heightSize > 0 && N > 0.0f &&
            (widthSize != P || heightSize != Q || N != O)
        ) {
            val available = if (orientation == RecyclerView.VERTICAL) {
                widthSize - paddingLeft - paddingRight
            } else {
                heightSize - paddingTop - paddingBottom
            }
            spanCount = maxOf(1, kotlin.math.floor(available / N).toInt())
            O = N
            P = widthSize
            Q = heightSize
        }
        super.onLayoutChildren(recycler, state)
    }

    fun r1(i2: Int) {
        N = if (i2 >= 0) {
            M.resources.getDimension(i2)
        } else {
            M.resources.getDimension(com.bellatrixos.R.dimen.explorer_file_width)
        }
    }
}

/** EditText dengan geser vertikal halus untuk teks panjang. */
class SmoothScrollEditText @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    AppCompatEditText(c, a) {
    @JvmField val A: Int = ViewConfiguration.get(c).scaledTouchSlop
    @JvmField val B: android.widget.OverScroller = android.widget.OverScroller(c)
    @JvmField var C: VelocityTracker? = null
    @JvmField var D: Boolean = false
    @JvmField var E: Boolean = false
    @JvmField var F: Float = 0.0f
    @JvmField var G: Float = 0.0f
    @JvmField var H: Float = 0.0f
    @JvmField var I: Int = 0

    private fun maxScrollY(): Int {
        val layout = layout ?: return 0
        return maxOf(0, totalPaddingBottom + (totalPaddingTop + layout.height) - height)
    }

    fun b() {
        if (layout != null) I = maxScrollY()
    }

    override fun computeScroll() {
        super.computeScroll()
        if (B.computeScrollOffset()) {
            val currY = B.currY
            if (currY != scrollY) scrollTo(scrollX, currY)
            postInvalidateOnAnimation()
        }
    }

    override fun getText(): android.text.Editable =
        super.getText() ?: android.text.Editable.Factory.getInstance().newEditable("")

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        I = maxScrollY()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        I = maxScrollY()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE) return super.onTouchEvent(event)
        if (selectionStart != selectionEnd) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                F = event.x
                G = event.y
                H = event.y
                D = false
                if (B.isFinished) {
                    E = false
                } else {
                    B.abortAnimation()
                    E = true
                }
                C?.let { it.clear() } ?: VelocityTracker.obtain().also { C = it }
                C?.addMovement(event)
                if (E) return true
            }
            MotionEvent.ACTION_MOVE -> {
                C?.addMovement(event)
                val dx = abs(event.x - F)
                val dy = abs(event.y - G)
                if (!D && (E || (dy > A && dy > dx))) {
                    D = true
                    E = true
                    H = event.y
                    if (selectionStart != selectionEnd) setSelection(selectionEnd)
                }
                if (D) {
                    val delta = H - event.y
                    H = event.y
                    if (abs(delta) >= 1.0f) {
                        val target = maxOf(0, minOf(scrollY + delta.toInt(), I))
                        if (target != scrollY) scrollTo(0, target)
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (D) {
                    C?.let { tracker ->
                        tracker.addMovement(event)
                        tracker.computeCurrentVelocity(1000)
                        B.fling(0, scrollY, 0, (-tracker.yVelocity).toInt(), 0, 0, 0, I)
                        postInvalidateOnAnimation()
                        tracker.recycle()
                        C = null
                    }
                    D = false
                    E = false
                    return true
                }
                C?.recycle()
                C = null
                E = false
            }
            MotionEvent.ACTION_CANCEL -> {
                D = false
                E = false
                C?.recycle()
                C = null
            }
        }
        return super.onTouchEvent(event)
    }
}

class FilesRecyclerView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    RecyclerView(c, a) {
    fun interface OnEmptyLongPressListener {
        fun e(iArr: IntArray)
    }

    fun interface OnEmptyPressListener {
        fun a()
    }

    companion object { const val F1: Int = 0 }

    @JvmField val A1: Int = ViewConfiguration.get(c).scaledTouchSlop
    @JvmField var B1: Boolean = false
    @JvmField var C1: Boolean = false
    @JvmField val D1: com.bellatrixos.desktop.c = com.bellatrixos.desktop.c(this, 1)
    @JvmField val E1: Runnable = object : Runnable {
        override fun run() {
            if (!o1) {
                k1 = 0.0f
                return
            }
            val position = n1
            val speed = if (position >= 150.0f) {
                if (position > height - 150.0f) {
                    80.0f * ((position - (height - 150.0f)) / 150.0f)
                } else {
                    0.0f
                }
            } else {
                -80.0f * ((150.0f - position) / 150.0f)
            }
            k1 = lerp(speed, k1, 0.2f)
            if (abs(k1) > 0.5f) {
                l1 = false
                k1 = 0.0f
                removeCallbacks(E1)
                return
            }
            scrollBy(0, k1.toInt())
            r1?.let { overlay ->
                overlay.x = m1
                overlay.y = n1
                if (overlay.visibility != View.VISIBLE) overlay.visibility = View.VISIBLE
                overlay.invalidate()
                p0(overlay.getSelectionRect())
            }
            postOnAnimation(this)
        }
    }
    @JvmField val e1: GestureDetector = GestureDetector(c, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(event: MotionEvent) { o0(event, true) }
        override fun onSingleTapUp(event: MotionEvent): Boolean = o0(event, false)
    })
    @JvmField var f1: OnEmptyPressListener? = null
    @JvmField var g1: OnEmptyLongPressListener? = null
    @JvmField var h1: com.bellatrixos.app_explorer.files.interfaces.OnContextMenuListener<Any>? = null
    @JvmField val i1: IntArray = intArrayOf(10, 10)
    @JvmField var j1: Float = 0.0f
    @JvmField var k1: Float = 0.0f
    @JvmField var l1: Boolean = false
    @JvmField var m1: Float = 0.0f
    @JvmField var n1: Float = 0.0f
    @JvmField var o1: Boolean = false
    @JvmField var p1: Boolean = false
    @JvmField var q1: Boolean = false
    @JvmField var r1: SelectionOverlayView? = null
    @JvmField var s1: Int = 0
    @JvmField val t1: HashSet<Int> = HashSet()
    @JvmField val u1: HashSet<Int> = HashSet()
    @JvmField val v1: Rect = Rect()
    @JvmField var w1: Boolean = false
    @JvmField var x1: Float = 0.0f
    @JvmField var y1: Float = 0.0f
    @JvmField val z1: IntArray = IntArray(2)

    fun n0(overlay: SelectionOverlayView) {
        r1 = overlay
        j1 = resources.getDimension(com.bellatrixos.R.dimen.ui_2_4dp)
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                s1 = Integer.compare(dy, 0)
            }
        })
    }

    fun o0(event: MotionEvent, longPress: Boolean): Boolean {
        if (layoutManager == null) return false
        val rawX = event.rawX.toInt()
        val rawY = event.rawY.toInt()
        getLocationOnScreen(z1)
        if (findChildViewUnder((rawX - z1[0]).toFloat(), (rawY - z1[1]).toFloat()) != null && !p1) {
            return false
        }
        val adapter = adapter
        val fileAdapter = adapter as? com.bellatrixos.app_explorer.files.FileAdapter
        val hasItems = fileAdapter?.I?.any { fileAdapter.G(it) != -1 } == true
        if (!longPress) {
            f1?.let { it.a(); return true }
        }
        i1[0] = rawX
        i1[1] = rawY
        if (hasItems) {
            if (longPress && fileAdapter != null) {
                val file = fileAdapter.I.firstOrNull()
                if (file != null) {
                    h1?.d(i1, com.bellatrixos.windows.files.FileMeta(file))
                    return true
                }
            }
        } else if (longPress) {
            g1?.let { it.e(i1); return true }
        }
        return false
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.buttonState == 2) return o0(event, true)
        if (event.buttonState == 1) return o0(event, false)
        super.onGenericMotionEvent(event)
        return false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (r1 == null) return super.onInterceptTouchEvent(event)
        getLocationOnScreen(z1)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                x1 = event.rawX - z1[0]
                y1 = event.rawY - z1[1]
                w1 = false
                o1 = false
                B1 = event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE
                C1 = false
                if (!B1) postDelayed(D1, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_UP -> removeCallbacks(D1)
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - z1[0]) - x1
                val dy = (event.rawY - z1[1]) - y1
                val distance = kotlin.math.sqrt(dy * dy + dx * dx).toFloat()
                if (!B1 && !C1 && distance > A1) {
                    removeCallbacks(D1)
                } else if ((B1 && distance > j1) || (C1 && distance > j1)) {
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> removeCallbacks(D1)
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        e1.onTouchEvent(event)
        if (r1 == null) return super.onTouchEvent(event)
        getLocationOnScreen(z1)
        val rawX = event.rawX - z1[0]
        val rawY = event.rawY - z1[1]
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                x1 = rawX
                y1 = rawY
                w1 = false
                o1 = false
                B1 = event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE
                C1 = false
                if (!B1) postDelayed(D1, ViewConfiguration.getLongPressTimeout().toLong())
                return true
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(D1)
                l1 = false
                k1 = 0.0f
                removeCallbacks(E1)
                if (o1) {
                    r1?.let {
                        it.z = false
                        it.visibility = View.INVISIBLE
                        it.invalidate()
                    }
                    o1 = false
                    w1 = false
                    C1 = false
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = rawX - x1
                val dy = rawY - y1
                val distance = kotlin.math.sqrt(dy * dy + dx * dx).toFloat()
                m1 = rawX
                n1 = rawY
                if (B1 || C1 || distance <= A1) {
                    if ((B1 && distance > j1) || (C1 && distance > j1)) {
                        o1 = true
                        p1 = true
                        w1 = true
                        removeCallbacks(D1)
                        r1?.let {
                            it.x = x1
                            it.v = x1
                            it.y = y1
                            it.w = y1
                            it.z = true
                            it.invalidate()
                        }
                    }
                    if (o1 && w1) {
                        UIContextMenu.f4743f?.takeIf { it.isShowing }?.let {
                            it.dismiss()
                            UIContextMenu.f4743f = null
                        }
                        r1?.let {
                            it.x = rawX
                            it.y = rawY
                            if (it.visibility != View.VISIBLE) it.visibility = View.VISIBLE
                            it.invalidate()
                            p0(it.getSelectionRect())
                        }
                        if (!l1) {
                            l1 = true
                            postOnAnimation(E1)
                        }
                        return true
                    }
                } else {
                    removeCallbacks(D1)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(D1)
                l1 = false
                k1 = 0.0f
                removeCallbacks(E1)
                if (o1) {
                    r1?.let {
                        it.z = false
                        it.visibility = View.INVISIBLE
                        it.invalidate()
                    }
                    o1 = false
                    w1 = false
                    C1 = false
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun p0(rect: Rect) {
        val adapter = adapter as? com.bellatrixos.app_explorer.files.FileAdapter ?: return
        t1.clear()
        findChildViewUnder(x1, y1)?.let { view ->
            getChildViewHolder(view)?.bindingAdapterPosition?.let { position ->
                if (position != RecyclerView.NO_POSITION) {
                    adapter.J(position, true)
                    t1.add(position)
                }
            }
        }
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            child.getHitRect(v1)
            if (Rect.intersects(rect, v1)) {
                getChildViewHolder(child)?.bindingAdapterPosition?.let { position ->
                    if (position != RecyclerView.NO_POSITION) t1.add(position)
                }
            }
        }
        if (s1 == 0 || !l1) {
            for (position in u1) if (!t1.contains(position)) adapter.J(position, false)
            for (position in t1) if (!u1.contains(position)) adapter.J(position, true)
            u1.clear()
        } else {
            for (position in t1) if (!u1.contains(position)) adapter.J(position, true)
        }
        u1.addAll(t1)
        if (!q1 || t1.isEmpty()) return
        val min = t1.minOrNull() ?: return
        var anchor = -1
        for (i in 0 until adapter.itemCount) {
            val item = adapter.currentList.getOrNull(i)
            if (item is com.bellatrixos.windows.files.FileMeta && adapter.I.contains(item.file)) {
                anchor = i
                break
            }
        }
        if (anchor == -1) return
        val from = minOf(anchor, min)
        val to = maxOf(anchor, min)
        for (i in from..to) {
            val item = adapter.currentList.getOrNull(i)
            if (item is com.bellatrixos.windows.files.FileMeta) adapter.I.add(item.file)
        }
        adapter.notifyItemRangeChanged(
            from, to - from + 1,
            com.bellatrixos.app_explorer.files.FileAdapter.L
        )
    }

    fun setContextMenuListener(listener: com.bellatrixos.app_explorer.files.interfaces.OnContextMenuListener<Any>?) {
        h1 = listener
    }

    fun setOnEmptyLongPressListener(listener: OnEmptyLongPressListener?) { g1 = listener }

    fun setOnEmptyPressListener(listener: OnEmptyPressListener?) {
        f1 = listener
        p1 = false
    }

    fun setShiftPressed(value: Boolean) { q1 = value }
}

class FloatingLayout @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    FrameLayout(c, a) {
    fun interface OnAppFocusListener { fun a() }
    fun interface OnWindowTapListener { fun c() }

    companion object { const val M: Int = 0 }

    @JvmField val A: Float
    @JvmField var B: Boolean = true
    @JvmField var C: OnAppFocusListener? = null
    @JvmField var D: OnWindowTapListener? = null
    @JvmField val E: Int
    @JvmField val F: Int
    @JvmField val G: Int
    @JvmField val H: android.graphics.drawable.GradientDrawable
    @JvmField val I: android.graphics.drawable.GradientDrawable
    @JvmField var J: View? = null
    @JvmField var K: Runnable? = null
    @JvmField var L: Boolean = false
    @JvmField val u: Paint = Paint(2)
    @JvmField var v: Float = 0f
    @JvmField var w: Float = 0f
    @JvmField var x: Float = 0f
    @JvmField var y: Float = 0f
    @JvmField var z: Float = 0f

    init {
        if (com.bellatrixos.variables.Global.b) setWillNotDraw(false)
        u.alpha = if (com.bellatrixos.variables.Global.f4839f) 11 else 18
        A = c.resources.getDimension(com.bellatrixos.R.dimen.ui_38dp)
        isClickable = true
        val border = androidx.core.content.ContextCompat.getDrawable(c, com.bellatrixos.R.drawable.app_dialog_border)
        I = (border as? android.graphics.drawable.GradientDrawable)?.mutate() as? android.graphics.drawable.GradientDrawable
            ?: android.graphics.drawable.GradientDrawable().also { it.setColor(0) }
        H = (background as? android.graphics.drawable.GradientDrawable)
            ?: (androidx.core.content.ContextCompat.getDrawable(c, com.bellatrixos.R.drawable.app_dialog_bg)
                as? android.graphics.drawable.GradientDrawable
                ?: android.graphics.drawable.GradientDrawable()).also { background = it }
        foreground = I
        E = androidx.core.content.ContextCompat.getColor(c, com.bellatrixos.R.color.app_dialog_border_app)
        F = c.resources.getDimensionPixelSize(com.bellatrixos.R.dimen.ui_10dp)
        G = c.resources.getDimensionPixelSize(com.bellatrixos.R.dimen.ui_1dp)
    }

    fun a() {
        val bitmap = com.bellatrixos.windows.blur.UIBlurImage.f4886a ?: return
        v = maxOf(
            com.bellatrixos.windows.WindowSize.f4871a.toFloat() / bitmap.width,
            com.bellatrixos.windows.WindowSize.b.toFloat() / bitmap.height
        )
        w = bitmap.width * v
        x = bitmap.height * v
    }

    override fun dispatchDraw(canvas: Canvas) {
        val bitmap = com.bellatrixos.windows.blur.UIBlurImage.f4886a
        if (bitmap != null) {
            canvas.save()
            canvas.translate(
                (com.bellatrixos.windows.WindowSize.f4871a - w) * 0.5f - y,
                (com.bellatrixos.windows.WindowSize.m - x) * 0.5f - z
            )
            canvas.scale(v, v)
            canvas.drawBitmap(bitmap, 0f, 0f, u)
            canvas.restore()
        }
        super.dispatchDraw(canvas)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Any press restores focus, including the titlebar band. Limiting this to
        // the content area left an unfocused window unable to be dragged or resized.
        if (!B && event.action == MotionEvent.ACTION_DOWN) C?.a()
        if (event.action == MotionEvent.ACTION_DOWN) D?.c()
        return super.dispatchTouchEvent(event)
    }

    fun getDragHandle(): View? = J

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        super.onGenericMotionEvent(event)
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        a()
    }

    fun setBgStyle(style: Int) {
        if (style == 0 && com.bellatrixos.variables.Global.f4836a == 1) {
            H.cornerRadius = F.toFloat()
            I.setStroke(G, 0)
        } else {
            H.cornerRadius = 0f
            I.setStroke(G, E)
        }
        invalidate()
    }

    fun setBlurEnabled(enabled: Boolean) {
        setWillNotDraw(!enabled)
        if (enabled) a()
        invalidate()
    }

    fun setFocusState(focused: Boolean) { B = focused }
    fun setOnFocusListener(listener: OnAppFocusListener?) { C = listener }
    fun setOnWindowTapListener(listener: OnWindowTapListener?) { D = listener }
}

class MaxWidthHorizontalScrollView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    HorizontalScrollView(c, a) {
    var maxWidthPx: Int = -1
    fun getMaxWidth(): Int = maxWidthPx
    fun setMaxWidth(value: Int) {
        maxWidthPx = value
        requestLayout()
    }
    override fun onMeasure(w: Int, h: Int) {
        var width = w
        if (maxWidthPx > 0) {
            val mode = View.MeasureSpec.getMode(width)
            val size = View.MeasureSpec.getSize(width)
            if (size > maxWidthPx) width = View.MeasureSpec.makeMeasureSpec(maxWidthPx, mode)
        }
        super.onMeasure(width, h)
    }
}

class SelectionOverlayView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    View(c, a) {
    @JvmField val u: Paint = Paint().apply {
        color = android.graphics.Color.argb(80, 0, 120, 215)
        style = Paint.Style.FILL
    }
    @JvmField var v: Float = 0.0f
    @JvmField var w: Float = 0.0f
    @JvmField var x: Float = 0.0f
    @JvmField var y: Float = 0.0f
    @JvmField var z: Boolean = false

    fun getSelectionRect(): android.graphics.Rect = android.graphics.Rect(
        minOf(v, x).toInt(), minOf(w, y).toInt(),
        maxOf(v, x).toInt(), maxOf(w, y).toInt()
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (z) canvas.drawRect(getSelectionRect(), u)
    }
}

class TaskbarBtn @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    FrameLayout(c, a) {
    @JvmField var u: ImageView
    @JvmField var v: RelativeLayout

    init {
        View.inflate(c, com.bellatrixos.R.layout.taskbar_item, this)
        v = findViewById(com.bellatrixos.R.id.task_app)
        u = findViewById(com.bellatrixos.R.id.icon)
        if (a != null) {
            val array = c.obtainStyledAttributes(a, com.bellatrixos.R.styleable.f4629a)
            array.getDrawable(0)?.let { setIcon(it) }
            val label = array.getString(1)
            if (label != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.tooltipText = label
            }
            array.recycle()
        }
    }

    fun setIcon(resId: Int) { u.setImageResource(resId) }
    fun setIcon(drawable: android.graphics.drawable.Drawable?) { u.setImageDrawable(drawable) }
    fun setOnBtnClickListener(listener: OnClickListener?) { v.setOnClickListener(listener) }
    fun setOnBtnOnTouchListener(listener: OnTouchListener?) { v.setOnTouchListener(listener) }
}

class UINavigationBar @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    FrameLayout(c, a) {
    fun interface OnExpandStateChangedListener {
        fun a(z: Boolean)
    }

    companion object { const val C: Int = 0 }

    @JvmField var A: Int = -1
    @JvmField var B: OnExpandStateChangedListener? = null
    @JvmField val u: View
    @JvmField var v: RadioGroup? = null
    @JvmField var w: Boolean = true
    @JvmField var x: Boolean = false
    @JvmField val y: Int
    @JvmField val z: Int

    init {
        View.inflate(c, com.bellatrixos.R.layout.ui_navigation_bar, this)
        u = findViewById(com.bellatrixos.R.id.nav_container)
        v = findViewById(com.bellatrixos.R.id.nav_items)
        y = c.resources.getDimensionPixelSize(com.bellatrixos.R.dimen.ui_220dp)
        z = c.resources.getDimensionPixelSize(com.bellatrixos.R.dimen.ui_60dp)
        findViewById<ImageButton>(com.bellatrixos.R.id.nav_toggle).setOnClickListener {
            if (w) c(false) else d()
        }
        post { a(this) }
    }

    private fun a(bar: UINavigationBar) {
        val group = bar.v ?: return
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child is BtnRadioButton && child.tag == null) child.tag = child.text
        }
    }

    fun b(widgetWidth: Int) {
        android.animation.ValueAnimator.ofInt(u.layoutParams.width, widgetWidth).apply {
            duration = 200L
            addUpdateListener(com.bellatrixos.widgets.d(this@UINavigationBar, 0))
            start()
        }
    }

    fun c(collapsed: Boolean) {
        w = false
        x = collapsed
        b(z)
        e(true)
        B?.a(w)
    }

    fun d() {
        w = true
        b(y)
        e(false)
        B?.a(w)
    }

    fun e(fadeText: Boolean) {
        val group = v ?: return
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child !is BtnRadioButton) continue
            val target = if (fadeText) android.R.color.transparent else com.bellatrixos.R.color.text_default
            android.animation.ValueAnimator.ofArgb(
                child.currentTextColor,
                androidx.core.content.ContextCompat.getColor(context, target)
            ).apply {
                duration = 150L
                addUpdateListener(com.bellatrixos.widgets.d(child, 1))
                start()
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is RadioGroup) {
                v = child
                break
            }
        }
        v?.let {
            removeView(it)
            (u as ViewGroup).addView(it)
        }
        post { a(this) }
    }

    fun setExpanded(expanded: Boolean) {
        if (expanded) d() else c(false)
    }

    fun setNavigationItem(id: Int) { v?.check(id) }
    fun setOnExpandStateChangedListener(listener: OnExpandStateChangedListener?) { B = listener }

    fun setOnItemSelectedListener(listener: RadioGroup.OnCheckedChangeListener?) {
        v?.setOnCheckedChangeListener { group, id ->
            if (id == -1) return@setOnCheckedChangeListener
            if (id == A) return@setOnCheckedChangeListener
            A = id
            listener?.onCheckedChanged(group, id)
        }
    }
}

class UIProgressBarLinear @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    View(c, a) {
    companion object { const val G: Int = 0 }

    @JvmField val A: Int
    @JvmField var B: Int
    @JvmField var C: Boolean
    @JvmField var D: Float = 0.0f
    @JvmField var E: Float = 0.0f
    @JvmField var F: android.animation.ValueAnimator? = null
    @JvmField val u: Paint = Paint(1)
    @JvmField val v: RectF = RectF()
    @JvmField var w: Int = 0
    @JvmField var x: Int = 0
    @JvmField val y: RectF = RectF()
    @JvmField val z: Path = Path()

    init {
        val array = c.obtainStyledAttributes(a, com.bellatrixos.R.styleable.b, 0, 0)
        A = array.getDimensionPixelSize(
            2,
            (c.resources.displayMetrics.density * 4.0f).toInt()
        )
        B = array.getColor(1, androidx.core.content.ContextCompat.getColor(c, com.bellatrixos.R.color.color_primary_default))
        C = array.getBoolean(0, false)
        array.recycle()
        b()
    }

    fun a() {
        if (F == null) {
            F = android.animation.ValueAnimator.ofFloat(0.0f, 1.0f).apply {
                duration = 2000L
                repeatCount = -1
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                addUpdateListener(com.bellatrixos.widgets.d(this@UIProgressBarLinear, 2))
            }
        }
        if (F?.isRunning != true) F?.start()
    }

    fun b() {
        u.color = B
        u.style = Paint.Style.FILL
        u.strokeWidth = A.toFloat()
        u.strokeCap = Paint.Cap.ROUND
    }

    fun getColor(): Int = B

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (C) postDelayed({ a() }, 600L)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        F?.cancel()
        F = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val count = canvas.save()
        canvas.clipPath(z)
        canvas.drawRoundRect(v, A / 2.0f, A / 2.0f, u)
        canvas.restoreToCount(count)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        w = measuredWidth
        x = measuredHeight
        setMeasuredDimension(w, x)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        y.set(0.0f, 0.0f, width.toFloat(), height.toFloat())
        val radius = x / 2.0f
        z.reset()
        z.addRoundRect(y, radius, radius, Path.Direction.CW)
    }

    fun setAnimating(animating: Boolean) {
        if (C == animating) return
        C = animating
        if (animating) a() else F?.cancel()
    }

    fun setColor(color: Int) {
        B = color
        b()
        invalidate()
    }
}

class UIProgressCircular @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    View(c, a) {
    companion object { const val G: Int = 0 }

    @JvmField var A: Int = 0
    @JvmField val B: Int
    @JvmField val C: Int
    @JvmField var D: Float = 0.0f
    @JvmField var E: Float = 0.0f
    @JvmField var F: Float = -90.0f
    @JvmField val u: Paint = Paint(1)
    @JvmField var v: Int = 0
    @JvmField val w: RectF = RectF()
    @JvmField var x: Boolean = true
    @JvmField var y: android.animation.AnimatorSet? = null
    @JvmField var z: Int = 0

    init {
        val array = c.obtainStyledAttributes(a, com.bellatrixos.R.styleable.c, 0, 0)
        z = array.getDimensionPixelSize(1, (c.resources.displayMetrics.density * 4.0f).toInt())
        A = array.getColor(0, androidx.core.content.ContextCompat.getColor(c, com.bellatrixos.R.color.color_primary_default))
        B = 3600
        C = 2
        array.recycle()
        d()
    }

    fun a() {
        x = false
        visibility = View.VISIBLE
        y = android.animation.AnimatorSet()
        val set = y ?: return
        var previous: android.animation.AnimatorSet? = null
        for (i in 0 until C) {
            val f3 = ((C - 1) * 360.0f) / C + 15.0f
            val f4 = lerp(f3, 15.0f, i.toFloat())
            val v1 = android.animation.ValueAnimator.ofFloat(15.0f, f3).apply {
                duration = (B / C / 2).toLong()
                interpolator = android.view.animation.DecelerateInterpolator(1.0f)
                addUpdateListener { D = it.animatedValue as Float; invalidate() }
            }
            val v2 = android.animation.ValueAnimator.ofFloat(
                i * 1440.0f / C, (i + 0.5f) * 1440.0f / C
            ).apply {
                duration = (B / C / 2).toLong()
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener { E = it.animatedValue as Float }
            }
            val v3 = android.animation.ValueAnimator.ofFloat(f4, f4 + f3 - 15.0f).apply {
                duration = (B / C / 2).toLong()
                interpolator = android.view.animation.DecelerateInterpolator(1.0f)
                addUpdateListener {
                    val value = it.animatedValue as Float
                    F = value
                    D = f3 - value + f4
                    invalidate()
                }
            }
            val v4 = android.animation.ValueAnimator.ofFloat(
                (i + 0.5f) * 1440.0f / C, (i + 1.0f) * 1440.0f / C
            ).apply {
                duration = (B / C / 2).toLong()
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener { E = it.animatedValue as Float }
            }
            val inner = android.animation.AnimatorSet().apply {
                play(v1).with(v2)
                play(v3).with(v4).after(v2)
            }
            val builder = set.play(inner)
            previous?.let { builder.after(it) }
            previous = inner
        }
        set.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: android.animation.Animator) { x = true }
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (!x) a()
            }
        })
        set.start()
    }

    fun b() {
        y?.cancel()
        y = null
        F = -90.0f
        visibility = View.INVISIBLE
    }

    fun c() {
        val left = paddingLeft
        val top = paddingTop
        w.set(
            (left + z).toFloat(), (top + z).toFloat(),
            (v - left - z).toFloat(), (v - top - z).toFloat()
        )
    }

    fun d() {
        u.color = A
        u.style = Paint.Style.STROKE
        u.strokeWidth = z.toFloat()
        u.strokeCap = Paint.Cap.ROUND
    }

    fun getColor(): Int = A
    fun getThickness(): Int = z

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (y?.isRunning == true) y?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(w, E + F, D, false, u)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val horizontal = paddingLeft + paddingRight
        val vertical = paddingTop + paddingBottom
        val size = minOf(measuredWidth - horizontal, measuredHeight - vertical)
        v = size
        setMeasuredDimension(horizontal + size, size + vertical)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        v = minOf(width, height)
        c()
    }

    fun setColor(color: Int) { A = color; d(); invalidate() }
    fun setThickness(thickness: Int) { z = thickness; d(); c(); invalidate() }

    override fun setVisibility(visibility: Int) {
        val old = getVisibility()
        super.setVisibility(visibility)
        if (visibility != old) {
            if (visibility == View.VISIBLE) a() else if (visibility == View.INVISIBLE || visibility == View.GONE) b()
        }
    }
}

class NoUnderlineClickableSpan(
    @JvmField val f4853a: Any,
    @JvmField val b: android.content.res.ColorStateList
) : android.text.style.ClickableSpan() {
    companion object {
        @JvmField val f4851d: IntArray = intArrayOf(android.R.attr.state_pressed)
        @JvmField val f4852e: IntArray = IntArray(0)
    }

    @JvmField var c: Boolean = false

    override fun onClick(widget: View) {
        (f4853a as? View.OnClickListener)?.onClick(widget)
    }

    override fun updateDrawState(ds: android.text.TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = false
        ds.color = if (c) b.getColorForState(f4851d, b.defaultColor) else b.defaultColor
    }
}

class UISplashLayout @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    FrameLayout(c, a) {
    companion object { const val u: Int = 0 }

    init {
        android.view.LayoutInflater.from(c).inflate(com.bellatrixos.R.layout.ui_splash_layout, this, true)
        a?.let {
            val array = c.obtainStyledAttributes(it, com.bellatrixos.R.styleable.l)
            array.getDrawable(0)?.let { drawable ->
                findViewById<ImageView>(com.bellatrixos.R.id.splash_image)?.setImageDrawable(drawable)
            }
            array.recycle()
        }
    }

    /** Animasi hilang hanya saat tidak ada saved state (`a(Bundle)`). */
    fun a(bundle: Bundle?) {
        if (bundle == null) fadeOut()
    }

    fun fadeOut() {
        animate().alpha(0f).scaleX(0.98f).scaleY(0.98f).setDuration(220L).withEndAction { alpha = 0f }.start()
    }
}

abstract class TitleBar @JvmOverloads constructor(c: Context, a: AttributeSet? = null) :
    FrameLayout(c, a) {
    abstract fun setCloseBtnListener(listener: OnClickListener?)
    abstract fun setMinimizeBtnListener(listener: OnClickListener?)
    abstract fun setOnDragListener(listener: OnTouchListener?)
    abstract fun setResizeBtn(i2: Int)
    open fun setResizeBtnEnable(z: Boolean) {}
    abstract fun setResizeBtnListener(listener: OnClickListener?)
    abstract fun setTitle(str: String?)
    open fun setTitleIconStyle(i2: Int) {}
}

open class UITitleBar @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : TitleBar(c, a) {
    @JvmField val u: TextView
    @JvmField val v: ImageButton
    @JvmField val w: ImageButton
    @JvmField val x: ImageButton

    init {
        View.inflate(context, com.bellatrixos.R.layout.ui_app_titlebar, this)
        u = findViewById(com.bellatrixos.R.id.title)
        v = findViewById(com.bellatrixos.R.id.btn_close)
        w = findViewById(com.bellatrixos.R.id.btn_minimize)
        x = findViewById(com.bellatrixos.R.id.btn_resize)
        val array = c.obtainStyledAttributes(
            a,
            intArrayOf(
                com.bellatrixos.R.attr.ui_icon_src,
                com.bellatrixos.R.attr.ui_title,
                com.bellatrixos.R.attr.ui_resize_btn
            )
        )
        try {
            val icon = array.getResourceId(0, 0)
            val title = array.getString(1)
            val resizeVisible = array.getBoolean(2, true)
            if (icon != 0) {
                u.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ContextCompat.getDrawable(c, icon), null, null, null
                )
            }
            if (title != null) setTitle(title)
            if (!resizeVisible) x.visibility = View.GONE
        } finally {
            array.recycle()
        }
    }

    override fun setCloseBtnListener(listener: OnClickListener?) { v.setOnClickListener(listener) }
    override fun setMinimizeBtnListener(listener: OnClickListener?) { w.setOnClickListener(listener) }
    override fun setOnDragListener(listener: OnTouchListener?) {
        findViewById<View>(com.bellatrixos.R.id.title_bar)?.setOnTouchListener(listener)
    }
    override fun setResizeBtn(i2: Int) {
        x.setImageResource(
            if (i2 == 0) com.bellatrixos.R.drawable.app_title_ic_resize_15
            else com.bellatrixos.R.drawable.app_title_ic_restore_15
        )
    }
    override fun setResizeBtnEnable(z: Boolean) { x.isEnabled = z }
    override fun setResizeBtnListener(listener: OnClickListener?) { x.setOnClickListener(listener) }
    override fun setTitle(str: String?) { u.text = str }
    override fun setTitleIconStyle(i2: Int) {
        when (i2) {
            0 -> {
                u.setCompoundDrawablePadding(
                    resources.getDimensionPixelSize(com.bellatrixos.R.dimen.ui_12dp)
                )
                (u.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                    params.marginStart = resources.getDimensionPixelSize(com.bellatrixos.R.dimen.ui_12dp)
                    u.layoutParams = params
                }
            }
            1 -> {
                u.setCompoundDrawablePadding(
                    resources.getDimensionPixelSize(com.bellatrixos.R.dimen.ui_45dp)
                )
                (u.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                    params.marginStart = resources.getDimensionPixelSize(com.bellatrixos.R.dimen.ui_21dp)
                    u.layoutParams = params
                }
            }
        }
    }
}

class UITitleBarApp @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : TitleBar(c, a) {
    @JvmField val A: ImageButton
    @JvmField val B: ImageButton
    @JvmField val u: TextView
    @JvmField val v: LinearLayout
    @JvmField val w: ProgressBar
    @JvmField val x: ImageButton
    @JvmField val y: ImageButton
    @JvmField val z: ImageButton

    init {
        View.inflate(context, com.bellatrixos.R.layout.ui_app_titlebar_webapp, this)
        v = findViewById(com.bellatrixos.R.id.title_bar)
        u = findViewById(com.bellatrixos.R.id.title)
        x = findViewById(com.bellatrixos.R.id.btn_close)
        y = findViewById(com.bellatrixos.R.id.btn_minimize)
        z = findViewById(com.bellatrixos.R.id.btn_resize)
        A = findViewById(com.bellatrixos.R.id.btn_reload)
        B = findViewById(com.bellatrixos.R.id.btn_settings)
        w = findViewById(com.bellatrixos.R.id.btn_ProgressBar)
        a?.let {
            val array = c.obtainStyledAttributes(it, intArrayOf(com.bellatrixos.R.attr.ui_title))
            try {
                array.getString(0)?.let { title -> setTitle(title) }
            } finally {
                array.recycle()
            }
        }
    }

    /** `getBtnMore()` mengembalikan tombol settings/more. */
    fun getBtnMore(): View = B
    fun getSettingsView(): ImageButton = B
    override fun setCloseBtnListener(listener: OnClickListener?) { x.setOnClickListener(listener) }
    fun setLoading(z: Boolean) {
        w.visibility = if (z) View.VISIBLE else View.GONE
        A.visibility = if (z) View.GONE else View.VISIBLE
    }
    override fun setMinimizeBtnListener(listener: OnClickListener?) { y.setOnClickListener(listener) }
    override fun setOnDragListener(listener: OnTouchListener?) { v.setOnTouchListener(listener) }
    fun setReloadBtnListener(listener: OnClickListener?) { A.setOnClickListener(listener) }
    override fun setResizeBtn(i2: Int) {
        z.setImageResource(
            if (i2 == 0) com.bellatrixos.R.drawable.app_title_ic_resize_15
            else com.bellatrixos.R.drawable.app_title_ic_restore_15
        )
    }
    override fun setResizeBtnListener(listener: OnClickListener?) { z.setOnClickListener(listener) }
    fun setSettingsBtnListener(listener: OnClickListener?) { B.setOnClickListener(listener) }
    override fun setTitle(str: String?) { u.text = str }
}

class UITitleBarImage @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : TitleBar(c, a) {
    @JvmField val A: ImageButton
    @JvmField val B: ImageButton
    @JvmField val C: ImageButton
    @JvmField val u: TextView
    @JvmField val v: ConstraintLayout
    @JvmField val w: ImageButton
    @JvmField val x: ImageButton
    @JvmField val y: ImageButton
    @JvmField val z: ImageButton

    init {
        View.inflate(context, com.bellatrixos.R.layout.ui_app_titlebar_image, this)
        v = findViewById(com.bellatrixos.R.id.title_bar)
        u = findViewById(com.bellatrixos.R.id.title)
        w = findViewById(com.bellatrixos.R.id.btn_close)
        x = findViewById(com.bellatrixos.R.id.btn_minimize)
        y = findViewById(com.bellatrixos.R.id.btn_resize)
        z = findViewById(com.bellatrixos.R.id.btn_rotate)
        A = findViewById(com.bellatrixos.R.id.btn_print)
        B = findViewById(com.bellatrixos.R.id.btn_share)
        C = findViewById(com.bellatrixos.R.id.btn_more)
        a?.let {
            val array = c.obtainStyledAttributes(it, intArrayOf(com.bellatrixos.R.attr.ui_title))
            try {
                array.getString(0)?.let { title -> setTitle(title) }
            } finally {
                array.recycle()
            }
        }
        if (com.bellatrixos.variables.Global.f4836a == 1) {
            A.visibility = View.GONE
            B.visibility = View.GONE
        }
    }

    fun getMoreBtnView(): View = C
    override fun setCloseBtnListener(listener: OnClickListener?) { w.setOnClickListener(listener) }
    override fun setMinimizeBtnListener(listener: OnClickListener?) { x.setOnClickListener(listener) }
    fun setMoreBtnListener(listener: OnClickListener?) { C.setOnClickListener(listener) }
    override fun setOnDragListener(listener: OnTouchListener?) { v.setOnTouchListener(listener) }
    fun setPrintBtnListener(listener: OnClickListener?) { A.setOnClickListener(listener) }
    override fun setResizeBtn(i2: Int) {
        y.setImageResource(
            if (i2 == 0) com.bellatrixos.R.drawable.app_title_ic_resize_15
            else com.bellatrixos.R.drawable.app_title_ic_restore_15
        )
    }
    override fun setResizeBtnListener(listener: OnClickListener?) { y.setOnClickListener(listener) }
    fun setRotateBtnListener(listener: OnClickListener?) { z.setOnClickListener(listener) }
    fun setShareBtnListener(listener: OnClickListener?) { B.setOnClickListener(listener) }
    override fun setTitle(str: String?) { u.text = str }
}
open class UITitleBarSearch @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : TitleBar(c, a) {
    fun interface OnSearchSuggestionClickListener {
        fun b(str: String)
    }

    companion object { const val B: Int = 0 }

    @JvmField var A: OnSearchSuggestionClickListener? = null
    @JvmField val u: TextView
    @JvmField val v: androidx.constraintlayout.widget.ConstraintLayout
    @JvmField val w: android.widget.AutoCompleteTextView
    @JvmField val x: ImageButton
    @JvmField val y: ImageButton
    @JvmField val z: ImageButton

    @JvmOverloads
    constructor(
        c: Context,
        title: CharSequence?,
        a: AttributeSet? = null
    ) : this(c, a) {
        setTitle(title?.toString())
    }

    init {
        View.inflate(c, com.bellatrixos.R.layout.ui_app_titlebar_search, this)
        v = findViewById(com.bellatrixos.R.id.title_bar)
        w = findViewById(com.bellatrixos.R.id.searchBar)
        u = findViewById(com.bellatrixos.R.id.title)
        x = findViewById(com.bellatrixos.R.id.btn_close)
        y = findViewById(com.bellatrixos.R.id.btn_minimize)
        z = findViewById(com.bellatrixos.R.id.btn_resize)
        if (a != null) {
            val array = c.obtainStyledAttributes(
                a,
                intArrayOf(com.bellatrixos.R.attr.ui_icon_src, com.bellatrixos.R.attr.ui_title)
            )
            try {
                val icon = array.getResourceId(0, 0)
                val title = array.getString(1)
                if (icon != 0) {
                    u.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        androidx.core.content.ContextCompat.getDrawable(c, icon), null, null, null
                    )
                }
                if (title != null) setTitle(title)
            } finally {
                array.recycle()
            }
        }
    }

    override fun setCloseBtnListener(listener: OnClickListener?) { x.setOnClickListener(listener) }
    override fun setMinimizeBtnListener(listener: OnClickListener?) { y.setOnClickListener(listener) }
    override fun setOnDragListener(listener: OnTouchListener?) { v.setOnTouchListener(listener) }
    override fun setResizeBtn(i2: Int) {
        z.setImageResource(
            if (i2 == 0) com.bellatrixos.R.drawable.app_title_ic_resize_15
            else com.bellatrixos.R.drawable.app_title_ic_restore_15
        )
    }
    override fun setResizeBtnListener(listener: OnClickListener?) { z.setOnClickListener(listener) }
    override fun setTitle(str: String?) { u.text = str }

    fun setOnSearchSuggestionClickListener(listener: OnSearchSuggestionClickListener?) { A = listener }
}

/** Toast kustom yang dipanggil banyak modul. */
object UINotification {
    @JvmField var f4854a: String? = null
    @JvmField var b: PopupWindow? = null
    @JvmField val c: Handler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun a(context: Context, message: String?, title: String?) {
        b(context, message, title, 2000, "", null)
    }

    @JvmStatic
    fun b(
        context: Context,
        message: String?,
        title: String?,
        duration: Int,
        actionLabel: String?,
        action: Runnable?
    ) {
        val popup = b ?: run {
            val view = LayoutInflater.from(context).inflate(com.bellatrixos.R.layout.widget_toast, null, false)
            f4854a = context.resources.getString(com.bellatrixos.R.string.action_center)
            val close = view.findViewById<ImageButton>(com.bellatrixos.R.id.btn_close)
            val created = PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            b = created
            created.isOutsideTouchable = false
            created.isFocusable = false
            created.isClippingEnabled = false
            created.animationStyle = com.bellatrixos.R.style.AnimationWidgets
            close.setOnClickListener { created.dismiss() }
            created
        }
        val content = popup.contentView ?: return
        content.findViewById<TextView>(com.bellatrixos.R.id.toast_text).text = message
        content.findViewById<TextView>(com.bellatrixos.R.id.toast_title).text = title ?: f4854a
        val actionView = content.findViewById<TextView>(com.bellatrixos.R.id.btn_action)
        if (action != null) {
            actionView.text = actionLabel
            actionView.visibility = View.VISIBLE
            actionView.setOnClickListener { action.run() }
        } else {
            actionView.visibility = View.GONE
        }
        if (popup.isShowing) popup.dismiss()
        com.bellatrixos.windows.drivers.Sounds.a(1)
        val decor = (context as? Activity)?.window?.decorView ?: return
        if (decor.windowToken != null) {
            popup.showAtLocation(
                decor, 8388693,
                com.bellatrixos.windows.InsetManager.f4865d,
                com.bellatrixos.windows.InsetManager.b
            )
        }
        c.removeCallbacksAndMessages(null)
        c.postDelayed({ if (popup.isShowing) popup.dismiss() }, duration.toLong())
    }
}
