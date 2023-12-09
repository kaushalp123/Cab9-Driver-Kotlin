package com.cab9.driver.ui.account.profile

import com.cab9.driver.BuildConfig
import com.cab9.driver.ui.web.BaseWebFragment

class AboutWebFragment : BaseWebFragment() {

    override val url: String
        get() = BuildConfig.ABOUT_URL

}