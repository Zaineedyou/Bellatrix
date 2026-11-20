package com.bellatrixos.windows

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable

/** Logo Windows berbentuk kotak gradient dengan irisan silang transparan. */
class WinLogoDrawable(f2: Float, f3: Float, i2: Int, i3: Int) : Drawable() {
    @JvmField val f4867a: Paint = Paint(1).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        strokeWidth = f3
        style = Paint.Style.STROKE
    }

    @JvmField val b: GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(i2, i3)
    ).apply { cornerRadius = f2 }

    override fun draw(canvas: Canvas) {
        val layer = canvas.saveLayer(null, null)
        val rect = RectF(bounds)
        b.setBounds(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
        b.draw(canvas)
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        canvas.drawLine(centerX, rect.top, centerX, rect.bottom, f4867a)
        canvas.drawLine(rect.left, centerY, rect.right, centerY, f4867a)
        canvas.restoreToCount(layer)
    }

    override fun getOpacity(): Int = b.opacity

    override fun setAlpha(alpha: Int) {
        b.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        b.colorFilter = colorFilter
    }
}
