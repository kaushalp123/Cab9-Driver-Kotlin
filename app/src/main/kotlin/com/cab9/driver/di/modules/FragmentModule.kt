package com.cab9.driver.di.modules

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.fragment.app.Fragment
import com.cab9.driver.di.qualifiers.FragmentBiometricHandler
import com.cab9.driver.settings.BiometricHandler
import com.cab9.driver.settings.BiometricHandlerCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(FragmentComponent::class)
class FragmentModule {

    @Provides
    @FragmentBiometricHandler
    fun provideBiometricHandler(
        @ApplicationContext context: Context,
        fragment: Fragment,
        biometricManager: BiometricManager
    ) = BiometricHandler(context, biometricManager, fragment as BiometricHandlerCallback)
}