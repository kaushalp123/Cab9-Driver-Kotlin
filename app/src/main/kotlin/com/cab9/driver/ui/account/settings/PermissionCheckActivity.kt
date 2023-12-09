package com.cab9.driver.ui.account.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseActivity
import com.cab9.driver.databinding.ActivityRequiredPermissionBinding
import com.cab9.driver.databinding.ItemPermissionDetailBinding
import com.cab9.driver.ext.areNotificationsEnabled
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ext.visibility
import com.cab9.driver.settings.AppSettings
import com.cab9.driver.ui.account.Cab9RequiredPermission
import com.cab9.driver.ui.home.HomeActivity
import com.cab9.driver.utils.*
import com.cab9.driver.utils.map.LOCATION_PERMISSIONS
import com.cab9.driver.utils.map.hasLocationPermissions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PermissionCheckActivity : BaseActivity(R.layout.activity_required_permission) {

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, PermissionCheckActivity::class.java)
            context.startActivity(starter)
        }
    }

    private val binding by viewBinding(ActivityRequiredPermissionBinding::bind)

    @Inject
    lateinit var appSettings: AppSettings

    private lateinit var missingPermissions: List<Cab9RequiredPermission>

    private val notificationSettings =
        registerForActivityResult(NotificationSettings()) { moveToNextPermission() }

    private val batteryOptimization =
        registerForActivityResult(BatteryOptimization()) { moveToNextPermission() }

    private val systemDrawOverlay =
        registerForActivityResult(SystemOverlay()) { moveToNextPermission() }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { moveToNextPermission() }

    private val requestAutoDateTimeSetPermission =
        registerForActivityResult(AutoDateTimeSettings()) {
            moveToNextPermission()
        }

    private val requestLocationPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            moveToNextPermission()
        }

    private val requestDeviceGPSPermission =
        registerForActivityResult(DeviceGPSSettings()) {
            moveToNextPermission()
        }

    @SuppressLint("InlinedApi")
    private val onRequestPermission: (Cab9RequiredPermission) -> Unit = {
        if (it.isGranted(this)) moveToNextPermission()
        else {
            when (it) {
                Cab9RequiredPermission.LOCATION_PERMISSION -> {
                    if (!hasLocationPermissions(this)) {
                        requestLocationPermissions.launch(LOCATION_PERMISSIONS)
                    } else moveToNextPermission()
                }

                Cab9RequiredPermission.NOTIFICATION -> {
                    if (!hasNotificationPermission(this)) {
                        requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (!areNotificationsEnabled()) notificationSettings.launch()
                    else moveToNextPermission()
                }

                Cab9RequiredPermission.BATTERY -> batteryOptimization.launch()
                Cab9RequiredPermission.OVERLAY -> systemDrawOverlay.launch()
                Cab9RequiredPermission.RECORD_AUDIO -> requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                Cab9RequiredPermission.AUTO_DATE_TIME -> requestAutoDateTimeSetPermission.launch()
                Cab9RequiredPermission.GPS -> requestDeviceGPSPermission.launch()
            }
        }
    }

    private val onIgnorePermission: (Cab9RequiredPermission) -> Unit = {
        moveToNextPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        missingPermissions = Cab9RequiredPermission.values().filter { !it.isGranted(this) }
        if (missingPermissions.isEmpty()) finishNow()
        else {
            binding.permissionPager.isUserInputEnabled = false
            binding.permissionPager.adapter =
                PermissionAdapter(missingPermissions, onRequestPermission, onIgnorePermission)
        }
    }

    private fun moveToNextPermission() {
        val itemCount = binding.permissionPager.adapter?.itemCount ?: 0
        val currentPosition = binding.permissionPager.currentItem
        val nextPosition = currentPosition + 1
        if (nextPosition < itemCount) binding.permissionPager.currentItem = nextPosition
        else finishNow()
    }

    private fun finishNow() {
        appSettings.isPermissionIntroCompleted = true
        HomeActivity.start(this, bundleOf(BUNDLE_KEY_PERMISSION_SHOWN to true))
        finish()
    }

    private class PermissionAdapter(
        private val permissions: List<Cab9RequiredPermission>,
        private val onRequestPermission: (Cab9RequiredPermission) -> Unit,
        private val onIgnorePermission: (Cab9RequiredPermission) -> Unit
    ) : RecyclerView.Adapter<PermissionAdapter.ViewHolder>() {

        private val mandatoryPermissions = Cab9RequiredPermission.mandatoryPermissions()

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder = ViewHolder(
            ItemPermissionDetailBinding.inflate(parent.layoutInflater, parent, false),
            mandatoryPermissions,
            onRequestPermission,
            onIgnorePermission
        )

        override fun getItemCount(): Int = permissions.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(permissions[position])
        }

        class ViewHolder(
            private val binding: ItemPermissionDetailBinding,
            private val mandatoryPermissions: List<Cab9RequiredPermission>,
            private val onRequestPermission: (Cab9RequiredPermission) -> Unit,
            private val onIgnorePermission: (Cab9RequiredPermission) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(permission: Cab9RequiredPermission) {
                binding.imgPermissionIllustrator.setImageResource(permission.imageResId)
                binding.lblPermissionSummary.text = permission.getSummary(itemView.context)
                binding.lblPermissionTitle.setText(permission.titleResId)

                binding.btnRequestPermission.setOnClickListener {
                    onRequestPermission.invoke(permission)
                }

                binding.btnIgnorePermission.visibility(!mandatoryPermissions.contains(permission))
                binding.btnIgnorePermission.setOnClickListener {
                    onIgnorePermission.invoke(permission)
                }
            }
        }
    }

}