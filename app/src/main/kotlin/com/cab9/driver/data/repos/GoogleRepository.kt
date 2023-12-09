package com.cab9.driver.data.repos

import com.cab9.driver.data.mapper.GooglePredictionListMapper
import com.cab9.driver.data.models.GeoPlace
import com.cab9.driver.data.models.Prediction
import com.cab9.driver.di.user.LoggedInScope
import com.cab9.driver.network.apis.GoogleAPI
import com.cab9.driver.settings.SessionManager
import javax.inject.Inject

interface GoogleRepository {
    suspend fun searchPlaces(searchTerm: String): List<Prediction.UiModel>

    suspend fun getPlaceDetail(placeId: String): GeoPlace?
}

@LoggedInScope
class GoogleRepositoryImpl @Inject constructor(
    private val googleApi: GoogleAPI,
    private val sessionManager: SessionManager,
    private val listMapper: GooglePredictionListMapper
) : GoogleRepository {

    private val apiKey: String?
        get() = sessionManager.googleApiKey

    override suspend fun searchPlaces(searchTerm: String): List<Prediction.UiModel> {
        if (apiKey.isNullOrEmpty()) throw IllegalArgumentException("Google API key not found!")
        val response = googleApi.searchPlaces(searchTerm, apiKey = apiKey.orEmpty())
        return listMapper.map(response.predictions.orEmpty())
    }

    override suspend fun getPlaceDetail(placeId: String): GeoPlace? {
        if (apiKey.isNullOrEmpty()) throw IllegalArgumentException("Google API key not found!")
        val response = googleApi.getPlaceDetail(placeId, apiKey.orEmpty())
        return response.results?.firstOrNull()
    }

}