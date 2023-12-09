package com.cab9.driver.ui.zone

import androidx.recyclerview.widget.DiffUtil
import com.cab9.driver.data.models.ZoneModel

class ZonalCallback(private val oldData: List<ZoneModel>, private val newData: List<ZoneModel>) :
    DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldData.size

    override fun getNewListSize(): Int = newData.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldData[oldItemPosition].areaName == newData[newItemPosition].areaName

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldData[oldItemPosition] == newData[newItemPosition]
                // to maintain old even row colors
                && oldItemPosition % 2 == newItemPosition % 2

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        // for item animator
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}