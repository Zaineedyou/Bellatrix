package com.bellatrixos.widgets.cards

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.animation.PathInterpolator
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.bellatrixos.R
import com.bellatrixos.widgets.UISpinnerAdapter

/** Kartu pengaturan dasar. Layout asli masih dipakai supaya tampilan tidak berubah. */
open class UISettingsCard @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : FrameLayout(c, a) {
    @JvmField val u: ImageView
    @JvmField val v: RelativeLayout
    @JvmField val w: TextView
    @JvmField val x: TextView

    init {
        (c.getSystemService("layout_inflater") as LayoutInflater).inflate(R.layout.widget_card, this, true)
        v = findViewById(R.id.btn_ui_card)
        u = findViewById(R.id.img_ui_card)
        val iconEnd = findViewById<ImageView>(R.id.icon_end_ui_card)
        w = findViewById(R.id.title_ui_card)
        x = findViewById(R.id.subtitle_ui_card)
        val array = c.obtainStyledAttributes(a, R.styleable.f4630d)
        try {
            val icon = array.getResourceId(2, 0)
            val iconEndRes = array.getResourceId(1, 0)
            val type = array.getInt(0, 0)
            val title = array.getText(4)
            val subtitle = array.getText(3)
            if (type == 1) {
                v.setBackgroundResource(R.drawable.ui_settings_card_header)
            } else if (type == 2) {
                v.setBackgroundResource(R.drawable.ui_settings_card_footer)
            } else if (type == 3) {
                iconEnd.setImageResource(R.drawable.ic_open_20_regular)
            }
            if (icon != 0) u.setImageResource(icon)
            if (iconEndRes != 0) iconEnd.setImageResource(iconEndRes)
            if (title != null) w.text = title
            if (subtitle != null) {
                x.text = subtitle
            } else {
                a()
            }
        } finally {
            array.recycle()
        }
    }

    /** Sembunyikan subtitle dan pusatkan judul. */
    fun a() {
        if (x.visibility != View.GONE) {
            x.visibility = View.GONE
            val lp = w.layoutParams as RelativeLayout.LayoutParams
            if (lp.rules[RelativeLayout.CENTER_VERTICAL] == 0) {
                @Suppress("DEPRECATION")
                lp.addRule(RelativeLayout.CENTER_VERTICAL)
                w.layoutParams = lp
            }
        }
    }

    /** More air between the title and the subtitle under it, and between the subtitle's own lines. */
    fun spreadText() {
        x.setPadding(x.paddingLeft, (3 * resources.displayMetrics.density).toInt(), x.paddingRight, x.paddingBottom)
        x.setLineSpacing(0.0f, 1.15f)
    }

    override fun setEnabled(z: Boolean) {
        super.setEnabled(z)
        v.isEnabled = z
        v.alpha = if (z) 1.0f else 0.6f
    }

    fun setImage(i2: Int) {
        u.setImageResource(i2)
    }

    fun setOnCardClickListener(listener: View.OnClickListener?) {
        v.setOnClickListener(listener)
    }

    fun setSubtitle(text: CharSequence?) {
        if (text == null) {
            a()
        } else {
            x.visibility = View.VISIBLE
            x.text = text
        }
    }

    fun setTitle(text: CharSequence?) {
        w.text = text
    }
}

