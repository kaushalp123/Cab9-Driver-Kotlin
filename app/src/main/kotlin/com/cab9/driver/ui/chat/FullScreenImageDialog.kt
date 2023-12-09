package com.cab9.driver.ui.chat

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cab9.driver.R
import com.cab9.driver.data.models.Attachments
import com.cab9.driver.databinding.ViewFullScreenImageDialogBinding
import com.cab9.driver.ui.chat.adapter.FullScreenImageViewRecyclerAdapter

abstract class FullScreenImageDialog(
    private val context: Context,
    private var attachments: List<Attachments>
) : Dialog(context) {

    private var adapter: FullScreenImageViewRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ViewFullScreenImageDialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        setCanceledOnTouchOutside(false)
        setCancelable(false)

        adapter = FullScreenImageViewRecyclerAdapter(attachments.toMutableList())

        binding.imagesList.adapter = adapter
        binding.imagesList.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        binding.imgCloseDialog.setOnClickListener {
            dismiss()
        }

        val layoutManager = binding.imagesList.layoutManager as LinearLayoutManager
        binding.lblCount.text = buildString {
            append(context.getString(R.string.showing))
            append("1/${adapter?.itemCount}")
        }

        binding.imagesList.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val position = getCurrentItem(layoutManager)
                    binding.lblCount.text =
                        buildString {
                            append(context.getString(R.string.showing))
                            append(position.plus(1))
                            append("/")
                            append(adapter?.itemCount)
                        }
                }
            }
        })
    }

    private fun getCurrentItem(layoutManager: LinearLayoutManager): Int {
        return layoutManager.findFirstVisibleItemPosition()
    }
}