package com.bellatrixos.app_music_player

import java.util.Locale

/** Format durasi audio menjadi mm:ss atau hh:mm:ss. */
object AudioManager {
    @JvmStatic
    fun a(millis: Int): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val rest = seconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, rest)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, rest)
        }
    }
}
