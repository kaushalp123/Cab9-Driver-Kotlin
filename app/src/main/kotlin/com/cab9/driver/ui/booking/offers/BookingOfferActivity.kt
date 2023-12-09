package com.cab9.driver.ui.booking.offers

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseActivity
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Booking
import com.cab9.driver.data.models.BookingOfferPayload
import com.cab9.driver.data.models.Driver
import com.cab9.driver.databinding.ActivityBookingOfferNewBinding
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.colorInt
import com.cab9.driver.ext.gone
import com.cab9.driver.ext.isLockscreenEnabled
import com.cab9.driver.ext.localBroadcastManager
import com.cab9.driver.ext.show
import com.cab9.driver.ext.toast
import com.cab9.driver.ext.visibility
import com.cab9.driver.network.BookingOfferException
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.ui.home.HomeActivity
import com.cab9.driver.utils.ACTION_REFRESH_MOBILE_STATE
import com.cab9.driver.utils.ACTION_REFRESH_UPCOMING
import com.cab9.driver.utils.ACTION_TEST_BOOKING_OFFER_EVENT
import com.cab9.driver.utils.BOOKING_OFFER_NOTIFICATION_ID
import com.cab9.driver.utils.EXTRA_ACCEPTED_OFFER_BOOKING_ID
import com.cab9.driver.utils.SpeechAction
import com.cab9.driver.utils.SpeechRecognizerHandler
import com.cab9.driver.widgets.dialog.okButton
import com.squareup.moshi.Moshi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BookingOfferActivity : BaseActivity(R.layout.activity_booking_offer_new),
    BookingOfferScreenClickListener {

    companion object {
        private const val EXTRA_BOOKING_OFFER = "booking_offer"
        private const val EXTRA_BOOKING_OFFER_SOURCE = "booking_offer_source"
        private const val TAG_REJECTION_REASON_DIALOG = "rejection_reason_dialog"

        fun newIntent(context: Context, source: OfferSource, payload: String): Intent =
            Intent(context, BookingOfferActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(EXTRA_BOOKING_OFFER, payload)
                putExtra(EXTRA_BOOKING_OFFER_SOURCE, source.name)
            }
    }

    private val binding by viewBinding(ActivityBookingOfferNewBinding::bind)
    private val viewModel by viewModels<BookingOfferViewModel>()

    @Inject
    lateinit var moshi: Moshi

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var speechRecognizerHandler: SpeechRecognizerHandler

    @Inject
    lateinit var userComponentManager: UserComponentManager

    private var isKeyguardListenerAttached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = colorInt(R.color.bg_offer_timer)
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }

        if (isLockscreenEnabled()) {
            isKeyguardListenerAttached = true
            // TODO - keep a check weather device locked or not before calling this
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // if keypad is open when device is locked , dismiss the keyboard and show offer.
                getSystemService<KeyguardManager>()?.requestDismissKeyguard(this, null)
            } else window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.listener = this

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Disable back button here
            }
        })

        lifecycleScope.launch {
            launch {
                viewModel.timerExpired.collectLatest { isExpired ->
                    if (isExpired) {
                        binding.btnAcceptOffer.isEnabled = false
                        binding.btnRejectOffer.isEnabled = false
                        finishNow(getString(R.string.msg_offer_expired))
                    }
                }
            }
            launch {
                viewModel.acceptBookingOfferOutcome.collectLatest {
                    binding.pBarBookingOffer.visibility(it is Outcome.Progress)
                    when (it) {
                        is Outcome.Progress -> {
                            binding.btnAcceptOffer.isEnabled = false
                            binding.btnRejectOffer.isEnabled = false
                        }

                        is Outcome.Success -> onBookingAcceptedSuccessfully(it.data.second)
                        is Outcome.Failure -> finishNow(it.msg)
                        else -> {}
                    }
                }
            }
            launch {
                viewModel.rejectBookingOfferOutcome.collectLatest {
                    binding.pBarBookingOffer.visibility(it is Outcome.Progress)
                    when (it) {
                        is Outcome.Progress -> {
                            binding.btnAcceptOffer.isEnabled = false
                            binding.btnRejectOffer.isEnabled = false
                        }

                        is Outcome.Success -> {
                            toast(R.string.msg_booking_rejected)
                            sendRefreshBroadcast()
                            finish()
                        }

                        is Outcome.Failure -> {
                            sendRefreshBroadcast()
                            finishNow(it.msg)
                        }

                        else -> {}
                    }
                }
            }
            launch {
                viewModel.bookingDetailOutcome.collectLatest {
                    binding.pBarBookingOffer.visibility(it is Outcome.Progress)
                    if (it is Outcome.Success) onBookingDetailLoaded(it.data)
                    else if (it is Outcome.Failure) finishNow(it.msg)
                }
            }
            launch {
                speechRecognizerHandler.speechListenerUiVisibility.collectLatest { isListening ->
                    binding.llSpeechListener.visibility(isListening)
                }
            }
            launch {
                speechRecognizerHandler.speechListenerAction.collectLatest { action ->
                    when (action) {
                        SpeechAction.ACCEPT -> viewModel.acceptOffer()
                        SpeechAction.REJECT -> viewModel.rejectOffer(null)
                        else -> {
                            // do nothing
                        }
                    }
                }
            }
        }

        intent?.let(::handleBookingOfferRequest)
    }

    override fun onDestroy() {
        // Enable lockscreen support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }

        if (isKeyguardListenerAttached) {
            // Enable keyguard
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            }
        }
        // Remove if any notification is showing
        NotificationManagerCompat.from(this).cancel(BOOKING_OFFER_NOTIFICATION_ID)

        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("New offer intent received...")
        intent?.let(::handleBookingOfferRequest)
    }

    private fun handleBookingOfferRequest(data: Intent) {
        sessionManager.clearSavedBookingOfferData()
        val strJsonOffer = data.extras?.getString(EXTRA_BOOKING_OFFER)
        if (!strJsonOffer.isNullOrEmpty()) {
            val offerPayload = moshi.adapter(BookingOfferPayload::class.java).fromJson(strJsonOffer)
            if (offerPayload != null) {
                if (offerPayload.bookingId == viewModel.bookingId) {
                    Timber.d("Offer already showing, ignore new request".uppercase())
                    return
                }

                val strName = intent.getStringExtra(EXTRA_BOOKING_OFFER_SOURCE)
                if (!strName.isNullOrEmpty()) {
                    val source = OfferSource.valueOf(strName)
                    binding.imgBookingOfferSource.show()
                    if (source == OfferSource.SOCKET) {
                        binding.imgBookingOfferSource.setImageResource(R.drawable.baseline_insert_link_24)
                    } else binding.imgBookingOfferSource.setImageResource(R.drawable.baseline_notifications_24)
                }

                if (offerPayload.isTestOffer) updateUiBasedOnTestOffer(offerPayload)
                else updateUiBasedOnActualOffer(offerPayload)
            } else {
                Timber.e(BookingOfferException("Error in converting offer payload: $strJsonOffer"))
                finish()
            }
        } else {
            Timber.e(BookingOfferException("Offer payload is null or empty"))
            finish()
        }
    }

    private fun updateUiBasedOnTestOffer(payload: BookingOfferPayload) {
        // Update viewModel booking detail
        viewModel.setBookingOfferPayload(payload)
        // Update ui buttons
        binding.btnAcceptOffer.isEnabled = true
        binding.btnRejectOffer.isEnabled = true
        binding.grpBookingOfferActionBtns.show()
        binding.imgBookingOfferAccepted.show()
        // Hide timer for test offers
        binding.lblBookingOfferTimer.gone()
        binding.btnStartLater.gone()
    }

    private fun updateUiBasedOnActualOffer(payload: BookingOfferPayload) {
        // TODO: Find a better way to do this
        val difference = Duration.between(Instant.now(), payload.expiresIn).toMillis()
        if (difference > 0) {
            // Stop and start speech recognizer again
//            speechRecognizerHandler.stopListening()
//            speechRecognizerHandler.startListening(
//                TimeUnit.MILLISECONDS.toSeconds(difference).toInt()
//            )

            // Update viewModel booking detail
            viewModel.setBookingOfferPayload(payload)
            // Update ui buttons
            binding.btnAcceptOffer.isEnabled = true
            binding.btnRejectOffer.isEnabled = true
            binding.grpBookingOfferActionBtns.show()
            binding.lblBookingOfferTimer.show()
            // This will be shown when offer expires or accepted, hide now
            binding.btnStartLater.gone()
            binding.imgBookingOfferAccepted.gone()
            // If any dialog showing remove now
            hideDialog()
            //changeWithAnimation()
        } else {
            Timber.e(BookingOfferException("Offer received after ${difference / 1000} sec(s)"))
            finishNow(getString(R.string.msg_offer_expired))
        }
    }

