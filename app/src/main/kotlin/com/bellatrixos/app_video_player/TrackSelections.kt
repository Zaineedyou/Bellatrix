package com.bellatrixos.app_video_player

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import com.bellatrixos.R
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.interfaces.MainActivityIn
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.Locale

/** Audio track choices for one video player; state must not leak between windows or media. */
class TrackSelections {

    class TrackId(@JvmField val f4726a: Int, @JvmField val b: Int)

    class TrackItem(@JvmField val f4727a: Int, @JvmField val b: String, @JvmField val c: Boolean)

    /** Index of the selected item in [items], or -1 when nothing is selected. */
    var selected: Int = -1
        private set

    /** Menu index to (group, track), rebuilt for each media. */
    var ids: HashMap<Int, TrackId>? = null
        private set

    var items: MutableList<TrackItem> = ArrayList()
        private set

    /** Drops everything learned from the previous media. */
    fun reset() {
        selected = -1
        ids = null
        items = ArrayList()
    }

    fun a(exoPlayer: ExoPlayer, i2: Int) {
        val trackId = ids?.get(i2) ?: return
        val groups = exoPlayer.currentTracks.groups
        if (trackId.f4726a !in groups.indices) return
        val group = groups[trackId.f4726a]
        if (trackId.b !in 0 until group.length) return
        val override = TrackSelectionOverride(
            group.mediaTrackGroup, Collections.singletonList(trackId.b)
        )
        val builder = exoPlayer.trackSelectionParameters.buildUpon()
        builder.clearOverridesOfType(1)
        builder.setOverrideForType(override)
        exoPlayer.trackSelectionParameters = builder.build()
        selected = i2
    }

    fun b(context: Context, view: View, exoPlayer: ExoPlayer?, mainActivityIn: MainActivityIn, dVar: d) {
        if (exoPlayer == null) return
        val menu = mainActivityIn.l0() as? UIContextMenu ?: return
        val menuItems = menu.f4745d
        menuItems.clear()
        if (items.isEmpty()) {
            val map = HashMap<Int, TrackId>()
            val found = ArrayList<TrackItem>()
            val tracks = exoPlayer.currentTracks
            var index = 1
            for (groupIndex in tracks.groups.indices) {
                val group = tracks.groups[groupIndex]
                if (group.type != 1) continue
                if (index != 1) found.add(TrackItem(0, "", true))
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val language = format.language?.let { Locale(it).getDisplayLanguage(Locale.getDefault()) } ?: "Unknown"
                    if (group.isTrackSelected(trackIndex)) selected = index
                    found.add(TrackItem(index, language, false))
                    map[index] = TrackId(groupIndex, trackIndex)
                    index++
                }
            }
            ids = map
            items = found
        }
        if (items.isEmpty()) {
            Toast.makeText(context, "No audio tracks available", Toast.LENGTH_SHORT).show()
            return
        }
        for (item in items) {
            menuItems.add(
                UIMenuItem(
                    if (item.c) 1 else 0,
                    item.f4727a,
                    if (item.f4727a == selected) R.drawable.menu_ic_dot_6_regular
                    else R.drawable.menu_ic_blank_20_regular,
                    item.b
                )
            )
        }
        menu.d(UIContextMenu.OnMenuItemClickListener { _, which -> a(exoPlayer, which) })
        menu.a(R.style.AnimationDialogFadeUp)
        menu.setOnDismissListener(a(0, dVar))
        menu.h(view)
    }
}
