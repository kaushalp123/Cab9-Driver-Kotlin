package com.cab9.driver.di.modules

import android.app.Activity
import android.content.Context
import androidx.biometric.BiometricManager
import com.cab9.driver.di.qualifiers.ActivityBiometricHandler
import com.cab9.driver.settings.BiometricHandler
import com.cab9.driver.settings.BiometricHandlerCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    @Provides
    @ActivityBiometricHandler
    fun provideBiometricHandler(
        @ApplicationContext context: Context,
        activity: Activity,
        biometricManager: BiometricManager
    ) = BiometricHandler(context, biometricManager, activity as BiometricHandlerCallback)

}