//    private fun changeWithAnimation() {
//        val viewHeight = binding.llOfferContainer.height
//        binding.llOfferContainer.animate()
//            .translationY(viewHeight.toFloat())
//            .setDuration(300)
//            .withEndAction {
//                binding.llOfferContainer.animate()
//                    .translationY(0F)
//                    .setDuration(300)
//                    .start()
//            }
//            .start()
//    }

    private fun finishNow(message: String) {
        toast(message)
        finish()
    }

    override fun onAcceptBooking() {
        if (viewModel.isTestOffer) {
            localBroadcastManager.sendBroadcast(Intent(ACTION_TEST_BOOKING_OFFER_EVENT))
            finish()
            return
        }
        //speechRecognizerHandler.stopListening()
        speechRecognizerHandler.release()
        viewModel.acceptOffer()
    }

    override fun onRejectBooking() {
        if (viewModel.isTestOffer) {
            localBroadcastManager.sendBroadcast(Intent(ACTION_TEST_BOOKING_OFFER_EVENT))
            finish()
            return
        }
        //speechRecognizerHandler.stopListening()
        speechRecognizerHandler.release()
        if (!viewModel.isFollowOnOffer) {
            if (sessionManager.rejectionReasons.isNotEmpty()) {
                viewModel.stopOfferAudioPlayer()
                viewModel.stopOfferExpiryCounter()
                loadRejectionReasonBottomSheet()
            } else {
                if (binding.btnRejectOffer.text == getString(R.string.action_reject)) {
                    binding.btnRejectOffer.setText(R.string.action_confirm)
                } else viewModel.rejectOffer(null)
            }
        } else viewModel.rejectOffer(null)
    }

    private fun loadRejectionReasonBottomSheet() {
        RejectionReasonBottomSheet.newInstance()
            .show(supportFragmentManager, TAG_REJECTION_REASON_DIALOG)
    }

    override fun onStartLater() {
        finish()
    }

    private fun onBookingAcceptedSuccessfully(payload: BookingOfferPayload?) {
        if (viewModel.mobileState?.driverStatus == Driver.Status.ON_JOB
            || viewModel.mobileState?.driverStatus == Driver.Status.CLEARING
        ) onStartBookingLater(showAsAlert = true)
        else {
            // Fetch booking to show job immediately for enroute and reminder offers
            if (payload?.isReminder == true || payload?.isBookingEnRouteAfterAccept == true) {
                localBroadcastManager.sendBroadcast(Intent(ACTION_REFRESH_UPCOMING))
                viewModel.getBookingDetail()
            } else onStartBookingLater(showAsAlert = false)
        }
    }

    private fun onBookingDetailLoaded(booking: Booking) {
        // Check if the booking can be started immediately
        if (booking.status == Booking.Status.ON_ROUTE) {
            toast(R.string.msg_booking_accepted)
            startActivity(Intent(this, HomeActivity::class.java).apply {
                putExtras(bundleOf(EXTRA_ACCEPTED_OFFER_BOOKING_ID to booking.id))
                //flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        } else onStartBookingLater(showAsAlert = true)
    }

    private fun onStartBookingLater(showAsAlert: Boolean) {
        // Hide all action buttons and timer
        binding.lblBookingOfferTimer.gone()
        binding.grpBookingOfferActionBtns.gone()
        // Show offer accepted icon on top
        binding.imgBookingOfferAccepted.show()

        // Refresh mobile state and upcoming list
        sendRefreshBroadcast()

        if (showAsAlert) {
            binding.btnStartLater.gone()
            showMaterialAlert {
                isCancelable = false
                titleResource = R.string.title_alert
                messageResource = R.string.msg_offer_accepted
                okButton {
                    it.dismiss()
                    finish()
                }
            }
        } else binding.btnStartLater.show()
    }

    private fun sendRefreshBroadcast() {
        localBroadcastManager.sendBroadcast(Intent(ACTION_REFRESH_UPCOMING))
        localBroadcastManager.sendBroadcast(Intent(ACTION_REFRESH_MOBILE_STATE))
    }
}