/** Kartu pengaturan yang bisa dibuka-tutup. */
class UISettingsCardCollapse @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : FrameLayout(c, a) {
    @JvmField val A: PathInterpolator = PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f)
    @JvmField val u: RelativeLayout
    @JvmField val v: LinearLayout
    @JvmField val w: ImageView
    @JvmField var x: Boolean = false
    @JvmField var y: Boolean = false
    @JvmField var z: View.OnClickListener? = null

    private var fieldsReady = false

    init {
        LayoutInflater.from(c).inflate(R.layout.widget_card_collapse, this, true)
        u = findViewById(R.id.btn_ui_card)
        v = findViewById(R.id.container_expand)
        w = findViewById(R.id.icon_end_ui_card)
        val icon = findViewById<ImageView>(R.id.img_ui_card)
        val title = findViewById<TextView>(R.id.title_ui_card)
        val subtitle = findViewById<TextView>(R.id.subtitle_ui_card)
        val array = c.obtainStyledAttributes(a, R.styleable.f4631e)
        try {
            val iconRes = array.getResourceId(2, 0)
            val label = array.getText(4)
            val sub = array.getText(3)
            if (iconRes != 0) icon.setImageResource(iconRes)
            if (label != null) title.text = label
            if (sub != null) subtitle.text = sub else subtitle.visibility = View.GONE
        } finally {
            array.recycle()
        }
        v.visibility = View.GONE
        w.rotation = 0.0f
        fieldsReady = true

        u.setOnClickListener { view ->
            if (!y) {
                if (x) {
                    y = true
                    val measuredHeight = v.measuredHeight
                    v.animate().alpha(0.0f).translationY(-22.0f).setDuration(140L)
                        .setInterpolator(A).withEndAction {
                            val animator = ValueAnimator.ofInt(measuredHeight, 0)
                            animator.duration = 260L
                            animator.interpolator = A
                            animator.addUpdateListener(b(this@UISettingsCardCollapse, 1))
                            animator.addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    v.visibility = View.GONE
                                    v.alpha = 1.0f
                                    v.translationY = 0.0f
                                    y = false
                                }
                            })
                            animator.start()
                            w.animate().rotation(0.0f).setDuration(260L).setInterpolator(A).start()
                        }.start()
                    u.setBackgroundResource(R.drawable.ui_settings_card_clickable)
                } else {
                    y = true
                    u.setBackgroundResource(R.drawable.ui_settings_card_header_clickable)
                    v.visibility = View.VISIBLE
                    v.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        0
                    )
                    val measuredHeight = v.measuredHeight
                    val lp = v.layoutParams
                    lp.height = 0
                    v.layoutParams = lp
                    v.alpha = 0.0f
                    v.translationY = -22.0f
                    val animator = ValueAnimator.ofInt(0, measuredHeight)
                    animator.duration = 260L
                    animator.interpolator = A
                    animator.addUpdateListener(b(this@UISettingsCardCollapse, 0))
                    animator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            val lp2 = v.layoutParams
                            lp2.height = ViewGroup.LayoutParams.WRAP_CONTENT
                            v.layoutParams = lp2
                            y = false
                            w.animate().rotation(180.0f).setDuration(260L)
                                .setInterpolator(this@UISettingsCardCollapse.A).start()
                        }

                        override fun onAnimationStart(animation: Animator) {
                            v.animate().alpha(1.0f).translationY(0.0f).setStartDelay(160L)
                                .setDuration(220L).setInterpolator(this@UISettingsCardCollapse.A).start()
                        }
                    })
                    animator.start()
                }
                x = !x
            }
            z?.onClick(view)
        }
    }

    companion object {
        const val B: Int = 0
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (!fieldsReady) return
        var child: View? = null
        var index = childCount - 1
        while (index >= 0) {
            val candidate = getChildAt(index)
            if (candidate.id != R.id.root_widget_card) {
                removeViewAt(index)
                child = candidate
                break
            }
            index--
        }
        child?.let {
            v.removeAllViews()
            v.addView(it)
        }
    }

    fun setOnCardClickListener(listener: View.OnClickListener?) {
        z = listener
    }
}

/** Kartu pengaturan dengan progress bar. */
class UISettingsCardProgressBar @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : FrameLayout(c, a) {
    @JvmField val u: ProgressBar
    @JvmField val v: ImageView
    @JvmField val w: TextView
    @JvmField val x: TextView
    @JvmField val y: TextView

