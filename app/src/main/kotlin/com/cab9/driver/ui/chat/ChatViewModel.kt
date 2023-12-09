package com.cab9.driver.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.cab9.driver.BuildConfig
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Attachments
import com.cab9.driver.data.models.ChatMessage
import com.cab9.driver.data.models.ChatMessagesEntity
import com.cab9.driver.data.models.ConversationDetail
import com.cab9.driver.data.models.MessageType
import com.cab9.driver.data.models.MessageUiModel
import com.cab9.driver.data.models.Participant
import com.cab9.driver.data.models.QuickMessages
import com.cab9.driver.data.models.MessageStatus
import com.cab9.driver.data.models.RetryFailedMessageResponse
import com.cab9.driver.data.models.UploadFileResponse
import com.cab9.driver.data.repos.db.ChatRoomDataBase
import com.cab9.driver.data.repos.db.RemoteMediator
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.EEEE_dd_MMMM
import com.cab9.driver.ext.HH_mm
import com.cab9.driver.ext.ROOM_DB_DATE_SAVE_FORMAT
import com.cab9.driver.ext.emitFailureOverlay
import com.cab9.driver.ext.emitLoadingOverlay
import com.cab9.driver.ext.emitSuccess
import com.cab9.driver.ext.failure
import com.cab9.driver.ext.isSameDay
import com.cab9.driver.ext.loading
import com.cab9.driver.ext.loadingOverlay
import com.cab9.driver.ext.ofPattern
import com.cab9.driver.ext.success
import com.cab9.driver.ext.toJsonString
import com.cab9.driver.settings.SessionManager
import com.google.android.gms.common.api.ApiException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.threeten.bp.Instant
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val userComponentManager: UserComponentManager,
    database: ChatRoomDataBase,
    private val moshi: Moshi
) : ViewModel() {

    private val _participantsOutcomeResult =
        MutableStateFlow<Outcome<List<Participant>>>(Outcome.Empty)
    val participantsOutcomeResult = _participantsOutcomeResult.asStateFlow()

    private var conversationDetail: ConversationDetail? = null

    //private var pagingSource: ChatPagingSource? = null
    private var remoteMediator: RemoteMediator? = null

    private val _uploadOutcome = MutableStateFlow<UploadFileResponse.Response?>(null)
    val uploadOutcome = _uploadOutcome.asStateFlow()

    private val attachmentIdList = mutableListOf<String>()

    private var firstUnreadMessageId: String? = null
    private var lastUnreadMessageId: String? = null

    private val senderId = sessionManager.userId

    private val chatDao = database.chatDao()

    private val tenantId = BuildConfig.TENANT_ID

    private val pager = Pager(
        config = PagingConfig(pageSize = 5, enablePlaceholders = true),
        remoteMediator = RemoteMediator(
            database,
            userComponentManager.chatRepo
        ).also { remoteMediator = it }
    ) {
        chatDao.getMessagePagingSource()
    }

    val chatPagingFlow = _participantsOutcomeResult.flatMapLatest { participants ->
        if (participants is Outcome.Success) {
            pager.flow.map { pagingData ->
                pagingData
                    .map { message ->
                        val participant = participants.data.find { it.id == message.senderId }
                        val displayName =
                            if (participant?.id == sessionManager.userId) sessionManager.displayName
                            else participant?.name ?: "Member"
                        if (message.read == false) {
                            if (lastUnreadMessageId.isNullOrEmpty()) {
                                lastUnreadMessageId = message.id
                            } else {
                                firstUnreadMessageId = message.id
                            }
                        }
                        MessageUiModel.Message(
                            id = message.id,
                            text = message.body.orEmpty(),
                            name = displayName,
                            time = message.dateTime!!.ofPattern(HH_mm).orEmpty(),
                            dateTime = message.dateTime,
                            avatarUrl = participant?.imageUrl,
                            type = if (message.senderId == sessionManager.userId) MessageType.SENDER else MessageType.RECEIVER,
                            attachments = message.attachments,
                            isFailed = message.isFailed,
                            messageStatus = MessageStatus.RECEIVED,
                            refId = message.refId
                        )
                    }
                    .insertSeparators { before: MessageUiModel.Message?, after: MessageUiModel.Message? ->
                        return@insertSeparators if (before == null) {
                            // we're at the begin of the list
                            null
                        } else if (after == null || !isSameDay(
                                before.dateTime,
                                after.dateTime
                            )
                        ) {
                            // date above and below different, show separator
                            MessageUiModel.Separator(
                                before.dateTime.ofPattern(EEEE_dd_MMMM).orEmpty()
                            )
                        } else {
                            // no separator
                            null
                        }
                    }
            }.cachedIn(viewModelScope)
        } else emptyFlow()
    }

    fun fetchConversationDetail() {
        viewModelScope.launch {
            try {
                _participantsOutcomeResult.loading()
                conversationDetail = userComponentManager.chatRepo.getConversationDetail()
                _participantsOutcomeResult.success(conversationDetail?.participants.orEmpty())
            } catch (ex: Exception) {
                Timber.w(ex)
                _participantsOutcomeResult.failure(ex.message.orEmpty(), ex)
            }
        }
    }

    fun onNewMessageReceived(message: JSONObject) {
        try {
            val attachments = message.getJSONArray("Attachments")
            val type = Types.newParameterizedType(List::class.java, Attachments::class.java)
            val adapter = moshi.adapter<List<Attachments>>(type)

            val json = attachments.toString()
            val attachmentsList = adapter.fromJson(json) ?: emptyList()

            val chatMessageObject = message.toChatMessage(attachmentsList)

            viewModelScope.launch {
                try {
                    val incomingRefId = chatMessageObject.refId.orEmpty()
                    if (incomingRefId.isNotEmpty()) {
                        chatDao.getEntityById(incomingRefId)?.let {
                            // Message with the same RefId exists, delete and insert
                            chatDao.deleteSentMessage(incomingRefId)
                        }
                        val insertResult = chatDao.insertNewMessage(chatMessageObject)
                        Timber.i("insertResult:$insertResult")
                    } else chatDao.insertNewMessage(chatMessageObject)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            e.printStackTrace()
        }
    }

    private fun JSONObject.toChatMessage(attachmentsList: List<Attachments>): ChatMessagesEntity {
        val strDateTime: String = getString("DateTime")
        val instant = Instant.parse(strDateTime)

        val dateStr = instant.ofPattern(ROOM_DB_DATE_SAVE_FORMAT).orEmpty()
        val format = SimpleDateFormat(ROOM_DB_DATE_SAVE_FORMAT, Locale.UK)
        val dateObj = format.parse(dateStr)

        val refId = if(!isNull("RefId")) {
             get("RefId").toString()
        } else null

        return ChatMessagesEntity(
            id = getString("_id"),
            conversationId = getString("ConversationId"),
            body = getString("Body"),
            senderId = getString("SenderId"),
            tenantId = getString("TenantId"),
            attachments = attachmentsList as MutableList<Attachments>,
            refId = refId,
            dateTime = instant,
            savedTime = dateObj,
            read = getBoolean("Read"),
            isFailed = false,
            messageStatus = MessageStatus.RECEIVED
        )
    }

    fun onAttachmentRemoved(id : String?) {
        attachmentIdList.removeIf {
            it == id
        }
    }

    fun uploadFiles(byteArray: ByteArray, extension: String) {
        viewModelScope.launch {
            try {
                val result = userComponentManager.chatRepo.uploadFiles(
                    conversationDetail?.id.orEmpty(),
                    byteArray,
                    extension
                )

                _uploadOutcome.value = if (result.isNotEmpty()) {
                    UploadFileResponse.Response.Success(result)
                } else UploadFileResponse.Response.Failure(false)

                for (attachment in result) {
                    attachmentIdList.add(attachment.id.orEmpty())
                }
            } catch (ex: Exception) {
                val response = UploadFileResponse.Response.Failure(false)
                _uploadOutcome.value = response
            }
        }
    }

    fun sendNewMessage(newMessage: String) {
        val currentTime = System.currentTimeMillis()
        Timber.i("currentTime :$currentTime")
        val messageJson = JSONObject(
            mapOf(
                "Attachments" to attachmentIdList,
                "Body" to newMessage,
                "ConversationId" to conversationDetail?.id,
                "DateTime" to Date(),
                "Read" to false,
                "RefId" to currentTime.toString(),
                "SenderId" to senderId,
                "TenantId" to tenantId
            )
        )

        val attachmentsList = mutableListOf<Attachments>()
        if (attachmentIdList.isNotEmpty()) {
            for (i in 0 until attachmentIdList.size) {
                val attachments = Attachments(attachmentIdList[i], null, null, null)
                attachmentsList.add(attachments)
            }
        }

        val chatMessageObject = ChatMessagesEntity(
            id = "",
            conversationId = conversationDetail?.id,
            body = newMessage,
            senderId = senderId,
            tenantId = tenantId,
            attachments = attachmentsList,
            refId = currentTime.toString(),
            dateTime = Instant.now(),
            savedTime = Date(),
            read = false,
            isFailed = false,
            messageStatus = MessageStatus.SENDING
        )
        Timber.i("new message json:${messageJson}")

        viewModelScope.launch {
            val rowId = chatDao.insertNewMessage(chatMessageObject)
            Timber.i("new rowId:$rowId")
        }
        userComponentManager.socketManager.sendNewChatMessage(messageJson, currentTime)
        attachmentIdList.clear()
    }

    fun sendUserTypingEvent() {
        val message = JSONObject(
            mapOf(
                "ConversationId" to conversationDetail?.id,
                "UserId" to senderId,
                "Timestamp" to Date(),
                "TenantId" to tenantId,
            )
        )
        userComponentManager.socketManager.sendUserTypingEvent(message)
    }

    private val _quickMessagesOutcome =
        MutableSharedFlow<Outcome<List<QuickMessages>>>(replay = 0)
    val quickMessagesOutcome = _quickMessagesOutcome.asSharedFlow()

    fun getQuickMessages() {
        viewModelScope.launch {
            try {
                _quickMessagesOutcome.emitLoadingOverlay()
                val result = userComponentManager.chatRepo.getQuickMessages()
                _quickMessagesOutcome.emitSuccess(result)
            } catch (ex: Exception) {
                Timber.w(ex)
                _quickMessagesOutcome.emitFailureOverlay("error", ex)
            }
        }
    }

    private val _addQuickMessagesOutcome = MutableSharedFlow<Outcome<Boolean>>(replay = 0)
    val addQuickMessagesOutcome = _addQuickMessagesOutcome.asSharedFlow()

    fun addNewQuickMessage(message: String) {
        viewModelScope.launch {
            try {
                _addQuickMessagesOutcome.emitLoadingOverlay()
                val result = userComponentManager.chatRepo.addNewQuickMessages(message)
                _addQuickMessagesOutcome.emitSuccess(result)
            } catch (ex: Exception) {
                _addQuickMessagesOutcome.emitFailureOverlay("error", ex)
            }
        }
    }

    private val _deleteQuickMessagesOutcome = MutableSharedFlow<Outcome<Boolean>>(replay = 0)
    val deleteQuickMessagesOutcome = _deleteQuickMessagesOutcome.asSharedFlow()

    fun deleteQuickMessage(messageId: String) {
        viewModelScope.launch {
            try {
                _deleteQuickMessagesOutcome.emitLoadingOverlay()
                val result = userComponentManager.chatRepo.deleteQuickMessages(messageId)
                _deleteQuickMessagesOutcome.emitSuccess(result as Boolean)
            } catch (ex: Exception) {
                _deleteQuickMessagesOutcome.emitFailureOverlay("error", ex)
            }
        }
    }

    fun sendMessageReadStatus() {
        if (firstUnreadMessageId.isNullOrEmpty()) firstUnreadMessageId = lastUnreadMessageId
        val message = JSONObject(
            mapOf(
                "conversationId" to conversationDetail?.id,
                "firstMessageId" to firstUnreadMessageId,
                "lastMessageId" to lastUnreadMessageId,
                "participantId" to senderId,
                "timestamp" to Date()
            )
        )
        userComponentManager.socketManager.sendMessageReadStatusEvent(message)
        firstUnreadMessageId = null
        lastUnreadMessageId = null
    }


    private val _retrySendingOutcome = MutableSharedFlow<Outcome<RetryFailedMessageResponse>>(replay = 0)
    val retrySendingOutcome = _retrySendingOutcome.asSharedFlow()

    fun retryFailedMessage(failedMessage: ChatMessage?) {
        failedMessage?.let {
            val chatMessage = ChatMessage(
                ConversationId = failedMessage.ConversationId,
                Body = failedMessage.Body,
                SenderId = failedMessage.SenderId,
                TenantId = failedMessage.TenantId,
                Attachments = failedMessage.Attachments,
                RefId = failedMessage.RefId,
                DateTime = failedMessage.DateTime,
                Read = failedMessage.Read
            )

            Timber.i("failedMessage:$chatMessage")

            viewModelScope.launch {
                try {
                    _retrySendingOutcome.emitLoadingOverlay()
                    val result = userComponentManager.chatRepo.retryFailedMessage(chatMessage)
                    userComponentManager.socketManager.resetCurrentMessageObject()
                    userComponentManager.socketManager.removeSentMessagesRefId(failedMessage.RefId.toString())
                    chatDao.updateMessageStatusAsSent(failedMessage.RefId.toString(), result.message.attachments.orEmpty())
                    _retrySendingOutcome.emitSuccess(result)
                } catch (e: Exception) {
                    Timber.e(e)
                    updateMessageAsFailed(failedMessage.RefId.toString())
                    _retrySendingOutcome.emitFailureOverlay("Error", e)
                }
            }
        }
    }

    fun updateMessageAsFailed(refId: String?) {
        Timber.i("updateMessageAsFailed:$refId")
        if(!refId.isNullOrEmpty()) {
            viewModelScope.launch {
                chatDao.updateMessageStatusAsFailed(
                    refId, MessageStatus.FAILED
                )
            }
        }
    }

    fun updateMessageSendingStatusToSending(refId: String?) {
        Timber.i("updateMessageSendingStatusToSending:$refId")
        if(!refId.isNullOrEmpty()) {
            viewModelScope.launch {
                chatDao.updateMessageSendingStatusToSending(MessageStatus.SENDING,
                    refId
                )
            }
        }
    }

    init {
        fetchConversationDetail()
        getQuickMessages()
    }

}