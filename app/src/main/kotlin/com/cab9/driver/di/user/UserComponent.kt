package com.cab9.driver.di.user

import com.cab9.driver.data.repos.*
import com.cab9.driver.services.SocketManager
import com.cab9.driver.ui.booking.offers.BookingOfferManager
import com.cab9.driver.utils.SharedLocationManager
import dagger.hilt.DefineComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@LoggedInScope
@DefineComponent(parent = SingletonComponent::class)
interface UserComponent {

    @DefineComponent.Builder
    interface Builder {
        fun build(): UserComponent
    }
}

@EntryPoint
@InstallIn(UserComponent::class)
interface UserComponentEntryPoint {
    fun cab9Repository(): Cab9Repository
    fun accountRepository(): AccountRepository
    fun bookingRepository(): BookingRepository
    fun jobPoolBidRepository(): JobPoolBidRepository
    fun sumUpRepository(): SumUpRepository
    fun paymentRepository(): PaymentRepository
    fun googleRepository(): GoogleRepository
    fun sharedLocationManger(): SharedLocationManager
    fun socketManager(): SocketManager
    fun chatRepository(): ChatRepository
    fun bookingOfferManager(): BookingOfferManager
}