package com.cab9.driver.ui.home

import android.Manifest
import android.app.PictureInPictureParams
import android.content.*
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.animation.LinearInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.trackPipAnimationHintView
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.annotation.RawRes
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.BuildConfig
import com.cab9.driver.R
import com.cab9.driver.base.BaseActivity
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.HeatMapLatLng
import com.cab9.driver.data.repos.UserAuth
import com.cab9.driver.databinding.ActivityHomeBinding
import com.cab9.driver.databinding.FragmentWebviewBinding
import com.cab9.driver.di.booking.JobInProgress
import com.cab9.driver.di.booking.JobInProgressManager
import com.cab9.driver.di.booking.MeterChange
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.*
import com.cab9.driver.services.Cab9DriverService
import com.cab9.driver.services.Cab9DriverService.Companion.ACTION_LOCATION_PERMISSION_CHANGED
import com.cab9.driver.services.Cab9DriverService.Companion.ACTION_SHIFT_STARTED
import com.cab9.driver.settings.AppSettings
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.ui.account.Cab9RequiredPermission
import com.cab9.driver.ui.account.Cab9RequiredPermission.*
import com.cab9.driver.ui.booking.bid.JobPoolBidViewModel
import com.cab9.driver.ui.booking.ongoing.WebViewWrapper
import com.cab9.driver.ui.login.LoginActivity
import com.cab9.driver.ui.messages.PersistentMessageBottomDialogFragment
import com.cab9.driver.utils.*
import com.cab9.driver.utils.map.HEATMAP_ZOOM_LEVEL
import com.cab9.driver.utils.map.LOCATION_PERMISSIONS
import com.cab9.driver.utils.map.LatLngInterpolator
import com.cab9.driver.utils.map.MarkerAnimator
import com.cab9.driver.utils.map.NAVIGATION_ZOOM
import com.cab9.driver.utils.map.getMarkerIcon
import com.cab9.driver.utils.map.hasLocationPermissions
import com.cab9.driver.widgets.dialog.okButton
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.maps.android.heatmaps.HeatmapTileProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject


