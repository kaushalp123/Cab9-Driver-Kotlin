package com.cab9.driver.data.repos

import com.cab9.driver.data.models.ChatMessage
import com.cab9.driver.data.models.ConversationDetail
import com.cab9.driver.data.models.Message
import com.cab9.driver.data.models.QuickMessages
import com.cab9.driver.data.models.RetryFailedMessageResponse
import com.cab9.driver.data.models.UploadFileResponse
import com.cab9.driver.di.user.LoggedInScope
import com.cab9.driver.network.apis.ChatAPI
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject

interface ChatRepository {

    suspend fun getMessages(lastMessageId: String?): List<Message>

    suspend fun getConversationDetail(): ConversationDetail

    suspend fun uploadFiles(
        conversationId: String,
        byteArray: ByteArray,
        extension: String
    ): List<UploadFileResponse>

    suspend fun getQuickMessages() : List<QuickMessages>

    suspend fun addNewQuickMessages(message: String) : Boolean

    suspend fun deleteQuickMessages(message: String) : Boolean?

    suspend fun retryFailedMessage(message: ChatMessage) : RetryFailedMessageResponse
}

@LoggedInScope
class ChatRepositoryImpl @Inject constructor(private val chatApi: ChatAPI) : ChatRepository {

    override suspend fun getMessages(lastMessageId: String?): List<Message> {
        val result = chatApi.getMessages(lastMessageId)
        return result.messages.orEmpty()
    }

    override suspend fun uploadFiles(conversationId: String, byteArray: ByteArray, extension: String): List<UploadFileResponse> {
        val imagesList: MutableList<MultipartBody.Part> = mutableListOf()

        val filePartImage = MultipartBody.Part.createFormData(
            "files",
            "image.$extension",
            byteArray.toRequestBody("image/$extension".toMediaTypeOrNull())
        )
        imagesList.add(filePartImage)
        return chatApi.uploadFiles(mapOf("conversationId" to conversationId), imagesList)
    }

    override suspend fun getQuickMessages(): List<QuickMessages> {
        return chatApi.getQuickMessages()
    }

    override suspend fun addNewQuickMessages(message: String): Boolean {
        val result = chatApi.addNewQuickMessages(mapOf("message" to message))
        return result.isSuccessful
    }

    override suspend fun deleteQuickMessages(messageId: String): Boolean? {
        val result = chatApi.deleteQuickMessages(messageId)
        return result.isSuccess
    }

    override suspend fun retryFailedMessage(message: ChatMessage): RetryFailedMessageResponse {
        val map = mutableMapOf<String, ChatMessage>()
        map["message"] = message
        Timber.i("map:$map")
        return chatApi.retryFailedMessages(map)
    }

    override suspend fun getConversationDetail(): ConversationDetail {
        return chatApi.getConversationDetails()
    }

}