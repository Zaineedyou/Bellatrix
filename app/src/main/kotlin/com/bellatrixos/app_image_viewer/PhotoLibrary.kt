package com.bellatrixos.app_image_viewer

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import java.io.File

/** One picture the phone reports from shared storage. */
data class DevicePhoto(val name: String, val uri: Uri, val path: String?)

/**
 * The Photos window's source of pictures. MediaStore is the same library Android's gallery uses, so it finds photos
 * in DCIM, Camera, Downloads and every other folder the user has on the phone, not just files Bellatrix opened before.
 */
object PhotoLibrary {
    /** Android 13 replaced READ_EXTERNAL_STORAGE with READ_MEDIA_IMAGES for pictures. */
    fun permission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasAccess(context: Context): Boolean =
        ActivityCompat.checkSelfPermission(context, permission()) == PackageManager.PERMISSION_GRANTED

    /** Newest first. A path is returned only when the file is really readable as a File. */
    fun list(context: Context, limit: Int = 600): List<DevicePhoto> {
        if (!hasAccess(context)) return emptyList()
        val result = ArrayList<DevicePhoto>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
        )
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                while (idColumn >= 0 && cursor.moveToNext() && result.size < limit) {
                    val id = cursor.getLong(idColumn)
                    val name = if (nameColumn >= 0) cursor.getString(nameColumn) else null
                    val rawPath = if (dataColumn >= 0) cursor.getString(dataColumn) else null
                    val path = rawPath?.takeIf { it.isNotEmpty() && File(it).isFile }
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    result += DevicePhoto(name?.takeIf { it.isNotEmpty() } ?: "Photo $id", uri, path)
                }
            }
        } catch (_: Exception) {
            // A missing provider or a revoked permission just leaves the empty state visible.
        }
        return result
    }
}
