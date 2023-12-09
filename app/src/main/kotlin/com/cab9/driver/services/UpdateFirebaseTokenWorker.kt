package com.cab9.driver.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import com.cab9.driver.data.models.AddDeviceRequest
import com.cab9.driver.network.UpdateFirebaseTokenException
import com.cab9.driver.network.Cab9TokenException
import com.cab9.driver.network.apis.NodeAPI
import com.cab9.driver.network.isNetworkError
import com.cab9.driver.settings.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltWorker
class UpdateFirebaseTokenWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val EXTRA_KEY_FIREBASE_TOKEN = "firebase_token"
        const val NAME = "update_firebase_token"

        fun createRequest(token: String): OneTimeWorkRequest {
            val inputDataBuilder = Data.Builder()
            inputDataBuilder.putString(EXTRA_KEY_FIREBASE_TOKEN, token)
            return OneTimeWorkRequest.Builder(UpdateFirebaseTokenWorker::class.java)
                .setInputData(inputDataBuilder.build())
                // added backoff delay of 5 seconds to a retry if the work faces any
                // exception so that FCM token update should never fail.
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5000, TimeUnit.MILLISECONDS)
                .addTag(UpdateFirebaseTokenWorker::class.java.simpleName)
                .build()
        }
    }

    @Inject
    lateinit var nodeApi: NodeAPI

    @Inject
    lateinit var sessionManager: SessionManager

    override suspend fun doWork(): Result {
        Timber.d("Updating Firebase token...")
        if (runAttemptCount < 2) {
            val token = inputData.getString(EXTRA_KEY_FIREBASE_TOKEN)
            return if (!token.isNullOrEmpty()) {
                Timber.d("Firebase token found, update now...")
                try {
                    val addDeviceRequest = AddDeviceRequest(token)
                    val response = nodeApi.registerDevice(addDeviceRequest)
                    sessionManager.updateFirebaseToken(token, response.id)
                    Timber.d("Firebase token updated successfully!")
                    Result.success()
                } catch (ex: Exception) {
                    Timber.e(UpdateFirebaseTokenException(ex))
                    if (ex.isNetworkError) Result.retry() else Result.failure()
                }
            } else Result.failure()
        } else return Result.failure()
    }
}