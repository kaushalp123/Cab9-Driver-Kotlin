package com.cab9.driver.ui.web

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.cab9.driver.BuildConfig
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.databinding.FragmentWebviewBinding
import com.cab9.driver.ext.*
import com.cab9.driver.network.WebViewException
import com.cab9.driver.utils.ConnectivityLiveData
import com.cab9.driver.utils.FilePicker
import com.cab9.driver.utils.GOOGLE_DOCS_EMBEDDED_URL
import com.cab9.driver.utils.getWebViewErrorMessage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseWebFragment : BaseFragment(R.layout.fragment_webview) {

    @Inject
    lateinit var connectivityLiveData: ConnectivityLiveData

    abstract val url: String

    /**
     * List of mime types supported by this WebView.
     */
    open val supportedMimeTypes: Array<String>
        get() = arrayOf("image/*")

    /**
     * To be implemented by super class if instance of this binding need to survive fragment lifecycle
     */
    open fun onViewBindingComplete(binding: FragmentWebviewBinding) {
        // To be implemented by super class
    }

    /**
     * Use this to re-initialize previously saved bindings.
     */
    open fun initializePreviousBinding(binding: FragmentWebviewBinding) {
        _binding = binding
    }

    open fun shouldOverrideUrlLoading(url: String): Boolean = when {
        url.startsWith("tel:") -> {
            context?.openDialApp(url)
            true
        }

        else -> false
    }

    private val userAgent: String
        get() = "%s [%s/%s]".format(
            webView.settings.userAgentString.orEmpty(),
            getString(R.string.app_name),
            "v${BuildConfig.VERSION_NAME}"
        )

    private var _binding: FragmentWebviewBinding? = null

    private val progressBar: ProgressBar
        get() = _binding?.linearProgressBar!!

    private val webView: WebView
        get() = _binding?.webView!!

    private val fileChooserPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            listOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        else listOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            // if all permission are granted open file chooser dialog
            if (results.all { it.value }) filePicker.launch(supportedMimeTypes)
            else requireContext().toast(R.string.err_no_file_access)
        }

    private val filePicker = registerForActivityResult(FilePicker()) { result ->
        val imageUri = result?.firstOrNull()
        if (imageUri != null) {
            onReceiveWebViewValues(arrayOf(imageUri))
        } else onReceiveWebViewValues(null)
    }

    private val _webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (newProgress < 100) {
                if (!progressBar.isVisible) progressBar.show()
                progressBar.progress = newProgress
            } else progressBar.gone()
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            return onShowFileChooser(filePathCallback)
        }
    }

    private val _webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            progressBar.show()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.gone()
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url?.toString().orEmpty()
            return if (!shouldOverrideUrlLoading(url)) {
                super.shouldOverrideUrlLoading(view, request)
            } else true
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            error?.let { onWebViewError(it.errorCode, it.description?.toString()) }
        }
    }

    private val internetObserver = Observer<Boolean> { isConnected ->
        if (!isConnected) showInternetErrorView()
        else {
            // Reload web view only if error is showing
            if (_binding?.webErrorView?.isVisible == true) reloadWebView()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (_binding == null) {
            Timber.w("Inflating ${this.javaClass.simpleName} layout".uppercase())
            _binding = FragmentWebviewBinding.inflate(requireActivity().layoutInflater)

            webView.apply {
                setDarkTheme(requireContext().isDarkThemeActive)
                webViewClient = _webViewClient
                webChromeClient = _webChromeClient
            }
            webView.settings.apply {
                databaseEnabled = true
                domStorageEnabled = true
                javaScriptEnabled = true
                userAgentString = userAgent
                loadWithOverviewMode = true
                useWideViewPort = true
                allowFileAccess = true
                allowContentAccess = true
                setSupportZoom(false)
            }

            if (url.isPdfUrl()) {
                // handling the PDF extension url by opening it in google docs.
                webView.loadUrl(GOOGLE_DOCS_EMBEDDED_URL + url)
            } else webView.loadUrl(url)

            onViewBindingComplete(_binding!!)
        }
        connectivityLiveData.observe(viewLifecycleOwner, internetObserver)
        return _binding?.root
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    private fun onShowFileChooser(callback: ValueCallback<Array<Uri>>?): Boolean {
        return if (!requireContext().isPermissionGranted(fileChooserPermissions)) {
            requestPermissions.launch(fileChooserPermissions.toTypedArray())
            false
        } else {
            try {
                this.filePathCallback?.onReceiveValue(null)
                this.filePathCallback = callback
                filePicker.launch(supportedMimeTypes)
                true
            } catch (ex: Exception) {
                onReceiveWebViewValues(null)
                Timber.e(ex)
                false
            }
        }
    }

    private fun onReceiveWebViewValues(values: Array<Uri>?) {
        filePathCallback?.onReceiveValue(values)
        filePathCallback = null
    }

    private fun onWebViewError(errorCode: Int, errorDesc: String?) {
        val message = getWebViewErrorMessage(errorCode)
        Timber.w(WebViewException("${this.javaClass.simpleName} [${errorCode}] $errorDesc($message)"))
    }

    private fun showInternetErrorView() {
        _binding?.webErrorView?.show()
        _binding?.webView?.gone()
        _binding?.webErrorView?.errorTitle = getString(R.string.err_title_no_internet)
        _binding?.webErrorView?.errorSubtitle = getString(R.string.err_msg_no_internet)
        _binding?.webErrorView?.errorIcon =
            requireContext().drawable(R.drawable.ic_baseline_wifi_off_24)
    }

    private fun reloadWebView() {
        _binding?.webErrorView?.gone()
        _binding?.webView?.show()
        _binding?.webView?.loadUrl(url)
    }
}