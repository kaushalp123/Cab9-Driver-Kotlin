package com.cab9.driver.utils

import com.cab9.driver.BuildConfig
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrandUtils @Inject constructor() {

    val isClientNameEnabled: Boolean
        get() = Firebase.remoteConfig.getString("client_name")
            .contains(BuildConfig.FLAVOR_brand, true)


}