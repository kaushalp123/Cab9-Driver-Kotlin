package com.cab9.driver.ui.home

import com.cab9.driver.data.models.HeatMapLatLng
import com.cab9.driver.databinding.FragmentWebviewBinding
import com.cab9.driver.ui.booking.ongoing.WebViewWrapper

interface HomeView {

    val isHeatmapEnabled: Boolean

    val cab9GoWebViewWrapper: WebViewWrapper?

    val chatWebBinding: FragmentWebviewBinding?

    val dashboardWebBinding: FragmentWebviewBinding?

    fun showBottomNavSnack(message: String)

    fun onHeatmapLoaded(data: List<HeatMapLatLng>)

    fun onRemoveHeatmap()

    fun openChatScreen()

    fun getCurrentLocationWithPermissionCheck()

    fun startMissingPermissionFlow()

    fun onCreateCab9GoBinding(binding: FragmentWebviewBinding)

    fun onCreateDashboardBinding(binding: FragmentWebviewBinding)

    fun onCreateChatBinding(binding: FragmentWebviewBinding)

    fun destroyCab9WebBinding()

}