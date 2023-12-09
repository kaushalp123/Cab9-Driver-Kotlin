package com.cab9.driver.ui.zone

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cab9.driver.R
import com.cab9.driver.data.models.ZoneModel
import com.cab9.driver.databinding.ItemZonalOverviewBinding
import com.cab9.driver.ext.backgroundColor
import com.cab9.driver.ext.gone
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ext.show
import com.cab9.driver.ext.visibility

class ZonalAdapter(private val zones: MutableList<ZoneModel> = mutableListOf()) :
    RecyclerView.Adapter<ZonalAdapter.ViewHolder>() {

    fun addNewZones(newZones: List<ZoneModel>) {
        val callback = ZonalCallback(zones, newZones)
        val result = DiffUtil.calculateDiff(callback)

        zones.clear()
        zones.addAll(newZones)
        result.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder.create(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(zones[position])
    }

    override fun getItemCount(): Int = zones.size

    class ViewHolder(private val binding: ItemZonalOverviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(zone: ZoneModel) {
            if (bindingAdapterPosition % 2 == 0) {
                binding.root.backgroundColor(R.color.zone_item_bg_color_1)
            } else binding.root.backgroundColor(R.color.zone_item_bg_color_2)

            binding.lblDriverCount.text = "${zone.driverCount}"
            binding.lblZoneName.text = zone.areaName

            val hasRank = zone.rank != null && zone.rank > 0
            binding.lblDriverRank.visibility(hasRank)
            binding.lblDriverRank.text =
                binding.root.context.getString(R.string.temp_zone_rank, zone.rank)

            binding.lblZoneDistanceAway.visibility(!hasRank)
            binding.lblZoneDistanceAway.text =
                binding.root.context.getString(R.string.temp_zone_distance, zone.distance)

            listOf(
                binding.lblBookingCount1,
                binding.lblBookingCount2,
                binding.lblBookingCount3,
                binding.lblBookingCount4
            ).mapIndexed { index, materialTextView ->
                if (index < zone.data.size) {
                    materialTextView.show()
                    materialTextView.text =
                        if (zone.data[index] > 0) zone.data[index].toString() else "-"
                } else materialTextView.gone()
            }
        }


        companion object {
            fun create(parent: ViewGroup) =
                ViewHolder(
                    ItemZonalOverviewBinding.inflate(
                        parent.layoutInflater,
                        parent,
                        false
                    )
                )
        }
    }
}