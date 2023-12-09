package com.cab9.driver.ui.flagdown

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseActivity
import com.cab9.driver.base.Outcome
import com.cab9.driver.databinding.ActivityCreateFlagDownBookingBinding
import com.cab9.driver.ext.lat
import com.cab9.driver.ext.lng
import com.cab9.driver.ui.flagdown.CreateFlagDownBooking.Companion.EXTRA_BOOKING_ID
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateFlagDownBookingActivity : BaseActivity(R.layout.activity_create_flag_down_booking) {

    companion object {
        private const val EXTRA_PICKUP_LAT = "pickup_lat"
        private const val EXTRA_PICKUP_LNG = "pickup_lng"

        @JvmStatic
        fun newInstance(context: Context, latLng: LatLng): Intent {
            return Intent(context, CreateFlagDownBookingActivity::class.java)
                .putExtra(EXTRA_PICKUP_LAT, latLng.lat)
                .putExtra(EXTRA_PICKUP_LNG, latLng.lng)
        }
    }

    private val binding by viewBinding(ActivityCreateFlagDownBookingBinding::bind)
    private val viewModel by viewModels<CreateFlagDownBookingViewModel>()

    private val pickupLatLng: LatLng
        get() {
            val lat = intent.getDoubleExtra(EXTRA_PICKUP_LAT, 0.0)
            val lng = intent.getDoubleExtra(EXTRA_PICKUP_LNG, 0.0)
            return LatLng(lat, lng)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.flagDownToolbar)
        supportActionBar?.run {
            setTitle(R.string.title_activity_flag_down)
            setDisplayHomeAsUpEnabled(true)
        }

        if (savedInstanceState == null)
            supportFragmentManager.commit {
                replace(
                    R.id.flag_down_fragment_container,
                    CreateFlagDownBookingFragment.newInstance(pickupLatLng)
                )
            }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.createBookingOutcome.collectLatest {
                    if (it is Outcome.Success) {
                        val data = Intent().apply { putExtra(EXTRA_BOOKING_ID, it.data) }
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}