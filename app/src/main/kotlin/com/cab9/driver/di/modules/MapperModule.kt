package com.cab9.driver.di.modules

import android.content.Context
import com.cab9.driver.data.mapper.*
import com.cab9.driver.settings.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapperModule {

    @Singleton
    @Provides
    fun providesBookingModelMapper(
        @ApplicationContext context: Context,
        sessionManager: SessionManager
    ) = BookingModelMapper(context, sessionManager)

    @Singleton
    @Provides
    fun providesBookingModelListMapper(mapper: BookingModelMapper) = BookingModelListMapper(mapper)

    @Singleton
    @Provides
    fun providesJobBidModelMapper(
        @ApplicationContext context: Context,
        sessionManager: SessionManager
    ) = JobBidMapper(context, sessionManager)

    @Singleton
    @Provides
    fun providesJobBidModelListMapper(mapper: JobBidMapper) = JobBidListMapper(mapper)

    @Singleton
    @Provides
    fun providesInvoiceMapper() = InvoiceMapper()

    @Singleton
    @Provides
    fun providesDriverMapper() = DriverMapper()

    @Singleton
    @Provides
    fun providesPaymentMapper(
        @ApplicationContext context: Context,
        invoiceMapper: InvoiceMapper,
        driverMapper: DriverMapper
    ) = PaymentMapper(context, invoiceMapper, driverMapper)

    @Singleton
    @Provides
    fun providesPaymentListMapper(mapper: PaymentMapper) = PaymentListMapper(mapper)

    @Singleton
    @Provides
    fun providesGooglePredictionMapper() = GooglePredictionMapper()

    @Singleton
    @Provides
    fun providesGooglePredictionListMapper(mapper: GooglePredictionMapper) =
        GooglePredictionListMapper(mapper)

    @Singleton
    @Provides
    fun providesExpenseMapper(@ApplicationContext context: Context) = ExpenseMapper(context)

    @Singleton
    @Provides
    fun providesExpenseListMapper(mapper: ExpenseMapper) = ExpenseListMapper(mapper)

    @Singleton
    @Provides
    fun providesPaymentCardMapper() = PaymentCardMapper()

    @Singleton
    @Provides
    fun providesPaymentCardListMapper(mapper: PaymentCardMapper) = PaymentCardListMapper(mapper)

}