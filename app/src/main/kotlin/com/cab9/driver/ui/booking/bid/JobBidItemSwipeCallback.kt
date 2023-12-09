package com.cab9.driver.ui.booking.bid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.cab9.driver.R
import com.cab9.driver.data.models.BidCategory
import com.cab9.driver.ext.colorInt
import com.cab9.driver.ext.drawable


interface OnJobBidSwipeListener {
    fun onSwipeToBid(position: Int)
    fun onSwipeToArchive(position: Int)
    fun onSwipeToSubmitFromArchive(position: Int)
}

class ItemSwipeCallback(
    context: Context,
    swipeDirs: Int,
    private val bidCategory: BidCategory,
    private val listener: OnJobBidSwipeListener
) : ItemTouchHelper.SimpleCallback(0, swipeDirs) {

    private val textPaint: Paint

    private val archiveText: String
    private val acceptText: String

    private val leftBackgroundColor = ColorDrawable(context.colorInt(R.color.bg_color_swipe_submit))
    private val leftIcon = context.drawable(R.drawable.ic_baseline_wallet_24)

    private val leftIconHeight: Int
    private val leftIconWidth: Int

    private val rightBackgroundColor =
        ColorDrawable(context.colorInt(R.color.bg_color_swipe_archive))
    private val rightIcon = context.drawable(R.drawable.ic_baseline_archive_24)

    private val rightIconHeight: Int
    private val rightIconWidth: Int

    init {
        val boldTypeface = ResourcesCompat.getFont(context, R.font.sf_pro_display_semibold)
        textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            textAlign = Paint.Align.CENTER
            typeface = boldTypeface
        }

        archiveText = context.getString(R.string.action_archive)
        acceptText = context.getString(R.string.action_accept)

        leftIconHeight = leftIcon?.intrinsicHeight ?: 0
        leftIconWidth = leftIcon?.intrinsicWidth ?: 0

        rightIconHeight = rightIcon?.intrinsicHeight ?: 0
        rightIconWidth = rightIcon?.intrinsicWidth ?: 0
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        if (bidCategory == BidCategory.ALL || bidCategory == BidCategory.RECENT || bidCategory == BidCategory.NEAREST) {
            if (direction == ItemTouchHelper.RIGHT) listener.onSwipeToBid(viewHolder.bindingAdapterPosition)
            else if (direction == ItemTouchHelper.LEFT) listener.onSwipeToArchive(viewHolder.bindingAdapterPosition)
        } else if (bidCategory == BidCategory.ARCHIVED) {
            if (direction == ItemTouchHelper.RIGHT) listener.onSwipeToSubmitFromArchive(viewHolder.bindingAdapterPosition)
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {

        val itemView = viewHolder.itemView
        val itemHeight = itemView.height

        val leftIconMargin = (itemHeight - leftIconHeight) / 2
        val leftIconTop = itemView.top + (itemHeight + rightIconHeight) / 3.2.toInt()
        val leftIconBottom = leftIconTop + leftIconHeight

        val rightIconMargin = (itemHeight - rightIconHeight) / 2
        val rightIconTop = itemView.top + (itemHeight + rightIconHeight) / 3.2.toInt()
        val rightIconBottom = rightIconTop + rightIconHeight

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val height = itemView.bottom.toFloat() - itemView.top.toFloat()
            val width = height / 3
            if (dX > 0) {
                val iconLeft = itemView.left + leftIconMargin
                val iconRight = iconLeft + leftIconWidth
                leftIcon?.setBounds(iconLeft, leftIconTop, iconRight, leftIconBottom)
                leftBackgroundColor.setBounds(
                    itemView.left,
                    itemView.top,
                    itemView.left + dX.toInt() + 30,
                    itemView.bottom
                )
                leftBackgroundColor.draw(c)
                leftIcon?.draw(c)
                c.drawText(
                    acceptText,
                    iconLeft.toFloat() + 30,
                    (leftIconBottom + 40).toFloat(),
                    textPaint
                )
            } else if (dX < 0) {
                val iconLeft = itemView.right - rightIconMargin - rightIconWidth
                val iconRight = itemView.right - rightIconMargin
                rightIcon?.setBounds(iconLeft, rightIconTop, iconRight, rightIconBottom)
                rightBackgroundColor.setBounds(
                    itemView.right + dX.toInt() - 30,
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                rightBackgroundColor.draw(c)
                rightIcon?.draw(c)
                c.drawText(
                    archiveText,
                    iconRight.toFloat() - 30,
                    (rightIconBottom + 40).toFloat(),
                    textPaint
                )
            } else { // view is unSwiped
                rightIcon?.setBounds(0, 0, 0, 0)
                leftIcon?.setBounds(0, 0, 0, 0)
                rightBackgroundColor.setBounds(0, 0, 0, 0)
                leftBackgroundColor.setBounds(0, 0, 0, 0)
            }
        }
        //super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}