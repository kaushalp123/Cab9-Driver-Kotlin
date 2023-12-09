package com.cab9.driver.ui.chat

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.cab9.driver.R
import com.cab9.driver.databinding.AddQuickMessagePopupBinding
import com.cab9.driver.ext.hideKeyboard
import com.cab9.driver.ext.localBroadcastManager
import com.cab9.driver.ext.text
import com.cab9.driver.ext.toast
import com.cab9.driver.utils.ACTION_NEW_QUICK_MESSAGE_ADDED


class AddQuickMessageDialog(
    private val context: Context
) : Dialog(context), View.OnClickListener {


    val binding = AddQuickMessagePopupBinding.inflate(LayoutInflater.from(context))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCanceledOnTouchOutside(false)
        setCancelable(false)
        setContentView(binding.root)

        binding.btnAdd.setOnClickListener(this)
        binding.btnCancel.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_cancel ->  dismissDialog()

            R.id.btn_add -> addMessage()
        }
    }

    private fun addMessage() {
        if(binding.etQuickMessage.text().isNotEmpty()) {
            context.localBroadcastManager.sendBroadcast(Intent().apply {
                action = ACTION_NEW_QUICK_MESSAGE_ADDED
                putExtra("message", binding.etQuickMessage.text.toString())
            })
            dismissDialog()
        } else context.toast(context.getString(R.string.err_message_empty));
    }

    private fun dismissDialog() {
        binding.rootView.hideKeyboard()
        dismiss()
    }
}