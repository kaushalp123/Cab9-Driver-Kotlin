package com.cab9.driver.data.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cab9.driver.data.models.Message
import com.cab9.driver.data.repos.ChatRepository
import com.cab9.driver.ui.chat.ChatViewModel

//https://android-developers.googleblog.com/2020/07/getting-on-same-page-with-paging-3.html

class ChatPagingSource(private val chatRepo: ChatRepository, private val viewModel: ChatViewModel) :
    PagingSource<String, Message>() {

    override fun getRefreshKey(state: PagingState<String, Message>): String? {
        // We need to get the previous key (or next key if previous is null) of the page
        // that was closest to the most recently accessed index.
        // Anchor position is the most recently accessed index
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPageIndex = state.pages.indexOf(state.closestPageToPosition(anchorPosition))
            state.pages.getOrNull(anchorPageIndex + 1)?.prevKey
                ?: state.pages.getOrNull(anchorPageIndex - 1)?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Message> {
        val position = params.key
        return try {
            val messages = chatRepo.getMessages(position)
            val nextKey = if (messages.isNotEmpty()) messages.lastOrNull()?.id else null
            LoadResult.Page(
                data = messages,
                prevKey = null, // only paging forward
                nextKey = nextKey
            )
        } catch (ex: Exception) {
            LoadResult.Error(ex)
        }
    }

}