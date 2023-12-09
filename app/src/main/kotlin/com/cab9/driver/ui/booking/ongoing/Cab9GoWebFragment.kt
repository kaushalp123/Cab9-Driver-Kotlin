package com.cab9.driver.ui.booking.ongoing

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.webkit.WebViewFeature
import com.cab9.driver.BuildConfig
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Cab9GoParams
import com.cab9.driver.data.models.MobileState
import com.cab9.driver.data.models.SumUpMode
import com.cab9.driver.data.models.UpdateSumUpPaymentRequest
import com.cab9.driver.databinding.FragmentWebviewBinding
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.*
import com.cab9.driver.network.TransactionInfoException
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.sumup.SumUpPaymentHandler
import com.cab9.driver.sumup.SumUpPaymentHandler.Companion.STATUS_FAILED
import com.cab9.driver.sumup.SumUpPaymentHandler.Companion.STATUS_SUCCESSFUL
import com.cab9.driver.sumup.SumUpPaymentHandler.Companion.SUMUP_APP_PACKAGE_NAME
import com.cab9.driver.ui.home.HomeViewModel
import com.cab9.driver.utils.ConnectivityLiveData
import com.cab9.driver.utils.IntentFactory
import com.cab9.driver.widgets.dialog.okButton
import com.sumup.merchant.reader.models.TransactionInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class Cab9GoWebFragment : BaseFragment(R.layout.fragment_webview) {

    companion object {
        private const val JOB_ID_REGEX = "\"Job (\\d+)\""
    }

    private val args by navArgs<Cab9GoWebFragmentArgs>()
    private val homeViewModel by activityViewModels<HomeViewModel>()
    private val viewModel by viewModels<Cab9GoViewModel>()

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var connectivityLiveData: ConnectivityLiveData

    @Inject
    lateinit var userComponentManager: UserComponentManager

    private var sumUpPaymentHandler: SumUpPaymentHandler? = null

    private val url: String
        get() = buildString {
            append(BuildConfig.CAB9_GO_WEB_URL)
            append("?token=" + homeViewModel.authToken)
            append("&bookingId=" + args.bookingId)
        }

    private val binding: FragmentWebviewBinding?
        get() = homeView?.cab9GoWebViewWrapper?.binding

    private val webView: WebView?
        get() = binding?.webView

    private val mobileState: MobileState?
        get() = homeViewModel.mobileState

    private val jobIdPattern = Regex(JOB_ID_REGEX)

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            binding?.run {
                if (linearProgressBar.isVisible) return
                else if (webErrorView.isVisible) destroyFragmentWithViews()
                else {
                    // Send physical back action to Cab9Go webView
                    val mainJson = JSONObject(
                        mapOf(
                            KEY_TYPE to TYPE_UI,
                            KEY_MESSAGE to ACTION_MESSAGE_PHYSICAL_BACK
                        )
                    )
                    webView.postWebMessageCompat(mainJson)
                }
            } ?: destroyFragmentWithViews()
        }
    }

    private val internetObserver = Observer<Boolean> { isConnected ->
        if (!isConnected) showInternetErrorView()
        else {
            // Reload web view only if error is showing
            if (binding?.webErrorView?.isVisible == true) reloadWebView()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        when {
            binding == null -> {
                Timber.w("Inflating ${this.javaClass.simpleName} layout with ID -> ${args.bookingId}")
                initializeViews()
            }

            webView?.tag?.toString() != args.bookingId -> {
                Timber.w("New booking ID found, recreate ${this.javaClass.simpleName} layout with ID -> ${args.bookingId}")
                initializeViews()
            }
        }
        homeView?.cab9GoWebViewWrapper?.setListener(this)
        connectivityLiveData.observe(viewLifecycleOwner, internetObserver)
        return binding?.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeViews() {
        val newBinding = FragmentWebviewBinding.inflate(requireActivity().layoutInflater)
        val appUserAgentString = "%s [%s/%s]".format(
            newBinding.webView.settings.userAgentString,
            getString(R.string.app_name),
            "v${BuildConfig.VERSION_NAME}"
        )
        newBinding.webView.tag = args.bookingId
        newBinding.webView.settings.apply {
            databaseEnabled = true
            domStorageEnabled = true
            javaScriptEnabled = true
            userAgentString = appUserAgentString
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        newBinding.webView.loadUrl(url)
        newBinding.webView.setDarkTheme(requireContext().isDarkThemeActive)

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            showUnsupportedWebViewDialog()
        }

        homeView?.onCreateCab9GoBinding(newBinding)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, backPressedCallback)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { if (binding == null) destroyFragmentWithViews() }
                launch {
                    viewModel.sumUpPaymentStatusOutcome.collectLatest {
                        binding?.indeterminateProgressBar?.visibility(it is Outcome.Progress)
                        if (it is Outcome.Success) {
                            postSumUpStatusOnWeb(it.data.second, "", it.data.third)
                        } else if (it is Outcome.Failure) showBottomNavSnack(it.msg)
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        sumUpPaymentHandler?.onActivityResult(requestCode, data)
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    private fun showUnsupportedWebViewDialog() {
        showMaterialAlert {
            isCancelable = false
            titleResource = R.string.dialog_title_warning
            messageResource = R.string.dialog_msg_unsupported_web_message
            okButton { it.dismiss() }
        }
    }

    fun destroyFragmentWithViews() {
        findNavController().popBackStack()
        homeView?.destroyCab9WebBinding()
        refreshMobileState()
    }

    fun refreshMobileState() {
        homeViewModel.fetchMobileState()
    }

    fun verifyAndTakePaymentViaSumUp(jsonObj: JSONObject) {
        when (mobileState?.sumUpMode) {
            // Take payment with SumUp installed app
            SumUpMode.DRIVER -> requestPaymentThroughSumUpApp()
            // Take payment with SumUp SDK
            SumUpMode.COMPANY -> requestPaymentThroughSumUpSdk(jsonObj)
            else -> showBottomNavSnack(getString(R.string.err_sumup_mode_not_defined))
        }
    }

    private fun requestPaymentThroughSumUpApp() {
        try {
            startActivity(
                requireContext().packageManager.getLaunchIntentForPackage(SUMUP_APP_PACKAGE_NAME)
                    ?: IntentFactory.playStoreIntent(SUMUP_APP_PACKAGE_NAME)
            )
        } catch (ex: Exception) {
            showBottomNavSnack(getString(R.string.err_sumup_app_not_installed))
        }
    }

    private fun requestPaymentThroughSumUpSdk(jsonObj: JSONObject) {
        if (mobileState?.isSumUpAvailable == true) {
            val params = Cab9GoParams(jsonObj)
            if (params.isValid) requestSumUpPayment(params)
            else showBottomNavSnack(getString(R.string.err_sumup_payment_data_not_defined))
        } else showBottomNavSnack(getString(R.string.err_sumup_not_available))
    }

    private fun requestSumUpPayment(params: Cab9GoParams) {
        val sumupDetail = mobileState?.sumUpDetail
        if (sumupDetail != null) {
            // Create sumup payment service instance and initaite payment
            object : SumUpPaymentHandler(
                viewLifecycleOwner.lifecycleScope,
                params,
                sumupDetail,
                userComponentManager.sumUpRepo
            ) {
                override fun onTransactionComplete(
                    message: String?,
                    params: Cab9GoParams,
                    transactionInfo: TransactionInfo?
                ) {
                    onSumUpTransactionComplete(message, params, transactionInfo)
                }

                override fun showSumUpMessage(msgResId: Int) {
                    requireContext().toast(msgResId)
                }
            }.also {
                it.checkout(requireActivity())
                sumUpPaymentHandler = it
            }
        } else showBottomNavSnack(getString(R.string.err_sumup_payment_data_not_defined))
    }

    private fun onSumUpTransactionComplete(
        resultMsg: String?,
        params: Cab9GoParams,
        transactionInfo: TransactionInfo?,
    ) {
        if (transactionInfo != null) {
            val transactionJsonStr = transactionInfo.toJsonString()
            logTransactionEvent(params.localId, transactionJsonStr)
            if (!resultMsg.isNullOrEmpty()) showBottomNavSnack(resultMsg)
            val transactionStatus = transactionInfo.status ?: STATUS_FAILED
            val request = UpdateSumUpPaymentRequest(
                status = transactionStatus,
                amount = params.amount.toString(),
                bookingId = params.bookingId,
                driverId = params.driverId,
                localId = params.localId,
                jsonStrTransactionInfo = transactionJsonStr
            )
            viewModel.updatePaymentStatus(transactionStatus == STATUS_SUCCESSFUL, request)
        } else postSumUpStatusOnWeb(STATUS_FAILED, resultMsg.orEmpty(), null)
    }

    private fun logTransactionEvent(localId: String, data: String) {
        try {
            if (!data.contains(localId)) {
                val transactionJobId = jobIdPattern.find(data)?.groupValues?.get(1)
                Timber.e(TransactionInfoException("SumUp Payment registered with ID[$transactionJobId] instead of [${localId}]"))
            }
        } catch (ex: Exception) {
            // ignore
        }
    }


    /**
     * Post SumUp payment status to web.
     *
     * @param status  SumUp payment status
     * @param message message to show
     */
    private fun postSumUpStatusOnWeb(status: String, message: String, response: String?) {
        try {
            // Setup location json object
            val sumUpJson = JSONObject()
            sumUpJson.put(KEY_STATUS, status)
            sumUpJson.put(KEY_MESSAGE, message)
            sumUpJson.put(KEY_BODY, response)
            // Set request json object
            val mainJson = JSONObject()
            mainJson.put(KEY_TYPE, ACTION_SUMUP_PAYMENT)
            mainJson.put(KEY_MESSAGE, TYPE_PAYMENT_STATUS)
            mainJson.put(KEY_DATA, sumUpJson)
            // Post message to webpage to handle
            webView?.postWebMessageCompat(mainJson)
        } catch (ex: Exception) {
            Timber.w(ex)
        }
    }

//    private fun onWebViewError(errorCode: Int, errorDesc: String?) {
//        val message = getWebViewErrorMessage(errorCode)
//        Timber.w(WebViewException("${this.javaClass.simpleName} [${errorCode}] $errorDesc($message)"))
//    }

    private fun showInternetErrorView() {
        binding?.linearProgressBar?.gone()
        binding?.webErrorView?.show()
        binding?.webView?.gone()
        binding?.webErrorView?.errorTitle = getString(R.string.err_title_no_internet)
        binding?.webErrorView?.errorSubtitle = getString(R.string.err_msg_no_internet)
        binding?.webErrorView?.errorIcon =
            requireContext().drawable(R.drawable.ic_baseline_wifi_off_24)
        binding?.webErrorView?.errorBtnText = getString(R.string.action_close)
        binding?.webErrorView?.onActionBtnClick { destroyFragmentWithViews() }
    }

    private fun reloadWebView() {
        binding?.webErrorView?.gone()
        binding?.webView?.show()
        binding?.webView?.loadUrl(url)
    }
}