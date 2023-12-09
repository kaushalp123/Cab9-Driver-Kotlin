package com.cab9.driver.ui.flagdown

import android.os.Bundle
import android.view.View
import androidx.activity.result.launch
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.CreateFlagDownBookingRequest
import com.cab9.driver.databinding.FragmentCreateFlagDownBookingBinding
import com.cab9.driver.ext.lat
import com.cab9.driver.ext.lng
import com.cab9.driver.ext.snack
import com.cab9.driver.ext.text
import com.cab9.driver.ext.visibility
import com.cab9.driver.ui.search.SearchPlaceResult
import com.cab9.driver.ui.search.SearchPlaces
import com.cab9.driver.widgets.dialog.cancelButton
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateFlagDownBookingFragment : BaseFragment(R.layout.fragment_create_flag_down_booking),
    View.OnClickListener {

    companion object {

        const val BUNDLE_PICKUP_LAT = "pickup_lat"
        const val BUNDLE_PICKUP_LNG = "pickup_lng"

        fun newInstance(latLng: LatLng) = CreateFlagDownBookingFragment().apply {
            arguments = bundleOf(
                BUNDLE_PICKUP_LAT to latLng.latitude,
                BUNDLE_PICKUP_LNG to latLng.longitude
            )
        }
    }

    private val binding by viewBinding(FragmentCreateFlagDownBookingBinding::bind)
    private val viewModel by activityViewModels<CreateFlagDownBookingViewModel>()

    private val searchPlaces = registerForActivityResult(SearchPlaces()) {
        if (it != null) {
            viewModel.dropPlace = it
            binding.txtFlagDownDropAddress.setText(it.name)
        } //else binding.root.snack(R.string.err_invalid_location)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtFlagDownDropAddress.setOnClickListener(this)
        binding.btnStartFlagDownRide.setOnClickListener(this)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pickupAddress.collectLatest {
                        if (it is Outcome.Success) binding.txtFlagDownPickupAddress.setText(it.data)
                    }
                }
                launch {
                    viewModel.createBookingOutcome.collectLatest {
                        binding.pBarFlagDown.visibility(it is Outcome.Progress)
                        binding.btnStartFlagDownRide.isEnabled = it !is Outcome.Progress
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_start_flag_down_ride -> validateCreateBookingParams()
            R.id.txt_flag_down_drop_address -> searchPlaces.launch()
        }
    }

    private fun validateCreateBookingParams() {
        when {
            binding.txtFlagDownPickupAddress.text().isEmpty() -> {
                binding.root.snack(R.string.err_no_pickup_location)
            }

            else -> createBooking(viewModel.dropPlace)
        }
    }

    private fun createBooking(dropResult: SearchPlaceResult?) {
        if (dropResult == null || binding.txtFlagDownDropAddress.text().isEmpty()) {
            showMaterialAlert {
                titleResource = R.string.dialog_title_are_you_sure
                messageResource = R.string.dialog_msg_start_as_directed_booking
                cancelButton { it.dismiss() }
                positiveButton(R.string.action_continue) { createAsDirectedBooking() }
            }
        } else {
            showMaterialAlert {
                titleResource = R.string.dialog_title_are_you_sure
                messageResource = R.string.dialog_msg_start_flag_down_booking
                cancelButton { it.dismiss() }
                positiveButton(R.string.action_continue) {
                    createFlagDownBooking(dropResult.lat, dropResult.lng)
                }
            }
        }
    }

    private fun createFlagDownBooking(dropLat: Double, dropLng: Double) {
        val request = CreateFlagDownBookingRequest(
            pickupLat = viewModel.pickupLatLng.lat,
            pickupLng = viewModel.pickupLatLng.lng,
            pickupStopSummary = binding.txtFlagDownPickupAddress.text(),
            asDirected = false,
            dropLat = dropLat,
            dropLng = dropLng,
            dropStopSummary = binding.txtFlagDownDropAddress.text()
        )
        viewModel.createBooking(request)
    }

    private fun createAsDirectedBooking() {
        val request = CreateFlagDownBookingRequest(
            pickupLat = viewModel.pickupLatLng.lat,
            pickupLng = viewModel.pickupLatLng.lng,
            pickupStopSummary = binding.txtFlagDownPickupAddress.text(),
            asDirected = true
        )
        viewModel.createBooking(request)
    }

}