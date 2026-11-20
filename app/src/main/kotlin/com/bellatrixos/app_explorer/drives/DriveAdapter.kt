package com.bellatrixos.app_explorer.drives

import android.content.Context
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.AppExecutors
import com.bellatrixos.R

/** Adapter daftar drive, folder, dan judul pada halaman penyimpanan. */
class DriveAdapter(@JvmField val y: Context, @JvmField val z: List<DriveItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class DriveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: ImageView = view.findViewById(R.id.drive_image)
        @JvmField var v: TextView = view.findViewById(R.id.drive_name)
        @JvmField var w: TextView = view.findViewById(R.id.drive_space)
        @JvmField var x: ProgressBar = view.findViewById(R.id.progress_bar)
    }

    class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: ImageView = view.findViewById(R.id.folder_image)
        @JvmField var v: TextView = view.findViewById(R.id.folder_name)
    }

    class TitleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: TextView = view.findViewById(R.id.title_text)
    }

    fun interface OnItemClickListener {
        fun c(i2: Int)
    }

    @JvmField var A: GestureDetector? = null
    @JvmField var B: OnItemClickListener? = null
    @JvmField var x: Int = 0
    @JvmField var C: Boolean = false

    init {
        AppExecutors.c().post {
            A = GestureDetector(y, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(event: MotionEvent): Boolean {
                    val listener = B
                    if (listener == null || !C) return true
                    listener.c(x)
                    return true
                }

                override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                    val listener = B
                    if (listener == null || C) return true
                    listener.c(x)
                    return true
                }
            })
        }
    }

    override fun getItemCount(): Int = z.size

    override fun getItemViewType(position: Int): Int = z[position].f()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder.itemView
        if (view.isFocusable != C) view.isFocusable = C
        if (view.isFocusableInTouchMode != C) view.isFocusableInTouchMode = C

        val item = z[position]
        when (holder) {
            is FolderViewHolder -> {
                holder.u.setImageResource(item.a())
                holder.v.text = item.b()
            }
            is DriveViewHolder -> {
                holder.u.setImageResource(item.a())
                holder.v.text = item.b()
                holder.w.text = item.d()
                holder.x.progress = item.e()
            }
            is TitleViewHolder -> holder.u.text = item.b()
        }
        view.setOnTouchListener { _, event ->
            x = holder.bindingAdapterPosition
            val detector = A ?: return@setOnTouchListener false
            detector.onTouchEvent(event)
            false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(y)
        return when (viewType) {
            2 -> TitleViewHolder(inflater.inflate(R.layout.explorer_item_title, parent, false))
            1 -> FolderViewHolder(inflater.inflate(R.layout.explorer_item_lg_folder, parent, false))
            else -> DriveViewHolder(inflater.inflate(R.layout.explorer_item_lg_drive, parent, false))
        }
    }
}
