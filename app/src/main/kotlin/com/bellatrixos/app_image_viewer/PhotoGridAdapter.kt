package com.bellatrixos.app_image_viewer

import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bumptech.glide.Glide

/** Thumbnails of the pictures found in the phone's storage, opened in this same Photos window. */
class PhotoGridAdapter(
    private val photos: List<DevicePhoto>,
    private val onClick: (DevicePhoto) -> Unit,
) : RecyclerView.Adapter<PhotoGridAdapter.PhotoHolder>() {

    class PhotoHolder(val image: ImageView) : RecyclerView.ViewHolder(image)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
        val height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            108f,
            parent.context.resources.displayMetrics,
        ).toInt()
        val image = ImageView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(3, 3, 3, 3)
        }
        return PhotoHolder(image)
    }

    override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
        val photo = photos[position]
        Glide.with(holder.image)
            .load(photo.uri)
            .placeholder(R.drawable.img_file_image)
            .error(R.drawable.img_file_image)
            .into(holder.image)
        holder.itemView.setOnClickListener { onClick(photo) }
    }

    override fun getItemCount(): Int = photos.size
}
