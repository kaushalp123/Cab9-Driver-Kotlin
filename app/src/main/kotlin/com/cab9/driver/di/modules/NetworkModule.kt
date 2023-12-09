package com.cab9.driver.di.modules

import android.content.Context
import com.cab9.driver.BuildConfig
import com.cab9.driver.di.qualifiers.AuthOkHttpClient
import com.cab9.driver.di.qualifiers.NoAuthOkHttpClient
import com.cab9.driver.network.*
import com.cab9.driver.network.apis.*
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.utils.AUTHORIZATION
import com.cab9.driver.utils.BEARER
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    @Singleton
    fun providesOkHttpLogInterceptor() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    @Provides
    @Singleton
    fun providesChuckerInterceptor(@ApplicationContext appContext: Context): ChuckerInterceptor {
        // Create the Collector
        val chuckerCollector = ChuckerCollector(
            context = appContext,
            // Toggles visibility of the notification
            showNotification = true,
            // Allows to customize the retention period of collected data
            retentionPeriod = RetentionManager.Period.ONE_HOUR
        )
        // Create the Interceptor
        return ChuckerInterceptor.Builder(appContext)
            // The previously created Collector
            .collector(chuckerCollector)
            // The max body content length in bytes, after this responses will be truncated.
            .maxContentLength(250_000L)
            // List of headers to replace with ** in the Chucker UI
            .redactHeaders(AUTHORIZATION, BEARER)
            // Read the whole response body even when the client does not consume the response completely.
            // This is useful in case of parsing errors or when the response body
            // is closed before being read like in Retrofit with Void and Unit types.
            .alwaysReadResponseBody(true)
            .build()
    }

    @Provides
    @Singleton
    fun providesOkHttpCache(@ApplicationContext context: Context) =
        Cache(File(context.cacheDir, "http_cache"), 10L * 1024L * 1024L)

    @Singleton
    @Provides
    fun providesMoshi(): Moshi = Moshi.Builder()
        .add(UTCDateTimeAdapter())
        .add(UTCOffsetDateTimeAdapter())
        //.add(TimeZoneAdapter())
        //.add(ZoneOffsetDateTimeAdapter())
        .build()

    @Provides
    @Singleton
    fun providesTokenAuthenticator(loginApi: LoginAPI, sessionManager: SessionManager) =
        AuthTokenAuthenticator(loginApi, sessionManager)

    @Provides
    @Singleton
    fun providesAuthInterceptor(sessionManager: SessionManager) =
        AuthTokenInterceptor(sessionManager)

    @Provides
    @Singleton
    @NoAuthOkHttpClient
    fun providesOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
        cache: Cache
    ): OkHttpClient {
        val okHttpClient = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            okHttpClient
                .addInterceptor(chuckerInterceptor)
                .addInterceptor(loggingInterceptor)

        }
        return okHttpClient.cache(cache).build()
    }

    @Provides
    @Singleton
    @AuthOkHttpClient
    fun providesAuthBasedOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        tokenAuthenticator: AuthTokenAuthenticator,
        authInterceptor: AuthTokenInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
        cache: Cache
    ): OkHttpClient {
        val okHttpBuilder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            okHttpBuilder.addInterceptor(loggingInterceptor)
                .addInterceptor(chuckerInterceptor)
        }
        return okHttpBuilder.cache(cache)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
    }

    @Provides
    @Singleton
    fun providesLoginService(
        @NoAuthOkHttpClient okHttpClient: OkHttpClient,
        mosh: Moshi
    ): LoginAPI = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(mosh))
        .baseUrl(BuildConfig.BASE_LOGIN_URL)
        .build()
        .create(LoginAPI::class.java)

    @Provides
    @Singleton
    fun providesGoogleApiService(
        @NoAuthOkHttpClient okHttpClient: OkHttpClient,
        mosh: Moshi
    ): GoogleAPI = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(mosh))
        .baseUrl(BuildConfig.GOOGLE_API)
        .build()
        .create(GoogleAPI::class.java)

    @Provides
    @Singleton
    fun providesNodeService(
        @AuthOkHttpClient okHttpClient: OkHttpClient,
        mosh: Moshi
    ): NodeAPI = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(mosh))
        .baseUrl(BuildConfig.BASE_NODE_URL)
        .build()
        .create(NodeAPI::class.java)

    @Provides
    @Singleton
    fun providesCab9Service(
        @AuthOkHttpClient okHttpClient: OkHttpClient,
        mosh: Moshi
    ): Cab9API = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(mosh))
        .baseUrl(BuildConfig.BASE_CAB9_URL)
        .build()
        .create(Cab9API::class.java)

    @Provides
    @Singleton
    fun providesSumUpService(
        @NoAuthOkHttpClient okHttpClient: OkHttpClient,
        mosh: Moshi
    ): SumUpTokenAPI = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(mosh))
        .baseUrl(BuildConfig.SUMUP_BASE_URL)
        .build()
        .create(SumUpTokenAPI::class.java)

    @Provides
    @Singleton
    fun providesChangePasswordService(
        @AuthOkHttpClient okHttpClient: OkHttpClient,
        mosh: Moshi
    ): PasswordAPI = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(mosh))
        .baseUrl(BuildConfig.BASE_LOGIN_URL)
        .build()
        .create(PasswordAPI::class.java)

    @Provides
    @Singleton
    fun providesChatService(
        @AuthOkHttpClient okHttpClient: OkHttpClient,
        mosh: Moshi
    ): ChatAPI = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(mosh))
        .baseUrl(BuildConfig.BASE_NODE_URL)
        .build()
        .create(ChatAPI::class.java)
}