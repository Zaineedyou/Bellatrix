package com.bellatrixos.app_settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.widgets.cards.UISettingsCardProgressBar
import com.bellatrixos.widgets.cards.UISettingsCardSeekBar
import com.bellatrixos.windows.drivers.Sounds
import com.bellatrixos.windows.drivers.volume.UIVolume

/** Halaman suara. */
class UISound : Fragment() {
    @JvmField var s0: TextView? = null
    @JvmField var t0: UISettingsCardSeekBar? = null
    @JvmField var u0: UISettingsCardProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_sound, container, false)
        t0 = view.findViewById(R.id.card_app_volume)
        u0 = view.findViewById(R.id.card_device_volume)
        s0 = view.findViewById(R.id.device_sound_settings)
        t0?.setProgressMax(100)
        u0?.setProgressMax(100)
        t0?.setProgressValue((Sounds.f4903d * 100.0f).toInt())
        u0?.setProgressValue(UIVolume.f4911a)
        t0?.setItemSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Sounds.f4903d = Math.max(0.0f, Math.min(1.0f, progress / 100.0f))
                t0?.setProgressTxt(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 0
                SharedPrefs.f4755a?.edit()?.putFloat("system_sound_volume", progress / 100.0f)?.apply()
            }
        })
        s0?.setOnClickListener(C(0))
        return view
    }
}
