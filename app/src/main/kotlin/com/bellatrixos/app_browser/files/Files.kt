package com.bellatrixos.app_browser.files

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import com.bellatrixos.R

/** Pemilih berkas untuk WebView (input type=file). */
object Files {
    @JvmStatic
    fun a(activityResult: ActivityResult?, valueCallback: ValueCallback<Array<Uri>>?) {
        if (valueCallback == null) return
        val intent = activityResult?.data
        val uris: Array<Uri>? = if (activityResult == null ||
            activityResult.resultCode != Activity.RESULT_OK || intent == null
        ) {
            null
        } else {
            val clip = intent.clipData
            when {
                clip != null -> Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
                intent.data != null -> arrayOf(intent.data!!)
                else -> null
            }
        }
        valueCallback.onReceiveValue(uris)
    }

    @JvmStatic
    fun b(
        context: Context,
        fileChooserParams: WebChromeClient.FileChooserParams,
        activityResultLauncher: ActivityResultLauncher<Intent>
    ) {
        val multiple = fileChooserParams.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
        val acceptTypes = fileChooserParams.acceptTypes
        var wantsImage = false
        if (acceptTypes != null) {
            for (type in acceptTypes) {
                if (type != null && type.lowercase().startsWith("image/")) {
                    wantsImage = true
                    break
                }
            }
        }

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = if (wantsImage) "image/*" else "*/*"
        if (wantsImage) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        }
        if (multiple) intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        activityResultLauncher.launch(
            Intent.createChooser(intent, context.getString(R.string.settings_accounts_choose_img_btn))
        )
    }
}
