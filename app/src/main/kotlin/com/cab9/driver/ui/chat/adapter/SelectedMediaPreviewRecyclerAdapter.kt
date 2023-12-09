package com.cab9.driver.ui.chat.adapter

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.cab9.driver.data.models.ImageUploadClass
import com.cab9.driver.databinding.ItemAttachmentPreviewBinding
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ext.visibility

class SelectedMediaPreviewRecyclerAdapter(
    private val mContext: Context,
    private val uris: MutableList<ImageUploadClass>,
    private val onAttachmentItemRemoved: (Int) -> Unit
) :
    RecyclerView.Adapter<SelectedMediaPreviewRecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder.create(parent, onAttachmentItemRemoved)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(uris, position, mContext)
    }

    override fun getItemCount(): Int = uris.size

    fun removeAttachment(index: Int) {
        uris.removeAt(index)
        notifyDataSetChanged()
    }

    fun addNewAttachments(newUris: MutableList<ImageUploadClass>) {
        uris.addAll(newUris)
        notifyDataSetChanged()
    }

    class ViewHolder(
        private val binding: ItemAttachmentPreviewBinding,
        private val onAttachmentItemRemoved: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: MutableList<ImageUploadClass>, position: Int, context: Context) {
            val item = uri[position]
            item.imageUri?.let { uri ->
                binding.pBarUploadImage.visibility =
                    if (item.isUploading) View.VISIBLE else View.INVISIBLE

                item.uploadFileResponse?.let {
                    binding.selectedPreviewImg.load(it.thumbnailUrl)
                } ?: run {
                    val bitmap =
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    binding.selectedPreviewImg.setImageBitmap(bitmap)
                }

                binding.imgRemove.setOnClickListener {
                    onAttachmentItemRemoved.invoke(bindingAdapterPosition)
                }
            }
        }

        companion object {
            fun create(parent: ViewGroup, onAttachmentItemRemoved: (Int) -> Unit): ViewHolder =
                ViewHolder(
                    ItemAttachmentPreviewBinding.inflate(
                        parent.layoutInflater,
                        parent,
                        false
                    ), onAttachmentItemRemoved
                )
        }
    }
}