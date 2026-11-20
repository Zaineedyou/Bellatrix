package com.bellatrixos.app_settings.fragments

import android.annotation.SuppressLint
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.app_explorer.drives.StorageDirs
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.cards.UISettingsCard
import java.io.File
import java.io.IOException
import java.util.Locale

/** Halaman penyimpanan. */
class UIStorage : Fragment() {
    @JvmField var s0: Context? = null
    @JvmField var t0: ProgressBar? = null
    @JvmField var u0: UISettingsCard? = null
    @JvmField var v0: TextView? = null
    @JvmField var w0: TextView? = null
    @JvmField var x0: TextView? = null
    @JvmField var y0: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = s0 ?: return
        if (Global.x == null) {
            val stat = Global.w ?: StorageDirs.a(context, context.getExternalFilesDir(null))
            Global.x = stat
        }
        val stat = Global.x ?: return
        v0?.text = context.getString(R.string.settings_storage_local_disk, stat.d().toString())
        w0?.text = stat.c()
        t0?.progress = stat.b()

        if (Build.VERSION.SDK_INT >= 26) {
            y0?.visibility = View.VISIBLE
            x0?.visibility = View.VISIBLE
            val manager = com.bellatrixos.a.c(context.getSystemService("storagestats")) as? StorageStatsManager
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            if (manager != null && storageManager != null) {
                @SuppressLint("WrongConstant")
                try {
                    val appInfo = context.applicationInfo
                    val uuid = storageManager.getUuidForPath(File(appInfo.dataDir))
                    val stats = manager.queryStatsForUid(uuid, appInfo.uid)
                    val user = UIStorage.a0(stats.dataBytes)
                    b0(x0 ?: return, context.getString(R.string.settings_storage_files_cache), UIStorage.a0(stats.cacheBytes))
                    b0(y0 ?: return, context.getString(R.string.settings_storage_files_user), user)
                } catch (e: IOException) {
                    Log.e("AppStorageUtil", "Error getting app storage stats: ${e.message}")
                }
            }
        }
        u0?.setOnCardClickListener(ViewOnClickListenerC0052a(3, this))
    }

    fun b0(textView: TextView, str: String, str2: String) {
        val context = s0 ?: return
        val spannable = SpannableString("$str\n$str2")
        @Suppress("DEPRECATION")
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_default)),
            0,
            str.length,
            33
        )
        @Suppress("DEPRECATION")
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_muted)),
            str.length + 1,
            spannable.length,
            33
        )
        @Suppress("DEPRECATION")
        spannable.setSpan(RelativeSizeSpan(0.86f), str.length + 1, spannable.length, 33)
        textView.text = spannable
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_storage, container, false)
        s0 = view.context
        v0 = view.findViewById(R.id.title_storage)
        w0 = view.findViewById(R.id.sub_title_storage)
        t0 = view.findViewById(R.id.progress_bar)
        u0 = view.findViewById(R.id.btn_thumbnail_cleanup)
        x0 = view.findViewById(R.id.data_cache)
        y0 = view.findViewById(R.id.data_user)
        return view
    }

    companion object {
        @JvmStatic
        fun a0(j: Long): String {
            if (j <= 0) return "0 KB"
            val mb = j / 1048576.0
            return if (mb >= 1.0) String.format(Locale.US, "%.1f MB", mb) else "${j / 1024} KB"
        }
    }
}
