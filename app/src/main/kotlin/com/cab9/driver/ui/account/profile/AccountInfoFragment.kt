package com.cab9.driver.ui.account.profile

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.IdRes
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cab9.driver.R
import com.cab9.driver.base.FragmentX
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Tag
import com.cab9.driver.databinding.FragmentAccountInfoBinding
import com.cab9.driver.databinding.ItemTagBinding
import com.cab9.driver.ext.*
import com.cab9.driver.widgets.decorators.RecyclerViewGridSpacing
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountInfoFragment : FragmentX<FragmentAccountInfoBinding>(R.layout.fragment_account_info),
    AccountInfoScreenClickListener {

    private val viewModel by hiltNavGraphViewModels<ProfileViewModel>(R.id.nav_grp_profile)

    private val adapter: TagAdapter
        get() = binding.profileTagList.adapter as TagAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.listener = this

        val itemDecorator = RecyclerViewGridSpacing(requireContext(), R.dimen.tag_space)
        val flexLayoutManager = FlexboxLayoutManager(requireContext())
        flexLayoutManager.flexDirection = FlexDirection.ROW
        flexLayoutManager.justifyContent = JustifyContent.FLEX_START

        binding.profileTagList.addItemDecoration(itemDecorator)
        binding.profileTagList.layoutManager = flexLayoutManager
        binding.profileTagList.adapter = TagAdapter()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userProfileOutcome.collectLatest {
                        if (it is Outcome.Success) {
                            binding.profileTagList.visibility(!it.data.tags.isNullOrEmpty())
                            binding.lblTagEmpty.visibility(it.data.tags.isNullOrEmpty())
                            adapter.submitList(it.data.tags.orEmpty())
                        }
                    }
                }
                launch {
                    viewModel.updateEmailOutcome.collectLatest {
                        //binding.pBarUpdateEmail.visibility(it is Outcome.Progress)
                        binding.btnUpdateAccountEmail.visibility(it !is Outcome.Progress)
                        if (it is Outcome.Failure) binding.root.snack(it.msg)
                    }
                }
                launch {
                    viewModel.updateBankNameOutcome.collectLatest {
                        //binding.pBarUpdateBankName.visibility(it is Outcome.Progress)
                        binding.btnUpdateBankName.visibility(it !is Outcome.Progress)
                        if (it is Outcome.Failure) binding.root.snack(it.msg)
                    }
                }
                launch {
                    viewModel.updateBankSortCodeOutcome.collectLatest {
                        //binding.pBarUpdateBankSortCode.visibility(it is Outcome.Progress)
                        binding.btnUpdateBankSortCode.visibility(it !is Outcome.Progress)
                        if (it is Outcome.Failure) binding.root.snack(it.msg)
                    }
                }
                launch {
                    viewModel.updateBankAccountNoOutcome.collectLatest {
                        //binding.pBarUpdateBankAccountNo.visibility(it is Outcome.Progress)
                        binding.btnUpdateBankAccountNo.visibility(it !is Outcome.Progress)
                        if (it is Outcome.Failure) binding.root.snack(it.msg)
                    }
                }
            }
        }
    }

    override fun updateEmail() {
        if (binding.txtAccountEmail.isEnabled) {
            val newEmail = binding.txtAccountEmail.text()
            when {
                !newEmail.isValidEmail() -> binding.root.snack(R.string.err_invalid_email)
                else -> {
                    enableEditingFor(0)
                    viewModel.updateEmailAddress(newEmail)
                }
            }
        } else enableEditingFor(R.id.txtAccountEmail)
    }

    override fun changePassword() {
        findNavController().navigate(
            AccountInfoFragmentDirections
                .actionAccountInfoFragmentToChangePasswordFragment()
        )
    }

    override fun updateBankName() {
        if (binding.txtBankName.isEnabled) {
            enableEditingFor(0)
            viewModel.updateBankName(binding.txtBankName.text())
        } else enableEditingFor(R.id.txtBankName)
    }

    override fun updateBankSortCode() {
        if (binding.txtBankSortCode.isEnabled) {
            enableEditingFor(0)
            viewModel.updateBankSortCode(binding.txtBankSortCode.text())
        } else enableEditingFor(R.id.txtBankSortCode)
    }

    override fun updateBankAccountNumber() {
        if (binding.txtBankAccountNo.isEnabled) {
            enableEditingFor(0)
            viewModel.updateBankAccountNo(binding.txtBankAccountNo.text())
        } else enableEditingFor(R.id.txtBankAccountNo)
    }

    /**
     * Disable all editable views expect the view for which id is passed. Pass 0 to disable all.
     *
     * @param viewId editable view id
     */
    private fun enableEditingFor(@IdRes viewId: Int) {
        fun showKeyboard(txtView: EditText) {
            txtView.setSelection(txtView.length())
            txtView.showKeyboard()
        }
        binding.txtBankAccountNo.isEnabled = R.id.txtBankAccountNo == viewId
        binding.txtBankSortCode.isEnabled = R.id.txtBankSortCode == viewId
        binding.txtBankName.isEnabled = R.id.txtBankName == viewId
        binding.txtAccountEmail.isEnabled = R.id.txtAccountEmail == viewId

        binding.btnUpdateBankAccountNo.setText(if (R.id.txtBankAccountNo == viewId) R.string.action_save else R.string.action_edit)
        binding.btnUpdateBankSortCode.setText(if (R.id.txtBankSortCode == viewId) R.string.action_save else R.string.action_edit)
        binding.btnUpdateBankName.setText(if (R.id.txtBankName == viewId) R.string.action_save else R.string.action_edit)
        binding.btnUpdateAccountEmail.setText(if (R.id.txtAccountEmail == viewId) R.string.action_save else R.string.action_edit)

        if (binding.txtBankAccountNo.isEnabled) showKeyboard(binding.txtBankAccountNo)
        if (binding.txtBankSortCode.isEnabled) showKeyboard(binding.txtBankSortCode)
        if (binding.txtBankName.isEnabled) showKeyboard(binding.txtBankName)
        if (binding.txtAccountEmail.isEnabled) showKeyboard(binding.txtAccountEmail)
    }

    private class TagAdapter : ListAdapter<Tag, TagAdapter.ViewHolder>(TAG_CALLBACK) {

        companion object {
            private val TAG_CALLBACK = object : DiffUtil.ItemCallback<Tag>() {
                override fun areItemsTheSame(
                    oldItem: Tag,
                    newItem: Tag
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: Tag,
                    newItem: Tag
                ): Boolean {
                    return oldItem == newItem
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.create(parent)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }

        class ViewHolder(private val binding: ItemTagBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(tag: Tag) {
                binding.tag = tag
                binding.executePendingBindings()
            }

            companion object {
                fun create(parent: ViewGroup) =
                    ViewHolder(ItemTagBinding.inflate(parent.layoutInflater, parent, false))
            }
        }
    }

}