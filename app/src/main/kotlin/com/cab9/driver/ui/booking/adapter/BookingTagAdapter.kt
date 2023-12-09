package com.cab9.driver.ui.booking.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cab9.driver.data.models.Booking
import com.cab9.driver.data.models.Tag
import com.cab9.driver.databinding.ItemBookingTagBinding
import com.cab9.driver.ext.layoutInflater

class BookingTagAdapter(private val tags: List<Tag>) :
    RecyclerView.Adapter<BookingTagAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder.create(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    override fun getItemCount(): Int = tags.size

    class ViewHolder(private val binding: ItemBookingTagBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tag: Tag) {
            binding.lblTagName.text = tag.name
        }

        companion object {
            fun create(parent: ViewGroup): ViewHolder =
                ViewHolder(ItemBookingTagBinding.inflate(parent.layoutInflater, parent, false))
        }
    }
}