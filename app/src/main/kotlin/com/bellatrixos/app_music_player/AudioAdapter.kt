package com.bellatrixos.app_music_player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.app_explorer.navigation.d
import java.util.Locale

/** Daftar lagu di pemutar musik. */
class AudioAdapter(
    @JvmField val y: List<*>,
    @JvmField val z: OnItemClick
) : RecyclerView.Adapter<AudioAdapter.ViewHolder>() {

    fun interface OnItemClick {
        fun b(audioModel: AudioModel)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: TextView? = null
        @JvmField var v: TextView? = null
    }

    @JvmField var x: Int = -1

    override fun getItemCount(): Int = y.size

    override fun onBindViewHolder(holder: ViewHolder, i2: Int) {
        val audio = y[i2] as AudioModel
        val view = holder.itemView
        view.setBackgroundResource(
            if (i2 % 2 == 0) R.drawable.ui_music_item_bg_even_selector
            else R.drawable.ui_music_item_bg_odd_selector
        )
        holder.u?.text = audio.b
        val seconds = audio.c / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val rest = seconds % 60
        holder.v?.text = if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, rest)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, rest)
        }
        view.isSelected = i2 == x
        view.setOnClickListener(d(this, holder, audio, 1))
    }

    override fun onCreateViewHolder(parent: ViewGroup, i2: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_music_player_item, parent, false)
        return ViewHolder(view).apply {
            u = view.findViewById(R.id.audio_name)
            v = view.findViewById(R.id.audio_duration)
        }
    }
}
