package com.cab9.driver.ui.web

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.cab9.driver.BuildConfig
import com.cab9.driver.databinding.FragmentWebviewBinding
import com.cab9.driver.ext.openExternalUrl
import com.cab9.driver.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardWebFragment : BaseWebFragment() {

    private val homeViewModel by activityViewModels<HomeViewModel>()

    override val url: String
        get() = buildString {
            append("https://")
            append(BuildConfig.DASHBOARD_HOSTNAME)
            append("/app/index.html")
            append("?token=" + homeViewModel.authToken)
        }

    override val supportedMimeTypes: Array<String>
        get() = arrayOf("application/pdf", "image/*")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeView?.dashboardWebBinding?.let { initializePreviousBinding(it) }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewBindingComplete(binding: FragmentWebviewBinding) {
        homeView?.onCreateDashboardBinding(binding)
    }

    override fun shouldOverrideUrlLoading(url: String): Boolean {
        if (Uri.parse(url).host == BuildConfig.DASHBOARD_HOSTNAME) {
            // This is my web site, so do not override; let my WebView load the page
            return false
        }
        // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
        context?.openExternalUrl(url)
        return true
    }
}