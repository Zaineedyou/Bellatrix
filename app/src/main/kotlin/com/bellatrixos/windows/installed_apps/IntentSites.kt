package com.bellatrixos.windows.installed_apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bellatrixos.R
import com.bellatrixos.app_settings.fragments.y
import com.bellatrixos.variables.AppLinks
import com.bellatrixos.widgets.UIToast
import java.io.File

/** Intent untuk pengaturan aplikasi, browser, Play Store, dan berbagi berkas. */
object IntentSites {
    @JvmStatic
    fun a(context: Context, str: String) {
        try {
            val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
            intent.data = Uri.parse("package:$str")
            context.startActivity(intent)
        } catch (ignored: ActivityNotFoundException) {
            context.startActivity(Intent("android.settings.MANAGE_APPLICATIONS_SETTINGS"))
        }
    }

    @JvmStatic
    fun b(context: Context, str: String) {
        try {
            val intent = Intent("android.intent.action.VIEW", Uri.parse(str))
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, R.string.toast_browser_available_no, Toast.LENGTH_SHORT).show()
            }
        } catch (ignored: Exception) {
            Toast.makeText(context, R.string.toast_browser_available_no, Toast.LENGTH_SHORT).show()
        }
    }

    /** Buka halaman Release di repo GitHub (dulu: Play Store). */
    @JvmStatic
    fun c(context: Context, runnable: y?) {
        b(context, AppLinks.RELEASES_URL)
        runnable?.run()
    }

    @JvmStatic
    fun d(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent("android.intent.action.SEND").apply {
                type = context.contentResolver.getType(uri)
                putExtra("android.intent.extra.STREAM", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.toast_open_share_file))
            )
        } catch (ignored: Exception) {
            UIToast.a(context, context.getString(R.string.not_found_file), R.drawable.ic_share_24_regular, 2000)
        }
    }
}
