package com.cab9.driver.ui.web

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs

class WebViewFragment : BaseWebFragment() {

    private val args by navArgs<WebViewFragmentArgs>()

    override val url: String
        get() = args.url

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbarTitle(args.title)
    }

}