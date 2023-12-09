package com.cab9.driver.ext

import android.content.Context
import android.text.TextUtils
import android.util.Patterns
import com.cab9.driver.R
import java.util.*

//inline fun SpannableStringBuilder.clickable(crossinline onClick: (View) -> Unit): ClickableSpan {
//    return object : ClickableSpan() {
//        override fun onClick(widget: View) {
//            onClick(widget)
//        }
//    }
//}

inline fun String.isValidEmail() = Patterns.EMAIL_ADDRESS.matcher(this).matches()
//public static final String MOBILE_NO_REGEX = "^([0]|\\+\\d{1,3}[- ]?)?\\d{10}$";

inline fun String.isValidMobileNumber() = this.matches("^([0]|\\+\\d{1,3}[- ]?)?\\d{10}$".toRegex())

inline fun prefixCurrency(context: Context, amount: Double): String {
    return context.getString(R.string.pound_symbol).plus("%.2f".format(amount))
}

inline fun formatToTwoDigit(value: Int) = "%02d".format(value)

inline fun formatToSingleDigitDecimal(value: Float) = "%.1f".format(value)


/**
 * this function checks weather the url being opened is a PDF link or not
 * @param url - url to be opened.
 * @return - true if its PDF link, false if not.
 */
fun String.isPdfUrl(): Boolean {
    var url = this
    if (!TextUtils.isEmpty(url)) {
        url = url.trim { it <= ' ' }
        val lastIndex = url.lowercase(Locale.getDefault()).lastIndexOf(".pdf")
        if (lastIndex != -1) {
            return url.substring(lastIndex).equals(".pdf", ignoreCase = true)
        }
    }
    return false
}
