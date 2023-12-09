package com.cab9.driver.ui.booking.sig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.Outcome
import com.cab9.driver.base.RoundedCornerBottomSheetDialogFragment
import com.cab9.driver.databinding.BottomDialogSignatureBinding
import com.cab9.driver.ext.anchorSnack
import com.cab9.driver.ext.toast
import com.cab9.driver.ext.visibility
import com.cab9.driver.ui.booking.detail.BookingDetailViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SignatureBottomDialogFragment : RoundedCornerBottomSheetDialogFragment(), OnClickListener {

    companion object {
        fun newInstance() = SignatureBottomDialogFragment()
    }

    private val binding by viewBinding(BottomDialogSignatureBinding::bind)
    private val viewModel by viewModels<BookingDetailViewModel>(ownerProducer = { requireParentFragment() })

    override val isDraggable: Boolean
        get() = false
    override val isCancelableOnTouch: Boolean
        get() = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = BottomDialogSignatureBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uploadSignatureOutcome.collectLatest {
                        binding.pBarSignatureUpload.visibility(it is Outcome.Progress)
                        binding.btnClearSignature.isEnabled = it !is Outcome.Progress
                        binding.btnCancelSignature.isEnabled = it !is Outcome.Progress
                        binding.btnSaveSignature.isEnabled = it !is Outcome.Progress
                        if (it is Outcome.Success) {
                            if (it.data) {
                                requireContext().toast(getString(R.string.msg_signature_added))
                                dismiss()
                            } else binding.btnSaveSignature.anchorSnack(R.string.err_msg_generic)
                        } else if (it is Outcome.Failure) binding.btnSaveSignature.anchorSnack(it.msg)
                    }

                }
            }
        }

        binding.btnClearSignature.setOnClickListener(this)
        binding.btnCancelSignature.setOnClickListener(this)
        binding.btnSaveSignature.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_clear_signature -> binding.signatureView.clearCanvas()
            R.id.btn_cancel_signature -> dismiss()
            R.id.btn_save_signature -> {
                val bitmap = binding.signatureView.signatureBitmap
                if (bitmap != null) viewModel.uploadSignature(bitmap)
                else binding.btnSaveSignature.anchorSnack(R.string.err_uploading_signature)
            }
        }
    }
}