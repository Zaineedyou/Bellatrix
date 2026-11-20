package com.bellatrixos.windows.config

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/** Pemeriksaan dan permintaan akses penyimpanan. */
object UIPermissions {
    @JvmStatic
    fun a(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == 0 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == 0
    }

    @JvmStatic
    fun b(launcher: ActivityResultLauncher<Intent>?, context: Context, str: String) {
        try {
            val intent = Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                data = Uri.parse("package:$str")
            }
            if (launcher != null) launcher.launch(intent) else context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION")
            if (launcher != null) launcher.launch(intent) else context.startActivity(intent)
        }
    }
}
