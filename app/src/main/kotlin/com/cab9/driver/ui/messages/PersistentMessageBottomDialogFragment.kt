package com.cab9.driver.ui.messages

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.base.RoundedCornerBottomSheetDialogFragment
import com.cab9.driver.databinding.BottomDialogMessageBinding
import com.cab9.driver.databinding.ItemPersistentMessageBinding
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.ui.home.HomeView
import com.cab9.driver.widgets.decorators.LinePagerIndicatorDecoration
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

typealias Message = Pair<String, String>

@AndroidEntryPoint
class PersistentMessageBottomDialogFragment : RoundedCornerBottomSheetDialogFragment() {

    companion object {
        fun newInstance() = PersistentMessageBottomDialogFragment()
    }

    private val binding by viewBinding(BottomDialogMessageBinding::bind)

    @Inject
    lateinit var sessionManager: SessionManager

    private var homeView: HomeView? = null

    override val isDraggable: Boolean
        get() = false
    override val isCancelableOnTouch: Boolean
        get() = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        homeView = context as? HomeView
    }

    override fun onDetach() {
        homeView = null
        super.onDetach()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = BottomDialogMessageBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messages = sessionManager.savedPersistentMessages
        val listLayoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL, false
        )
        binding.listPersistentMessage.apply {
            adapter = MessageAdapter(messages)
            layoutManager = listLayoutManager
        }
        binding.lblMessageTime.text = messages.firstOrNull()?.second

        if (messages.size > 1) {
            binding.listPersistentMessage
                .addItemDecoration(LinePagerIndicatorDecoration(requireContext()))
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(binding.listPersistentMessage)
            binding.listPersistentMessage.addOnScrollListener(object :
                RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val position: Int = listLayoutManager.findFirstVisibleItemPosition()
                        binding.lblMessageTime.text = messages[position].second
                    }
                }
            })
        }

        binding.btnAckMessage.setOnClickListener {
            sessionManager.clearAllPersistentMessages()
            dismiss()
        }

        binding.btnOpenMessage.setOnClickListener {
            sessionManager.clearAllPersistentMessages()
            homeView?.openChatScreen()
            dismiss()
        }
    }

    private class MessageAdapter constructor(private val messages: List<Message>) :
        RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                ItemPersistentMessageBinding.inflate(
                    parent.layoutInflater,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(messages[position].first)
        }

        override fun getItemCount(): Int = messages.size

        class ViewHolder(private val binding: ItemPersistentMessageBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(message: String) {
                binding.lblMessage.text = message
            }
        }
    }
}