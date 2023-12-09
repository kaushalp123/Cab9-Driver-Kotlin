package com.cab9.driver.utils

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.webkit.WebViewClient
import com.cab9.driver.R
import com.cab9.driver.ext.isPermissionGranted

fun getBookingOfferToneUri(context: Context): Uri =
    Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.tone_booking_offer)

fun getBookingCancelledToneUri(context: Context): Uri =
    Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.tone_cancel_booking)

fun getNotificationToneUri(context: Context): Uri =
    Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.tone_app_alert)

fun getNewBiddingToneUri(context: Context): Uri =
    Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.tone_new_bid)

fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
    } else true

fun enableStrictMode() {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork() // or .detectAll() for all detectable problems
            .penaltyLog()
            .build()
    )

    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .penaltyLog()
            .penaltyDeath()
            .build()
    )
}

fun getWebViewErrorMessage(errorCode: Int) = when (errorCode) {
    WebViewClient.ERROR_HOST_LOOKUP -> "Server or proxy hostname lookup failed"
    WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME -> "Unsupported authentication scheme (not basic or digest)"
    WebViewClient.ERROR_AUTHENTICATION -> "User authentication failed on server"
    WebViewClient.ERROR_PROXY_AUTHENTICATION -> "User authentication failed on proxy"
    WebViewClient.ERROR_CONNECT -> "Failed to connect to the server"
    WebViewClient.ERROR_IO -> "Failed to read or write to the server"
    WebViewClient.ERROR_TIMEOUT -> "Connection timed out"
    WebViewClient.ERROR_REDIRECT_LOOP -> "Too many redirects"
    WebViewClient.ERROR_UNSUPPORTED_SCHEME -> "Unsupported URI scheme"
    WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> "Failed to perform SSL handshake"
    WebViewClient.ERROR_BAD_URL -> "Malformed URL"
    WebViewClient.ERROR_FILE -> "Generic file error"
    WebViewClient.ERROR_FILE_NOT_FOUND -> "File not found"
    WebViewClient.ERROR_TOO_MANY_REQUESTS -> "Too many requests during this load"
    WebViewClient.ERROR_UNSAFE_RESOURCE -> "Resource load was canceled by Safe Browsing"
    else -> "Generic error"
}
