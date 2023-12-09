package com.cab9.driver.ext

import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.SpannedString
import android.text.method.LinkMovementMethod
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.Insets
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject

inline val ViewGroup.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(context)

inline fun <reified T : Enum<T>> TypedArray.getEnum(index: Int, default: T) =
    getInt(index, -1).let { if (it >= 0) enumValues<T>()[it] else default }

inline fun View.show() {
    if (visibility != View.VISIBLE) visibility = View.VISIBLE
}

inline fun View.hide() {
    if (visibility != View.INVISIBLE) visibility = View.INVISIBLE
}

inline fun View.gone() {
    if (visibility != View.GONE) visibility = View.GONE
}

inline fun View.visibility(show: Boolean) {
    if (show) show() else gone()
}

inline fun View.snack(@StringRes messageResId: Int) {
    Snackbar.make(this, messageResId, Snackbar.LENGTH_SHORT).show()
}

inline fun View.snack(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()
}

inline fun View.anchorSnack(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT)
        .setAnchorView(this)
        .show()
}

fun View?.fitSystemWindowsAndAdjustResize() = this?.let { view ->
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
        view.fitsSystemWindows = true
        val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

        WindowInsetsCompat
            .Builder()
            .setInsets(
                WindowInsetsCompat.Type.systemBars(),
                Insets.of(0, 0, 0, bottom)
            )
            .build()
            .apply {
                ViewCompat.onApplyWindowInsets(v, this)
            }
    }
}


inline fun View.anchorSnack(@StringRes messageResId: Int) {
    anchorSnack(context.getString(messageResId))
}

inline fun TextView.iconStart(@DrawableRes drawableResId: Int) {
    setCompoundDrawablesWithIntrinsicBounds(drawableResId, 0, 0, 0)
}

inline fun TextView.iconStart(drawable: Drawable) {
    setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
}

inline fun TextView.iconTint(@ColorRes colorResId: Int) {
    TextViewCompat.setCompoundDrawableTintList(
        this,
        ColorStateList.valueOf(context.colorInt(colorResId))
    )
}

inline fun ImageView.iconTint(@ColorRes colorResId: Int) {
    ImageViewCompat.setImageTintList(
        this,
        ColorStateList.valueOf(context.colorInt(colorResId))
    )
}

inline fun TextView.iconEnd(@DrawableRes drawableResId: Int) {
    setCompoundDrawablesWithIntrinsicBounds(0, 0, drawableResId, 0)
}

inline fun TextView.text() = text.trim().toString()

inline fun TextInputLayout.showError(@StringRes messageResId: Int) {
    showError(context.getString(messageResId))
}

inline fun TextInputLayout.showError(message: String) {
    isErrorEnabled = true
    error = message
}

inline fun TextInputLayout.hideError() {
    isErrorEnabled = false
    error = null
}

//fun EditText.textChanges(): StateFlow<String> {
//    val query = MutableStateFlow("")
//    doOnTextChanged { t, _, _, _ ->
//        query.value = t.toString()
//    }
//    return query
//}

fun EditText.textChanges() = callbackFlow {
    val listener = doOnTextChanged { t, _, _, _ ->
        trySend(t?.toString().orEmpty())
    }
    awaitClose { removeTextChangedListener(listener) }
}

fun View.onLayoutChangeListener() = callbackFlow {
    val listener = View.OnLayoutChangeListener {
            _, _, _, _, bottom, _, _, _, oldBottom ->
         trySend(Pair(bottom, oldBottom))
    }
    addOnLayoutChangeListener(listener)
    awaitClose { removeOnLayoutChangeListener(listener) }
}

fun RecyclerView.OnScrollListener() = callbackFlow {
    val listener = object : OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            trySend(newState)
        }
    }
    addOnScrollListener(listener)
    awaitClose { removeOnScrollListener(listener) }
}

fun View.keyboardVisibilityChanges() = callbackFlow {
    val listener = OnGlobalLayoutListener {
        val r = Rect()
        getWindowVisibleDisplayFrame(r)
        val screenHeight = rootView.height
        // r.bottom is the position above soft keypad or device button.
        // if keypad is shown, the r.bottom is smaller than that before.
        val keypadHeight = screenHeight - r.bottom
        // 0.15 ratio is perhaps enough to determine keypad height.
        if (keypadHeight > screenHeight * 0.15) trySend(true) // keyboard is opened
        else trySend(false) // keyboard is closed
    }
    viewTreeObserver.addOnGlobalLayoutListener(listener)
    awaitClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
}

inline fun TextView.setSpannedText(text: SpannedString) {
    setText(text, TextView.BufferType.SPANNABLE)
}

inline fun TextView.setHtmlText(text: String) {
    setText(HtmlCompat.fromHtml(text, FROM_HTML_MODE_COMPACT).trim()) // trim to remove extra space on the text.
    movementMethod = LinkMovementMethod()
}

inline fun View.backgroundColor(@ColorRes colorResId: Int) {
    setBackgroundColor(ContextCompat.getColor(context, colorResId))
}

fun WebView.setDarkTheme(enable: Boolean) {
    if (enable && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
    }
}

inline fun TextView.font(@FontRes fontResId: Int) {
    typeface = ResourcesCompat.getFont(context, fontResId)
}

inline fun TextView.textColor(@ColorRes colorResId: Int) {
    setTextColor(ColorStateList.valueOf(context.colorInt(colorResId)))
}

inline fun View.showKeyboard() {
    if (this.requestFocus()) {
        context.getSystemService<InputMethodManager>()
            ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

inline fun View.hideKeyboard() {
    if (this.requestFocus()) {
        context.getSystemService<InputMethodManager>()
            ?.hideSoftInputFromWindow(windowToken, 0)
    }
}

inline fun BottomNavigationView.selectItemSafely(@IdRes idRes: Int) {
    if (selectedItemId != idRes) selectedItemId = idRes
}

inline fun View.isKeyboardVisible(): Boolean {
    return ViewCompat.getRootWindowInsets(this)?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
}

fun ViewGroup.slideInBottom(@IdRes viewId: Int) {
    val slideIn = Slide(Gravity.BOTTOM).apply {
        duration = 300
        addTarget(viewId)
    }
    TransitionManager.beginDelayedTransition(this, slideIn)
    findViewById<View>(viewId).show()
}

fun ViewGroup.slideOutBottom(@IdRes viewId: Int) {
    val slideIn = Slide(Gravity.BOTTOM).apply {
        duration = 300
        addTarget(viewId)
    }
    TransitionManager.beginDelayedTransition(this, slideIn)
    findViewById<View>(viewId).gone()
}

inline fun BottomNavigationView.clearBadge(@IdRes navItemId: Int) {
    getBadge(navItemId)?.apply {
        isVisible = false
        clearNumber()
    }
}

inline fun BottomNavigationView.selectItemWithId(@IdRes navItemId: Int) {
    if (selectedItemId != navItemId) selectedItemId = navItemId
}

inline fun WebView.postWebMessageCompat(jsonMessage: JSONObject) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE)) {
        WebViewCompat.postWebMessage(
            this,
            WebMessageCompat(jsonMessage.toString()),
            Uri.parse("*")
        )
    }
}