    init {
        (c.getSystemService("layout_inflater") as LayoutInflater).inflate(R.layout.widget_card_progressbar, this, true)
        val button = findViewById<View>(R.id.btn_ui_card)
        v = findViewById(R.id.img_ui_card)
        w = findViewById(R.id.title_ui_card)
        y = findViewById(R.id.progress_txt)
        x = findViewById(R.id.subtitle_ui_card)
        u = findViewById(R.id.progress_bar)
        val array = c.obtainStyledAttributes(a, R.styleable.f4632f)
        try {
            val icon = array.getResourceId(3, 0)
            val clickable = array.getBoolean(1, false)
            val title = array.getText(5)
            val subtitle = array.getText(4)
            if (clickable) {
                button.setBackgroundResource(R.drawable.ui_settings_card_clickable)
                val stub = findViewById<ViewStub>(R.id.stub_icon_end_ui_card)
                if (stub != null && stub.parent != null) stub.inflate()
            }
            if (icon != 0) v.setImageResource(icon)
            if (title != null) w.text = title
            if (subtitle != null) {
                x.text = subtitle
            } else if (x.visibility != View.GONE) {
                x.visibility = View.GONE
            }
        } finally {
            array.recycle()
        }
    }

    fun setImage(i2: Int) {
        v.setImageResource(i2)
    }

    fun setProgressMax(i2: Int) {
        u.max = i2
    }

    fun setProgressValue(i2: Int) {
        u.progress = i2
        y.text = i2.toString()
    }

    fun setSubtitle(text: CharSequence?) {
        if (text != null) {
            x.visibility = View.VISIBLE
            x.text = text
        } else if (x.visibility != View.GONE) {
            x.visibility = View.GONE
        }
    }

    fun setTitle(text: CharSequence?) {
        w.text = text
    }
}

/** Kartu pengaturan dengan seek bar. */
class UISettingsCardSeekBar @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : FrameLayout(c, a) {
    @JvmField val u: SeekBar
    @JvmField val v: ImageView
    @JvmField val w: TextView
    @JvmField val x: TextView
    @JvmField val y: TextView

    init {
        (c.getSystemService("layout_inflater") as LayoutInflater).inflate(R.layout.widget_card_seekbar, this, true)
        val button = findViewById<View>(R.id.btn_ui_card)
        v = findViewById(R.id.img_ui_card)
        w = findViewById(R.id.title_ui_card)
        x = findViewById(R.id.subtitle_ui_card)
        u = findViewById(R.id.seekBar)
        y = findViewById(R.id.progress_txt)
        u.thumbTintList = null
        val array = c.obtainStyledAttributes(a, R.styleable.g)
        try {
            val icon = array.getResourceId(3, 0)
            val clickable = array.getBoolean(1, false)
            val type = array.getInt(0, 0)
            val title = array.getText(5)
            val subtitle = array.getText(4)
            if (clickable) {
                button.setBackgroundResource(R.drawable.ui_settings_card_clickable)
                val stub = findViewById<ViewStub>(R.id.stub_icon_end_ui_card)
                if (stub != null && stub.parent != null) stub.inflate()
            }
            if (type != 0) {
                if (type == 1) {
                    button.setBackgroundResource(R.drawable.ui_settings_card_header)
                } else if (type == 2) {
                    button.setBackgroundResource(R.drawable.ui_settings_card_footer)
                } else if (type == 3) {
                    button.setBackgroundResource(R.drawable.ui_settings_card_body)
                }
            }
            if (icon != 0) v.setImageResource(icon)
            if (title != null) w.text = title
            if (subtitle != null) {
                x.text = subtitle
            } else if (x.visibility != View.GONE) {
                x.visibility = View.GONE
            }
        } finally {
            array.recycle()
        }
    }

    fun setImage(i2: Int) {
        v.setImageResource(i2)
    }

    fun setItemSeekBarChangeListener(listener: SeekBar.OnSeekBarChangeListener?) {
        u.setOnSeekBarChangeListener(listener)
    }

    fun setProgressMax(i2: Int) {
        u.max = i2
    }

    fun setProgressTxt(i2: Int) {
        y.text = i2.toString()
    }

    fun setProgressValue(i2: Int) {
        u.progress = i2
        y.text = i2.toString()
    }

