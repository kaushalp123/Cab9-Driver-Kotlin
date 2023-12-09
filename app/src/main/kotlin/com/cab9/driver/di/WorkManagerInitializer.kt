package com.cab9.driver.di

import android.content.Context
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

//@Module
//@InstallIn(SingletonComponent::class)
//object WorkManagerInitializer : Initializer<WorkManager> {
//
//    @Provides
//    @Singleton
//    override fun create(@ApplicationContext context: Context): WorkManager {
//        val config = Configuration.Builder().build();
//        WorkManager.initialize(context, config)
//        Timber.d("WorkManager initialized...")
//        return WorkManager.getInstance(context);
//    }
//
//    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
//        return mutableListOf()
//    }
//}