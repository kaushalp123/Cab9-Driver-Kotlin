package com.cab9.driver.di.user

import com.cab9.driver.data.repos.AccountRepository
import com.cab9.driver.data.repos.BookingRepository
import com.cab9.driver.data.repos.Cab9Repository
import com.cab9.driver.data.repos.ChatRepository
import com.cab9.driver.data.repos.GoogleRepository
import com.cab9.driver.data.repos.JobPoolBidRepository
import com.cab9.driver.data.repos.PaymentRepository
import com.cab9.driver.data.repos.SumUpRepository
import com.cab9.driver.data.repos.UserAuth
import com.cab9.driver.data.repos.UserManager
import com.cab9.driver.services.SocketManager
import com.cab9.driver.ui.booking.offers.BookingOfferManager
import com.cab9.driver.utils.SharedLocationManager
import dagger.hilt.EntryPoints
import dagger.hilt.internal.GeneratedComponentManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

data class ComponentVersion internal constructor(private val version: Int = versionSeq.incrementAndGet()) {
    companion object {
        private val versionSeq = AtomicInteger(0)
        fun next(): ComponentVersion = ComponentVersion()
    }
}

@Singleton
class UserComponentManager @Inject constructor(
    private val userManager: UserManager,
    // Since UserComponentManager will be in charge of managing the UserComponent's
    // lifecycle, it needs to know how to create instances of it. We use the
    // provider (i.e. factory) Dagger generates for us to create instances of UserComponent.
    private val userComponentProvider: Provider<UserComponent.Builder>
) : GeneratedComponentManager<UserComponent> {

    private val _versionState = MutableStateFlow(ComponentVersion.next())
    private val versionState = _versionState.asStateFlow()

    /**
     *  UserComponent is specific to a logged in user. Holds an instance of
     *  UserComponent. This determines if the user is logged in or not, when the
     *  user logs in, a new Component will be created.
     *  When the user logs out, this will be null.
     */
    private var userComponent: UserComponent = userComponentProvider.get().build()

    val cab9Repo: Cab9Repository =
        EntryPoints.get(userComponent, UserComponentEntryPoint::class.java)
            .cab9Repository()

    val accountRepo: AccountRepository =
        EntryPoints.get(userComponent, UserComponentEntryPoint::class.java)
            .accountRepository()

    val bookingRepo: BookingRepository =
        EntryPoints.get(userComponent, UserComponentEntryPoint::class.java)
            .bookingRepository()

    val jobPoolBidRepo: JobPoolBidRepository =
        EntryPoints.get(userComponent, UserComponentEntryPoint::class.java)
            .jobPoolBidRepository()

    val paymentRepo: PaymentRepository =
        EntryPoints.get(userComponent, UserComponentEntryPoint::class.java)
            .paymentRepository()

    val sumUpRepo: SumUpRepository =
        EntryPoints.get(userComponent, UserComponentEntryPoint::class.java)
            .sumUpRepository()

    val googleRepo: GoogleRepository =
        EntryPoints.get(userComponent, UserComponentEntryPoint::class.java)
            .googleRepository()

    val sharedLocationManager: SharedLocationManager =
        EntryPoints.get(userComponent, UserComponentEntryPoint::class.java)
            .sharedLocationManger()

    val chatRepo: ChatRepository =
        EntryPoints.get(userComponent, UserComponentEntryPoint::class.java)
            .chatRepository()

    val socketManager: SocketManager =
        EntryPoints.get(userComponent, UserComponentEntryPoint::class.java)
            .socketManager()

    val bookingOfferManager: BookingOfferManager =
        EntryPoints.get(userComponent, UserComponentEntryPoint::class.java)
            .bookingOfferManager()

    private var lastAuthUser: UserAuth = userManager.userAuth.value

    init {
        MainScope().launch {
            userManager.userAuth.collectLatest { userAuth ->
                Timber.w("User auth status > $userAuth".uppercase())
                if (lastAuthUser == userAuth) return@collectLatest
                // Rebuild AuthUserComponent if current user is changed.
                lastAuthUser = userAuth
                rebuildComponent()
            }
        }
    }

    private suspend fun rebuildComponent() {
        Timber.w("Rebuilding AuthComponent...".uppercase())
        userComponent = userComponentProvider.get().build()
        _versionState.emit(ComponentVersion.next())
    }

    override fun generatedComponent(): UserComponent = userComponent
}