package com.cab9.driver.data.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cab9.driver.data.models.JobPoolBid
import com.cab9.driver.data.repos.JobPoolBidRepository
import com.cab9.driver.network.JobBidExpiredException
import com.cab9.driver.network.NoLocationFoundException
import com.cab9.driver.utils.CAB9_STARTING_PAGE_INDEX
import com.google.android.gms.maps.model.LatLng

class NearbyBidsPagingSource(
    private val jobPoolBidRepo: JobPoolBidRepository,
    private val latLng: LatLng?
) : PagingSource<Int, JobPoolBid>() {

    // The refresh key is used for subsequent refresh calls to PagingSource.load after the initial load
    override fun getRefreshKey(state: PagingState<Int, JobPoolBid>): Int? {
        // We need to get the previous key (or next key if previous is null) of the page
        // that was closest to the most recently accessed index.
        // Anchor position is the most recently accessed index
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, JobPoolBid> {
        val position = params.key ?: CAB9_STARTING_PAGE_INDEX
        return try {
            if (latLng == null) throw NoLocationFoundException()
            val bids = jobPoolBidRepo.getNearbyBids(latLng, params.loadSize, position)
            val nextKey = if (bids.isEmpty()) null
            else {
                // initial load size = 3 * NETWORK_PAGE_SIZE
                // ensure we're not requesting duplicating items, at the 2nd request
                //position + (params.loadSize / NETWORK_PAGE_SIZE)
                position + 1
            }
            LoadResult.Page(
                data = bids,
                prevKey = if (position == CAB9_STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey
            )
        } catch (ex: Exception) {
            LoadResult.Error(ex)
        }
    }
}