package com.cab9.driver.ui.booking.bid.auction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.RoundedCornerBottomSheetDialogFragment
import com.cab9.driver.databinding.BottomDialogSubmitBidBinding
import com.cab9.driver.ext.*
import com.cab9.driver.utils.*
import timber.log.Timber

class JobBidAuctionBottomDialogFragment : RoundedCornerBottomSheetDialogFragment(),
    OnClickListener {

    private val binding by viewBinding(BottomDialogSubmitBidBinding::bind)
    private val args by navArgs<JobBidAuctionBottomDialogFragmentArgs>()

    override val isDraggable: Boolean
        get() = false

    override val isCancelableOnTouch: Boolean
        get() = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = BottomDialogSubmitBidBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.bookingAmount > BALANCE_ZERO) {
            binding.txtBidAuctionAmount.setText(args.bookingAmount.toString())
            binding.lblBidActualAmount.text =
                getString(
                    R.string.temp_bid_amount,
                    prefixCurrency(requireContext(), args.bookingAmount.toDouble())
                )
        } else {
            binding.txtBidAuctionAmount.setText(getString(R.string.hint_min_amount))
            binding.lblBidActualAmount.text = getString(R.string.label_enter_amount)
        }

        binding.btnCancelSubmitBid.setOnClickListener(this)
        binding.btnConfirmSubmitBid.setOnClickListener(this)
        binding.imgAddAmount.setOnClickListener(this)
        binding.imgMinusAmount.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_cancel_submit_bid -> dismiss()
            R.id.img_add_amount -> {
                binding.txtBidAuctionAmount.text().toDoubleOrNull()?.let { amount ->
                    binding.txtBidAuctionAmount.setText((amount + 1).toString())
                }
            }
            R.id.img_minus_amount -> {
                binding.txtBidAuctionAmount.text().toDoubleOrNull()?.let { amount ->
                    if (amount > 1) binding.txtBidAuctionAmount.setText((amount - 1).toString())
                }
            }
            R.id.btn_confirm_submit_bid -> {
                val enteredBidAmount = binding.txtBidAuctionAmount.text().toDoubleOrNull()
                if (enteredBidAmount != null) {
                    if (enteredBidAmount <= args.bookingAmount) onSubmitBidAmount(enteredBidAmount)
                    else context?.toast(R.string.err_bid_amount_higher)
                } else context?.toast(R.string.err_invalid_bid_amount)
            }
        }
    }

    private fun onSubmitBidAmount(amount: Double) {
        setFragmentResult(
            REQUEST_KEY_SUBMIT_AUCTION_BID,
            bundleOf(
                BUNDLE_KEY_ENTERED_BID_AMOUNT to amount,
                BUNDLE_BID_BOOKING_ID to args.bookingId,
                BUNDLE_BID_TYPE to args.bidCategory.name
            )
        )
        findNavController().popBackStack()
    }
}