package com.cab9.driver.ui.account.vehicle

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.Vehicle
import com.cab9.driver.databinding.FragmentChooseVehicleBinding
import com.cab9.driver.databinding.ItemVehicleBinding
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ext.snack
import com.cab9.driver.ext.visibility
import com.cab9.driver.ui.home.HomeViewModel
import com.cab9.driver.widgets.dialog.cancelButton
import com.cab9.driver.widgets.dialog.okButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChooseVehicleFragment : BaseFragment(R.layout.fragment_choose_vehicle) {

    private val binding by viewBinding(FragmentChooseVehicleBinding::bind)

    private val viewModel by viewModels<VehicleViewModel>()
    private val homeViewModel by activityViewModels<HomeViewModel>()

    private val vehicleAdapter: VehicleAdapter
        get() = binding.vehicleList.adapter as VehicleAdapter

    private val onVehicleSelected: (Vehicle) -> Unit = { vehicle ->
        when (homeViewModel.mobileState?.driverStatus) {
            Driver.Status.ON_JOB, Driver.Status.CLEARING ->
                showMaterialAlert {
                    titleResource = R.string.dialog_title_update_not_allowed
                    messageResource = R.string.msg_vehicle_update_not_allowed_job
                    okButton { it.dismiss() }
                }

            Driver.Status.ONLINE, Driver.Status.ON_BREAK ->
                showMaterialAlert {
                    titleResource = R.string.dialog_title_update_not_allowed
                    messageResource = R.string.msg_vehicle_update_not_allowed_online
                    okButton { it.dismiss() }
                }

            else -> {
                showMaterialAlert {
                    titleResource = R.string.dialog_title_vehicle_update_confirm
                    messageResource = R.string.msg_vehicle_update_confirm_message
                    okButton {
                        viewModel.updateCurrentVehicle(vehicle.id.orEmpty())
                        it.dismiss()
                    }
                    cancelButton { it.dismiss() }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vehicleList.adapter = VehicleAdapter(onVehicleSelected)
        homeViewModel.mobileState?.currentVehicleId?.let {
            vehicleAdapter.onVehicleChanged(it)
        }

        binding.root.onRetryListener { viewModel.getVehicles(true) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.vehiclesOutcome.collectLatest {
                        binding.root.setState(it)
                        if (it is Outcome.Success) vehicleAdapter.vehicles = it.data
                    }
                }
                launch {
                    viewModel.updateCurrentVehicleOutcome.collectLatest {
                        binding.pBarVehicleList.visibility(it is Outcome.Progress)
                        if (it is Outcome.Success) {
                            vehicleAdapter.onVehicleChanged(it.data.first)
                            homeViewModel.fetchMobileState()
                        } else if (it is Outcome.Failure) binding.root.snack(it.msg)
                    }
                }
            }
        }
    }

    private class VehicleAdapter(private val onVehicleSelected: (Vehicle) -> Unit) :
        RecyclerView.Adapter<VehicleAdapter.ViewHolder>() {

        companion object {
            private var selectedVehicleId: String? = null
        }

        var vehicles: List<Vehicle> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun onVehicleChanged(newVehicleId: String) {
            selectedVehicleId = newVehicleId
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.create(parent, onVehicleSelected)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(vehicles[position])
        }

        override fun getItemCount(): Int = vehicles.size

        class ViewHolder(
            private val binding: ItemVehicleBinding,
            private val onVehicleSelected: (Vehicle) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(vehicle: Vehicle) {
                binding.vehicle = vehicle
                binding.cardVehicle.isChecked = selectedVehicleId == vehicle.id
                binding.root.setOnClickListener { onVehicleSelected.invoke(vehicle) }
                binding.executePendingBindings()
            }

            companion object {
                fun create(
                    parent: ViewGroup,
                    onVehicleSelected: (Vehicle) -> Unit
                ) =
                    ViewHolder(
                        ItemVehicleBinding.inflate(parent.layoutInflater, parent, false),
                        onVehicleSelected
                    )
            }
        }
    }

}