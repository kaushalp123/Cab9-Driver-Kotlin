package com.cab9.driver.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.cab9.driver.R
import com.cab9.driver.databinding.ViewErrorBinding
import com.cab9.driver.ext.gone
import com.cab9.driver.ext.show
import com.cab9.driver.ext.visibility

class ErrorView : FrameLayout {

    var errorTitle: String? = null
        set(value) {
            binding.lblErrorTitle.text = value
            binding.lblErrorTitle.visibility(!value.isNullOrEmpty())
            field = value
        }

    var errorSubtitle: String? = null
        set(value) {
            binding.lblErrorSubtitle.text = value
            binding.lblErrorSubtitle.visibility(!value.isNullOrEmpty())
            field = value
        }

    var errorBtnText: String? = null
        set(value) {
            binding.btnError.text = value
            binding.btnError.visibility(!value.isNullOrEmpty())
            field = value
        }

    var errorIcon: Drawable? = null
        set(value) {
            binding.imgErrorIcon.setImageDrawable(value)
            binding.imgErrorIcon.visibility(value != null)
            field = value
        }

    private var onActionBtnClick: (() -> Unit)? = null

    private var _binding: ViewErrorBinding? = null
    private val binding: ViewErrorBinding
        get() = _binding!!

    constructor(context: Context) :
            this(context, null, 0, 0)

    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs, 0, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        _binding = ViewErrorBinding.inflate(LayoutInflater.from(context), this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ErrorView)
        errorTitle = attributes.getString(R.styleable.ErrorView_errorTitle)
        errorSubtitle = attributes.getString(R.styleable.ErrorView_errorSubtitle)
        errorBtnText = attributes.getString(R.styleable.ErrorView_errorBtnText)
        errorIcon = attributes.getDrawable(R.styleable.ErrorView_errorIcon)
        attributes.recycle()

        binding.btnError.setOnClickListener {
            onActionBtnClick?.invoke()
        }
    }

    fun onActionBtnClick(action: () -> Unit) {
        onActionBtnClick = action
    }

    fun showActionBtn() {
        binding.btnError.show()
    }

    fun hideActionBtn() {
        binding.btnError.gone()
    }

}