    fun setSubtitle(text: CharSequence?) {
        if (text != null) {
            x.visibility = View.VISIBLE
            x.text = text
        } else if (x.visibility != View.GONE) {
            x.visibility = View.GONE
        }
    }

    fun setTitle(text: CharSequence?) {
        w.text = text
    }
}

/** Kartu pengaturan dengan spinner. */
class UISettingsCardSpinner @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : FrameLayout(c, a) {
    interface SpinnerCallback {
        fun c(i2: Int)
    }

    @JvmField var A: Boolean = true
    @JvmField val u: View
    @JvmField val v: Spinner
    @JvmField val w: ImageView
    @JvmField val x: TextView
    @JvmField val y: TextView
    @JvmField val z: Context

    init {
        z = c
        (c.getSystemService("layout_inflater") as LayoutInflater).inflate(R.layout.widget_card_spinner, this, true)
        u = findViewById(R.id.btn_ui_card)
        w = findViewById(R.id.img_ui_card)
        x = findViewById(R.id.title_ui_card)
        y = findViewById(R.id.subtitle_ui_card)
        v = findViewById(R.id.spinner_ui_card)
        val array = c.obtainStyledAttributes(a, R.styleable.f4633h)
        try {
            val icon = array.getResourceId(3, 0)
            val type = array.getInt(0, 0)
            val clickable = array.getBoolean(1, false)
            val title = array.getText(5)
            val subtitle = array.getText(4)
            if (clickable) {
                u.setBackgroundResource(R.drawable.ui_settings_card_clickable)
                val stub = findViewById<ViewStub>(R.id.stub_icon_end_ui_card)
                if (stub != null && stub.parent != null) stub.inflate()
            } else if (type != 0) {
                if (type == 1) {
                    u.setBackgroundResource(R.drawable.ui_settings_card_header)
                } else if (type == 2) {
                    u.setBackgroundResource(R.drawable.ui_settings_card_footer)
                } else if (type == 3) {
                    u.setBackgroundResource(R.drawable.ui_settings_card_body)
                }
            }
            if (icon != 0) w.setImageResource(icon)
            if (title != null) x.text = title
            if (subtitle != null) {
                y.text = subtitle
            } else if (y.visibility != View.GONE) {
                y.visibility = View.GONE
            }
        } finally {
            array.recycle()
        }
    }

    fun getSelectedPosition(): Int = v.selectedItemPosition

    override fun setActivated(z2: Boolean) {
        super.setActivated(z2)
        if (z2) {
            v.isActivated = true
            postDelayed(d(this@UISettingsCardSpinner, 0), 850L)
        }
    }

    fun setArray(i2: Int) {
        val adapter = UISpinnerAdapter(z, z.resources.getStringArray(i2))
        adapter.setDropDownViewResource(R.layout.ui_spinner_item)
        v.adapter = adapter
    }

    fun setImage(i2: Int) {
        w.setImageResource(i2)
    }

