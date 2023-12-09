package com.cab9.driver.ui.account.payments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.cab9.driver.R
import com.cab9.driver.base.FragmentX
import com.cab9.driver.base.Outcome
import com.cab9.driver.databinding.FragmentPaymentSummaryBinding
import com.cab9.driver.ext.snack
import com.cab9.driver.utils.openPdfFile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class PaymentSummaryFragment :
    FragmentX<FragmentPaymentSummaryBinding>(R.layout.fragment_payment_summary) {

    private val args by navArgs<PaymentSummaryFragmentArgs>()
    private val viewModel by viewModels<PaymentViewModel>()

    private var pdfFile: File? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.payment = args.paymentSummary

        binding.btnViewPdf.setOnClickListener {
            if (pdfFile == null) viewModel.getPaymentDocument(args.paymentSummary)
            else openPdfFile(requireContext(), pdfFile!!)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.paymentDocumentOutcome.collect {
                    binding.btnViewPdf.isEnabled = it !is Outcome.Progress
                    if (it is Outcome.Success && it.data != null) {
                        pdfFile = it.data
                        openPdfFile(requireContext(), it.data)
                    } else if (it is Outcome.Failure) binding.root.snack(it.msg)
                }
            }
        }
    }
}