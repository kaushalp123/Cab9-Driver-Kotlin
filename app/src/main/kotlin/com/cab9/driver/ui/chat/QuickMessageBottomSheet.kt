package com.cab9.driver.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.RoundedCornerBottomSheetDialogFragment
import com.cab9.driver.data.models.Attachments
import com.cab9.driver.data.models.QuickMessages
import com.cab9.driver.databinding.BottomDialogQuickMessageBinding
import com.cab9.driver.databinding.ItemQuickMessageBinding
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ext.toast
import com.cab9.driver.utils.ACTION_QUICK_MESSAGE_DELETED
import com.cab9.driver.utils.ACTION_QUICK_MESSAGE_SELECTED
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickMessageBottomSheet(private var quickMessagesList: MutableList<QuickMessages>) :
    RoundedCornerBottomSheetDialogFragment() {

    companion object {
        fun newInstance(quickMessagesList: MutableList<QuickMessages>) =
            QuickMessageBottomSheet(quickMessagesList)
    }

    private val binding by viewBinding(BottomDialogQuickMessageBinding::bind)

    override val isDraggable: Boolean
        get() = false
    override val isCancelableOnTouch: Boolean
        get() = false

    private lateinit var addQuickMessageDialog: AddQuickMessageDialog

    private val onMessageSelected: (QuickMessages) -> Unit = {
        val intent = Intent(ACTION_QUICK_MESSAGE_SELECTED)
        intent.putExtra("quickMessage", it.body)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
        dismiss()
    }

    private val onMessageDeleted: (QuickMessages) -> Unit = {
        val intent = Intent(ACTION_QUICK_MESSAGE_DELETED)
        intent.putExtra("messageId", it.id)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
        showLoader()
    }


    private lateinit var quickMessagesAdapter: QuickMessagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = BottomDialogQuickMessageBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        quickMessagesAdapter =
            QuickMessagesAdapter(emptyList(), onMessageSelected, onMessageDeleted)

        binding.listQuickMessages.apply {
            addItemDecoration(
                DividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL
                )
            )
            adapter = quickMessagesAdapter
        }

        displayMessages()

        binding.imgAddMessage.setOnClickListener {
            addQuickMessageDialog = AddQuickMessageDialog(requireContext())
            addQuickMessageDialog.show()
        }
    }

    private fun displayMessages() {
        hideLoader()
        if (quickMessagesList.isNotEmpty()) {
            quickMessagesAdapter.updateData(quickMessagesList)
        } else requireContext().toast(getString(R.string.no_quick_messages))
    }

    fun updateMessagesList(newList: MutableList<QuickMessages>) {
        quickMessagesList = newList
        displayMessages()
    }

    fun showLoader() {
        binding.pBarLoadMessages.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        binding.pBarLoadMessages.visibility = View.GONE
    }


    private class QuickMessagesAdapter(
        private var messagesList: List<QuickMessages>,
        private val onItemSelected: (QuickMessages) -> Unit,
        private val onItemDeleted: (QuickMessages) -> Unit
    ) : RecyclerView.Adapter<QuickMessagesAdapter.ViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder = ViewHolder.create(parent, onItemSelected, onItemDeleted)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(messagesList, position)
        }

        override fun getItemCount(): Int = messagesList.size

        fun updateData(newMessageList: List<QuickMessages>) {
            messagesList = newMessageList
            notifyDataSetChanged()
        }

        class ViewHolder(
            private val binding: ItemQuickMessageBinding,
            private val onItemSelected: (QuickMessages) -> Unit,
            private val onItemDeleted: (QuickMessages) -> Unit
        ) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(messagesList: List<QuickMessages>, position: Int) {
                val message = messagesList[position]
                binding.txtQuickMessage.text = message.body?.trim()
                binding.txtQuickMessage.setOnClickListener {
                    onItemSelected.invoke(messagesList[position])
                }

                binding.imgDelete.setOnClickListener {
                    onItemDeleted.invoke(messagesList[position])
                }
            }

            companion object {
                fun create(
                    parent: ViewGroup,
                    onItemSelected: (QuickMessages) -> Unit,
                    onItemDeleted: (QuickMessages) -> Unit
                ) =
                    ViewHolder(
                        ItemQuickMessageBinding.inflate(
                            parent.layoutInflater,
                            parent,
                            false
                        ), onItemSelected, onItemDeleted
                    )
            }
        }
    }
}
