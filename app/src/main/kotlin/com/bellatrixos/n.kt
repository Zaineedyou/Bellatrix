package com.bellatrixos

import android.database.Cursor
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.util.Supplier
import androidx.cursoradapter.widget.SimpleCursorAdapter
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.widgets.cards.UISettingsCardSpinner
import com.bellatrixos.widgets.cards.UISettingsCardSpinnerCompact
import com.bellatrixos.windows.Windows
import com.bumptech.glide.util.GlideSuppliers
import com.google.common.base.Function
import com.google.common.collect.ImmutableMultiset
import com.google.common.net.MediaType
import com.google.gson.internal.ObjectConstructor
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Collection
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.TreeMap
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap

/** Callback synthetic bersama untuk supplier, spinner, cursor dan Gson. */
class n(@JvmField val u: Int) :
    Supplier<Any?>,
    SimpleCursorAdapter.CursorToStringConverter,
    UISettingsCardSpinnerCompact.SpinnerCallback,
    UISettingsCardSpinner.SpinnerCallback,
    GlideSuppliers.GlideSupplier<Any?>,
    Function<Any?, Any?>,
    ObjectConstructor<Any?> {

    fun a(cursor: Cursor): String =
        cursor.getString(cursor.getColumnIndex("suggest_text_1"))

    override fun convertToString(cursor: Cursor): CharSequence = a(cursor)

    override fun apply(obj: Any?): Any? {
        return when (u) {
            5 -> {
                val str = obj as String
                if (str.isNotEmpty() && runCatching { MediaType.parse(str) }.getOrNull() != null) {
                    return str
                }
                val sb = StringBuilder(str.length + 16)
                sb.append('"')
                for (i in str.indices) {
                    val ch = str[i]
                    if (ch == '\r' || ch == '\\' || ch == '"') sb.append('\\')
                    sb.append(ch)
                }
                sb.append('"')
                sb.toString()
            }
            else -> ImmutableMultiset.copyOf(obj as Collection<Any?>)
        }
    }

    override fun c(i2: Int) {
        when (u) {
            2 -> SharedPrefs.f4755a?.edit()?.putInt("wallpaper_quality", i2)?.apply()
            else -> {
                SharedPrefs.f4755a?.edit()?.putInt("system_color_scheme", i2)?.apply()
                Handler().postDelayed({
                    when (i2) {
                        0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                }, 500L)
            }
        }
    }

    override fun construct(): Any? = when (u) {
        7 -> ArrayList<Any?>()
        8 -> ConcurrentHashMap<Any?, Any?>()
        9 -> ConcurrentSkipListMap<Any?, Any?>()
        10 -> LinkedHashSet<Any?>()
        11 -> TreeSet<Any?>()
        12 -> ArrayDeque<Any?>()
        // ConstructorConstructor.lambda$newMapConstructor$3() — helper Gson hasil obfuskasi.
        13 -> LinkedHashMap<Any?, Any?>()
        14 -> LinkedHashMap<Any?, Any?>()
        else -> TreeMap<Any?, Any?>()
    }

    override fun get(): Any? {
        return when (u) {
            0 -> Windows.f4882e
            else -> {
                // GlideBitmapFactory.GainmapDecoderWorkaroundStateCalculator.f5230a
                // tidak tersedia; hitung ulang nilai workaround secara lokal.
                var z = false
                if (Build.VERSION.SDK_INT == 34) {
                    val bitmapCreateBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
                    val bitmapCopy = bitmapCreateBitmap.copy(Bitmap.Config.HARDWARE, false)
                    bitmapCreateBitmap.recycle()
                    z = bitmapCopy == null
                    if (Log.isLoggable("GainmapWorkaroundCalc", 2)) {
                        Log.v("GainmapWorkaroundCalc", "calculateNeedsGainmapDecodeWorkaround=$z")
                    }
                    bitmapCopy?.recycle()
                }
                z
            }
        }
    }
}
