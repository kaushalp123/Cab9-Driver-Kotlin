package com.cab9.driver.base

import android.os.Bundle
import android.view.View
import com.cab9.driver.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class RoundedCornerBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.RoundedCornerBottomSheetTheme

    abstract val isDraggable: Boolean

    abstract val isCancelableOnTouch: Boolean

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as? BottomSheetDialog)?.run {
            setCancelable(isCancelable)
            setCanceledOnTouchOutside(isCancelable)
            behavior.isDraggable = isDraggable

            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

}