package com.bellatrixos.windows.blur

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Shader
import android.widget.ImageView
import com.bellatrixos.wallpaper.models.UIBounds
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Bitmap wallpaper yang sudah di-blur untuk latar jendela. */
object UIBlurImage {
    @JvmField var f4886a: Bitmap? = null
    @JvmField var b: Int = 0
    @JvmField var c: Int = 0

    @JvmStatic
    fun a(uIBounds: UIBounds): Bitmap? {
        val bitmap = f4886a ?: return null
        val iRound = (uIBounds.f4847a * 0.15f).roundToInt()
        val iRound2 = (uIBounds.b * 0.15f).roundToInt()
        val iRound3 = (uIBounds.c * 0.15f).roundToInt()
        val iRound4 = (uIBounds.f4848d * 0.15f).roundToInt()
        val iMax = max(0, min(iRound, bitmap.width - 1))
        val iMax2 = max(0, min(iRound2, bitmap.height - 1))
        val iMin = min(iRound3, bitmap.width - iMax)
        val iMin2 = min(iRound4, bitmap.height - iMax2)
        if (iMin <= 0 || iMin2 <= 0) return null
        return Bitmap.createBitmap(bitmap, iMax, iMax2, iMin, iMin2)
    }

    @JvmStatic
    fun b() {
        val bitmap2 = f4886a
        val bitmap: Bitmap?
        if (bitmap2 == null) {
            bitmap = null
        } else {
            val bitmapCopy = bitmap2.copy(Bitmap.Config.RGB_565, true)
            val width = bitmapCopy.width
            val height = bitmapCopy.height
            val i2 = width * height
            val iArr = IntArray(i2)
            bitmapCopy.getPixels(iArr, 0, width, 0, 0, width, height)
            val iArr2 = IntArray(i2)
            val iArr3 = IntArray(i2)
            val iArr4 = IntArray(i2)
            val iArr5 = IntArray(max(width, height))
            val iArr6 = IntArray(92416)
            var i3 = 0
            for (i4 in 0 until 92416) {
                iArr6[i4] = i4 / 361
            }
            val iArr7 = Array(37) { IntArray(3) }
            var i5 = 0
            var i6 = 0
            var i7 = 0
            while (i5 < height) {
                var i8 = i3
                var i9 = i8
                var i10 = i9
                var i11 = i10
                var i12 = i11
                var i13 = i12
                var i14 = i13
                var i15 = i14
                var i16 = -18
                var i17 = i15
                while (i16 <= 18) {
                    val i19 = iArr[max(i3, min(width - 1, i16)) + i6]
                    val iArr8 = iArr7[i16 + 18]
                    val i20 = i6
                    iArr8[i3] = (i19 shr 16) and 255
                    iArr8[1] = (i19 shr 8) and 255
                    iArr8[2] = i19 and 255
                    val iAbs = 19 - abs(i16)
                    val i21 = iArr8[i3]
                    i17 += i21 * iAbs
                    val i22 = iArr8[1]
                    i8 += i22 * iAbs
                    val i23 = iArr8[2]
                    i9 += iAbs * i23
                    if (i16 > 0) {
                        i13 += i21
                        i14 += i22
                        i15 += i23
                    } else {
                        i10 += i21
                        i11 += i22
                        i12 += i23
                    }
                    i16++
                    i6 = i20
                }
                var i24 = i3
                var i25 = 18
                while (i24 < width) {
                    iArr2[i6] = iArr6[i17]
                    iArr3[i6] = iArr6[i8]
                    iArr4[i6] = iArr6[i9]
                    val i26 = i17 - i10
                    val i27 = i8 - i11
                    val i28 = i9 - i12
                    var i29 = i25 + 19
                    if (i29 >= 37) i29 = i25 - 18
                    val iArr9 = iArr7[i29]
                    val i30 = i10 - iArr9[0]
                    val i31 = i11 - iArr9[1]
                    val i32 = i12 - iArr9[2]
                    val i33 = i24 + 19
                    if (i5 == 0) {
                        iArr5[i24] = min(i33, width - 1)
                    }
                    val i34 = iArr[i7 + iArr5[i24]]
                    val i35 = (i34 shr 16) and 255
                    iArr9[0] = i35
                    val i36 = (i34 shr 8) and 255
                    iArr9[1] = i36
                    val i37 = i34 and 255
                    iArr9[2] = i37
                    val i38 = i13 + i35
                    val i39 = i14 + i36
                    val i40 = i15 + i37
                    i17 = i26 + i38
                    i8 = i27 + i39
                    i9 = i28 + i40
                    i25++
                    if (i25 >= 37) i25 = 0
                    val iArr12 = iArr7[i25]
                    val i41 = iArr12[0]
                    i10 = i30 + i41
                    val i42 = iArr12[1]
                    i11 = i31 + i42
                    val i43 = iArr12[2]
                    i12 = i32 + i43
                    i13 = i38 - i41
                    i14 = i39 - i42
                    i15 = i40 - i43
                    i6++
                    i24++
                }
                i7 += width
                i5++
                i3 = 0
            }
            val iArr14 = iArr6
            var i44 = 0
            while (i44 < width) {
                var i45 = (-18) * width
                var i46 = -18
                var i47 = 0
                var i48 = 0
                var i49 = 0
                var i50 = 0
                var i51 = 0
                var i52 = 0
                var i53 = 0
                var i54 = 0
                var i55 = 0
                while (i46 <= 18) {
                    val i57 = height - 1
                    val i59 = i45
                    val iMaxIdx = (max(0, min(i57, i45)) * width) + i44
                    val iArr16 = iArr7[i46 + 18]
                    iArr16[0] = iArr2[iMaxIdx]
                    iArr16[1] = iArr3[iMaxIdx]
                    iArr16[2] = iArr4[iMaxIdx]
                    val iAbs2 = 19 - abs(i46)
                    i47 += iArr2[iMaxIdx] * iAbs2
                    i48 += iArr3[iMaxIdx] * iAbs2
                    i49 += iArr4[iMaxIdx] * iAbs2
                    if (i46 > 0) {
                        i53 += iArr16[0]
                        i54 += iArr16[1]
                        i55 += iArr16[2]
                    } else {
                        i50 += iArr16[0]
                        i51 += iArr16[1]
                        i52 += iArr16[2]
                    }
                    i45 = if (i46 < i57) i59 + 1 else i59
                    i46++
                }
                var i60 = i44
                var i61 = 0
                var i62 = 18
                while (i61 < height) {
                    iArr[i60] = (iArr[i60] and -16777216) or (iArr14[i47] shl 16) or (iArr14[i48] shl 8) or iArr14[i49]
                    val i63 = i47 - i50
                    val i64 = i48 - i51
                    val i65 = i49 - i52
                    val i66 = i60
                    var i67 = i62 + 19
                    if (i67 >= 37) i67 = i62 - 18
                    val iArr17 = iArr7[i67]
                    val i68 = i50 - iArr17[0]
                    val i69 = i51 - iArr17[1]
                    val i70 = i52 - iArr17[2]
                    val i71 = i61 + 19
                    if (i44 == 0) {
                        iArr5[i61] = min(i71, height - 1) * width
                    }
                    val i72 = iArr5[i61] + i44
                    val i73 = iArr2[i72]
                    iArr17[0] = i73
                    val i74 = iArr3[i72]
                    iArr17[1] = i74
                    val i75 = iArr4[i72]
                    iArr17[2] = i75
                    val i76 = i53 + i73
                    val i77 = i54 + i74
                    val i78 = i55 + i75
                    i47 = i63 + i76
                    i48 = i64 + i77
                    i49 = i65 + i78
                    var i79 = i62 + 1
                    if (i79 >= 37) i79 = 0
                    val iArr18 = iArr7[i79]
                    val i80 = iArr18[0]
                    i50 = i68 + i80
                    val i81 = iArr18[1]
                    i51 = i69 + i81
                    val i82 = iArr18[2]
                    i53 = i76 - i80
                    i54 = i77 - i81
                    i55 = i78 - i82
                    i61++
                    i62 = i79
                    i60 = i66 + width
                    i52 = i70 + i82
                }
                i44++
            }
            bitmapCopy.setPixels(iArr, 0, width, 0, 0, width, height)
            val canvas = Canvas(bitmapCopy)
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(4.0f)
            val paint = Paint(1)
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(bitmapCopy, 0.0f, 0.0f, paint)
            val width2 = bitmapCopy.width.toFloat()
            val height2 = bitmapCopy.height.toFloat()
            if (Noise.f4885a == null) {
                val bitmapCreateBitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
                val iArr19 = IntArray(16384)
                val random = Random()
                for (i83 in 0 until 16384) {
                    iArr19[i83] = Color.argb(random.nextInt(256), 255, 255, 255)
                }
                bitmapCreateBitmap.setPixels(iArr19, 0, 128, 0, 0, 128, 128)
                Noise.f4885a = bitmapCreateBitmap
                val bitmap4 = Noise.f4885a!!
                val tileMode = Shader.TileMode.REPEAT
                Noise.b = BitmapShader(bitmap4, tileMode, tileMode)
            }
            val paint2 = Paint(1)
            paint2.shader = Noise.b
            paint2.alpha = 32
            canvas.drawRect(0.0f, 0.0f, width2, height2, paint2)
            bitmap = bitmapCopy
        }
        f4886a = bitmap
    }

    @JvmStatic
    fun c(imageView: ImageView) {
        val width = imageView.width
        val height = imageView.height
        if (width <= 0 || height <= 0) return
        val iRound = (width * 0.15f).roundToInt()
        val iRound2 = (height * 0.15f).roundToInt()
        if (f4886a != null && b == iRound && c == iRound2) return
        b = iRound
        c = iRound2
        f4886a = Bitmap.createBitmap(iRound, iRound2, Bitmap.Config.RGB_565)
        val canvas = Canvas(f4886a!!)
        canvas.scale(0.15f, 0.15f)
        imageView.draw(canvas)
    }
}
