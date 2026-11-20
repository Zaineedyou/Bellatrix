package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.app_settings.UISettings
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.windows.dialogs.UIDialog

/** Halaman akun pengguna. */
class UIAccounts : Fragment() {
    @JvmField var A0: TextView? = null
    @JvmField val B0 = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        C0054c(0, this).onActivityResult(uri)
    }
    @JvmField var s0: Context? = null
    @JvmField var t0: String? = null
    @JvmField var u0: ImageView? = null
    @JvmField var v0: UISettings? = null
    @JvmField var w0: MainActivityIn? = null
    @JvmField var x0: TextView? = null
    @JvmField var y0: TextView? = null
    @JvmField var z0: TextView? = null

    override fun onDestroyView() {
        super.onDestroyView()
        v0 = null
        w0 = null
        x0?.setOnClickListener(null)
        y0?.setOnClickListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        v0 = parentFragment as? UISettings
        val name = SharedPrefs.f4755a?.getString("system_user_name", "This PC")
        t0 = name
        z0?.text = name
        A0?.text = t0
        u0?.clipToOutline = true
        u0?.setImageDrawable(w0?.U())
        x0?.setOnClickListener {
            val request = PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                .build()
            B0.launch(request)
        }
        y0?.setOnClickListener {
            val context = s0 ?: return@setOnClickListener
            val dialog = UIDialog(context, getString(R.string.settings_accounts_user_name), getString(R.string.settings_accounts_name_update_subtitle))
            dialog.z = getString(R.string.settings_accounts_name_update_btn)
            dialog.C = DialogInterfaceOnClickListenerC0055d(this, 0, dialog)
            dialog.setCanceledOnTouchOutside(true)
            dialog.x = getString(R.string.settings_accounts_name_update_hint)
            dialog.y = t0
            dialog.show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_accounts, container, false)
        val context = view.context
        s0 = context
        w0 = context as? MainActivityIn
        u0 = view.findViewById(R.id.img_user)
        x0 = view.findViewById(R.id.btn_select_user_img)
        z0 = view.findViewById(R.id.device_name_acc)
        A0 = view.findViewById(R.id.device_name_card)
        y0 = view.findViewById(R.id.btnUpdateUsername)
        return view
    }
}
