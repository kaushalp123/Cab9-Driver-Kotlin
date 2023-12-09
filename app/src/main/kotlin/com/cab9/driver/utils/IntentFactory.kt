package com.cab9.driver.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.cab9.driver.BuildConfig
import com.cab9.driver.R
import com.cab9.driver.ext.toast
import timber.log.Timber

object IntentFactory {

    private const val FEEDBACK_EMAIL = "suggestion@cab9.app"

    fun playStoreIntent(packageName: String): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        intent.setPackage("com.android.vending")
        return intent
    }


    /**
     * Opens the default email app for sending driver feedback on his/her app user experience.
     *
     * @param context  - context object required for start activity method.
     * @param callSign - driver callSign which will be used as a mail subject.
     */
    fun openMailForFeedback(context: Context, callSign: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:") // only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL)) // recipient email id
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback | $callSign") // email subject
            val body = context.getString(R.string.app_name) +  // app name
                    " | " + BuildConfig.VERSION_NAME +
                    " | " + Build.MANUFACTURER +
                    " | " + Build.MODEL +
                    " | " + Build.BRAND +
                    " | " + Build.VERSION.SDK_INT
            intent.putExtra(Intent.EXTRA_TEXT, body)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.w(e)
            context.toast(context.getString(R.string.err_no_email_app_found))
        } catch (e: Exception) {
            Timber.e(e)
            context.toast(context.getString(R.string.err_msg_generic))
        }
    }


}