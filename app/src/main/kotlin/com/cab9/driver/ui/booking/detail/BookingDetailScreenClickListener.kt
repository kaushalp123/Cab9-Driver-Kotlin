package com.cab9.driver.ui.booking.detail

interface BookingDetailScreenClickListener {

    fun onAcknowledgeBooking()

    fun canStartRide()

    fun addExpense()

    fun requestPayment()

    fun takeSignature()

}