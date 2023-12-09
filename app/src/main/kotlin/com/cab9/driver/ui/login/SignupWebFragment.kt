package com.cab9.driver.ui.login

import com.cab9.driver.BuildConfig
import com.cab9.driver.ui.web.BaseWebFragment

class SignupWebFragment : BaseWebFragment() {

    override val url: String
        get() = BuildConfig.ONBOARDING_URL.plus(BuildConfig.COMPANY_CODE)
}