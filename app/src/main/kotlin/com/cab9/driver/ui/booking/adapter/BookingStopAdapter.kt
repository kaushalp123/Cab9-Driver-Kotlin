package com.cab9.driver.ui.booking.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cab9.driver.data.models.Booking
import com.cab9.driver.databinding.ItemStopBinding
import com.cab9.driver.ext.layoutInflater

class BookingStopAdapter(private val stops: List<Booking.Stop>) :
    RecyclerView.Adapter<BookingStopAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder.create(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(stops[position])
    }

    override fun getItemCount(): Int = stops.size

    class ViewHolder(private val binding: ItemStopBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stop: Booking.Stop) {
            binding.lblStopAddress.text = stop.summary
        }

        companion object {
            fun create(parent: ViewGroup): ViewHolder =
                ViewHolder(ItemStopBinding.inflate(parent.layoutInflater, parent, false))
        }
    }
}