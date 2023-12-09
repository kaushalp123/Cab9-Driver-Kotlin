package com.cab9.driver.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.cab9.driver.R
import com.cab9.driver.ext.toast
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

//val docsDirectory: File
//    get() = File(context.filesDir, "docs")

inline fun File.mimeType(): String? =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(this.extension)

private fun getUriForFile(context: Context, file: File) =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

fun createTempImageUri(context: Context): Uri {
    val tmpFile = File.createTempFile("tmp_image_", ".jpg", context.cacheDir)
        .apply {
            createNewFile()
            deleteOnExit()
        }
    return getUriForFile(context, tmpFile)
}

fun getFileName(context: Context, uri: Uri): String? {
    context.contentResolver.query(uri, null, null, null, null)
        .use { cursor ->
            if (cursor != null) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                return cursor.getString(nameIndex)
            }
        }
    return null
}

fun getFileSizeInBytes(context: Context, uri: Uri): Long? {
    context.contentResolver.query(uri, null, null, null, null)
        .use { cursor ->
            if (cursor != null) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                return cursor.getLong(sizeIndex)
            }
        }
    return null
}

fun saveAsJpg(context: Context, bitmap: Bitmap): File? {
    var outputStream: FileOutputStream? = null
    try {
        val imageFile = File.createTempFile("sig_image", ".jpg", context.cacheDir)
            .apply {
                createNewFile()
                deleteOnExit()
            }
        outputStream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return imageFile
    } catch (ex: Exception) {
        Timber.w(ex)
    } finally {
        outputStream?.close()
        outputStream?.flush()
    }
    return null
}

fun openPdfFile(context: Context, file: File) {
    try {
        val uri = getUriForFile(context, file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.startActivity(intent)
    } catch (ex: Exception) {
        context.toast(R.string.err_msg_generic)
    }
}

fun compressImage(context: Context, uri: Uri): ByteArray? {
    try {
        val scaleDivider = 2
        val bitmap = MediaStore.Images.Media.getBitmap(
            context.contentResolver,
            uri
        )

        val scaleWidth = bitmap.width / scaleDivider
        val scaleHeight = bitmap.height / scaleDivider

        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap!!,
            scaleWidth,
            scaleHeight,
            true
        )

        // Instantiate the downsized image content as a byte[]
        val baos = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        return baos.toByteArray()
    }
    catch (e:Exception){
        return null
    }
}
