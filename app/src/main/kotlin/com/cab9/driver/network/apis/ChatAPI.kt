package com.cab9.driver.network.apis

import com.cab9.driver.data.models.ChatMessage
import com.cab9.driver.data.models.Conversation
import com.cab9.driver.data.models.ConversationDetail
import com.cab9.driver.data.models.GenericResponse
import com.cab9.driver.data.models.QuickMessages
import com.cab9.driver.data.models.RetryFailedMessageResponse
import com.cab9.driver.data.models.UploadFileResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ChatAPI {

    @GET("v2.0/driver-api/chat/messages")
    suspend fun getMessages(@Query("lastMessageId") lastMessageId: String?): Conversation

    @GET("v2.0/driver-api/chat/conversation-details")
    suspend fun getConversationDetails(): ConversationDetail

    @Multipart
    @POST("v2.0/driver-api/chat/upload-files")
    suspend fun uploadFiles(
        @QueryMap conversationId: Map<String, String?>,
        @Part image: List<MultipartBody.Part>
    ): List<UploadFileResponse>

    @GET("v2.0/driver-api/chat/pre-written-message")
    suspend fun getQuickMessages(): List<QuickMessages>

    @POST("v2.0/driver-api/chat/pre-written-message")
    suspend fun addNewQuickMessages(@Body message: Map<String, String>): Response<ResponseBody>

    @DELETE("v2.0/driver-api/chat/pre-written-message/{id}")
    suspend fun deleteQuickMessages(@Path("id") messageId: String): GenericResponse

    @POST("v2.0/driver-api/chat/message")
    suspend fun retryFailedMessages(@Body message: Map<String, ChatMessage>) : RetryFailedMessageResponse

}