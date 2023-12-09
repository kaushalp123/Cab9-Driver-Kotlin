package com.cab9.driver.base

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.cab9.driver.BuildConfig
import com.cab9.driver.R
import com.cab9.driver.data.repos.UserManager
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.startCab9DriverService
import com.cab9.driver.services.Cab9DriverService.Companion.ACTION_STOP
import com.cab9.driver.settings.AppSettings
import com.cab9.driver.ui.login.LoginActivity
import com.cab9.driver.utils.ReleaseTree
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jakewharton.threetenabp.AndroidThreeTen
import com.sumup.merchant.reader.api.SumUpState
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class Cab9DriverApp : Application(), Application.ActivityLifecycleCallbacks,
    OnMapsSdkInitializedCallback {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var userComponentManager: UserComponentManager

    private val isDriverOffline: Boolean
        get() = userComponentManager.cab9Repo.mobileState?.isDriverOffline == true

    // Holds reference for all activities which entered CREATED state
    private val createdActivities = linkedSetOf<String>()

    // Holds reference for all activities which entered STARTED state
    private val startedActivities = linkedSetOf<String>()

    private val sumUpActivities = linkedSetOf<String>()

    // condition to check whether sum-up app screen is opened or not to show the booking offer screen when received.
    val isSumUpAppOpen: Boolean
        get() = sumUpActivities.isNotEmpty()

    val isForeground: Boolean
        get() = startedActivities.isNotEmpty()

    private var isSumUpInitialised = false

    override fun onCreate() {
        super.onCreate()
        // Initialize Google Maps sdk to use latest changes
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)

        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        else Timber.plant(ReleaseTree())

        val config = if (BuildConfig.DEBUG) {
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
        } else {
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.ERROR)
                .build()
        }

        WorkManager.initialize(this, config)

        AndroidThreeTen.init(this)
        AppCompatDelegate.setDefaultNightMode(appSettings.displayMode)
        registerActivityLifecycleCallbacks(this)

        // Set default values for remote config
        Firebase.remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        MainScope().launch {
            // Listen to mobile state changes and activity SumUp SDK if required
            userComponentManager.cab9Repo.mobileStateAsFlow.collectLatest {
                if (it is Outcome.Success && it.data.isSumUpAvailable && !isSumUpInitialised) {
                    Timber.d("SumUp SDK initialized...".uppercase())
                    SumUpState.init(applicationContext)
                    isSumUpInitialised = true
                }
            }
        }
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        Timber.i("Map initialized with $renderer renderer!")
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        //isBookingOfferStarting = false
        createdActivities.add(activity.javaClass.simpleName)
        Timber.w("Activity Created -> ${activity.javaClass.simpleName}")
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities.add(activity.javaClass.simpleName)
        if (activity.javaClass.name.startsWith("com.sumup.merchant")) sumUpActivities.add(activity.javaClass.simpleName)
        Timber.w("Activity Started -> ${activity.javaClass.simpleName}")
    }

    override fun onActivityResumed(activity: Activity) {
        Timber.w("Activity Resumed -> ${activity.javaClass.simpleName}")
    }

    override fun onActivityPaused(activity: Activity) {
        Timber.w("Activity Paused -> ${activity.javaClass.simpleName}")
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities.remove(activity.javaClass.simpleName)
        sumUpActivities.remove(activity.javaClass.simpleName)
        Timber.w("Activity Stopped -> ${activity.javaClass.simpleName}")
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        Timber.w("Activity Instance Saved -> ${activity.javaClass.simpleName}")
    }

    override fun onActivityDestroyed(activity: Activity) {
        createdActivities.remove(activity.javaClass.simpleName)
        Timber.w("Activity Destroyed -> ${activity.javaClass.simpleName}")

        // Skip checking service status if LoginActivity is being destroyed
        if (activity.javaClass.simpleName == LoginActivity::class.java.simpleName) return

        // Stop service if app is closing and user is offline
        if ((createdActivities.isEmpty() && isDriverOffline) || !userManager.isLoggedIn) {
            // Stop service if the user is logged out
            startCab9DriverService(this, ACTION_STOP)
//            startForegroundServiceCompat(
//                Intent(this, Cab9DriverService::class.java)
//                    .apply { action = ACTION_STOP })
        }
    }
}