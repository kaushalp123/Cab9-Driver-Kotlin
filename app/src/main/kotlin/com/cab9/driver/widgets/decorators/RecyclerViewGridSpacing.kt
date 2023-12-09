package com.cab9.driver.widgets.decorators

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewGridSpacing : RecyclerView.ItemDecoration {

    private val itemSpacing: Int

    constructor(itemSpacing: Int) {
        this.itemSpacing = itemSpacing
    }

    constructor(
        context: Context,
        @DimenRes itemSpacingResId: Int
    ) : this(context.resources.getDimensionPixelSize(itemSpacingResId))

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect[itemSpacing, itemSpacing - 3, itemSpacing] = itemSpacing
    }

}