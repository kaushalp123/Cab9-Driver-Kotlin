package com.cab9.driver.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Prediction
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.failureOverlay
import com.cab9.driver.ext.loadingOverlay
import com.cab9.driver.ext.success
import com.cab9.driver.network.ApiErrorHandler
import com.cab9.driver.network.NoLocationFoundException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchPlaceViewModel @Inject constructor(
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    private val _googlePredictions =
        MutableStateFlow<Outcome<List<Prediction.UiModel>>>(Outcome.Empty)
    val googlePredictions = _googlePredictions.asStateFlow()

    private var searchJob: Job? = null

    fun searchPlaces(searchTerm: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                _googlePredictions.loadingOverlay()
                val result = userComponentManager.googleRepo.searchPlaces(searchTerm)
                _googlePredictions.success(result)
            } catch (ex: Exception) {
                _googlePredictions.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _placeDetail = MutableStateFlow<Outcome<SearchPlaceResult>>(Outcome.Empty)
    val placeDetail = _placeDetail.asStateFlow()

    fun getPlaceDetail(prediction: Prediction.UiModel) {
        viewModelScope.launch {
            try {
                _placeDetail.loadingOverlay()
                val response = userComponentManager.googleRepo
                    .getPlaceDetail(prediction.placeId.orEmpty())
                val lat = response?.geometry?.latLng?.lat
                val lng = response?.geometry?.latLng?.lng
                if (lat != null && lng != null) {
                    val result = SearchPlaceResult(prediction.desc, lat, lng)
                    _placeDetail.success(result)
                } else throw NoLocationFoundException()
            } catch (ex: Exception) {
                _placeDetail.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

}