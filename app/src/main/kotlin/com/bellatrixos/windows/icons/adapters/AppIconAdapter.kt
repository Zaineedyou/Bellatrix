package com.bellatrixos.windows.icons.adapters

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.windows.icons.IconEngine

/** Adapter grid ikon aplikasi untuk pengganti ikon. */
class AppIconAdapter(
    @JvmField val x: Context,
    @JvmField val z: ArrayList<Any>
) : RecyclerView.Adapter<AppIconAdapter.IconViewHolder>() {

    class IconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: ImageView = view as ImageView
    }

    fun interface OnItemClickListener {
        fun b(str: String)
    }

    @JvmField var y: OnItemClickListener? = null

    override fun getItemCount(): Int = z.size

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val str = z[position] as? String ?: return
        IconEngine.b(x).c(str, IconEngine.Callback { bitmap -> holder.u.setImageBitmap(bitmap) })
        holder.u.setOnClickListener { y?.b(str) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                -1,
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    56f,
                    parent.resources.displayMetrics
                ).toInt()
            )
            setPadding(12, 12, 12, 12)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        return IconViewHolder(view)
    }
}
