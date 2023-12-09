package com.cab9.driver.ui.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.*
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseActivity
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Prediction
import com.cab9.driver.databinding.ActivitySearchPlacesBinding
import com.cab9.driver.databinding.ItemSearchPlaceBinding
import com.cab9.driver.ext.textChanges
import com.cab9.driver.ui.search.SearchPlaces.Companion.EXTRA_PREDICTION
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchPlacesActivity : BaseActivity(R.layout.activity_search_places), View.OnClickListener {

    private val binding by viewBinding(ActivitySearchPlacesBinding::bind)
    private val viewModel by viewModels<SearchPlaceViewModel>()

    private val onPlaceSelected: (Prediction.UiModel) -> Unit = {
        viewModel.getPlaceDetail(it)
    }

    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.txtSearchPlaces.textChanges()
            .drop(1)
            .debounce(500L)
            .distinctUntilChanged()
            .map { viewModel.searchPlaces(it) }
            .launchIn(lifecycleScope)

        binding.searchPlacesList.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(context)
            adapter = SearchPlaceListAdapter(onPlaceSelected)
        }

        binding.imgBtnCloseSearch.setOnClickListener(this)
        binding.txtSearchPlaces.requestFocus()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.googlePredictions.collect {
                        binding.rlSearchResult.setState(it)
                        if (it is Outcome.Success) getAdapter().submitList(it.data)
                    }
                }
                launch {
                    viewModel.placeDetail.collect { result ->
                        binding.rlSearchResult.setState(result)
                        if (result is Outcome.Success) {
                            setResult(
                                Activity.RESULT_OK,
                                Intent().apply { putExtra(EXTRA_PREDICTION, result.data) })
                            finish()
                        }
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.imgBtnCloseSearch) finish()
    }

    private fun getAdapter() = binding.searchPlacesList.adapter as SearchPlaceListAdapter

    private class SearchPlaceListAdapter(private val onPlaceSelected: (Prediction.UiModel) -> Unit) :
        ListAdapter<Prediction.UiModel, SearchPlaceListAdapter.ViewHolder>(PREDICTION_COMPARATOR) {

        companion object {
            private val PREDICTION_COMPARATOR =
                object : DiffUtil.ItemCallback<Prediction.UiModel>() {
                    override fun areItemsTheSame(
                        oldItem: Prediction.UiModel,
                        newItem: Prediction.UiModel
                    ): Boolean {
                        return oldItem.placeId == newItem.placeId
                    }

                    override fun areContentsTheSame(
                        oldItem: Prediction.UiModel,
                        newItem: Prediction.UiModel
                    ): Boolean {
                        return oldItem == newItem
                    }
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.create(parent, onPlaceSelected)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }

        class ViewHolder(
            private val binding: ItemSearchPlaceBinding,
            private val onPlaceSelected: (Prediction.UiModel) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(prediction: Prediction.UiModel) {
                binding.prediction = prediction
                binding.root.setOnClickListener { onPlaceSelected.invoke(prediction) }
                binding.executePendingBindings()
            }

            companion object {
                fun create(
                    parent: ViewGroup,
                    onPlaceSelected: (Prediction.UiModel) -> Unit
                ): ViewHolder {
                    val layoutInflater = LayoutInflater.from(parent.context)
                    val binding = ItemSearchPlaceBinding.inflate(layoutInflater, parent, false)
                    return ViewHolder(binding, onPlaceSelected)
                }
            }
        }
    }

}