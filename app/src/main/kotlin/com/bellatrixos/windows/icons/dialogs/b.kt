package com.bellatrixos.windows.icons.dialogs

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import com.bellatrixos.windows.icons.adapters.AppIconAdapter

/** Callback pemilih ikon: hasil gallery atau paket aplikasi. */
class b(@JvmField val u: DialogAppIcons) :
    ActivityResultCallback<ActivityResult>,
    AppIconAdapter.OnItemClickListener {

    override fun b(str: String) {
        u.O0?.let { listener ->
            listener.a(str)
            u.dismiss()
        }
    }

    override fun onActivityResult(result: ActivityResult) {
        val intent: Intent? = result.data
        if (result.resultCode != -1 || intent == null) return
        try {
            u.requireContext().contentResolver.openInputStream(intent.data ?: return)?.use { stream ->
                val drawable = Drawable.createFromStream(stream, null)
                u.N0?.b(drawable)
                u.dismiss()
            }
        } catch (ignored: Exception) {
        }
    }
}
