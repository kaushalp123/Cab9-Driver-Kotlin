package com.cab9.driver.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Prediction(
    @field:Json(name = "description") val desc: String?,
    @field:Json(name = "place_id") val placeId: String?,
    @field:Json(name = "structured_formatting") val structuredFormat: StructuredFormat?,
) {

    @JsonClass(generateAdapter = true)
    data class StructuredFormat(
        @field:Json(name = "main_text") val mainText: String?,
        @field:Json(name = "secondary_text") val secondaryText: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Response(
        @field:Json(name = "predictions") val predictions: List<Prediction>?,
        @field:Json(name = "status") val status: String?
    )

    data class UiModel(
        val desc: String,
        val placeId: String?,
        val mainText: String,
        val secondaryText: String
    )
}

@JsonClass(generateAdapter = true)
data class GeoPlace(
    @field:Json(name = "id") val id: String?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "place_id") val placeId: String?,
    @field:Json(name = "types") val types: List<String>?,
    @field:Json(name = "address_components") val addresses: List<Address>?,
    @field:Json(name = "adr_address") val adrAddress: String?,
    @field:Json(name = "formatted_address") val formattedAddress: String?,
    @field:Json(name = "geometry") val geometry: Geometry?,
    @field:Json(name = "icon") val iconUrl: String?,
    @field:Json(name = "plus_code") val zipcode: Zipcode?,
) {

    @JsonClass(generateAdapter = true)
    data class Address(
        @field:Json(name = "long_name") val longName: String?,
        @field:Json(name = "short_name") val shortName: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Geometry(
        @field:Json(name = "location") val latLng: LatLng?,
    ) {
        @JsonClass(generateAdapter = true)
        data class LatLng(
            @field:Json(name = "lat") val lat: Double?,
            @field:Json(name = "lng") val lng: Double?
        )
    }

    @JsonClass(generateAdapter = true)
    data class Zipcode(
        @field:Json(name = "compound_code") val compoundCode: String?,
        @field:Json(name = "global_code") val globalCode: String?
    )


    @JsonClass(generateAdapter = true)
    data class Response(
        @field:Json(name = "results") val results: List<GeoPlace>?,
        @field:Json(name = "status") val status: String?
    )
}