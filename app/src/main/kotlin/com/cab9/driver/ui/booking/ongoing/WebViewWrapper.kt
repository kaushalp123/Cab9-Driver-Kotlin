package com.cab9.driver.ui.booking.ongoing

import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.fragment.findNavController
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.cab9.driver.BuildConfig
import com.cab9.driver.databinding.FragmentWebviewBinding
import com.cab9.driver.ext.gone
import com.cab9.driver.ext.openDialApp
import com.cab9.driver.ext.openExternalUrl
import com.cab9.driver.ext.openSmsApp
import com.cab9.driver.network.UnsupportedWebViewException
import com.cab9.driver.ui.account.NavigationMode
import com.cab9.driver.utils.getWebViewErrorMessage
import org.json.JSONObject
import timber.log.Timber
import java.lang.ref.WeakReference

private const val CAB9_GO_JS = "cab9GoWeb"

const val KEY_STATUS = "status"
const val KEY_BODY = "body"
const val KEY_MESSAGE = "message"
const val KEY_TYPE = "type"
const val KEY_DATA = "data"

const val TYPE_UI = "ui"

const val TYPE_PAYMENT_STATUS = "paymentStatus"

const val ACTION_MESSAGE_PHYSICAL_BACK = "physicalBack"
const val ACTION_SUMUP_PAYMENT = "sumUp"

private const val ACTION_NAVIGATE_BACK = "navigateBack"
private const val ACTION_DESTROY_PAGE = "destroyPage"
private const val ACTION_JOURNEY_COMPLETED = "journeyCompleted"
private const val ACTION_REFRESH_MOBILE_STATE = "recheckMobileState"

class WebViewWrapper(var binding: FragmentWebviewBinding?) {

    private val context: Context?
        get() = _fragmentRef?.get()?.context

    private var _fragmentRef: WeakReference<Cab9GoWebFragment>? = null

    private val webMessageListener = WebViewCompat.WebMessageListener { _, message, _, _, _ ->
        val strWebMessage = message.data
        Timber.d("Cab9Go Web Message Received -> $strWebMessage")
        if (!strWebMessage.isNullOrEmpty()) {
            val messageObj = JSONObject(strWebMessage)
            when (messageObj[KEY_MESSAGE]) {
                ACTION_REFRESH_MOBILE_STATE -> _fragmentRef?.get()?.refreshMobileState()
                ACTION_NAVIGATE_BACK -> _fragmentRef?.get()?.findNavController()?.popBackStack()
                ACTION_DESTROY_PAGE, ACTION_JOURNEY_COMPLETED ->
                    _fragmentRef?.get()?.destroyFragmentWithViews()

                ACTION_SUMUP_PAYMENT -> {
                    val sumUpJsonObj = messageObj.getJSONObject(KEY_DATA)
                    _fragmentRef?.get()?.verifyAndTakePaymentViaSumUp(sumUpJsonObj)
                }
            }
        }
    }

    private val _webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (newProgress < 100) binding?.linearProgressBar?.show()
            else binding?.linearProgressBar?.gone()
            binding?.linearProgressBar?.progress = newProgress
        }
    }

    private val _webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            binding?.linearProgressBar?.show()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            binding?.linearProgressBar?.gone()
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            error?.let { Timber.w("onReceivedError: ${getWebViewErrorMessage(it.errorCode)} ") }
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url?.toString()
            return when {
                url.isNullOrEmpty() -> super.shouldOverrideUrlLoading(view, request)
                url.contains("google.com/maps/dir") -> {
                    context?.let { NavigationMode.GOOGLE_MAPS.openSafely(it, url) }
                    true
                }

                url.contains("waze.com/ul") -> {
                    context?.let { NavigationMode.WAZE.openSafely(it, url) }
                    true
                }

                url.startsWith("tel:") -> {
                    context?.openDialApp(url)
                    true
                }

                url.startsWith("sms:") -> {
                    context?.openSmsApp(url)
                    true
                }

                url.startsWith("https://www.google.com/search") -> {
                    context?.openExternalUrl(url)
                    true
                }

                else -> super.shouldOverrideUrlLoading(view, request)
            }
        }
    }

    init {
        binding?.webView?.run {
            Timber.w("CAB9GO Web clients and listeners added".uppercase())
            webViewClient = _webViewClient
            webChromeClient = _webChromeClient
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                WebViewCompat.addWebMessageListener(
                    this,
                    CAB9_GO_JS,
                    setOf(BuildConfig.CAB9_GO_WEB_URL),
                    webMessageListener
                )
            } else Timber.e(UnsupportedWebViewException())
        }
    }

    fun setListener(fragment: Cab9GoWebFragment) {
        _fragmentRef = WeakReference(fragment)
    }

    fun destroy() {
        Timber.w("destroying cab9go webview components".uppercase())
        binding?.run {
            root.removeView(webView)
            webView.removeAllViews()
            webView.destroy()
        }
        binding = null
    }

}