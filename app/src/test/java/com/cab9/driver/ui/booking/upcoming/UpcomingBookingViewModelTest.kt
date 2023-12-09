package com.cab9.driver.ui.booking.upcoming

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.cab9.driver.MainCoroutineRule
import com.cab9.driver.data.repos.BookingRepository
import com.cab9.driver.network.ApiErrorHandler
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpcomingBookingViewModelTest {

    @MockK
    private lateinit var bookingRepo: BookingRepository

    @MockK
    private lateinit var errorHandler: ApiErrorHandler

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    lateinit var viewModel: UpcomingBookingViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
//        viewModel = UpcomingBookingViewModel(bookingRepo, errorHandler)
//
//        every { bookingRepo } returns mockk {
//            coEvery { bookingRepo.canStartRide(any(), any()) } answers {
//                val bookingId = firstArg<String>()
//                val latLng = secondArg<LatLng>()
//                true
//            }
//        }
    }

    @Test
    fun `empty booking id passed to start ride`() = runTest {
    }

    @Test
    fun startBooking() {
    }
}