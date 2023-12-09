package com.cab9.driver.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.cab9.driver.R
import com.cab9.driver.databinding.ViewZoneColumnBinding

class ZoneLabelView : LinearLayout {

    private var binding: ViewZoneColumnBinding? = null

    var name: String? = null
        set(value) {
            binding?.lblColumnName?.text = value
            field = value
        }

    var icon: Drawable? = null
        set(value) {
            binding?.imgColumnIcon?.setImageDrawable(value)
            field = value
        }


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
        binding = ViewZoneColumnBinding.inflate(LayoutInflater.from(context), this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ZoneLabelView)
        val title = attributes.getString(R.styleable.ZoneLabelView_zoneLabel)
        val icon = attributes.getDrawable(R.styleable.ZoneLabelView_zoneIcon)
        attributes.recycle()

        gravity = Gravity.CENTER
        orientation = LinearLayout.VERTICAL

        binding?.imgColumnIcon?.setImageDrawable(icon)
        binding?.lblColumnName?.text = title
    }
}