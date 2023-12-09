package com.cab9.driver.network.apis

import com.cab9.driver.data.models.GeoPlace
import com.cab9.driver.data.models.Prediction
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleAPI {

    @GET("maps/api/place/autocomplete/json")
    suspend fun searchPlaces(
        @Query("input") searchTerm: String,
        @Query("sensor") sensor: Boolean = true,
        @Query("components") components: String = "country:gb",
        @Query("key") apiKey: String
    ): Prediction.Response

    @GET("maps/api/geocode/json")
    suspend fun getPlaceDetail(
        @Query("place_id") placeId: String,
        @Query("key") apiKey: String
    ): GeoPlace.Response
}