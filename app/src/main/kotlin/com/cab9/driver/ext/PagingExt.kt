package com.cab9.driver.ext

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState

inline val CombinedLoadStates.isEmpty: Boolean
    get() = this.refresh is LoadState.NotLoading && this.append.endOfPaginationReached