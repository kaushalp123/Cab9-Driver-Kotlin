package com.cab9.driver.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.RoundedCornerBottomSheetDialogFragment
import com.cab9.driver.databinding.DialogChooseFilesBinding

class ChooseAttachmentBottomDialogFragment : RoundedCornerBottomSheetDialogFragment(), OnClickListener {

    private val binding by viewBinding(DialogChooseFilesBinding::bind)

    override val isDraggable: Boolean
        get() = false

    override val isCancelableOnTouch: Boolean
        get() = true

/*    private val fileChooserPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            listOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        else listOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)

    private val filePicker = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { result ->
        if(result.isNotEmpty()) {
            Timber.i("image uri found")
            if(binding.attachmentList.adapter != null) { // if user selected more items, it will be appended to the already selected items.
                selectedMediaPreviewRecyclerAdapter.addNewAttachments(result.toMutableList())
            } else { // adapter null and new item has been selected
                binding.attachmentList.adapter = SelectedMediaPreviewRecyclerAdapter(requireContext(), result.toMutableList())
                binding.attachmentList.visibility = View.VISIBLE
            }
        } else {
            Timber.i("image uri null")
            binding.attachmentList.visibility = View.GONE
            binding.attachmentList.adapter = null
        }
    }*/

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = DialogChooseFilesBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgUploadDoc.setOnClickListener(this)
        binding.imgUploadPhoto.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.img_upload_doc -> openDocumentsPicker()
            R.id.img_upload_photo -> openImagePicker()
        }
    }

    private fun openImagePicker() {
       // filePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    private fun openDocumentsPicker() {

    }
}