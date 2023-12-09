package com.cab9.driver.di.user

import com.cab9.driver.data.repos.*
import com.cab9.driver.di.qualifiers.DefaultDispatcher
import com.cab9.driver.utils.SharedLocationManager
import com.cab9.driver.utils.SharedLocationManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(UserComponent::class)
abstract class UserModule {

    @LoggedInScope
    @Binds
    abstract fun bindCab9Repository(cab9RepoImpl: Cab9RepositoryImpl): Cab9Repository

    @LoggedInScope
    @Binds
    abstract fun bindAccountRepository(accountRepoImpl: AccountRepositoryImpl): AccountRepository

    @LoggedInScope
    @Binds
    abstract fun bindBookingRepository(bookingRepoImpl: BookingRepositoryImpl): BookingRepository

    @LoggedInScope
    @Binds
    abstract fun bindJobPoolBidRepository(jobPoolBidRepoImpl: JobPoolBidRepositoryImpl): JobPoolBidRepository

    @LoggedInScope
    @Binds
    abstract fun bindSumUpRepository(sumUpRepoImpl: SumUpRepositoryImpl): SumUpRepository

    @LoggedInScope
    @Binds
    abstract fun bindPaymentRepository(paymentRepoImpl: PaymentRepositoryImpl): PaymentRepository

    @LoggedInScope
    @Binds
    abstract fun bindGoogleRepository(googleRepoImpl: GoogleRepositoryImpl): GoogleRepository

    @LoggedInScope
    @Binds
    abstract fun bindSharedLocationManager(sharedLocationManagerImpl: SharedLocationManagerImpl): SharedLocationManager

    @LoggedInScope
    @Binds
    abstract fun bindChatRepository(chatRepoImpl: ChatRepositoryImpl): ChatRepository

    companion object {
        @LoggedInScope // Provide always the same instance
        @Provides
        fun providesCoroutineScope(@DefaultDispatcher defaultDispatcher: CoroutineDispatcher): CoroutineScope {
            // Run this code when providing an instance of CoroutineScope
            return CoroutineScope(SupervisorJob() + defaultDispatcher)
        }
    }

}