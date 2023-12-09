package com.cab9.driver.ui.account.settings

import android.animation.ObjectAnimator
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.activity.result.launch
import androidx.core.animation.addListener
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.base.Outcome
import com.cab9.driver.databinding.FragmentAppDiagnosticsBinding
import com.cab9.driver.ext.colorInt
import com.cab9.driver.ext.gone
import com.cab9.driver.ext.isBatteryOptimised
import com.cab9.driver.ext.isDrawOverlayAllowed
import com.cab9.driver.ext.localBroadcastManager
import com.cab9.driver.ext.setSpannedText
import com.cab9.driver.ext.show
import com.cab9.driver.ext.textColor
import com.cab9.driver.ext.toast
import com.cab9.driver.utils.ACTION_TEST_BOOKING_OFFER_RECEIVED
import com.cab9.driver.utils.BatteryOptimization
import com.cab9.driver.utils.ConnectivityLiveData
import com.cab9.driver.utils.NotificationSettings
import com.cab9.driver.utils.SystemOverlay
import com.cab9.driver.widgets.dialog.okButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AppDiagnosticsFragment : BaseFragment(R.layout.fragment_app_diagnostics) {

    private val binding by viewBinding(FragmentAppDiagnosticsBinding::bind)
    private val viewModel by viewModels<SettingsViewModel>()

    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var connectivityLiveData: ConnectivityLiveData

    private var isInternetConnected = false
    private var offerSentAtTimeInMillis: Long? = null
    private var timerJob: Job? = null

    private var animator: ObjectAnimator? = null

    private val testOfferReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, data: Intent?) {
            if (data?.action == ACTION_TEST_BOOKING_OFFER_RECEIVED) {
                timerJob?.cancel()
                findNavController().popBackStack()
            }
        }
    }

    private val batteryOptimization = registerForActivityResult(BatteryOptimization()) {
        // ignore result
    }

    private val notificationSettings = registerForActivityResult(NotificationSettings()) {
        // ignore result
    }

    private val systemDrawOverlay = registerForActivityResult(SystemOverlay()) { }

    private val internetObserver = Observer<Boolean> { isConnected ->
        isInternetConnected = isConnected
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationManager = requireContext().getSystemService()!!
        audioManager = requireContext().getSystemService()!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectivityLiveData.observe(viewLifecycleOwner, internetObserver)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                timerJob = launch {
                    while (isActive) {
                        delay(1000)
                        offerSentAtTimeInMillis?.let { sentTimeInMillis ->
                            val diff = System.currentTimeMillis() - sentTimeInMillis
                            if (diff > (5 * 60 * 1000)) {
                                timerJob?.cancel()
                                showMaterialAlert {
                                    titleResource = R.string.dialog_title_test_notification
                                    messageResource = R.string.dialog_msg_test_notification
                                    okButton {
                                        it.dismiss()
                                        findNavController().popBackStack()
                                    }
                                }
                            }
                        }
                    }
                }
                launch {
                    viewModel.testNotificationResult.collectLatest {
                        if (it is Outcome.Success) {
                            offerSentAtTimeInMillis = System.currentTimeMillis()
                            onSettingsInEnabledState(
                                binding.lblSendTestNotificationTitle,
                                binding.lblSendTestNotificationStatus,
                                binding.pBarSendTestNotification,
                                getString(R.string.label_sending_test_notification),
                                getString(R.string.action_sent)
                            )
                        } else if (it is Outcome.Failure) {
                            offerSentAtTimeInMillis = null
                            onSettingsInDisabledState(
                                binding.lblSendTestNotificationTitle,
                                binding.lblSendTestNotificationStatus,
                                binding.pBarSendTestNotification,
                                getString(R.string.label_sending_test_notification)
                            ) {
                                //notificationSettings.launch()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startDiagnostic()
        requireContext().localBroadcastManager.registerReceiver(
            testOfferReceiver, IntentFilter(ACTION_TEST_BOOKING_OFFER_RECEIVED)
        )
    }

    override fun onPause() {
        releaseAnimator()
        requireContext().localBroadcastManager.unregisterReceiver(testOfferReceiver)
        super.onPause()
    }

    override fun onDestroyView() {
        if (timerJob?.isActive == true) timerJob?.cancel()
        super.onDestroyView()
    }

    private fun startDiagnostic() {
        resetAllUiState()
        startInternetCheck()
    }

    private fun resetAllUiState() {
        offerSentAtTimeInMillis = null
        // Reset all ui state
        listOf(
            binding.pBarInternetSettings,
            binding.pBarNotificationSettings,
            binding.pBarSoundSettings,
            binding.pBarBatteryOptimise,
            binding.pBarSendTestNotification,
            binding.pBarDrawOverlay
        ).map { it.progress = 0 }

        listOf(
            binding.lblInternetStatus,
            binding.lblNotificationStatus,
            binding.lblSoundStatus,
            binding.lblBatteryOptimiseStatus,
            binding.lblDrawOverlayStatus,
            binding.lblSendTestNotificationStatus
        ).map {
            it.text = ""
            it.gone()
        }
    }

    private fun startInternetCheck() {
        animateCheckProgress(binding.pBarInternetSettings, {
            onCheckStarted(
                binding.lblInternetSettingsTitle,
                binding.lblInternetStatus,
                getString(R.string.title_check_internet)
            )
        }, {
            if (isInternetConnected) {
                onSettingsInEnabledState(
                    binding.lblInternetSettingsTitle,
                    binding.lblInternetStatus,
                    binding.pBarInternetSettings,
                    getString(R.string.label_network_settings),
                    getString(R.string.label_connected)
                )
                startNotificationSettingsCheck()
            } else {
                onSettingsInDisabledState(
                    binding.lblInternetSettingsTitle,
                    binding.lblInternetStatus,
                    binding.pBarInternetSettings,
                    getString(R.string.label_network_settings)
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                    } else startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS))
                }
            }
        })
    }

    private fun startNotificationSettingsCheck() {
        val isNotificationDisabled =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !notificationManager.areNotificationsEnabled()
            ) true
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationManager.notificationChannels.any {
                        it.importance == NotificationManagerCompat.IMPORTANCE_NONE
                    }
                } else false
            }

        animateCheckProgress(binding.pBarNotificationSettings, {
            onCheckStarted(
                binding.lblNotificationCheckTitle,
                binding.lblNotificationStatus,
                getString(R.string.title_check_notification)
            )
        }, {
            if (isNotificationDisabled)
                onSettingsInDisabledState(
                    binding.lblNotificationCheckTitle,
                    binding.lblNotificationStatus,
                    binding.pBarNotificationSettings,
                    getString(R.string.label_notification_settings)
                ) {
                    notificationSettings.launch()
                }
            else {
                onSettingsInEnabledState(
                    binding.lblNotificationCheckTitle,
                    binding.lblNotificationStatus,
                    binding.pBarNotificationSettings,
                    getString(R.string.label_notification_settings),
                    getString(R.string.label_notifications_enabled)
                )
                startSoundSettingsCheck()
            }
        })
    }

    private fun startSoundSettingsCheck() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        val maxNotificationVolume =
            audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        val notificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)

        val notificationVolumeInPercent =
            (notificationVolume / maxNotificationVolume.toFloat()) * 100
        val volumeInPercent = (volume / maxVolume.toFloat()) * 100

        Timber.d("Current volume is: $volumeInPercent")
        Timber.d("Current notification volume is: $notificationVolumeInPercent")

        val isSoundDisabled =
            volumeInPercent.toInt() < 80 || notificationVolumeInPercent.toInt() < 80

        animateCheckProgress(binding.pBarSoundSettings, {
            onCheckStarted(
                binding.lblSoundSettingsTitle,
                binding.lblSoundStatus,
                getString(R.string.title_check_sound)
            )
        }, {
            if (isSoundDisabled) {
                onSettingsInDisabledState(
                    binding.lblSoundSettingsTitle,
                    binding.lblSoundStatus,
                    binding.pBarSoundSettings,
                    getString(R.string.label_sound_settings),
                ) {
                    fixSoundSettings()
                    onSettingsInEnabledState(
                        binding.lblSoundSettingsTitle,
                        binding.lblSoundStatus,
                        binding.pBarSoundSettings,
                        getString(R.string.label_sound_settings),
                        getString(R.string.label_volume_enabled)
                    )
                    startBatterySettingsCheck()
                }
            } else {
                onSettingsInEnabledState(
                    binding.lblSoundSettingsTitle,
                    binding.lblSoundStatus,
                    binding.pBarSoundSettings,
                    getString(R.string.label_sound_settings),
                    getString(R.string.label_volume_enabled)
                )
                startBatterySettingsCheck()
            }
        })
    }

    private fun fixSoundSettings() {
        try {
            // Maximize notification sound and music sound
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val maxNotificationVolume =
                audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotificationVolume, 0)
        } catch (ex: SecurityException) {
            Timber.w(ex, "Probably in silent mode...")
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            } else requireContext().toast(R.string.err_msg_generic)
        }
    }

    private fun startBatterySettingsCheck() {
        val isBatteryOptimised = requireContext().isBatteryOptimised()
        animateCheckProgress(binding.pBarBatteryOptimise, {
            onCheckStarted(
                binding.lblBatteryOptimiseTitle,
                binding.lblBatteryOptimiseStatus,
                getString(R.string.title_check_battery)
            )
        }, {
            if (!isBatteryOptimised) {
                onSettingsInDisabledState(
                    binding.lblBatteryOptimiseTitle,
                    binding.lblBatteryOptimiseStatus,
                    binding.pBarBatteryOptimise,
                    getString(R.string.label_battery_optimisation),
                ) {
                    batteryOptimization.launch()
                }
            } else {
                onSettingsInEnabledState(
                    binding.lblBatteryOptimiseTitle,
                    binding.lblBatteryOptimiseStatus,
                    binding.pBarBatteryOptimise,
                    getString(R.string.label_battery_optimisation),
                    getString(R.string.label_battery_enabled)
                )
                startSystemDrawSettingsCheck()
            }
        })
    }

    private fun startSystemDrawSettingsCheck() {
        val isDrawOverlayAllowed = requireContext().isDrawOverlayAllowed()
        animateCheckProgress(binding.pBarDrawOverlay, {
            onCheckStarted(
                binding.lblDrawOverlayTitle,
                binding.lblDrawOverlayStatus,
                getString(R.string.title_check_draw_overlay)
            )
        }, {
            if (!isDrawOverlayAllowed) {
                onSettingsInDisabledState(
                    binding.lblDrawOverlayTitle,
                    binding.lblDrawOverlayStatus,
                    binding.pBarDrawOverlay,
                    getString(R.string.label_draw_overlay),
                ) {
                    systemDrawOverlay.launch()
                }
            } else {
                onSettingsInEnabledState(
                    binding.lblDrawOverlayTitle,
                    binding.lblDrawOverlayStatus,
                    binding.pBarDrawOverlay,
                    getString(R.string.label_draw_overlay),
                    getString(R.string.label_draw_overlay_enabled)
                )
                sendTestNotification()
            }
        })
    }

    private fun sendTestNotification() {
        viewModel.sendTestNotification()
        animateCheckProgress(binding.pBarSendTestNotification, {
            onCheckStarted(
                binding.lblSendTestNotificationTitle,
                binding.lblSendTestNotificationStatus,
                getString(R.string.title_send_test_notification)
            )
        }, {})
    }

    private fun animateCheckProgress(
        progressBar: CircularProgressIndicator,
        onStart: () -> Unit,
        onEnd: () -> Unit
    ) {
        progressBar.setIndicatorColor(requireContext().colorInt(R.color.indicator_color_pBar))
        ObjectAnimator.ofInt(progressBar, "progress", 0, 100).apply {
            duration = 1000
            setAutoCancel(true)
            interpolator = DecelerateInterpolator()
            addListener(
                onStart = { onStart.invoke() },
                onEnd = { onEnd.invoke() })
        }.also {
            animator = it
            it.start()
        }
    }

    private fun releaseAnimator() {
        animator?.removeAllListeners()
        animator?.cancel()
    }

    private fun onCheckStarted(lblTitle: TextView, lblStatus: TextView, titleText: String) {
        lblTitle.text = titleText
        lblStatus.gone()
    }

    private fun onSettingsInEnabledState(
        lblTitle: TextView,
        lblStatus: TextView,
        progressBar: CircularProgressIndicator,
        title: String,
        statusText: String
    ) {
        lblStatus.show()
        lblTitle.text = title
        lblStatus.textColor(R.color.text_color_2)
        progressBar.setIndicatorColor(requireContext().colorInt(R.color.indicator_color_pBar))
        lblStatus.setSpannedText(buildSpannedString { append(statusText) })
        lblStatus.setOnClickListener(null)
    }

    private fun onSettingsInDisabledState(
        lblTitle: TextView,
        lblStatus: TextView,
        progressBar: CircularProgressIndicator,
        titleText: String,
        fix: () -> Unit
    ) {
        lblTitle.text = titleText
        lblStatus.show()
        lblStatus.textColor(android.R.color.holo_red_dark)
        progressBar.setIndicatorColor(requireContext().colorInt(android.R.color.holo_red_dark))
        lblStatus.setSpannedText(buildSpannedString {
            bold { append(getString(R.string.action_fix_now)) }
        })
        lblStatus.setOnClickListener { fix.invoke() }
    }


}