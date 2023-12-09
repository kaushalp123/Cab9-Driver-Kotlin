package com.cab9.driver.ui.chat

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VISIBLE
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.load
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Attachments
import com.cab9.driver.data.models.ChatMessage
import com.cab9.driver.data.models.ImageUploadClass
import com.cab9.driver.data.models.MessageStatus
import com.cab9.driver.data.models.MessageType
import com.cab9.driver.data.models.MessageUiModel
import com.cab9.driver.data.models.Participant
import com.cab9.driver.data.models.QuickMessages
import com.cab9.driver.data.models.UploadFileResponse
import com.cab9.driver.data.models.UserTyping
import com.cab9.driver.databinding.FragmentChatBinding
import com.cab9.driver.databinding.ItemMessageReceivedBinding
import com.cab9.driver.databinding.ItemMessageSentBinding
import com.cab9.driver.databinding.ItemMessageSeparatorBinding
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.colorInt
import com.cab9.driver.ext.hideKeyboard
import com.cab9.driver.ext.iconTint
import com.cab9.driver.ext.isPermissionGranted
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ext.localBroadcastManager
import com.cab9.driver.ext.onLayoutChangeListener
import com.cab9.driver.ext.setHtmlText
import com.cab9.driver.ext.text
import com.cab9.driver.ext.toast
import com.cab9.driver.services.SocketEvent
import com.cab9.driver.services.SocketManager
import com.cab9.driver.ui.chat.adapter.SelectedMediaPreviewRecyclerAdapter
import com.cab9.driver.utils.ACTION_NEW_QUICK_MESSAGE_ADDED
import com.cab9.driver.utils.ACTION_NEW_QUICK_MESSAGE_REQUEST
import com.cab9.driver.utils.ACTION_QUICK_MESSAGE_DELETED
import com.cab9.driver.utils.ACTION_QUICK_MESSAGE_SELECTED
import com.cab9.driver.utils.compressImage
import com.cab9.driver.widgets.drawables.TextDrawable
import com.squareup.moshi.Moshi
import com.sumup.base.common.extensions.isShowing
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.json.JSONException
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class ChatFragment : BaseFragment(R.layout.fragment_chat), View.OnClickListener {

    private val viewModel by viewModels<ChatViewModel>()
    private val binding by viewBinding(FragmentChatBinding::bind)

    private val imageUploadList = mutableListOf<ImageUploadClass>()

    private var quickMessagesList: MutableList<QuickMessages> = mutableListOf()
    private lateinit var quickMessageBottomSheet: QuickMessageBottomSheet
    private var participantsList: MutableList<Participant> = mutableListOf()
    private var lastTypingEventSentTime: Long = 0L
    private var failedMessage: MutableList<ChatMessage?> = mutableListOf()
    private var isConversationDataLoaded = false

    companion object {
        private const val TAG_QUICK_MESSAGE_DIALOG = "quick_message_dialog"
    }

    private val fileChooserPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            listOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        else listOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)

    private val onAttachmentItemRemoved: (Int) -> Unit = { itemPosition ->
        selectedMediaPreviewRecyclerAdapter.removeAttachment(itemPosition)
        viewModel.onAttachmentRemoved(imageUploadList[itemPosition].uploadFileResponse?.id)
        if (binding.attachmentList.size < 0) binding.attachmentList.visibility = View.GONE
        enableSendButton()
    }

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { result ->
            if (result.isNotEmpty()) {

                val newList = mutableListOf<ImageUploadClass>()

                for (item in result) {
                    val imageUploadClass = ImageUploadClass(imageUri = item, isUploading = true, null)
                    newList.add(imageUploadClass)
                    imageUploadList.add(imageUploadClass)
                }

                if (binding.attachmentList.adapter == null) {
                    // Create a new adapter if it doesn't exist
                    binding.attachmentList.adapter = SelectedMediaPreviewRecyclerAdapter(
                        requireContext(),
                        newList,
                        onAttachmentItemRemoved
                    )
                } else {
                    // Append new items to the existing list
                    selectedMediaPreviewRecyclerAdapter.addNewAttachments(newList)
                }
                binding.attachmentList.visibility = View.VISIBLE
                disableSendButton()
                addAttachments(result)
            } else {
                binding.attachmentList.visibility = View.GONE
                binding.attachmentList.adapter = null
            }
        }

    private fun addAttachments(result: List<Uri>?) {
        try {
            result?.let { uris ->
                uris.forEach { uri ->

                    val imageType = requireContext().contentResolver.getType(uri)
                    val extension = imageType?.substringAfter("/") ?: "unknown"

                    val byteImg = compressImage(requireContext(), uri)
                    if (byteImg != null) viewModel.uploadFiles(byteImg, extension)
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
            requireContext().toast(getString(R.string.err_upload_chat_image))
        }
    }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            // if all permission are granted open file chooser dialog
            if (results.all { it.value }) launchPicker()
            else requireContext().toast(R.string.err_no_file_access)
        }

    private val selectedMediaPreviewRecyclerAdapter: SelectedMediaPreviewRecyclerAdapter
        get() = binding.attachmentList.adapter as SelectedMediaPreviewRecyclerAdapter


    @Inject
    lateinit var userComponentManager: UserComponentManager

    @Inject
    lateinit var moshi: Moshi

    private val socketManager: SocketManager
        get() = userComponentManager.socketManager

    private val onImageSelectedListener: (List<Attachments>) -> Unit = {
        showFullScreenImageDialog(it)
    }

    private val onSendRetryClickedListener: (MessageUiModel.Message) -> Unit = {
        retryFailedMessage(it)
    }

    private val quickMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, data: Intent?) {
            // No need to update state fro cancelled bids
            when (data?.action) {
                ACTION_QUICK_MESSAGE_SELECTED -> {
                    val message = data.getStringExtra("quickMessage")
                    populateMessageContentForRetry(message)
                }

                ACTION_QUICK_MESSAGE_DELETED -> {
                    val messageId = data.getStringExtra("messageId")
                    viewModel.deleteQuickMessage(messageId.orEmpty())
                }

                ACTION_NEW_QUICK_MESSAGE_ADDED -> {
                    val message = data.getStringExtra("message")
                    viewModel.addNewQuickMessage(message.orEmpty())
                    if (isAddMessageBottomSheetActive()) quickMessageBottomSheet.showLoader()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = MessagePagingAdapter(onImageSelectedListener, onSendRetryClickedListener)
        binding.messageList.adapter = adapter

        val layoutManager = binding.messageList.layoutManager
        if (layoutManager is LinearLayoutManager) {
            // setting these two properties programmatically to fix the issue where recycler view was not scrolling to the bottom when a new item is inserted.
            layoutManager.stackFromEnd = true
            layoutManager.reverseLayout = true
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // added this layout change listener for setting the recyclerview item visible on top of the keyboard.
                launch {
                    binding.messageList.onLayoutChangeListener().collectLatest {
                        if(it.first < it.second) binding.messageList.scrollBy(0, it.second - it.first)
                    }
                }

                /*launch {
                    binding.messageList.OnScrollListener().collectLatest {
                        if(layoutManager is LinearLayoutManager) {
                            val position = layoutManager.findFirstVisibleItemPosition()
                            if(it == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                                if(position != 0) binding.floatingImgScrollDown.visibility = VISIBLE
                            } else binding.floatingImgScrollDown.visibility = View.GONE
                        }
                    }
                }*/

                launch {
                    viewModel.participantsOutcomeResult.collectLatest {
                        binding.chatListContainer.setState(it)
                        if (it is Outcome.Success) {
                            isConversationDataLoaded = true
                            participantsList.addAll(it.data)
                        }
                    }
                }

                launch {
                    socketManager.socketCallback.collectLatest { event ->
                        val color =
                            if (event is SocketEvent.Error || event is SocketEvent.Disconnected) R.color.bg_offer_reject else R.color.bg_offer_accept
                        DrawableCompat.setTint(
                            binding.socketStatusIndicatorImg.drawable,
                            ContextCompat.getColor(requireContext(), color)
                        )

                        when (event) {
                            is SocketEvent.ChatMessage -> {
                                viewModel.onNewMessageReceived(event.message)
                                enableSendButton()
                            }

                            is SocketEvent.UserTypingEvent -> handleUserTypingEvent(event.userTyping)

                            else -> Timber.i("different event")
                        }
                    }
                }

                launch {
                    viewModel.chatPagingFlow.collectLatest {
                        try {
                            adapter.submitData(it)
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }


                launch {
                    adapter.loadStateFlow.collectLatest { loadStates ->
                        Timber.i("loadStates:$loadStates")
                        binding.chatListContainer.setState(loadStates, adapter)
                        binding.retryButtonAppend.isVisible = loadStates.mediator?.append is LoadState.Error

                        // hiding the loader on retry button when the retry process is over, this loader doesn't need to be shown on the normal loading,
                        // it was made visible only when clicked on retry button
                        if(loadStates.mediator?.append is LoadState.NotLoading)  binding.progressBarAppend.visibility = View.GONE

                        if(loadStates.mediator?.append is LoadState.Error) {
                            Timber.i("append failed:${loadStates.append}")
                        } else if (loadStates.refresh is LoadState.NotLoading) {
                            viewModel.sendMessageReadStatus() // send read status when all data is loaded and load state changes to not loading.
                        }
                    }
                }

                launch {
                    adapter.loadStateFlow
                        // Use a state-machine to track LoadStates such that we only transition to
                        // NotLoading from a RemoteMediator load if it was also presented to UI.
                        // Only emit when REFRESH changes, as we only want to react on loads replacing the
                        // list.
                        .distinctUntilChangedBy { it.refresh }
                        // Only react to cases where REFRESH completes i.e., NotLoading.
                        .filter { it.refresh is LoadState.NotLoading }
                        // Scroll to top is synchronous with UI updates, even if remote load was triggered.
                        .collect { binding.messageList.scrollToPosition(0) }
                }

                launch {
                    viewModel.uploadOutcome.collectLatest {
                        if (it is UploadFileResponse.Response.Failure) {
                            requireContext().toast("failed to upload image")
                            imageUploadList.clear()
                            binding.attachmentList.visibility = View.GONE
                            binding.attachmentList.adapter = null
                        } else {
                            imageUploadList.firstOrNull { item ->
                                item.uploadFileResponse == null
                            }?.apply {
                                uploadFileResponse = it?.result?.get(0)
                                isUploading = false
                            }

                            if (imageUploadList.all { items -> !items.isUploading }) {
                                enableSendButton()
                            }
                            binding.attachmentList.adapter?.notifyDataSetChanged()
                        }
                    }
                }

                launch {
                    viewModel.quickMessagesOutcome.collectLatest { quickMessage ->
                        if (quickMessage is Outcome.Success) {
                            clearQuickMessages()
                            quickMessagesList.addAll(quickMessage.data)
                            if (isAddMessageBottomSheetActive()) quickMessageBottomSheet.updateMessagesList(
                                quickMessagesList
                            )
                        }
                    }
                }

                launch {
                    viewModel.addQuickMessagesOutcome.collectLatest {
                        if (it is Outcome.Success) {
                            Timber.i("add val success:" + it.data)
                            viewModel.getQuickMessages()
                        } else if (it is Outcome.Failure) {
                            requireContext().toast(requireContext().getString(R.string.err_message_adding))
                        }
                    }
                }

                launch {
                    viewModel.deleteQuickMessagesOutcome.collectLatest {
                        if (it is Outcome.Success) {
                            if(it.data)  viewModel.getQuickMessages()
                            else requireContext().toast(requireContext().getString(R.string.err_message_deleting))
                        } else if (it is Outcome.Failure) {
                            requireContext().toast(requireContext().getString(R.string.err_message_deleting))
                        }
                    }
                }

                launch {
                    socketManager.failedMessageFlow.collectLatest { message ->
                        if (message.length() != 0) {
                            with(message) {
                                try {
                                    val failedMsg = moshi.adapter(ChatMessage::class.java)
                                        .fromJson(this.toString())
                                    failedMessage.add(failedMsg)
                                    val refId = failedMsg?.RefId as String
                                    viewModel.updateMessageAsFailed(refId)
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }

                                enableSendButton()
                            }
                        }
                    }
                }

                launch {
                    viewModel.retrySendingOutcome.collectLatest { outCome ->
                        when (outCome) {
                            is Outcome.Success -> {
                                enableSendButton()
                            }

                            is Outcome.Failure -> {
                                enableSendButton()
                                adapter.notifyDataSetChanged()
                                requireActivity().hideKeyboard()
                                requireContext().toast(getString(R.string.err_message_retry))
                            }

                            is Outcome.Progress -> {
                                disableSendButton()
                            }

                            else -> Timber.i("not a retry outcome")
                        }
                    }
                }
            }
        }

        binding.imgSendMsg.setOnClickListener(this)
        binding.imgAttach.setOnClickListener(this)
        binding.imgQuickMessage.setOnClickListener(this)
        binding.retryButtonAppend.setOnClickListener {
            binding.progressBarAppend.visibility = VISIBLE
            adapter.retry()
        }
        binding.chatListContainer.onRetryListener {
            viewModel.fetchConversationDetail()
        }

       /* binding.floatingImgScrollDown.setOnClickListener{
            if (layoutManager is LinearLayoutManager) {
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (firstVisibleItemPosition != RecyclerView.NO_POSITION && firstVisibleItemPosition != 0) {
                    //layoutManager.scrollToPosition(0)
                    binding.messageList.smoothScrollToPosition(0)
                }
            }
        }*/

        binding.etMessageContent.doOnTextChanged { text, _, _, _ ->
            if (text?.isNotEmpty() == true) {
                if (lastTypingEventSentTime == 0L || System.currentTimeMillis() - lastTypingEventSentTime > 2000) {
                    viewModel.sendUserTypingEvent()
                    lastTypingEventSentTime = System.currentTimeMillis()
                }
            }
        }

        adapter.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                // scroll to first item when a new message is loaded only if the list is at the bottom of the screen i.e when latest item visible.
                if (layoutManager is LinearLayoutManager) {
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    if (firstVisibleItemPosition != RecyclerView.NO_POSITION) {
                        // The adapter position of the first visible item
                        // Do something with firstVisibleItemPosition
                        Timber.i("firstVisibleItemPosition:$firstVisibleItemPosition")
                        if (firstVisibleItemPosition == 0) {
                            layoutManager.scrollToPosition(0)
                        }
                    }
                }
            }
        })

    }

    override fun onStart() {
        super.onStart()
        requireContext().localBroadcastManager.registerReceiver(
            quickMessageReceiver,
            IntentFilter().apply {
                addAction(ACTION_NEW_QUICK_MESSAGE_REQUEST)
                addAction(ACTION_QUICK_MESSAGE_SELECTED)
                addAction(ACTION_NEW_QUICK_MESSAGE_ADDED)
                addAction(ACTION_QUICK_MESSAGE_DELETED)
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if(!isConversationDataLoaded)  viewModel.fetchConversationDetail()// fetching the conversation details again from beginning if the conversation details are not loaded.
    }

    override fun onStop() {
        super.onStop()
        requireContext().localBroadcastManager.unregisterReceiver(quickMessageReceiver)
        clearQuickMessages()
    }

    private fun clearQuickMessages() {
        if (quickMessagesList.isNotEmpty()) quickMessagesList.clear()
    }

    private fun sendChatMessage(text: String) {
        clearMessageContent()
        imageUploadList.clear()
        binding.attachmentList.adapter = null
        binding.attachmentList.visibility = View.GONE
        disableSendButton()
        viewModel.sendNewMessage(text)
    }

    private fun isAddMessageBottomSheetActive() =
        ::quickMessageBottomSheet.isInitialized && quickMessageBottomSheet.isShowing()

    private class MessagePagingAdapter(
        private val onImageSelectedListener: (List<Attachments>) -> Unit,
        private val onItemFailedListener: (MessageUiModel.Message) -> Unit
    ) :
        PagingDataAdapter<MessageUiModel, ViewHolder>(MESSAGE_CALLBACK) {

        companion object {
            private val MESSAGE_CALLBACK = object : DiffUtil.ItemCallback<MessageUiModel>() {
                override fun areContentsTheSame(
                    oldItem: MessageUiModel,
                    newItem: MessageUiModel
                ): Boolean = oldItem == newItem

                override fun areItemsTheSame(
                    oldItem: MessageUiModel,
                    newItem: MessageUiModel
                ): Boolean = when {
                    oldItem is MessageUiModel.Message && newItem is MessageUiModel.Message -> oldItem.id == newItem.id
                    oldItem is MessageUiModel.Separator && newItem is MessageUiModel.Separator -> oldItem.title == newItem.title
                    else -> false
                }

            }
        }

        override fun getItemViewType(position: Int): Int = when (val message = getItem(position)) {
            is MessageUiModel.Message -> {
                if (message.type == MessageType.SENDER) R.layout.item_message_sent
                else R.layout.item_message_received
            }

            else -> R.layout.item_message_separator
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            when (viewType) {
                R.layout.item_message_received -> ReceivedViewHolder.create(
                    parent,
                    onImageSelectedListener
                )

                R.layout.item_message_sent -> SentViewHolder.create(
                    parent,
                    onImageSelectedListener,
                    onItemFailedListener
                )

                else -> SeparatorViewHolder.create(parent)
            }


        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            getItem(position)?.let {
                when (it) {
                    is MessageUiModel.Message -> {
                        if (it.type == MessageType.SENDER) (holder as SentViewHolder).bind(it)
                        else (holder as ReceivedViewHolder).bind(it)
                    }

                    is MessageUiModel.Separator -> (holder as SeparatorViewHolder).bind(it)
                }
            }
        }

        class ReceivedViewHolder(
            private val binding: ItemMessageReceivedBinding,
            private val onItemSelectedListener: (List<Attachments>) -> Unit
        ) :
            ViewHolder(binding.root) {
            fun bind(message: MessageUiModel.Message) {
                binding.lblReceivedText.setHtmlText(message.text)
                binding.lblReceivedText.visibility =
                    if (message.text.isEmpty()) View.GONE else View.VISIBLE
                binding.lblReceivedTime.text = message.time
                binding.lblReceivedUserName.text = message.name

                val nameChar = message.name.firstOrNull()?.titlecaseChar()?.toString().orEmpty()
                    .ifEmpty { "M" }

                binding.imgReceivedUserAvatar.load(message.avatarUrl) {
                    placeholder(R.drawable.img_circular_avatar_placeholder)
                    error(
                        TextDrawable.builder().buildRound(
                            nameChar,
                            binding.root.context.colorInt(R.color.brand_color)
                        )
                    )
                }

                if (!message.attachments.isNullOrEmpty()) {
                    val noOfAttachments = message.attachments.size
                    val attachments = message.attachments

                    binding.llReceivedImages.visibility =
                        if (noOfAttachments > 0) View.VISIBLE else View.GONE
                    binding.layoutMoreImages.visibility =
                        if (noOfAttachments > 1) View.VISIBLE else View.GONE


                    if (noOfAttachments > 0) {
                        loadImage(binding.receivedImage1, 0, attachments)
                        binding.receivedImage1.setOnClickListener {
                            onItemSelectedListener.invoke(
                                message.attachments
                            )
                        }
                    }

                    if (noOfAttachments > 1) {
                        loadImage(binding.receivedImage2, 1, attachments)
                        binding.receivedImage2.setOnClickListener {
                            onItemSelectedListener.invoke(
                                message.attachments
                            )
                        }
                    }

                    if (noOfAttachments > 2) {
                        binding.lblNoOfMoreImages.visibility = View.VISIBLE
                        binding.lblNoOfMoreImages.text = "+${noOfAttachments - 1}"
                    } else binding.lblNoOfMoreImages.visibility = View.GONE

                } else {
                    binding.llReceivedImages.visibility = View.GONE
                    binding.layoutMoreImages.visibility = View.GONE
                }
            }

            companion object {
                fun create(
                    parent: ViewGroup,
                    onItemSelectedListener: (List<Attachments>) -> Unit
                ) =
                    ReceivedViewHolder(
                        ItemMessageReceivedBinding.inflate(
                            parent.layoutInflater,
                            parent,
                            false
                        ),
                        onItemSelectedListener
                    )

                fun loadImage(imageView: ImageView, index: Int, attachments: List<Attachments>) {
                    imageView.load(attachments[index].thumbnailUrl) {
                        placeholder(R.drawable.img_chat_files_placeholder)
                    }
                }
            }
        }

        class SentViewHolder(
            private val binding: ItemMessageSentBinding,
            private val onItemSelectedListener: (List<Attachments>) -> Unit,
            private val onSendRetryClickedListener: (MessageUiModel.Message) -> Unit
        ) :
            ViewHolder(binding.root) {
            fun bind(message: MessageUiModel.Message) {
                binding.lblSentText.setHtmlText(message.text)
                binding.lblSentText.visibility =
                    if (message.text.isEmpty()) View.GONE else View.VISIBLE
                binding.lblSentTime.text = message.time
                binding.lblSentUserName.setText(R.string.you)

                binding.imgFailedIcon.isVisible = message.isFailed && message.messageStatus != MessageStatus.SENDING
                binding.pBarRetrySend.isVisible = message.messageStatus == MessageStatus.SENDING

                val nameChar = message.name.firstOrNull()?.titlecaseChar()?.toString().orEmpty()
                    .ifEmpty { "M" }

                binding.imgSentUserAvatar.load(message.avatarUrl) {
                    placeholder(R.drawable.img_circular_avatar_placeholder)
                    error(
                        TextDrawable.builder().buildRound(
                            nameChar,
                            binding.root.context.colorInt(R.color.brand_color)
                        )
                    )
                }

                binding.imgFailedIcon.apply {
                    setOnClickListener {
                        binding.imgFailedIcon.visibility = View.GONE
                        binding.pBarRetrySend.visibility = VISIBLE
                        onSendRetryClickedListener.invoke(message)
                    }
                }

                if (!message.attachments.isNullOrEmpty()) {
                    val noOfAttachments = message.attachments.size
                    val attachments = message.attachments

                    binding.llSentImages.visibility =
                        if (noOfAttachments > 0) View.VISIBLE else View.GONE
                    binding.layoutMoreImages.visibility =
                        if (noOfAttachments > 1) View.VISIBLE else View.GONE

                    if (noOfAttachments > 0) {
                        loadImage(binding.sentImage1, 0, attachments)
                        binding.sentImage1.setOnClickListener {
                            onItemSelectedListener.invoke(
                                message.attachments
                            )
                        }
                    }

                    if (noOfAttachments > 1) {
                        loadImage(binding.sentImage2, 1, attachments)
                        binding.sentImage2.setOnClickListener {
                            onItemSelectedListener.invoke(
                                message.attachments
                            )
                        }
                    }

                    if (noOfAttachments > 2) {
                        binding.lblNoOfMoreImages.visibility = View.VISIBLE
                        binding.lblNoOfMoreImages.text = "+${noOfAttachments - 1}"
                    } else binding.lblNoOfMoreImages.visibility = View.GONE

                } else {
                    binding.llSentImages.visibility = View.GONE
                    binding.layoutMoreImages.visibility = View.GONE
                }
            }

            companion object {
                fun create(
                    parent: ViewGroup,
                    onItemSelectedListener: (List<Attachments>) -> Unit,
                    onSendRetryClickedListener: (MessageUiModel.Message) -> Unit
                ) =
                    SentViewHolder(
                        ItemMessageSentBinding.inflate(
                            parent.layoutInflater,
                            parent,
                            false
                        ),
                        onItemSelectedListener,
                        onSendRetryClickedListener
                    )

                fun loadImage(imageView: ImageView, index: Int, attachments: List<Attachments>) {
                    imageView.load(attachments[index].thumbnailUrl) {
                        placeholder(R.drawable.img_chat_files_placeholder)
                    }
                }
            }
        }

        class SeparatorViewHolder(private val binding: ItemMessageSeparatorBinding) :
            ViewHolder(binding.root) {
            fun bind(message: MessageUiModel.Separator) {
                binding.lblSeparatorTitle.setHtmlText(message.title)
            }

            companion object {
                fun create(parent: ViewGroup) =
                    SeparatorViewHolder(
                        ItemMessageSeparatorBinding.inflate(
                            parent.layoutInflater,
                            parent,
                            false
                        )
                    )
            }
        }

    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.img_attach -> checkFilesPermission()

            R.id.img_quick_message -> loadQuickMessageBottomSheet()

            R.id.img_send_msg -> {
                if (binding.imgSendMsg.isEnabled) {
                    val text = binding.etMessageContent.text()
                    if (text.isNotEmpty() || imageUploadList.isNotEmpty()) {
                        sendChatMessage(text)
                    } else requireContext().toast(getString(R.string.err_chat_message_empty))
                } else requireContext().toast(getString(R.string.err_chat_image_upload_in_progress))
            }
        }
    }

    private fun enableSendButton() {
        binding.imgSendMsg.isEnabled = true
        binding.imgSendMsg.iconTint(R.color.brand_color)
    }

    private fun disableSendButton() {
        binding.imgSendMsg.isEnabled = false
        binding.imgSendMsg.iconTint(R.color.inactive_chat_send_button)
    }

    private fun clearMessageContent() {
        binding.etMessageContent.text?.clear()
    }

    private fun populateMessageContentForRetry(message: String?) {
        Timber.i("message:" + message)
        if (!message.isNullOrEmpty()) {
            binding.etMessageContent.apply {
                setText(message)
                setSelection(length())
            }
        }
    }

    private fun checkFilesPermission() {
        if (!requireContext().isPermissionGranted(fileChooserPermissions)) requestPermissions.launch(
            fileChooserPermissions.toTypedArray()
        ) else launchPicker()
    }

    private fun launchPicker() {
        filePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun showFullScreenImageDialog(attachments: List<Attachments>) {
        val imgDialog = object : FullScreenImageDialog(requireContext(), attachments) {}
        imgDialog.show()
    }

    private fun loadQuickMessageBottomSheet() {
        quickMessageBottomSheet = QuickMessageBottomSheet.newInstance(quickMessagesList)
        quickMessageBottomSheet.show(parentFragmentManager, TAG_QUICK_MESSAGE_DIALOG)
    }

    private fun handleUserTypingEvent(userTyping: UserTyping) {
        val user = participantsList.find { it.id == userTyping.userId }
        user?.name?.let { displayUserTypingView(it) }
    }

    private fun displayUserTypingView(name: String) {
        lifecycleScope.launch {
            binding.lblUserTyping.text = getString(R.string.label_user_typing, name)
            binding.typingAnimLayout.visibility = VISIBLE
            delay(3000L)
            binding.typingAnimLayout.visibility = View.GONE
        }
    }

    private fun retryFailedMessage(message: MessageUiModel.Message) {
        val msg = failedMessage.find { it?.RefId ==  message.refId}
        viewModel.updateMessageSendingStatusToSending(msg?.RefId as String)
        viewModel.retryFailedMessage(msg)
    }

}