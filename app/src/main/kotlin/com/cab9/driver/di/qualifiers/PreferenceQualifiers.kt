package com.cab9.driver.di.qualifiers

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UserSessionPreferences

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class SettingsPreferences