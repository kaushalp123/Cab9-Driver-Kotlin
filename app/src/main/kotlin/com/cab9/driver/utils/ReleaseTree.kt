package com.cab9.driver.utils

import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.ktx.setCustomKeys
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class ReleaseTree : Timber.Tree() {

    companion object {
        private const val CRASHLYTICS_KEY_PRIORITY = "priority"
        private const val CRASHLYTICS_KEY_TAG = "tag"
        private const val CRASHLYTICS_KEY_MESSAGE = "message"
    }

    private val firebaseCrashlytics = Firebase.crashlytics

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.ERROR && t !is CancellationException) {

            val caughtException = t ?: Exception(message)

            firebaseCrashlytics.setCustomKeys {
                key(CRASHLYTICS_KEY_PRIORITY, priority)
                key(CRASHLYTICS_KEY_MESSAGE, message)
                tag?.let { key(CRASHLYTICS_KEY_TAG, it) }
            }

            // Firebase Crash Reporting
            firebaseCrashlytics.recordException(caughtException)
        }
    }
}