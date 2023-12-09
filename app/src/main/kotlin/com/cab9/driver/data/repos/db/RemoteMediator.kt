package com.cab9.driver.data.repos.db

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.cab9.driver.data.models.Attachments
import com.cab9.driver.data.models.ChatMessagesEntity
import com.cab9.driver.data.models.Message
import com.cab9.driver.data.models.MessageStatus
import com.cab9.driver.data.models.RemoteKey
import com.cab9.driver.data.repos.ChatRepository
import com.cab9.driver.ext.HH_mm
import com.cab9.driver.ext.ROOM_DB_DATE_SAVE_FORMAT
import com.cab9.driver.ext.ofPattern
import org.json.JSONObject
import org.threeten.bp.Instant
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalPagingApi::class)
class RemoteMediator(
    private val database: ChatRoomDataBase,
    private val chatRepo: ChatRepository
) : RemoteMediator<Int, ChatMessagesEntity>() {

    private val chatDao = database.chatDao()
    private val remoteKeysDao = database.remoteKeysDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ChatMessagesEntity>
    ): MediatorResult {
        return try {
            // The network load method takes an optional after=<user.id>
            // parameter. For every page after the first, pass the last user
            // ID to let it continue from where it left off. For REFRESH,
            // pass null to load the first page.
            val loadKey = when (loadType) {
                LoadType.REFRESH -> {
                    Timber.i("LOAD TYPE IS LoadType.REFRESH")
                    null
                }
                // In this example, you never need to prepend, since REFRESH
                // will always load the first page in the list. Immediately
                // return, reporting end of pagination.
                LoadType.PREPEND -> {
                    Timber.i("LOAD TYPE IS LoadType.PREPEND")
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    Timber.i("LOAD TYPE IS LoadType.APPEND")
                    val remoteKey = database.withTransaction {
                        remoteKeysDao.remoteKeyByQuery()
                    }

                    // You must explicitly check if the page key is null when
                    // appending, since null is only valid for initial load.
                    // If you receive null for APPEND, that means you have
                    // reached the end of pagination and there are no more
                    // items to load.
                    if (remoteKey.isEmpty()) {
                        return MediatorResult.Success(
                            endOfPaginationReached = true
                        )
                    }

                    remoteKey.lastOrNull()?.nextKey
                        ?: // End of pagination reached, no need to make additional network calls.
                        return MediatorResult.Success(
                            endOfPaginationReached = true
                        )
                }
            }

            // Suspending network load via Retrofit. This doesn't need to be
            // wrapped in a withContext(Dispatcher.IO) { ... } block since
            // Retrofit's Coroutine CallAdapter dispatches on a worker
            // thread.
                Timber.i("loadKey:$loadKey")
                val response = chatRepo.getMessages(loadKey)
                val nextKey = if (response.isNotEmpty()) response.lastOrNull()?.id else null
                Timber.i("nextKey:$nextKey")

                database.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        chatDao.clearAll()
                        remoteKeysDao.deleteByQuery()
                    }

                    // Update RemoteKey for this query.
                    remoteKeysDao.insertOrReplace(
                        RemoteKey(nextKey.orEmpty())
                    )

                    // Insert new users into database, which invalidates the
                    // current PagingData, allowing Paging to present the updates
                    // in the DB.
                    val chatMessagesEntityList: List<ChatMessagesEntity> = response.map { message ->

                        val dateStr =
                            message.dateTime?.ofPattern(ROOM_DB_DATE_SAVE_FORMAT).orEmpty()
                        val format = SimpleDateFormat(ROOM_DB_DATE_SAVE_FORMAT, Locale.UK) // using SimpleDateFormat since room db was not recognizing instant date time.
                        val dateObj = format.parse(dateStr)

                        ChatMessagesEntity(
                            id = message.id.orEmpty(),
                            conversationId = message.conversationId,
                            body = message.text,
                            senderId = message.senderId,
                            tenantId = message.tenantId,
                            attachments = message.attachments as MutableList<Attachments>,
                            refId = "",
                            dateTime = message.dateTime,
                            savedTime = dateObj,
                            read = message.isRead ?: false,
                            isFailed = false,
                            messageStatus = MessageStatus.RECEIVED
                        )
                    }

                    val row = chatDao.insertAll(chatMessagesEntityList)
                    Timber.i("row:${row.size}")
                }

                MediatorResult.Success(
                    endOfPaginationReached = nextKey == null
                )
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }
}