    fun setItemSelectListener(callback: SpinnerCallback?) {
        v.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (A) {
                    A = false
                    return
                }
                callback?.c(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    fun setItemSelectedListener(listener: AdapterView.OnItemSelectedListener?) {
        v.onItemSelectedListener = listener
    }

    fun setOnCardClickListener(listener: View.OnClickListener?) {
        u.setOnClickListener(listener)
    }

    fun setSelection(i2: Int) {
        v.setSelection(i2)
    }

    fun setSubtitle(text: CharSequence?) {
        if (text != null) {
            y.visibility = View.VISIBLE
            y.text = text
        } else if (y.visibility != View.GONE) {
            y.visibility = View.GONE
        }
    }

    fun setTitle(text: CharSequence?) {
        x.text = text
    }

    fun setArray(items: Array<String>) {
        val adapter = UISpinnerAdapter(z, items)
        adapter.setDropDownViewResource(R.layout.ui_spinner_item)
        v.adapter = adapter
    }
}

/** Versi ringkas dari kartu spinner. */
class UISettingsCardSpinnerCompact @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : FrameLayout(c, a) {
    interface SpinnerCallback {
        fun c(i2: Int)
    }

    @JvmField val u: View
    @JvmField val v: Spinner
    @JvmField val w: TextView
    @JvmField val x: Context
    @JvmField var y: Boolean = true

    init {
        x = c
        (c.getSystemService("layout_inflater") as LayoutInflater).inflate(R.layout.widget_card_spinner_compact, this, true)
        u = findViewById(R.id.btn_ui_card)
        w = findViewById(R.id.title_ui_card)
        v = findViewById(R.id.spinner_ui_card)
        val array = c.obtainStyledAttributes(a, R.styleable.f4634i)
        try {
            val icon = array.getResourceId(1, 0)
            val type = array.getInt(0, 0)
            val title = array.getText(2)
            if (type == 1) {
                u.setBackgroundResource(R.drawable.ui_settings_card_header)
            } else if (type == 2) {
                u.setBackgroundResource(R.drawable.ui_settings_card_footer)
            }
            setImage(icon)
            if (title != null) w.text = title
        } finally {
            array.recycle()
        }
    }

    fun getSelectedPosition(): Int = v.selectedItemPosition

    override fun setActivated(z2: Boolean) {
        super.setActivated(z2)
        if (z2) {
            v.isActivated = true
            postDelayed(e(this@UISettingsCardSpinnerCompact, 0), 850L)
        }
    }

    fun setArray(i2: Int) {
        val adapter = UISpinnerAdapter(x, x.resources.getStringArray(i2))
        adapter.setDropDownViewResource(R.layout.ui_spinner_item)
        v.adapter = adapter
    }

    fun setImage(i2: Int) {
        w.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (i2 != 0) ContextCompat.getDrawable(x, i2) else null,
            null,
            null,
            null
        )
    }

    fun setItemSelectListener(callback: SpinnerCallback?) {
        v.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (y) {
                    y = false
                    return
                }
                callback?.c(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    fun setItemSelectedListener(listener: AdapterView.OnItemSelectedListener?) {
        v.onItemSelectedListener = listener
    }

    fun setOnCardClickListener(listener: View.OnClickListener?) {
        u.setOnClickListener(listener)
    }

    fun setSelection(i2: Int) {
        v.setSelection(i2)
    }

    fun setTitle(text: CharSequence?) {
        w.text = text
    }

    fun setArray(items: Array<String>) {
        val adapter = UISpinnerAdapter(x, items)
        adapter.setDropDownViewResource(R.layout.ui_spinner_item)
        v.adapter = adapter
    }
}

/** Kartu pengaturan dengan teks spinner. */
class UISettingsCardSpinnerText @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : FrameLayout(c, a) {
    @JvmField val u: ImageView
    @JvmField val v: TextView
    @JvmField val w: TextView
    @JvmField val x: TextView

    init {
        (c.getSystemService("layout_inflater") as LayoutInflater).inflate(R.layout.widget_card_spinner_text, this, true)
        u = findViewById(R.id.img_ui_card)
        w = findViewById(R.id.title_ui_card)
        x = findViewById(R.id.subtitle_ui_card)
        v = findViewById(R.id.btn_download)
        val array = c.obtainStyledAttributes(a, R.styleable.j)
        try {
            val icon = array.getResourceId(2, 0)
            val title = array.getText(5)
            val subtitle = array.getText(4)
            val spinnerText = array.getText(3)
            if (icon != 0) {
                u.setImageResource(icon)
            } else {
                val lp = u.layoutParams as ViewGroup.MarginLayoutParams
                if (lp.width != 0) {
                    lp.width = 0
                    lp.setMarginEnd(0)
                    u.layoutParams = lp
                }
            }
            if (title != null) w.text = title
            x.setText(subtitle)
            x.visibility = if (subtitle == null) View.GONE else View.VISIBLE
            if (spinnerText != null) v.text = spinnerText
        } finally {
            array.recycle()
        }
    }

    fun setImage(i2: Int) {
        u.setImageResource(i2)
    }

    fun setOnCardClickListener(listener: View.OnClickListener?) {
        v.isEnabled = true
        v.setOnClickListener(listener)
    }

    fun setSpinnerText(text: CharSequence?) {
        v.text = text
    }

    fun setSubtitle(text: CharSequence?) {
        if (text != null) {
            x.visibility = View.VISIBLE
            x.text = text
        } else if (x.visibility != View.GONE) {
            x.visibility = View.GONE
        }
    }

    fun setTitle(text: CharSequence?) {
        w.text = text
    }
}

/** Kartu pengaturan dengan switch. */
class UISettingsCardSwitch @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : FrameLayout(c, a) {
    @JvmField val u: ImageView
    @JvmField val v: SwitchCompat
    @JvmField val w: TextView
    @JvmField val x: TextView
    @JvmField val y: RelativeLayout

    init {
        (c.getSystemService("layout_inflater") as LayoutInflater).inflate(R.layout.widget_card_switch, this, true)
        y = findViewById(R.id.btn_ui_card)
        u = findViewById(R.id.img_ui_card)
        w = findViewById(R.id.title_ui_card)
        x = findViewById(R.id.subtitle_ui_card)
        v = findViewById(R.id.switch_ui_card)
        val array = c.obtainStyledAttributes(a, R.styleable.f4635k)
        try {
            val icon = array.getResourceId(2, 0)
            val type = array.getInt(0, 0)
            val title = array.getText(4)
            val subtitle = array.getText(3)
            if (type != 0) {
                if (type == 1) {
                    y.setBackgroundResource(R.drawable.ui_settings_card_header)
                } else if (type == 2) {
                    y.setBackgroundResource(R.drawable.ui_settings_card_footer)
                } else if (type == 3) {
                    y.setBackgroundResource(R.drawable.ui_settings_card_body)
                }
            }
            if (icon != 0) u.setImageResource(icon)
            if (title != null) w.text = title
            if (subtitle != null) {
                x.text = subtitle
            } else {
                a()
            }
        } finally {
            array.recycle()
        }
    }

    /** Sembunyikan subtitle dan pusatkan judul. */
    fun a() {
        if (x.visibility != View.GONE) {
            x.visibility = View.GONE
            val lp = w.layoutParams as RelativeLayout.LayoutParams
            if (lp.rules[RelativeLayout.CENTER_VERTICAL] == 0) {
                @Suppress("DEPRECATION")
                lp.addRule(RelativeLayout.CENTER_VERTICAL)
                w.layoutParams = lp
            }
        }
    }

    override fun setActivated(z2: Boolean) {
        super.setActivated(z2)
        if (z2) {
            v.isActivated = true
            postDelayed(f(this@UISettingsCardSwitch, 0), 850L)
        }
    }

    override fun setEnabled(z: Boolean) {
        super.setEnabled(z)
        v.isEnabled = z
        y.isEnabled = z
        y.alpha = if (z) 1.0f else 0.6f
    }

    fun setImage(i2: Int) {
        u.setImageResource(i2)
    }

    fun setOnSwitchCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener?) {
        v.setOnCheckedChangeListener(listener)
    }

    fun setSubtitle(text: CharSequence?) {
        if (text == null) {
            a()
        } else {
            x.visibility = View.VISIBLE
            x.text = text
        }
    }

    fun setSwitchChecked(z: Boolean) {
        v.isChecked = z
    }

    fun setSwitchVisible(z: Boolean) {
        v.visibility = if (z) View.VISIBLE else View.GONE
    }

    fun setTitle(text: CharSequence?) {
        w.text = text
    }
}

/** Versi ringkas dari kartu switch. */
class UISettingsCardSwitchCompact @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : FrameLayout(c, a) {
    @JvmField val u: SwitchCompat
    @JvmField val v: TextView
    @JvmField val w: View

    init {
        (c.getSystemService("layout_inflater") as LayoutInflater).inflate(R.layout.widget_card_switch_compact, this, true)
        w = findViewById(R.id.btn_ui_card)
        v = findViewById(R.id.title_ui_card)
        u = findViewById(R.id.switch_ui_card)
        val array = c.obtainStyledAttributes(a, R.styleable.f4635k)
        try {
            val type = array.getInt(0, 0)
            val title = array.getText(4)
            if (type != 0) {
                if (type == 1) {
                    w.setBackgroundResource(R.drawable.ui_settings_card_header)
                } else if (type == 2) {
                    w.setBackgroundResource(R.drawable.ui_settings_card_footer)
                } else if (type == 3) {
                    w.setBackgroundResource(R.drawable.ui_settings_card_body)
                }
            }
            if (title != null) v.text = title
        } finally {
            array.recycle()
        }
    }

    override fun setActivated(z2: Boolean) {
        super.setActivated(z2)
        if (z2) {
            u.isActivated = true
            postDelayed(g(this@UISettingsCardSwitchCompact, 0), 850L)
        }
    }

    override fun setEnabled(z: Boolean) {
        super.setEnabled(z)
        u.isEnabled = z
        w.isEnabled = z
        w.alpha = if (z) 1.0f else 0.6f
    }

    fun setOnSwitchCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener?) {
        u.setOnCheckedChangeListener(listener)
    }

    fun setSwitchChecked(z: Boolean) {
        u.isChecked = z
    }

    fun setSwitchVisible(z: Boolean) {
        u.visibility = if (z) View.VISIBLE else View.GONE
    }

    fun setTitle(text: CharSequence?) {
        v.text = text
    }
}

/** Update listener untuk animasi tinggi kartu collapse. */
class b(@JvmField val b: UISettingsCardCollapse, @JvmField val f4859a: Int) : ValueAnimator.AnimatorUpdateListener {
    override fun onAnimationUpdate(animation: ValueAnimator) {
        val height = animation.animatedValue as Int
        val lp = b.v.layoutParams
        lp.height = height
        b.v.layoutParams = lp
    }
}

/** Runnable efek aktif untuk spinner. */
class d(@JvmField val v: UISettingsCardSpinner, @JvmField val u: Int) : Runnable {
    override fun run() {
        when (u) {
            0 -> {
                v.v.isActivated = false
                v.postDelayed(d(v, 1), 450L)
            }
            1 -> {
                v.v.isActivated = true
                v.postDelayed(d(v, 2), 650L)
            }
            else -> {
                v.v.isActivated = false
                v.isActivated = false
            }
        }
    }
}

/** Runnable efek aktif untuk spinner compact. */
class e(@JvmField val v: UISettingsCardSpinnerCompact, @JvmField val u: Int) : Runnable {
    override fun run() {
        when (u) {
            0 -> {
                v.v.isActivated = false
                v.postDelayed(e(v, 1), 450L)
            }
            1 -> {
                v.v.isActivated = true
                v.postDelayed(e(v, 2), 650L)
            }
            else -> {
                v.v.isActivated = false
                v.isActivated = false
            }
        }
    }
}

/** Runnable efek aktif untuk switch. */
class f(@JvmField val v: UISettingsCardSwitch, @JvmField val u: Int) : Runnable {
    override fun run() {
        when (u) {
            0 -> {
                v.v.isActivated = false
                v.postDelayed(f(v, 1), 450L)
            }
            1 -> {
                v.v.isActivated = true
                v.postDelayed(f(v, 2), 650L)
            }
            else -> {
                v.v.isActivated = false
                v.isActivated = false
            }
        }
    }
}

/** Runnable efek aktif untuk switch compact. */
class g(@JvmField val v: UISettingsCardSwitchCompact, @JvmField val u: Int) : Runnable {
    override fun run() {
        when (u) {
            0 -> {
                v.u.isActivated = false
                v.postDelayed(g(v, 1), 450L)
            }
            1 -> {
                v.u.isActivated = true
                v.postDelayed(g(v, 2), 650L)
            }
            else -> {
                v.u.isActivated = false
                v.isActivated = false
            }
        }
    }
}
