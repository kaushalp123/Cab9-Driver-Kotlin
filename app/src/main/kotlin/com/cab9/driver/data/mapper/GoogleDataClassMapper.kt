package com.cab9.driver.data.mapper

import com.cab9.driver.data.models.Prediction

class GooglePredictionMapper : Mapper<Prediction, Prediction.UiModel> {
    override fun map(input: Prediction): Prediction.UiModel =
        Prediction.UiModel(
            desc = input.desc.orEmpty(),
            placeId = input.placeId,
            mainText = input.structuredFormat?.mainText.orEmpty(),
            secondaryText = input.structuredFormat?.secondaryText.orEmpty()
        )
}

class GooglePredictionListMapper(mapper: GooglePredictionMapper) :
    ListMapperImpl<Prediction, Prediction.UiModel>(mapper)