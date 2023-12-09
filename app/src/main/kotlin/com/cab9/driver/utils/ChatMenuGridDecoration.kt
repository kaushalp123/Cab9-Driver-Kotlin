package com.cab9.driver.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cab9.driver.ext.toPx
import java.lang.Math.ceil

class ChatMenuGridDecoration: RecyclerView.ItemDecoration() {

    private val marginBottom = 16.toPx()

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val manager = parent.layoutManager as GridLayoutManager
        val spanCount = manager.spanCount
        val totalItem = manager.itemCount
        val noMarginLine = ceil(totalItem / spanCount.toDouble()).toInt()
        val position = parent.getChildLayoutPosition(view)

        if (position == RecyclerView.NO_POSITION) super.getItemOffsets(outRect, view, parent, state)

        val currentLine = kotlin.math.ceil((position + 1) / spanCount.toDouble()).toInt()
        if (currentLine != noMarginLine) {
            outRect.bottom = marginBottom.toInt()
        }
    }
}