@AndroidEntryPoint
class HomeActivity : BaseActivity(R.layout.activity_home), OnMapReadyCallback,
    OnCameraMoveListener, NavController.OnDestinationChangedListener, HomeView {

    companion object {
        private const val TAG_MESSAGE_DIALOG = "tag_message_dialog"

        @JvmStatic
        fun start(context: Context, bundle: Bundle? = null) {
            Timber.d("Starting HomeActivity with bundle -> $bundle")
            context.startActivity(Intent(context, HomeActivity::class.java)
                .apply { bundle?.let(::putExtras) })
        }
    }

    private val binding by viewBinding(ActivityHomeBinding::bind)
    private val viewModel by viewModels<HomeViewModel>()
    private val jobBidViewModel by viewModels<JobPoolBidViewModel>()
    private val appConfigViewModel by viewModels<AppConfigViewModel>()

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var jobInProgressManager: JobInProgressManager

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var connectivityLiveData: ConnectivityLiveData

    @Inject
    lateinit var appUpdateManager: AppUpdateManager

    @Inject
    lateinit var inAppNotificationPlayer: InAppNotificationPlayer

    @Inject
    lateinit var userComponentManager: UserComponentManager

    private val sharedLocationManager: SharedLocationManager
        get() = userComponentManager.sharedLocationManager

    private val isInPictureInPictureState: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode

    private val noToolbarScreenIds = setOf(
        R.id.nav_home,
        R.id.nav_upcoming_bookings,
        R.id.nav_dashboard,
        R.id.nav_job_pool_bids,
        R.id.nav_chat,
        R.id.cab9GoWebFragment
    )

    private val bottomNavScreenIds = setOf(
        R.id.nav_home,
        R.id.nav_upcoming_bookings,
        R.id.nav_dashboard,
        R.id.nav_job_pool_bids,
        R.id.nav_chat
    )

    private val notificationNavIds =
        setOf(R.id.nav_chat, R.id.nav_job_pool_bids, R.id.nav_upcoming_bookings)

    private val mapScreenIds = setOf(R.id.nav_home)

    private var _cab9GoWebViewWrapper: WebViewWrapper? = null

    override val cab9GoWebViewWrapper: WebViewWrapper?
        get() = _cab9GoWebViewWrapper

    private var _dashboardWebBinding: FragmentWebviewBinding? = null
    private var _chatWebBinding: FragmentWebviewBinding? = null

    override val chatWebBinding: FragmentWebviewBinding?
        get() = _chatWebBinding

    override val dashboardWebBinding: FragmentWebviewBinding?
        get() = _dashboardWebBinding

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private var isShowingMap = true
    private var supportMapFragment: SupportMapFragment? = null

    private var googleMap: GoogleMap? = null
    private var carMarker: Marker? = null

    private var heatMapTileOverlay: TileOverlay? = null
    private var heatMapTileProvider: HeatmapTileProvider? = null

    private var actionSnackBar: Snackbar? = null

    private var isZoomingOutForHeatmap = false
    private var isInAppMessageShowing = false
    private var isMobileStateOutOfSync = false

    private var isAppUpdateListenerRegistered: Boolean = false
    private var installUpdateStateListener: InstallStateUpdatedListener? = null
    private var inAppMessageVisibilityJob: Job? = null

    private val latLngInterpolator = LatLngInterpolator.LinearFixed()
    private val linearInterpolator = LinearInterpolator()
    private var markerAnimator: MarkerAnimator? = null

    private val missingPermissions: Stack<Cab9RequiredPermission> = Stack()

    private val isCab9GoWebViewShowing: Boolean
        get() = navController.currentDestination?.id == R.id.cab9GoWebFragment

    override val isHeatmapEnabled: Boolean
        get() = heatMapTileOverlay != null && heatMapTileProvider != null

    private val notificationIntentFilter: IntentFilter
        get() = IntentFilter().apply {
            addAction(TYPE_SHIFT_TIMEOUT_ALERT)
            addAction(TYPE_SHIFT_CLOSED_ALERT)
            addAction(TYPE_BOOKING_ALLOCATED)
            addAction(TYPE_BOOKING_UNALLOCATED)
            addAction(TYPE_BOOKING_CANCELLED)
            addAction(TYPE_NEW_BIDDING)
            addAction(TYPE_BOOKING_CHANGED)
            addAction(TYPE_CANCEL_BIDDING)
            addAction(TYPE_DRIVER_FORCED_BREAK)
            addAction(TYPE_NEW_CHAT_MESSAGE)
            addAction(TYPE_DRIVER_DOCUMENT_EXPIRATION)
            addAction(TYPE_UNKNOWN_NOTIFICATION)
        }

    private val requestLocationPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.all { it.value }) {
                // Send new location status to service
//                startForegroundServiceCompat(
//                    Intent(this, Cab9DriverService::class.java)
//                        .apply { action = ACTION_LOCATION_PERMISSION_CHANGED })
                startCab9DriverService(this, ACTION_LOCATION_PERMISSION_CHANGED)
                updateCurrentLocationWithPermissionCheck()
            }
            askRequiredPermissions()
        }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { askRequiredPermissions() }

    private val batteryOptimization =
        registerForActivityResult(BatteryOptimization()) { askRequiredPermissions() }

    private val notificationSettings =
        registerForActivityResult(NotificationSettings()) { askRequiredPermissions() }

    private val systemDrawOverlay =
        registerForActivityResult(SystemOverlay()) { askRequiredPermissions() }

    private val autoDateTimeSettings =
        registerForActivityResult(AutoDateTimeSettings()) { askRequiredPermissions() }

    private val deviceGPSSettings = registerForActivityResult(DeviceGPSSettings()) {
        updateCurrentLocationWithPermissionCheck()
        askRequiredPermissions()
    }

    private val appUpdateResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            Timber.d("App update flow request code: ${result.resultCode}")
        }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, data: Intent?) {
            Timber.d("Notification action received -> ${data?.action}")
            // No need to update state fro cancelled bids
            if (data?.action != TYPE_CANCEL_BIDDING) viewModel.fetchMobileState()
            when (data?.action) {
                TYPE_SHIFT_TIMEOUT_ALERT,
                TYPE_SHIFT_CLOSED_ALERT,
                TYPE_DRIVER_FORCED_BREAK -> handleShiftRelatedNotification(data)

                TYPE_BOOKING_ALLOCATED,
                TYPE_BOOKING_UNALLOCATED -> handleBookingAllocationNotification(data)

                TYPE_BOOKING_CANCELLED -> handleBookingCancelledNotification(data)
                TYPE_NEW_CHAT_MESSAGE -> handleIncomingChatNotification(data)
                TYPE_NEW_BIDDING, TYPE_CANCEL_BIDDING -> handleBiddingNotification(data)
                else -> handleUnknownNotification(data)
            }
        }
    }

    private val inAppIntentFilter: IntentFilter
        get() = IntentFilter().apply {
            addAction(ACTION_REFRESH_UPCOMING)
            addAction(ACTION_REFRESH_MOBILE_STATE)
            addAction(ACTION_TEST_BOOKING_OFFER_EVENT)
        }

    private val inAppBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, data: Intent?) {
            when (data?.action) {
                ACTION_REFRESH_UPCOMING -> viewModel.getUpcomingBookings()
                ACTION_REFRESH_MOBILE_STATE -> viewModel.fetchMobileState()
                ACTION_TEST_BOOKING_OFFER_EVENT -> {
                    showInAppNotification(
                        title = null,
                        message = getString(R.string.msg_set_for_notification)
                    )
                }
            }
        }
    }

    private val internetConnectionObserver = Observer<Boolean> { isConnected ->
        Timber.w("Internet connected: $isConnected")
        if (!isConnected) isMobileStateOutOfSync = true
        // Refresh mobile state when internet is back
        if (isMobileStateOutOfSync && isConnected) {
            isMobileStateOutOfSync = false
            viewModel.fetchMobileState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setSupportActionBar(binding.homeToolbar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContent) { view, windowInsets ->
            val statusInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomInset = if (windowInsets.isVisible(WindowInsetsCompat.Type.ime()))
                windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            else windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updatePadding(top = statusInsets.top, bottom = bottomInset)
            WindowInsetsCompat.CONSUMED
        }

        // Since Android 14, starting any foreground permission of type location need permissions,
        // to accepted before starting a service
        if (hasLocationPermissions(this)) {
            startCab9DriverService(this)
        }

        // Inflate google map fragment as base fragment
        supportMapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment_view) as? SupportMapFragment
                ?: throw RuntimeException("Embedded map fragment not found!")
        supportMapFragment?.getMapAsync(this)

        // Inflate fragment graph controller
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_upcoming_bookings,
                R.id.nav_dashboard,
                R.id.nav_job_pool_bids,
                R.id.nav_chat
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener(this)

        lifecycleScope.launch {
            launch {
                // Observe login state changes
                viewModel.userAuth.collectLatest {
                    if (it is UserAuth.Unauthenticated) gotoLoginScreen()
                }
            }

            launch {
                // Observe mobile state changes
                viewModel.mobileStateFlow.collectLatest {
                    if (it is Outcome.Success) {
                        if (it.data.isDriverOnline) {
                            startCab9DriverService(this@HomeActivity, ACTION_SHIFT_STARTED)
                        }
                        isMobileStateOutOfSync = false
                    } else if (it is Outcome.Failure) {
                        isMobileStateOutOfSync = true
                    }
                }
            }

            launch {
                viewModel.jobInProgressFlow.collectLatest {
                    // Check if mobile state holds any current booking
                    if (it is JobInProgress.Unassigned) popCab9GoWebWithCheck()
                }
            }

            launch {
                jobInProgressManager.meterChangeFlow.collectLatest {
                    it?.let(::onMeterChange)
                }
            }

            launch {
                // Observe one time fetch current location change
                viewModel.currentLocationOutcome.collectLatest {
                    if (it is Outcome.Success && it.data != null) updateCarMarkerOnMap(it.data)
                }
            }

            launch {
                viewModel.upcomingBookingsOutcome.collectLatest {
                    if (it is Outcome.Success
                        // Update coming tab icon count if not currently selected
                        && navController.currentDestination?.id != R.id.nav_upcoming_bookings
                    ) updateBottomBadgeNavCount(R.id.nav_upcoming_bookings, it.data.size)
                }
            }

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedLocationManager.locationUpdatesFlow
                        //.debounce(800)
                        .collectLatest {
                            it?.let(::onNewLocation)
                        }
                }
                if (hasPictureInPictureFeature) {
                    // Use trackPipAnimationHint view to make a smooth enter/exit pip transition.
                    launch { trackPipAnimationHintView(binding.llRunningMeter) }
                }
                // Look for app update only for production build
                if (!BuildConfig.DEBUG) {
                    launch { startAppUpdateTaskIfNecessary() }
                }

                // Open notification tap screen only if app is STARTED state
                launch {
                    viewModel.openScreenId.collectLatest { id ->
                        if (notificationNavIds.contains(id)) {
                            binding.bottomNavigation.selectItemWithId(id)
                            viewModel.openScreen(0)
                        }
                    }
                }

                // Open booking screen only if app is STARTED state
                launch {
                    viewModel.acceptedBookingId.collectLatest { bookingId ->
                        if (!bookingId.isNullOrEmpty()) {
                            popCab9GoWebWithCheck()
                            navController.navigate(
                                R.id.cab9GoWebFragment,
                                bundleOf(BUNDLE_KEY_BOOKING_ID to bookingId)
                            )
                            viewModel.openBookingScreen(null)
                        }
                    }
                }
            }
        }

        // Monitor Internet connection
        connectivityLiveData.observe(this, internetConnectionObserver)

        intent?.let(::handleIntentData)
        // Register broadcast to listen booking offer triggers
        localBroadcastManager.registerReceiver(inAppBroadcastReceiver, inAppIntentFilter)
        // create all necessary notification channels
        createBookingRelatedNotificationChannels()

        if (intent.extras?.containsKey(BUNDLE_KEY_PERMISSION_SHOWN) != true) {
            Timber.d("Permission screen is skipped, check for missing permissions...")
            startMissingPermissionFlow()
        }
    }

    override fun onResume() {
        super.onResume()
        // Few clients complained about job disappearing after accept, to make sure it appears
        // as banner in home screen, we update the mobile state from here
        viewModel.fetchMobileState()
        // Update count on each tabs
        updateBottomNavViewBadgeCount()
        // Check if mobile state needs update
        verifyCancelledOrUnallocatedBookings()
        // Show persistent dialog only if app is in RESUMED state
        showPersistentMessageDialog()
    }

    override fun onCreateCab9GoBinding(binding: FragmentWebviewBinding) {
        Timber.d("Initializing cab9go web binding...")
        destroyCab9WebBinding()
        WebViewWrapper(binding).also { _cab9GoWebViewWrapper = it }
    }

    override fun onCreateChatBinding(binding: FragmentWebviewBinding) {
        Timber.d("Initializing chat web view...")
        _chatWebBinding = binding
    }

    override fun onCreateDashboardBinding(binding: FragmentWebviewBinding) {
        Timber.d("Initializing dashboard web view...")
        _dashboardWebBinding = binding
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.map {
            it.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNewIntent(data: Intent?) {
        super.onNewIntent(data)
        data?.let(::handleIntentData)
    }

    private fun handleIntentData(data: Intent) {
        data.extras?.let { bundle ->
            Timber.d("New Intent received -> ${data.extras}")
            if (bundle.containsKey(EXTRA_ACCEPTED_OFFER_BOOKING_ID)) {
                // New booking offer accepted, open ongoing job screen
                viewModel.openBookingScreen(bundle.getString(EXTRA_ACCEPTED_OFFER_BOOKING_ID))
            } else if (bundle.containsKey(NOTIFICATION_TYPE)) {
                when (bundle.getString(NOTIFICATION_TYPE)) {
                    TYPE_NEW_CHAT_MESSAGE -> openChatScreen()
                    TYPE_NEW_BIDDING -> {
                        jobBidViewModel.refreshIncomingBids()
                        viewModel.openScreen(R.id.nav_job_pool_bids)
                    }

                    TYPE_BOOKING_ALLOCATED -> {
                        viewModel.openScreen(R.id.nav_upcoming_bookings)
                        viewModel.getUpcomingBookings()
                    }
                }
            }
        }
        // Remove all extras after consumed
        intent.replaceExtras(Bundle())
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        localBroadcastManager.registerReceiver(notificationReceiver, notificationIntentFilter)
    }

    override fun onStop() {
        // Remove app install listener
        unregisterInstallUpdateListener()
        toggleMessageVisibility(false)
        localBroadcastManager.unregisterReceiver(notificationReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        inAppMessageVisibilityJob?.cancel()
        supportFragmentManager.removeSafely(TAG_MESSAGE_DIALOG)
        localBroadcastManager.unregisterReceiver(inAppBroadcastReceiver)
        destroyWebBindings()
        super.onDestroy()
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        // Avoid menu item selected crash when graph is not set
        if (this::navController.isInitialized && navController.currentDestination?.parent == null) return
        // Show toolbar nav on selected screens
        if (noToolbarScreenIds.contains(destination.id)) supportActionBar?.hide() else supportActionBar?.show()
        // Show bottom nav on selected screens
        binding.bottomNavigation.visibility(bottomNavScreenIds.contains(destination.id))
        // Show map only on home screen
        if (mapScreenIds.contains(destination.id)) showMapFragment() else hideMapFragment()
        // Clear badge on selection of particular tab
        binding.bottomNavigation.clearBadge(destination.id)
        // Remove badge count for selected nav item
        if (destination.id == R.id.nav_chat) sessionManager.resetChatMessageCount()
        if (destination.id == R.id.nav_job_pool_bids) sessionManager.isJobPoolUpdated = false
        //if (destination.id == R.id.nav_upcoming_bookings) sessionManager.resetUpcomingBookingCount()
        // Refresh mobile state whenever Cab9Go is opened
        if (destination.id == R.id.cab9GoWebFragment) viewModel.fetchMobileState()
        if (destination.id == R.id.nav_chat) onChatScreenSelected()
    }

    private fun onChatScreenSelected() {
        Timber.d("Chat screen selected, post visibility message...")
        chatWebBinding?.webView?.postWebMessageCompat(
            JSONObject(
                mapOf(
                    "type" to "visibilityChange",
                    "data" to mapOf("visible" to true)
                )
            )
        )
    }

    override fun showBottomNavSnack(message: String) {
        if (binding.bottomNavigation.isVisible) {
            binding.bottomNavigation.anchorSnack(message)
        } else binding.root.snack(message)
    }

    override fun onHeatmapLoaded(data: List<HeatMapLatLng>) {
        googleMap?.let { map ->
            val latLngs = data
                .filter { it.lat != null && it.lng != null }
                .map { LatLng(it.lat!!, it.lng!!) }

            if (latLngs.isEmpty()) {
                showBottomNavSnack(getString(R.string.err_no_data))
                return
            }

            if (heatMapTileOverlay == null) {
                // Create a heat map tile provider
                heatMapTileProvider = HeatmapTileProvider.Builder() //.radius(HEAT_MAP_RADIUS)
                    .data(latLngs)
                    .maxIntensity(8.0)
                    .build()
                // Set radius here instead of builder pattern
                heatMapTileProvider?.setRadius(100)
            } else {
                // Set heat map tile data
                heatMapTileProvider!!.setData(latLngs)
            }

            if (heatMapTileOverlay == null) {
                // Add a tile overlay to the map, using the heat map tile provider.
                val options = TileOverlayOptions()
                    .fadeIn(true)
                    .tileProvider(heatMapTileProvider!!)
                heatMapTileOverlay = map.addTileOverlay(options)
            } else heatMapTileOverlay?.clearTileCache()

            if (map.cameraPosition.zoom > HEATMAP_ZOOM_LEVEL) {
                // Zoom out map to appropriate heatmap level
                viewModel.currentLocation?.toLatLng()?.let {
                    isZoomingOutForHeatmap = true
                    runOnUiThread {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, HEATMAP_ZOOM_LEVEL))
                    }
                }
            }
        }
    }

    override fun onRemoveHeatmap() {
        heatMapTileOverlay?.remove()
        heatMapTileOverlay = null
        heatMapTileProvider = null
        updateCurrentLocationWithPermissionCheck()
    }

    override fun openChatScreen() {
        viewModel.openScreen(R.id.nav_chat)
    }

    private fun showMapFragment() {
        if (!isShowingMap) {
            supportMapFragment?.let { fragment ->
                supportFragmentManager.commit { show(fragment) }
                isShowingMap = true
            }
        }
    }

    private fun hideMapFragment() {
        if (isShowingMap) {
            supportMapFragment?.let { fragment ->
                supportFragmentManager.commit { hide(fragment) }
                isShowingMap = false
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map.apply {
            isBuildingsEnabled = false
            isIndoorEnabled = false
            isTrafficEnabled = false
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isCompassEnabled = false
            setOnCameraMoveListener(this@HomeActivity)
        }

        applyDarkThemeIfRequired()
        updateCurrentLocationWithPermissionCheck()
    }

    override fun onCameraMove() {
        googleMap?.let { map ->
            if (isHeatmapEnabled) {
                val newZoom = map.cameraPosition.zoom
                // ignore initial zoom in to avoid unwanted toast message
                if (isZoomingOutForHeatmap) {
                    if (newZoom > HEATMAP_ZOOM_LEVEL) return
                    else isZoomingOutForHeatmap = false
                }
                if (newZoom > HEATMAP_ZOOM_LEVEL) {
                    showHeatmapZoomWarningMessage()
                    heatMapTileOverlay?.hide()
                } else heatMapTileOverlay?.show()
            }
        }
    }

    private fun showHeatmapZoomWarningMessage() {
        if (!isInAppMessageShowing) {
            showInAppNotification(
                title = null,
                message = getString(R.string.msg_heatmap_zoom),
                playSound = false
            )
        }
    }

    private fun applyDarkThemeIfRequired() {
        if (isDarkThemeActive) {
            try {
                googleMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))
            } catch (ex: Resources.NotFoundException) {
                // ignore
            }
        }
    }

    override fun getCurrentLocationWithPermissionCheck() {
        if (hasLocationPermissions(this)) viewModel.getCurrentLocation()
        else requestLocationPermissions.launch(LOCATION_PERMISSIONS)
    }

    private fun updateCurrentLocationWithPermissionCheck() {
        when {
            !hasLocationPermissions(this) -> showLocationRequiredDialog()
            !isGPSEnabled() -> showGPSSettingsDialog()
            else -> {
                try {
                    googleMap?.isMyLocationEnabled = false
                    viewModel.getCurrentLocation()
                } catch (e: SecurityException) {
                    Timber.e(e)
                }
            }
        }
    }

    private fun onNewLocation(location: Location) {
        updateCarMarkerOnMap(location)
        // Update new location on Cab9 webView
        cab9GoWebViewWrapper?.binding?.webView?.let {
            jobInProgressManager.postLocationUpdate(it, location)
        }
        // Update flag down manager to check if current location is inside enable zone
        appConfigViewModel.onNewLocation(location)
    }

    private fun updateCarMarkerOnMap(location: Location) {
        // do not animate map camera when heatmap is active
        if (isHeatmapEnabled) return

        // Update car marker on map
        if (carMarker != null) animateCarMarkerMovement(location)
        else {
            googleMap?.addMarker(
                MarkerOptions()
                    .position(location.toLatLng())
                    .anchor(0.5F, 0.5F)
                    .rotation(location.bearing)
                    .flat(true)
                    .icon(getMarkerIcon(applicationContext))
            )?.also { carMarker = it }
        }

        // Zoom into location
        val builder = CameraPosition.builder()
            .zoom(NAVIGATION_ZOOM)
            .bearing(location.bearing)
            .target(location.toLatLng())
        runOnUiThread {
            googleMap?.animateCamera(
                CameraUpdateFactory.newCameraPosition(builder.build()),
                1000,
                null
            )
        }
    }

    private fun animateCarMarkerMovement(newLocation: Location) {
        carMarker?.let { marker ->
            val startPos = marker.position
            val endPos = newLocation.toLatLng()
            val startRotation = marker.rotation
            MarkerAnimator(
                marker,
                latLngInterpolator,
                linearInterpolator,
                startPos,
                endPos,
                startRotation,
                newLocation
            )
                .also { markerAnimator = it }
                .start()
        }
    }

    private fun handleShiftRelatedNotification(data: Intent) {
        // Get notification title & message from bundle
        val dialogTitle = data.getStringExtra(NOTIFICATION_TITLE)
        val dialogMsg = data.getStringExtra(NOTIFICATION_BODY)
        if (dialogTitle.isNullOrEmpty() && dialogMsg.isNullOrEmpty()) return
        else {
            inAppNotificationPlayer.play()
            showMaterialAlert {
                isCancelable = false
                when {
                    !dialogMsg.isNullOrEmpty() && !dialogTitle.isNullOrEmpty() -> {
                        title = dialogTitle
                        message = dialogMsg
                    }

                    !dialogTitle.isNullOrEmpty() -> message = dialogTitle
                    !dialogMsg.isNullOrEmpty() -> message = dialogMsg
                }
                okButton { it.dismiss() }
            }
        }
    }

    private fun handleBookingAllocationNotification(data: Intent) {
        // Check if upcoming screen is in front
        val dialogTitle = data.getStringExtra(NOTIFICATION_TITLE)
        val dialogMsg = data.getStringExtra(NOTIFICATION_BODY)
        if (navController.currentDestination?.id == R.id.nav_upcoming_bookings) {
            showActionSnackBar(dialogTitle.orEmpty().ifEmpty { dialogMsg.orEmpty() }) {
                viewModel.getUpcomingBookings()
            }
        } else {
            // User in other screen, refresh upcoming now
            viewModel.getUpcomingBookings()
            // Hide in-app notification if booking detail screen is open
            // This type of notification is handled in detail screen
            if (navController.currentDestination?.id != R.id.bookingDetailFragment) {
                showInAppNotification(dialogTitle, dialogMsg.orEmpty())
            }
        }
    }

    private fun handleBookingCancelledNotification(data: Intent) {
        viewModel.getUpcomingBookings()
        val dialogTitle = data.getStringExtra(NOTIFICATION_TITLE)
        val dialogMsg = data.getStringExtra(NOTIFICATION_BODY)
        showInAppNotification(
            dialogTitle,
            dialogMsg.orEmpty(),
            bgColor = R.color.bg_color_cancellation,
            toneResId = R.raw.tone_cancel_booking
        )
    }

    /**
     * Remove Cab9 WebView fragment from navigation stack
     */
    private fun popCab9GoWebWithCheck() {
        if (navController.isOnBackStack(R.id.cab9GoWebFragment)) {
            Timber.w("Popping cab9go web from back stack".uppercase())
            navController.popBackStack(R.id.cab9GoWebFragment, true)
        }
        destroyCab9WebBinding()
    }

    override fun destroyCab9WebBinding() {
        _cab9GoWebViewWrapper?.destroy()
        _cab9GoWebViewWrapper = null
    }

    private fun destroyWebBindings() {
        Timber.w("Destroying all webViews".uppercase())
        destroyCab9WebBinding()
        _dashboardWebBinding?.webView?.destroy()
        _dashboardWebBinding = null
        _chatWebBinding?.webView?.destroy()
        _chatWebBinding = null
    }

    /**
     * Handles all incoming chat related messages.
     */
    private fun handleIncomingChatNotification(data: Intent) {
        if (navController.currentDestination?.id != R.id.nav_chat) {
            // update chat message count
            sessionManager.incrementMessageCount()
            // get messages from bundle to show
            val title = data.getStringExtra(NOTIFICATION_TITLE)
            val message = data.getStringExtra(NOTIFICATION_BODY)
            val payload = data.getStringExtra(NOTIFICATION_PAYLOAD)
            // Show persistent dialog based on type
            if (!message.isNullOrEmpty()
                && NotificationUtils.isPersistentMessage(payload)
            ) {
                sessionManager.saveNewPersistentMessage(message)
                showPersistentMessageDialog()
            } else {
                updateBottomBadgeNavCount(R.id.nav_chat, sessionManager.unreadChatMessageCount)
                showInAppNotification(title, message.orEmpty())
            }
        }
    }

    private fun handleBiddingNotification(data: Intent) {
        if (data.action == TYPE_NEW_BIDDING) {
            // Show in-app notification
            val notificationTitle = data.getStringExtra(NOTIFICATION_TITLE)
            val notificationMsg = data.getStringExtra(NOTIFICATION_BODY)
                .orEmpty().ifEmpty { getString(R.string.msg_new_bid_available) }
            showInAppNotification(
                title = notificationTitle,
                message = notificationMsg,
                toneResId = R.raw.tone_new_bid
            )
        }
        // Show new updates available icon on bottom view if job pool screen is not in front
        binding.bottomNavigation.getOrCreateBadge(R.id.nav_job_pool_bids).isVisible =
            navController.currentDestination?.id != R.id.nav_job_pool_bids
        // Refresh list
        jobBidViewModel.refreshIncomingBids()
    }

    private fun handleUnknownNotification(data: Intent?) {
        val title = data?.getStringExtra(NOTIFICATION_TITLE)
        val message = data?.getStringExtra(NOTIFICATION_BODY)
        if (title.isNullOrEmpty() && message.isNullOrEmpty()) return
        showInAppNotification(title, message.orEmpty())
    }

    private fun showInAppNotification(
        title: String?,
        message: String,
        playSound: Boolean = true,
        @RawRes toneResId: Int = R.raw.tone_app_alert,
        @ColorRes bgColor: Int = R.color.brand_color_scim
    ) {
        // Avoid showing empty messages
        if (title.isNullOrEmpty() && message.isEmpty()) return
        // Avoid showing messages when PictureInPicture mode
        if (isInPictureInPictureState) {
            showNotification(
                applicationContext,
                title.orEmpty().ifEmpty { getString(R.string.msg_new_notification) },
                message
            )
            return
        }
        // Assign respective data to views
        binding.lblAppNotificationTitle.visibility(!title.isNullOrEmpty())
        binding.lblAppNotificationTitle.text = title
        binding.lblAppNotificationMsg.text = message
        // Change color if required
        binding.llInAppMessageContainer.setBackgroundColor(colorInt(bgColor))
        // Play sound and show message
        if (playSound) inAppNotificationPlayer.play(toneResId)
        // Show message view
        toggleMessageVisibility(true)
        // Hide after 3 secs
        inAppMessageVisibilityJob = lifecycleScope.launch {
            delay(IN_APP_NOTIFICATION_TIMEOUT)
            toggleMessageVisibility(false)
        }
    }

    private fun toggleMessageVisibility(show: Boolean) {
        isInAppMessageShowing = show
        binding.llInAppMessageContainer.visibility(show)
    }

    private fun showPersistentMessageDialog() {
        if (sessionManager.savedPersistentMessages.isNotEmpty()
            // Do not show persistent message dialog when app in PictureInPicture mode
            && !isInPictureInPictureState
        ) {
            inAppNotificationPlayer.play()
            supportFragmentManager.showDialogSafely(
                PersistentMessageBottomDialogFragment.newInstance(),
                TAG_MESSAGE_DIALOG
            )
        }
    }

    private fun showActionSnackBar(message: String, block: () -> Unit) {
        //playNotificationSound()
        inAppNotificationPlayer.play()
        // Remove previous snack bar if showing
        actionSnackBar?.dismiss()
        // Show new snack bar
        Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
            .setAnchorView(binding.bottomNavigation)
            .setAction(R.string.action_refresh) {
                block.invoke()
                actionSnackBar?.dismiss()
            }
            .also { actionSnackBar = it }
            .show()
    }

    private fun updateBottomNavViewBadgeCount() {
        // Update any change in job pool bookings
        binding.bottomNavigation.getOrCreateBadge(R.id.nav_job_pool_bids).isVisible =
            sessionManager.isJobPoolUpdated
        // Update upcoming booking count
        if (sessionManager.unreadChatMessageCount > 0)
            updateBottomBadgeNavCount(R.id.nav_chat, sessionManager.unreadChatMessageCount)

    }

    private fun updateBottomBadgeNavCount(@IdRes navId: Int, count: Int) {
        if (count > 0) {
            binding.bottomNavigation.getOrCreateBadge(navId)
                .apply {
                    number = count
                    isVisible = true
                }
        } else {
            binding.bottomNavigation.getBadge(navId)?.isVisible = false
            binding.bottomNavigation.getBadge(navId)?.clearNumber()
        }
    }

    override fun startMissingPermissionFlow() {
        val permissions = Cab9RequiredPermission.mandatoryPermissions()
            .filter { !it.isGranted(this) }.reversed()
        missingPermissions.clear()
        missingPermissions.addAll(permissions)
        Timber.d("${permissions.size} missing permissions found...")
        askRequiredPermissions()
    }

    private fun askRequiredPermissions() {
        if (missingPermissions.isEmpty()) return
        else {
            val permission = missingPermissions.pop() ?: return
            Timber.d("Ask $permission now")
            when (permission) {
                // Required
                LOCATION_PERMISSION -> showLocationRequiredDialog()
                // Required
                GPS -> showGPSSettingsDialog()
                // Required
                AUTO_DATE_TIME -> {
                    showRequestPermissionRationaleDialog(
                        getString(R.string.title_date_mismatch),
                        getString(R.string.msg_time_zone_mismatch),
                        denyAction = { askRequiredPermissions() },
                        okAction = { autoDateTimeSettings.launch() }
                    )
                }
                // Required
                NOTIFICATION -> {
                    if (!hasNotificationPermission(this)) {
                        showRequestPermissionRationaleDialog(
                            getString(R.string.dialog_title_get_notified),
                            getString(R.string.dialog_msg_notification_off),
                            denyAction = { askRequiredPermissions() },
                            okAction = { requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        )
                    } else if (!areNotificationsEnabled()) {
                        showRequestPermissionRationaleDialog(
                            getString(R.string.dialog_title_get_notified),
                            getString(R.string.dialog_msg_notification_off),
                            denyAction = { askRequiredPermissions() },
                            okAction = { notificationSettings.launch() }
                        )
                    }
                }
                // Optional
                OVERLAY -> {
                    showRequestPermissionRationaleDialog(
                        getString(R.string.dialog_title_draw_over),
                        getString(
                            R.string.temp_dialog_msg_draw_over,
                            getString(R.string.app_name)
                        ),
                        denyAction = { askRequiredPermissions() },
                        okAction = { systemDrawOverlay.launch() }
                    )
                }
                // Optional
                RECORD_AUDIO -> {
                    showRequestPermissionRationaleDialog(
                        getString(R.string.dialog_title_use_speech),
                        getString(R.string.dialog_msg_use_speech, getString(R.string.app_name)),
                        denyAction = { askRequiredPermissions() },
                        okAction = { requestPermission.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                }
                // Optional
                BATTERY -> {
                    showRequestPermissionRationaleDialog(
                        getString(R.string.dialog_title_battery_optimize),
                        getString(
                            R.string.temp_dialog_msg_battery_optimize,
                            getString(R.string.app_name)
                        ),
                        denyAction = { askRequiredPermissions() },
                        okAction = { batteryOptimization.launch() }
                    )
                }
            }
        }
    }

    private fun showRequestPermissionRationaleDialog(
        dialogTitle: String,
        dialogMessage: String,
        denyAction: (() -> Unit)? = null,
        okAction: () -> Unit,
    ) {
        showMaterialAlert {
            title = dialogTitle
            message = dialogMessage
            isCancelable = false
            okButton {
                okAction.invoke()
                it.dismiss()
            }
            if (denyAction != null) {
                negativeButton(R.string.action_deny) {
                    denyAction.invoke()
                    it.dismiss()
                }
            }
        }
    }

    /**
     * To show message regarding missing location permissions.
     */
    private fun showLocationRequiredDialog() {
        showRequestPermissionRationaleDialog(
            getString(R.string.dialog_title_location_access),
            getString(R.string.temp_dialog_msg_location_data, getString(R.string.app_name)),
        ) {
            requestLocationPermissions.launch(LOCATION_PERMISSIONS)
        }
    }

    /**
     * To show message regarding disabled GPS settings.
     */
    private fun showGPSSettingsDialog() {
        showRequestPermissionRationaleDialog(
            getString(R.string.dialog_title_gps_request),
            getString(R.string.dialog_msg_gps_request, getString(R.string.app_name))
        ) { deviceGPSSettings.launch() }
    }

    /**
     * Use to create all necessary notification channels.
     */
    private fun createBookingRelatedNotificationChannels() {
        createNotificationChannel(
            this,
            getString(R.string.booking_offer_channel_id),
            getString(R.string.booking_offer_channel_name),
            getString(R.string.booking_offer_channel_desc),
            getBookingOfferToneUri(applicationContext)
        )

        // Create cancelled booking notification channel with different sound
        createNotificationChannel(
            this,
            getString(R.string.booking_cancel_channel_id),
            getString(R.string.booking_cancel_channel_name),
            getString(R.string.booking_cancel_channel_desc),
            getBookingCancelledToneUri(applicationContext)
        )

        // Create job bid notification channel with different sound
        createNotificationChannel(
            this,
            getString(R.string.new_bidding_channel_id),
            getString(R.string.new_bidding_channel_name),
            getString(R.string.new_bidding_channel_desc),
            getNewBiddingToneUri(applicationContext)
        )
    }

    /**
     * Check if current booking is cancelled or unallocated and update mobile state.
     */
    private fun verifyCancelledOrUnallocatedBookings() {
        if (!sessionManager.recentCancelledBookingId.isNullOrEmpty()) {
            Timber.d("Cancelled/Unallocated booking id found, refresh mobile state now...")
            sessionManager.clearCancelledBookingIds()
            viewModel.getUpcomingBookings()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!userComponentManager.bookingOfferManager.isStarting
            && canEnterPictureInPictureMode
            && isCab9GoWebViewShowing
            && jobInProgressManager.isBookingDetailValid
        ) {
            Timber.d("Entering PictureInPicture mode...".uppercase())
            enterPictureInPictureMode(updatePictureInPictureParams())
            jobInProgressManager.fetchBookingDetail()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        Timber.d("onPictureInPictureModeChanged -> $isInPictureInPictureMode")
        if (isInPictureInPictureMode) {
            binding.llAppContent.gone()
            binding.llRunningMeter.show()
        } else {
            binding.llAppContent.show()
            binding.llRunningMeter.hide()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePictureInPictureParams(): PictureInPictureParams {
        // Calculate the aspect ratio of the PiP screen.
        //val aspectRatio = Rational(binding.mainContainer.width, binding.mainContainer.height)
        // The home screen turns into the picture-in-picture mode.
        //val visibleRect = Rect()
        //binding.mainContainer.getGlobalVisibleRect(visibleRect)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        // Specify the portion of the screen that turns into the picture-in-picture mode.
        // This makes the transition animation smoother.
        //.setSourceRectHint(visibleRect)
        doFromSdk(Build.VERSION_CODES.S) { builder.setSeamlessResizeEnabled(false) }
        val params = builder.build()
        setPictureInPictureParams(params)
        return params
    }

    private fun onMeterChange(meterChange: MeterChange) {
        if (binding.llRunningMeter.isVisible) Timber.d("PictureInPicture is showing")
        else Timber.d("PictureInPicture not showing")
        binding.lblPipActualCost.text = meterChange.meterText
        binding.lblPipActualDistance.text =
            getString(R.string.temp_actual_distance, meterChange.distance)
        if (meterChange.waitingCost.isNotEmpty()) {
            binding.lblPipWaitingCost.show()
            binding.lblPipWaitingCost.text =
                getString(R.string.temp_waiting_cost, meterChange.waitingCost)
        } else binding.lblPipWaitingCost.gone()
    }

    private fun gotoLoginScreen() {
        finishAffinity()
        LoginActivity.start(this@HomeActivity)
    }

    private suspend fun startAppUpdateTaskIfNecessary() {
        try {
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
            Timber.d("Check for app update availability...")
            when {
                appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED -> popupSnackBarForCompleteUpdate()
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    startUpdateFlowForResult(appUpdateInfo)
                }
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private fun startUpdateFlowForResult(appUpdateInfo: AppUpdateInfo) {
        Timber.d("Start app update flow...")
        val listener = InstallStateUpdatedListener { state ->
            // (Optional) Provide a download progress bar.
            if (state.installStatus() == InstallStatus.DOWNLOADING) {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                val downloadedInPercent = (bytesDownloaded / totalBytesToDownload.toFloat()) * 100
                Timber.w("Downloading $downloadedInPercent%")
            } else if (state.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackBarForCompleteUpdate()
            }
        }

        // Before starting an update, register a listener for updates.
        registerInstallUpdateListener(listener)
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            appUpdateResultLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        )
    }

    private fun registerInstallUpdateListener(listener: InstallStateUpdatedListener) {
        if (!isAppUpdateListenerRegistered) {
            Timber.d("Register app update install listener")
            isAppUpdateListenerRegistered = true
            installUpdateStateListener = listener
            appUpdateManager.registerListener(listener)
        }
    }

    private fun unregisterInstallUpdateListener() {
        if (isAppUpdateListenerRegistered) {
            Timber.d("Unregister app update install listener")
            isAppUpdateListenerRegistered = false
            installUpdateStateListener?.let { appUpdateManager.unregisterListener(it) }
            installUpdateStateListener = null
        }
    }

    private fun popupSnackBarForCompleteUpdate() {
        unregisterInstallUpdateListener()
        Timber.d("Ask user to update the app...")
        Snackbar.make(
            binding.root,
            R.string.msg_app_update_downloaded,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAnchorView(binding.bottomNavigation)
            .setAction(R.string.action_restart) { appUpdateManager.completeUpdate() }
            .show()
    }
}