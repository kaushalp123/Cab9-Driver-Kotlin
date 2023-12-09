package com.cab9.driver.ui.booking.bid.auction

import com.cab9.driver.data.models.BidCategory

interface OnSubmitBidAmountListener {

    fun onSubmitBidAmount(
        enteredAmount: Double,
        bookingId: String? = null,
        category: BidCategory? = null,
        itemPosition: Int? = null
    )

}