package com.cab9.driver.di.modules

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import androidx.biometric.BiometricManager
import androidx.room.RoomDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.work.WorkManager
import com.cab9.driver.base.Cab9DriverApp
import com.cab9.driver.data.repos.db.ChatRoomDataBase
import com.cab9.driver.di.qualifiers.SettingsPreferences
import com.cab9.driver.di.qualifiers.UserSessionPreferences
import com.cab9.driver.settings.AppSettings.Companion.PREFERENCE_NAME
import com.google.android.gms.location.LocationServices
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providesCab9DriverApplication(app: Application): Cab9DriverApp = app as Cab9DriverApp

    @Provides
    @Singleton
    fun providesAppUpdateMangerFactory(@ApplicationContext context: Context) =
        AppUpdateManagerFactory.create(context)

    @Provides
    @Singleton
    @UserSessionPreferences
    fun providesEncryptedPreference(@ApplicationContext context: Context): SharedPreferences {
        // Using recommended key generation parameter specification,
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
        return EncryptedSharedPreferences.create(
            "prefs::user", mainKeyAlias, context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Provides
    @Singleton
    @SettingsPreferences
    fun providesSettingsPreference(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun providesBiometricManager(@ApplicationContext context: Context) =
        BiometricManager.from(context)

    @Provides
    @Singleton
    fun providesFusedLocationProviderClient(@ApplicationContext context: Context) =
        LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun providesGeocoder(@ApplicationContext context: Context) = Geocoder(context)

    @Provides
    @Singleton
    fun providesWorkManager(@ApplicationContext context: Context) = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun providesRoomDataBase(@ApplicationContext context: Context) = ChatRoomDataBase.getDatabase(context)

}