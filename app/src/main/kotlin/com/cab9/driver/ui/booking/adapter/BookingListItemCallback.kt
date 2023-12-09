package com.cab9.driver.ui.booking.adapter

import androidx.recyclerview.widget.DiffUtil
import com.cab9.driver.data.models.BookingListModel

class BookingListItemCallback : DiffUtil.ItemCallback<BookingListModel>() {

    override fun areItemsTheSame(
        oldItem: BookingListModel,
        newItem: BookingListModel
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: BookingListModel,
        newItem: BookingListModel
    ): Boolean = oldItem == newItem

}
