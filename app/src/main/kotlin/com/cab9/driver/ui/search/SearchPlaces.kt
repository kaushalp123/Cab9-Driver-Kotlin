package com.cab9.driver.ui.search

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import com.cab9.driver.ext.parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchPlaceResult(
    val name: String,
    val lat: Double,
    val lng: Double
) : Parcelable

class SearchPlaces : ActivityResultContract<Unit, SearchPlaceResult?>() {

    companion object {
        const val EXTRA_PREDICTION = "prediction"
    }

    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(context, SearchPlacesActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): SearchPlaceResult? {
        return try {
            if (resultCode == Activity.RESULT_OK && intent != null)
                intent.parcelable(EXTRA_PREDICTION)
            else null
        } catch (ex: Exception) {
            null
        }
    }

}