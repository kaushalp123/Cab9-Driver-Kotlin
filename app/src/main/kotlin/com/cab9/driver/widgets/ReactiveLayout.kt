package com.cab9.driver.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import com.cab9.driver.R
import com.cab9.driver.base.Outcome
import com.cab9.driver.databinding.ViewReactiveLayoutBinding
import com.cab9.driver.ext.drawable
import com.cab9.driver.ext.gone
import com.cab9.driver.ext.hide
import com.cab9.driver.ext.isEmpty
import com.cab9.driver.ext.show
import com.cab9.driver.ext.visibility
import com.cab9.driver.network.ChatConversationAPIException
import com.cab9.driver.network.NoLocationFoundException
import com.cab9.driver.network.isNetworkError
import timber.log.Timber

class ReactiveLayout : FrameLayout {

    private lateinit var errorView: ErrorView
    private lateinit var completeView: View
    private lateinit var progressBarOverlay: View

    private var loadingView: View? = null

    private var emptyTitle: String? = null
    private var emptySubtitle: String? = null
    private var emptyIcon: Drawable? = null

    private var retryBlock: (() -> Unit)? = null

    constructor(context: Context) :
            this(context, null, 0, 0)

    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs, 0, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        ViewReactiveLayoutBinding.inflate(LayoutInflater.from(context), this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ReactiveLayout)
        emptyTitle = attributes.getString(R.styleable.ReactiveLayout_emptyTitle)
        emptySubtitle = attributes.getString(R.styleable.ReactiveLayout_emptySubtitle)
        emptyIcon = attributes.getDrawable(R.styleable.ReactiveLayout_emptyIcon)
        attributes.recycle()

        errorView = findViewWithTag(context.getString(R.string.v_tag_error))
        progressBarOverlay = findViewWithTag(context.getString(R.string.v_tag_loading_default))

        errorView.onActionBtnClick { retryBlock?.invoke() }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        completeView = findViewWithTag(context.getString(R.string.v_tag_complete))
        val loadingView = findViewWithTag<View>(context.getString(R.string.v_tag_loading))
        if (loadingView != null) this.loadingView = loadingView
        val overlayLoadingView =
            findViewWithTag<View>(context.getString(R.string.v_tag_loading_overlay))
        if (overlayLoadingView != null) progressBarOverlay = overlayLoadingView
    }

    fun setResources(emptyTitle: String, emptySubTitle: String, emptyIcon: Drawable?) {
        this.emptyTitle = emptyTitle
        this.emptySubtitle = emptySubTitle
        this.emptyIcon = emptyIcon
    }

    /**
     * Use this to show loading state while list paging is used.
     *
     * @param state Pagination [LoadState]
     * @param adapter paging adapter
     */
    fun setState(state: CombinedLoadStates, adapter: PagingDataAdapter<*, *>) {
        //val isLoading = state.refresh is LoadState.Loading
        if (state.refresh !is LoadState.Loading) loadingView?.hide()
        if (state.refresh is LoadState.Error) {
            completeView.gone()
            showErrorView(state.refresh as LoadState.Error)
        } else {
            if (state.isEmpty && adapter.itemCount == 0) {
                completeView.gone()
                showEmptyListUI()
            } else {
                completeView.show()
                errorView.gone()
            }
        }
    }

    private fun showErrorView(state: LoadState.Error) {
        errorView.show()
        val errTitle =
            if (state.error.isNetworkError) context.getString(R.string.err_title_no_internet)
            else context.getString(R.string.title_error)
        // Set error icon
        val errorIconResId =
            if (state.error.isNetworkError) R.drawable.ic_baseline_wifi_off_24
            else if (state.error is NoLocationFoundException) R.drawable.baseline_location_off_24
            else R.drawable.ic_baseline_error_outline_24

        // Set error icon
        errorView.errorIcon = context.drawable(errorIconResId)

        // Set error button messages
        errorView.errorBtnText = context.getString(R.string.action_retry)
        errorView.showActionBtn()

        // show empty view messages
        errorView.errorTitle = errTitle
        errorView.errorSubtitle =
            when {
                state.error.isNetworkError -> context.getString(R.string.err_msg_no_internet)
                state.error is NoLocationFoundException -> context.getString(R.string.err_current_location)
                else -> state.error.message
            }
    }

    private fun showEmptyListUI() {
        errorView.show()
        // show empty view messages
        errorView.errorIcon = emptyIcon
        errorView.errorTitle = emptyTitle
        errorView.errorSubtitle = emptySubtitle
        // No need to show action button here
        errorView.hideActionBtn()
    }

    fun setState(outcome: Outcome<*>) {
        when (outcome) {
            is Outcome.Progress -> {
                if (outcome.asOverlay) progressBarOverlay.show()
                else {
                    completeView.gone()
                    errorView.gone()
                    if (loadingView != null) loadingView?.show()
                    else progressBarOverlay.show()
                }
            }

            is Outcome.Failure -> {
                loadingView?.gone()
                progressBarOverlay.gone()
                if (!outcome.showAsOverlay) {
                    completeView.gone()
                    errorView.show()
                    val errTitle =
                        if (outcome.e.isNetworkError) context.getString(R.string.err_title_no_internet)
                        else context.getString(R.string.title_error)
                    // Set error icon
                    val errorIconResId =
                        if (outcome.e.isNetworkError) R.drawable.ic_baseline_wifi_off_24
                        else R.drawable.ic_baseline_error_outline_24

                    // Set error icon
                    errorView.errorIcon = context.drawable(errorIconResId)

                    // Set error button messages
                    errorView.errorBtnText = context.getString(R.string.action_retry)
                    errorView.showActionBtn()

                    // show empty view messages
                    errorView.errorTitle = errTitle
                    errorView.errorSubtitle = if(outcome.e.isNetworkError) context.getString(R.string.err_msg_no_internet) else outcome.msg
                }
            }

            is Outcome.Success, is Outcome.Empty -> {
                loadingView?.gone()
                progressBarOverlay.gone()
                completeView.show()
                if (outcome is Outcome.Success) {
                    if (outcome.isEmptyList) showEmptyListUI()
                    else errorView.gone()
                } else errorView.gone()
            }
        }
    }

    fun onRetryListener(block: () -> Unit) {
        retryBlock = block
    }

    /**
     * When set to true, layout will show circular progress bar on top of the content.
     */
    fun isProcessing(isLoading: Boolean) {
        progressBarOverlay.visibility(isLoading)
    }
}