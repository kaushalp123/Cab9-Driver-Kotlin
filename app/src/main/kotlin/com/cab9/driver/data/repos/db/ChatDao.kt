package com.cab9.driver.data.repos.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cab9.driver.data.models.Attachments
import com.cab9.driver.data.models.ChatMessagesEntity
import com.cab9.driver.data.models.MessageStatus
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chatMessagesEntity: List<ChatMessagesEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewMessage(chatMessagesEntity: ChatMessagesEntity): Long

    @Query("SELECT * FROM driver_chat WHERE refId = :refId")
    suspend fun getEntityById(refId: String): ChatMessagesEntity?

    @Query("UPDATE driver_chat SET Failed = 1, sendingState = :status  WHERE RefId = :refId")
    suspend fun updateMessageStatusAsFailed(refId: String, status: MessageStatus)

    @Query("UPDATE driver_chat SET Failed = 0,attachments = :attachments  WHERE RefId = :refId")
    suspend fun updateMessageStatusAsSent(refId: String, attachments: List<Attachments>)

    @Query("UPDATE driver_chat SET sendingState = :messageStatus WHERE RefId = :refId")
    suspend fun updateMessageSendingStatusToSending(messageStatus: MessageStatus, refId: String)

    @Query("SELECT * FROM driver_chat ORDER BY time DESC")
    fun getMessagePagingSource() : PagingSource<Int, ChatMessagesEntity>

    @Query("DELETE FROM driver_chat WHERE RefId = :refId")
    suspend fun deleteSentMessage(refId: String)

    @Query("DELETE FROM driver_chat")
    suspend fun clearAll()

}