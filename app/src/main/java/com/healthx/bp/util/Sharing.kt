package com.healthx.bp.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object Sharing {

    private fun authority(context: Context) = "${context.packageName}.fileprovider"

    /** Write [content] to a cache file and return a shareable content:// Uri. */
    fun cacheTextUri(context: Context, fileName: String, content: String): Uri {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    private const val JPEG_QUALITY = 90

    /** Pick compress format + quality from the file/display name extension. */
    private fun formatFor(name: String): Pair<Bitmap.CompressFormat, Int> =
        if (name.endsWith(".jpg", true) || name.endsWith(".jpeg", true))
            Bitmap.CompressFormat.JPEG to JPEG_QUALITY
        else Bitmap.CompressFormat.PNG to 100

    /** Write [bitmap] to a cache image and return a shareable content:// Uri. */
    fun cacheBitmapUri(context: Context, fileName: String, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, fileName)
        val (fmt, q) = formatFor(fileName)
        FileOutputStream(file).use { bitmap.compress(fmt, q, it) }
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    fun shareFile(context: Context, uri: Uri, mime: String, title: String = "分享") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Save a bitmap to the public Pictures/HealthX album. Returns true on success. */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String): Boolean {
        val (fmt, q) = formatFor(displayName)
        val mime = if (fmt == Bitmap.CompressFormat.JPEG) "image/jpeg" else "image/png"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HealthX")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                resolver.openOutputStream(uri)?.use { bitmap.compress(fmt, q, it) }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "HealthX").apply { mkdirs() }
                val file = File(dir, displayName)
                FileOutputStream(file).use { bitmap.compress(fmt, q, it) }
                MediaStore.Images.Media.insertImage(context.contentResolver, file.absolutePath, displayName, null)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
