package com.cab9.driver.ui.flagdown

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.gms.maps.model.LatLng

class CreateFlagDownBooking : ActivityResultContract<LatLng, String?>() {

    companion object {
        const val EXTRA_BOOKING_ID = "booking_id"
    }

    override fun createIntent(context: Context, input: LatLng): Intent {
        return CreateFlagDownBookingActivity.newInstance(context, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.getStringExtra(EXTRA_BOOKING_ID)
    }

}