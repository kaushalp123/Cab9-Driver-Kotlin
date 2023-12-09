package com.cab9.driver.data.models

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cab9.driver.base.Outcome
import com.cab9.driver.network.UTCDateTime
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.threeten.bp.Instant
import java.util.Date

@JsonClass(generateAdapter = true)
data class Conversation(
    @field:Json(name = "conversationId") val id: String?,
    @field:Json(name = "messages") val messages: List<Message>?
)

@JsonClass(generateAdapter = true)
data class Message(
    @field:Json(name = "_id") val id: String?,
    @field:Json(name = "Attachments") val attachments: List<Attachments>?,
    @field:Json(name = "Read") val isRead: Boolean?,
    @field:Json(name = "isDeleted") val isDeleted: Boolean?,
    @field:Json(name = "SenderId") val senderId: String?,
    @field:Json(name = "Body") val text: String?,
    @UTCDateTime @field:Json(name = "DateTime") val dateTime: Instant?,
    @field:Json(name = "NotifyStaff") val notifyStaff: Boolean?,
    @field:Json(name = "TenantId") val tenantId: String?,
    @field:Json(name = "conversationId") val conversationId: String?,
    @field:Json(name = "Type") val type: String?,
)

@JsonClass(generateAdapter = true)
data class Participant(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "UserType") val userType: String?,
    @field:Json(name = "ImageUrl") val imageUrl: String?,
    @field:Json(name = "Name") val name: String?,
    @field:Json(name = "Status") val status: String?,
)

@JsonClass(generateAdapter = true)
data class ConversationDetail(
    @field:Json(name = "_id") val id: String?,
    @field:Json(name = "Participants") val participants: List<Participant>?,
    @field:Json(name = "TenantId") val tenantId: String?,
    @field:Json(name = "ConversationType") val type: String?,
    @field:Json(name = "ConversationSubject") val subject: String?,
    @field:Json(name = "ConversationStatus") val status: String?,
)
class ParticipantDetails(
    @field:Json(name = "_id") val conversationId: String?,
    @field:Json(name = "Participants") val participant: List<Participant>
)

@JsonClass(generateAdapter = true)
data class RetryFailedMessageResponse(
    @field:Json(name = "conversation") val conversation: ConversationDetail,
    @field:Json(name = "message") val message: Message
)

@JsonClass(generateAdapter = true)
data class UploadFileResponse(
    @field:Json(name = "_id") val id: String?,
    @field:Json(name = "ThumbnailURL") val thumbnailUrl: String?
) {
        sealed class Response(val result: List<UploadFileResponse>?, val success: Boolean) {
            data class Success(val response: List<UploadFileResponse>) : Response(response, true)
            data class Failure(val response: Boolean) : Response(null, false)

            companion object {
                fun success(response: List<UploadFileResponse>): Response = Success(response)

                fun failure(response: Boolean): Response = Failure(response)
            }
        }
}

data class ImageUploadClass(
    var imageUri: Uri?,
    var isUploading: Boolean = false,
    var uploadFileResponse: UploadFileResponse?
)

@JsonClass(generateAdapter = true)
data class NewMessage(
    @field:Json(name = "message") val message: Message?,
    @field:Json(name = "conversation") val conversation: ConversationDetail?,
)

@JsonClass(generateAdapter = true)
data class Attachments(
    @field:Json(name = "_id") val id: String?,
    @field:Json(name = "URL") val url: String?,
    @field:Json(name = "Name") val name: String?,
    @field:Json(name = "ThumbnailURL") val thumbnailUrl: String?,
)

@JsonClass(generateAdapter = true)
data class QuickMessages(
    @field:Json(name = "_id") val id: String?,
    @field:Json(name = "DriverId") val driverId: String?,
    @field:Json(name = "Body") val body: String?
)

@JsonClass(generateAdapter = true)
data class UserTyping(
    @field:Json(name="ConversationId") val conversationId: String?,
    @field:Json(name="UserId") val userId: String?,
    @UTCDateTime @field:Json(name="Timestamp") val timeStamp: Instant?,
    @field:Json(name="TenantId") val tenantId: String?,
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    @field:Json(name="ConversationId") val ConversationId: String?,
    @field:Json(name="Body") val Body: String?,
    @field:Json(name="SenderId") val SenderId: String?,
    @field:Json(name="TenantId") val TenantId: String?,
    @field:Json(name="Attachments") val Attachments: MutableList<String>?,
    @field:Json(name="RefId") val RefId: Any?,
    @field:Json(name="DateTime") val DateTime: String?,
    @field:Json(name="Read") val Read: Boolean?
)


@Entity(tableName = "driver_chat")
class ChatMessagesEntity(
    @PrimaryKey (autoGenerate = true) var rowId: Int = 0,
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "ConversationId") val conversationId: String?,
    @ColumnInfo(name = "Body") val body: String?,
    @ColumnInfo(name = "SenderId") val senderId: String?,
    @ColumnInfo(name = "TenantId") val tenantId: String?,
    @ColumnInfo(name = "Attachments") val attachments: MutableList<Attachments>?,
    @ColumnInfo(name = "RefId") val refId: String?,
    @ColumnInfo(name = "DateTime") val dateTime: Instant?,
    @ColumnInfo(name = "time") val savedTime: Date?,
    @ColumnInfo(name = "Read") val read: Boolean?,
    @ColumnInfo(name = "Failed") val isFailed: Boolean = false,
    @ColumnInfo(name = "sendingState") val messageStatus: MessageStatus
)

@Entity(tableName = "remote_keys")
data class RemoteKey(@PrimaryKey val nextKey: String)


enum class MessageType {
    SENDER, RECEIVER;
}

enum class MessageStatus {
    SENT,
    SENDING,
    FAILED,
    RECEIVED
}

sealed class MessageUiModel {

    data class Message(
        val id: String,
        val text: String,
        val name: String,
        val time: String,
        val avatarUrl: String?,
        val dateTime: Instant,
        val type: MessageType,
        val attachments: List<Attachments>?,
        val isFailed : Boolean,
        var messageStatus: MessageStatus,
        val refId: String?
    ) : MessageUiModel()

    data class Separator(val title: String) : MessageUiModel()
}