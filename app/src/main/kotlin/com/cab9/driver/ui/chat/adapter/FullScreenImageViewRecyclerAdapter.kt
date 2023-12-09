package com.cab9.driver.ui.chat.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.cab9.driver.R
import com.cab9.driver.data.models.Attachments
import com.cab9.driver.databinding.ItemFullScreenImageBinding
import com.cab9.driver.ext.layoutInflater

class FullScreenImageViewRecyclerAdapter(
    private val uris: MutableList<Attachments>
) :
    RecyclerView.Adapter<FullScreenImageViewRecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder.create(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(uris, position)
    }

    override fun getItemCount(): Int = uris.size

    class ViewHolder(
        private val binding: ItemFullScreenImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: MutableList<Attachments>, position: Int) {
            val item = uri[position]
            if (!item.url.isNullOrEmpty()) {
                binding.image.load(item.url) {
                    placeholder(R.drawable.img_chat_files_placeholder)
                }
            } else binding.image.load(R.drawable.img_chat_files_placeholder)

            if (uri.size <= 1 || position == uri.size - 1) {
                binding.lblSwipeNext.visibility = View.GONE
            } else binding.lblSwipeNext.visibility = View.VISIBLE
        }

        companion object {
            fun create(parent: ViewGroup): ViewHolder =
                ViewHolder(
                    ItemFullScreenImageBinding.inflate(
                        parent.layoutInflater,
                        parent,
                        false
                    )
                )
        }
    }
}