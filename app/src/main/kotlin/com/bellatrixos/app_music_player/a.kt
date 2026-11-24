package com.bellatrixos.app_music_player

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.windows.files.MediaPaths
import com.bellatrixos.widgets.UINavigationBar

/** Tugas inisialisasi, pemuatan metadata, dan daftar lagu. */
class a(
    @JvmField val v: UIMusic,
    @JvmField val w: Any?,
    @JvmField val u: Int
) : Runnable {

    override fun run() {
        when (u) {
            0 -> {
                val music = v
                music.D0.layoutManager = LinearLayoutManager(music.u0)
                val adapter = AudioAdapter(music.R0, e(music))
                music.Q0 = adapter
                music.D0.adapter = adapter
                val bundle: Bundle? = music.A
                if (bundle == null || bundle.getString("path") == null) {
                    music.C0.setNavigationItem(R.id.nav_home)
                } else {
                    val path = bundle.getString("path")
                    music.Y0 = true
                    music.q0(path)
                    music.C0.setNavigationItem(R.id.nav_music)
                    music.B0.setText(R.string.app_music_title)
                }
                music.D0.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val layout = recyclerView.layoutManager as? LinearLayoutManager ?: return
                        if (!music.b1 && layout.findLastVisibleItemPosition() >= music.R0.size - 3 &&
                            music.V0 < music.S0.size
                        ) {
                            music.b1 = true
                            AppExecutors.c().post(d(3, this))
                        }
                    }
                })
                @Suppress("UNCHECKED_CAST")
                music.F0.a(w as? Bundle)
                return
            }
            1 -> {
                val path = w as? String ?: return
                val music = v
                val retriever = MediaMetadataRetriever()
                try {
                    val uri = MediaPaths.uri(path)
                    if (uri != null) retriever.setDataSource(music.u0, uri) else retriever.setDataSource(MediaPaths.host(path).absolutePath)
                    AppExecutors.c().post(a(music, retriever.embeddedPicture, 4))
                } catch (_: Exception) {
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Exception) {
                    }
                }
            }
            2 -> {
                val music = v
                if (!music.q()) return
                val width = ((w as? View)?.width ?: 0) /
                    music.u0.resources.displayMetrics.density
                val bar: UINavigationBar = music.C0
                if (width.toInt() < 600) {
                    if (bar.w) bar.c(true)
                } else if (bar.x) {
                    bar.d()
                }
                bar.setOnExpandStateChangedListener(e(music))
            }
            3 -> {
                val music = v
                if (music.q() && music.c0 != null && music.Q0 != null) {
                    @Suppress("UNCHECKED_CAST")
                    val items = w as? List<Any?> ?: emptyList()
                    val all = music.S0
                    all.clear()
                    all.addAll(items)
                    music.V0 = 0
                    val visible = music.R0
                    visible.clear()
                    visible.addAll(music.p0())
                    music.Q0.notifyDataSetChanged()
                    val empty = visible.isEmpty()
                    music.D0.visibility = if (empty) 8 else 0
                    if (empty) {
                        val placeholder = music.e1
                        if (placeholder == null) {
                            music.e1 = music.d1.inflate()
                        } else {
                            placeholder.visibility = 0
                        }
                    } else {
                        music.e1?.visibility = 8
                    }
                    music.c1 = true
                }
                music.a1 = false
            }
            else -> {
                val music = v
                if (!music.q()) return
                val cover: ImageView = music.x0 ?: return
                val bytes = w as? ByteArray
                if (bytes != null) {
                    cover.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                } else {
                    cover.setImageResource(R.drawable.ui_music_ic_88_regular)
                }
            }
        }
    }
}
