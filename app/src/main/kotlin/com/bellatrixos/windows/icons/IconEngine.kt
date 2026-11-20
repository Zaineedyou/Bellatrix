package com.bellatrixos.windows.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.collection.LruCache
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.windows.installed_apps.AppsDatabase
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import org.xmlpull.v1.XmlPullParser

/** Loader ikon aplikasi dengan cache dan dukungan icon pack. */
class IconEngine(context: Context) {
    fun interface Callback {
        fun i(bitmap: Bitmap?)
    }

    inner class IconTask(private val key: String, private val pkg: String, private val callbackCallback: Callback) :
        Runnable, Comparable<IconTask> {
        override fun compareTo(other: IconTask): Int = 0

        override fun run() {
            val pack = g
            val pm = c.packageManager
            // Urutan sama dengan Java asli: ikon app bawaan -> ikon dari DB -> icon pack -> ikon paket.
            // Exception apa pun jatuh ke ikon generik (img_file_exe), bukan bitmap kosong.
            val bitmap: Bitmap? = try {
                var drawable: Drawable? = when (pkg.substringBefore("::::")) {
                    "UIInstaller" -> ContextCompat.getDrawable(c, R.drawable.img_app_installer)
                    "UINotepad" -> ContextCompat.getDrawable(c, R.drawable.img_app_notepad)
                    "UITerminal" -> ContextCompat.getDrawable(c, R.drawable.img_app_terminal)
                    "UISettings" -> ContextCompat.getDrawable(c, R.drawable.img_app_settings)
                    "UIExplorer" -> ContextCompat.getDrawable(c, R.drawable.img_app_explorer)
                    "UIMusic", "UIVideo" -> ContextCompat.getDrawable(c, R.drawable.img_app_music)
                    "UIPhotos" -> ContextCompat.getDrawable(c, R.drawable.img_app_photos)
                    "UIBrowser" -> ContextCompat.getDrawable(c, R.drawable.img_app_chrome)
                    else -> null
                }
                if (drawable == null) {
                    val path = AppsDatabase.f4954d[pkg]?.iconPath
                    if (!path.isNullOrEmpty() && File(path).exists()) {
                        drawable = BitmapDrawable(c.resources, BitmapFactory.decodeFile(path))
                    }
                }
                if (drawable == null && pack != null && pack != "system") {
                    val resId = f4947h[pkg]
                    if (resId != null) {
                        drawable = ResourcesCompat.getDrawable(pm.getResourcesForApplication(pack), resId, null)
                    }
                }
                if (drawable == null) {
                    drawable = pm.getApplicationInfo(pkg, 0).loadIcon(pm)
                }
                IconEngine.a(drawable!!)
            } catch (_: Exception) {
                ContextCompat.getDrawable(c, R.drawable.img_file_exe)?.let { IconEngine.a(it) }
            }
            if (bitmap != null) f4948a.put(key, bitmap)
            b.remove(key)
            f4949d.post(a(this, bitmap, 0))
            // Kirim juga ke semua peminta lain yang menunggu key yang sama.
            waiting.remove(key)?.forEach { cb -> f4949d.post { cb.i(bitmap) } }
        }

        fun deliver(bitmap: Bitmap?) = callbackCallback.i(bitmap)
    }

    inner class IconTaskExe(private val key: String, private val file: File, private val callbackCallback: Callback) :
        Runnable, Comparable<IconTaskExe> {
        override fun compareTo(other: IconTaskExe): Int = 0

        override fun run() {
            var bitmap: Bitmap? = null
            try {
                ZipFile(file).use { zip ->
                    var entry = zip.getEntry("icon.png")
                    if (entry == null) {
                        val entries = zip.entries()
                        while (entries.hasMoreElements()) {
                            val next = entries.nextElement()
                            if (next.name.equals("icon.png", ignoreCase = true) || next.name.endsWith("/icon.png")) {
                                entry = next
                                break
                            }
                        }
                    }
                    if (entry != null) {
                        zip.getInputStream(entry).use { bitmap = BitmapFactory.decodeStream(it) }
                        if (bitmap != null) f4948a.put(key, bitmap)
                    }
                }
            } catch (_: Exception) {
            }
            b.remove(key)
            f4949d.post(a(this, bitmap, 1))
        }

        fun deliver(bitmap: Bitmap?) = callbackCallback.i(bitmap)
    }

    companion object {
        @JvmField @Volatile var f4946f: IconEngine? = null
        @JvmField @Volatile var g: String? = null
        @JvmField val f4947h: ConcurrentHashMap<String, Int> = ConcurrentHashMap()

        @JvmStatic
        fun a(drawable: Drawable): Bitmap {
            if (drawable is BitmapDrawable) return drawable.bitmap
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }

        @JvmStatic
        fun b(context: Context): IconEngine {
            f4946f?.let { return it }
            return synchronized(this) {
                f4946f ?: IconEngine(context.applicationContext).also { f4946f = it }
            }
        }
    }

    @JvmField val c: Context = context.applicationContext
    @JvmField val b: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val waiting = ConcurrentHashMap<String, MutableList<Callback>>()
    @JvmField val f4949d: Handler = Handler(Looper.getMainLooper())
    @JvmField val f4950e: ThreadPoolExecutor = ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS, PriorityBlockingQueue())
    @JvmField val f4948a: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024).toInt() / 8
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    init {
        g(SharedPrefs.f4755a?.getString("system_icon_pack", "system") ?: "system")
    }

    fun c(str: String, callback: Callback) {
        val key = "$str|$g"
        val bitmap = f4948a.get(key)
        if (bitmap != null) {
            callback.i(bitmap)
        } else if (b.add(key)) {
            f4950e.execute(IconTask(key, str, callback))
        } else {
            // Key sedang dimuat: simpan callback agar tidak hilang (dulu dibuang -> ikon kosong).
            waiting.getOrPut(key) { java.util.concurrent.CopyOnWriteArrayList() }.add(callback)
            f4948a.get(key)?.let { ready ->
                if (waiting[key]?.remove(callback) == true) callback.i(ready)
            }
        }
    }

    fun d(file: File, callback: Callback) {
        val key = "${file.name}|$g"
        val bitmap = f4948a.get(key)
        if (bitmap != null) {
            callback.i(bitmap)
        } else if (b.add(key)) {
            f4950e.execute(IconTaskExe(key, file, callback))
        }
    }

    fun f(str: String) {
        val key = "$str|$g"
        if (f4948a.get(key) != null) f4948a.remove(key)
    }

    fun g(str: String?) {
        val pack = str ?: "system"
        if (pack == g) return
        g = pack
        f4948a.evictAll()
        f4947h.clear()
        if (pack == "system") return
        try {
            val res = c.packageManager.getResourcesForApplication(pack)
            val id = res.getIdentifier("appfilter", "xml", pack)
            if (id == 0) return
            res.getXml(id).use { parser ->
                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (component != null && drawable != null && component.startsWith("ComponentInfo{") && component.contains("/")) {
                            val pkg = component.substring(component.indexOf('{') + 1, component.indexOf('/'))
                            val iconId = res.getIdentifier(drawable, "drawable", pack)
                            if (pkg.isNotEmpty() && iconId != 0) f4947h[pkg] = iconId
                        }
                    }
                    parser.next()
                }
            }
        } catch (_: Exception) {
        }
    }
}
