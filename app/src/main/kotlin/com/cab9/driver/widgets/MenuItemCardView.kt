package com.cab9.driver.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.cab9.driver.R
import com.cab9.driver.databinding.ViewMenuItemBinding
import com.google.android.material.card.MaterialCardView

class MenuItemCardView : MaterialCardView {

    constructor(context: Context) :
            this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val binding = ViewMenuItemBinding.inflate(LayoutInflater.from(context), this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.MenuItemCardView)
        val title = attributes.getString(R.styleable.MenuItemCardView_menuItemTitle)
        val icon = attributes.getDrawable(R.styleable.MenuItemCardView_menuItemIcon)
        attributes.recycle()

        binding.imgMenuItemIcon.setImageDrawable(icon)
        binding.lblMenuItemTitle.text = title
    }
}