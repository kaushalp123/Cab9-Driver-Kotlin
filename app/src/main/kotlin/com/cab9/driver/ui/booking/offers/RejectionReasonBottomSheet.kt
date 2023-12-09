package com.cab9.driver.ui.booking.offers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.base.RoundedCornerBottomSheetDialogFragment
import com.cab9.driver.data.models.RejectionReason
import com.cab9.driver.databinding.BottomDialogRejectionReasonBinding
import com.cab9.driver.databinding.ItemRejectionReasonBinding
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.settings.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RejectionReasonBottomSheet : RoundedCornerBottomSheetDialogFragment() {

    companion object {
        fun newInstance() = RejectionReasonBottomSheet()
    }

    private val binding by viewBinding(BottomDialogRejectionReasonBinding::bind)
    private val bookingOfferViewModel by activityViewModels<BookingOfferViewModel>()

    @Inject
    lateinit var sessionManager: SessionManager

    override val isDraggable: Boolean
        get() = false
    override val isCancelableOnTouch: Boolean
        get() = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = BottomDialogRejectionReasonBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.listRejectionReason.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = RejectionReasonsAdapter(sessionManager.rejectionReasons) { reason ->
                bookingOfferViewModel.apply {
                    rejectOffer(reason.id)
                }
                dismiss()
            }
        }
    }

    private class RejectionReasonsAdapter(
        private val reasonsList: List<RejectionReason>,
        private val onItemClick: ((RejectionReason) -> Unit)
    ) : RecyclerView.Adapter<RejectionReasonsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder = ViewHolder.create(parent)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(reason = reasonsList[position])
            holder.itemView.setOnClickListener {
                onItemClick.invoke(reasonsList[position])
            }
        }

        override fun getItemCount(): Int = reasonsList.size

        class ViewHolder(private val binding: ItemRejectionReasonBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(reason: RejectionReason) {
                binding.txtRejectionReason.text = reason.rejectReason
            }

            companion object {
                fun create(parent: ViewGroup) =
                    ViewHolder(
                        ItemRejectionReasonBinding.inflate(
                            parent.layoutInflater,
                            parent,
                            false
                        )
                    )
            }
        }
    }
}
