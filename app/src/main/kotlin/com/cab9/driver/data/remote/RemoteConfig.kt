package com.cab9.driver.data.remote

import com.cab9.driver.BuildConfig
import com.cab9.driver.network.RemoteConfigException
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfig @Inject constructor(private val moshi: Moshi) {

    private val parameterType = Types.newParameterizedType(List::class.java, String::class.java)

    val isMeterEnabled: Boolean by lazy {
        try {
            val strJsonArr = Firebase.remoteConfig.getString("meter_enabled_tenant_ids")
            moshi.adapter<List<String>>(parameterType).fromJson(strJsonArr)
                ?.find { it.equals(BuildConfig.TENANT_ID, true) }
                .orEmpty().isNotEmpty()
        } catch (ex: Exception) {
            Timber.e(RemoteConfigException(ex))
            false
        }
    }

    val isClientNameVisibilityDisabled: Boolean by lazy {
        try {
            val strJsonArr = Firebase.remoteConfig.getString("client_name_disabled_tenant_ids")
            moshi.adapter<List<String>>(parameterType).fromJson(strJsonArr)
                ?.find { it.equals(BuildConfig.TENANT_ID, true) }
                .orEmpty().isNotEmpty()
        } catch (ex: Exception) {
            Timber.e(RemoteConfigException(ex))
            false
        }
    }

    val isZoneNowTabEnabled: Boolean by lazy {
        try {
            val strJsonArr = Firebase.remoteConfig.getString("zone_now_enabled_tenant_ids")
            moshi.adapter<List<String>>(parameterType).fromJson(strJsonArr)
                ?.find { it.equals(BuildConfig.TENANT_ID, true) }
                .orEmpty().isNotEmpty()
        } catch (ex: Exception) {
            Timber.e(RemoteConfigException(ex))
            false
        }
    }

    suspend fun fetchAndActivate(): Boolean {
        // Fetch and activate latest remote config changes
        return Firebase.remoteConfig.fetchAndActivate().await()
    }

}