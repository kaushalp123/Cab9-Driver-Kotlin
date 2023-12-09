package com.cab9.driver.utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.BindingAdapter
import coil.load
import coil.transform.CircleCropTransformation
import com.cab9.driver.R
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.PaymentMode
import com.cab9.driver.ext.colorInt
import com.cab9.driver.ext.hideError
import com.cab9.driver.ext.showError
import com.cab9.driver.ext.visibility
import com.cab9.driver.widgets.ReactiveLayout
import com.cab9.driver.widgets.drawables.TextDrawable
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("state")
fun setState(view: View, outcome: Outcome<*>?) {
    when (view) {
        is ReactiveLayout -> outcome?.let { view.setState(outcome) }
        is ProgressBar -> view.visibility(outcome is Outcome.Progress)
        else -> view.isEnabled = outcome !is Outcome.Progress
    }
}

@BindingAdapter("errorMsg")
fun setErrorMessage(view: TextInputLayout, message: String?) {
    if (message.isNullOrEmpty()) view.hideError()
    else view.showError(message)
}


@BindingAdapter("errorMsg")
fun setEditable(view: EditText, isEditable: Boolean) {
    view.isClickable = isEditable
    view.isFocusable = isEditable
    view.isFocusableInTouchMode = isEditable
    view.requestFocus()
}

@BindingAdapter("imageUrl")
fun setImageUrl(view: ImageView, imageUrl: String?) {
    //if (imageUrl.isNullOrEmpty()) view.setImageResource(R.drawable.img_circular_avatar_placeholder)
    view.load(imageUrl)
}

@BindingAdapter("mapImageUrl")
fun setMapImageUrl(view: ImageView, imageUrl: String?) {
    view.load(imageUrl) {
        placeholder(R.drawable.img_map_placeholder)
        error(R.drawable.img_map_placeholder)
    }
}

@BindingAdapter("paymentIcon")
fun setPaymentModeIcon(view: ImageView, paymentMode: PaymentMode?) {
    paymentMode?.let {
        view.setImageResource(it.iconResId)
        view.backgroundTintList = ColorStateList.valueOf(view.context.colorInt(it.colorResId))
    }
}

@BindingAdapter("backgroundTintColor")
fun setBackgroundTintColor(view: ImageView, colorResId: Int?) {
    if (colorResId != null) {
        view.backgroundTintList = ColorStateList.valueOf(view.context.colorInt(colorResId))
    }
}

@BindingAdapter("vehicleTypeBackgroundTint")
fun setVehicleTypeBackgroundTint(view: TextView, color: String?) {
    if (color.isNullOrEmpty()) view.backgroundTintList =
        ColorStateList.valueOf(view.context.colorInt(R.color.brand_color))
    else view.backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
}

@BindingAdapter("circularAvatarUrl", "displayName", requireAll = true)
fun setCircularAvatarUrl(view: ImageView, url: String?, name: String?) {
    if (url.isNullOrEmpty()) {
        val nameChar = name?.first()?.titlecaseChar()?.toString().orEmpty().ifEmpty { "#" }
        val avatarDrawable = TextDrawable.builder()
            .beginConfig()
            .useFont(ResourcesCompat.getFont(view.context, R.font.sf_pro_display_bold))
            .endConfig()
            .buildRound(nameChar, view.context.colorInt(R.color.brand_color))
        view.setImageDrawable(avatarDrawable)
    } else view.load(url) {
        placeholder(R.drawable.img_circular_avatar_placeholder)
        error(R.drawable.img_circular_avatar_placeholder)
        transformations(CircleCropTransformation())
    }
}

@BindingAdapter("imageRes")
fun setIconResource(view: ImageView, @DrawableRes imageResId: Int?) {
    view.setImageResource(imageResId ?: 0)
}