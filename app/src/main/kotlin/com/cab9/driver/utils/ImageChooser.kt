package com.cab9.driver.utils

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import com.cab9.driver.R

/**
 * Created by hussain on 17/10/22
 **/
class FilePicker : ActivityResultContract<Array<String>, List<Uri>>() {

    private var cameraUri: Uri? = null

    override fun createIntent(context: Context, input: Array<String>): Intent {
        val documentPickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .putExtra(Intent.EXTRA_MIME_TYPES, input)
            .setType("*/*")

        cameraUri = createTempImageUri(context)
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
        }

        return Intent
            .createChooser(documentPickerIntent, context.getString(R.string.title_file_chooser))
            .apply { putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent)) }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        // Use a LinkedHashSet to maintain any ordering
        // that may be present in the ClipData
        val resultSet = LinkedHashSet<Uri?>()
        if (intent?.data != null) resultSet.add(intent.data)
        val clipData = intent?.clipData
        if (clipData == null && resultSet.isEmpty()) {
            // do nothing
        } else if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                val uri = clipData.getItemAt(i).uri
                if (uri != null) resultSet.add(uri)
            }
        }

        val validUris = resultSet.mapNotNull { it }
        return if (resultCode == RESULT_OK) {
            validUris.ifEmpty { cameraUri?.let { listOf(it) } ?: emptyList() }
            //validUris.ifEmpty { listOf(cameraUri) }
        } else emptyList()
    }
}