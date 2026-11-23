package com.kinetix.shop.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kinetix.shop.BuildConfig
import com.kinetix.shop.core.concurrency.ApplicationScope
import com.kinetix.shop.core.concurrency.DefaultDispatcher
import com.kinetix.shop.core.concurrency.IoDispatcher
import com.kinetix.shop.core.config.AppConfigCheckResult
import com.kinetix.shop.core.config.AppConfigValid
import com.kinetix.shop.core.config.AppConfigValidator
import com.kinetix.shop.core.config.NetworkPolicy
import com.kinetix.shop.core.network.RequestCorrelationInterceptor
import com.kinetix.shop.core.network.RequestCorrelationRecorder
import com.kinetix.shop.services.config.FeatureFlagProvider
import com.kinetix.shop.services.config.FirebaseFeatureFlagProvider
import com.kinetix.shop.services.config.LocalFeatureFlagProvider
import com.kinetix.shop.services.local.CatalogCacheDao
import com.kinetix.shop.services.local.KinetixCacheDatabase
import com.kinetix.shop.services.local.KinetixLedgerDatabase
import com.kinetix.shop.services.local.OutboxDao
import com.kinetix.shop.services.local.ProfileCacheDao
import com.kinetix.shop.services.remote.CatalogApiService
import com.kinetix.shop.services.remote.IdentityApiService
import com.kinetix.shop.services.remote.PricingApiService
import com.kinetix.shop.services.remote.ReviewApiService
import com.kinetix.shop.services.remote.SearchApiService
import com.kinetix.shop.services.storage.EncryptedFileTokenStore
import com.kinetix.shop.services.storage.SessionCipher
import com.kinetix.shop.services.storage.TokenStore
import com.kinetix.shop.services.telemetry.AnalyticsTracker
import com.kinetix.shop.services.telemetry.CrashReporter
import com.kinetix.shop.services.telemetry.FirebaseAnalyticsTracker
import com.kinetix.shop.services.telemetry.FirebaseCrashReporter
import com.kinetix.shop.services.telemetry.LocalAnalyticsTracker
import com.kinetix.shop.services.telemetry.LocalCrashReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
    }

    @Provides
    @Singleton
    fun provideAppConfigCheckResult(): AppConfigCheckResult {
        return AppConfigValidator().validate(
            httpBase = BuildConfig.HTTP_BASE,
            wsBase = BuildConfig.WS_BASE,
        )
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        requestCorrelationInterceptor: RequestCorrelationInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(NetworkPolicy.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkPolicy.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkPolicy.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(NetworkPolicy.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .addInterceptor(requestCorrelationInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        configCheckResult: AppConfigCheckResult,
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        val validConfig = configCheckResult as? AppConfigValid
            ?: throw IllegalStateException("Cannot initialize Retrofit when AppConfigCheckResult is invalid")
        val contentType = "application/json; charset=UTF-8".toMediaType()
        return Retrofit.Builder()
            .baseUrl("${validConfig.config.httpBase}/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideIdentityApiService(
        retrofit: Retrofit,
    ): IdentityApiService {
        return retrofit.create(IdentityApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCatalogApiService(
        retrofit: Retrofit,
    ): CatalogApiService {
        return retrofit.create(CatalogApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSearchApiService(
        retrofit: Retrofit,
    ): SearchApiService {
        return retrofit.create(SearchApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePricingApiService(
        retrofit: Retrofit,
    ): PricingApiService {
        return retrofit.create(PricingApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideReviewApiService(
        retrofit: Retrofit,
    ): ReviewApiService {
        return retrofit.create(ReviewApiService::class.java)
    }

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher {
        return Dispatchers.Default
    }

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob() + defaultDispatcher)
    }

    @Provides
    @Singleton
    fun provideTokenStore(
        @ApplicationContext context: Context,
        cipher: SessionCipher,
        json: Json,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): TokenStore {
        val storageDirectory = File(
            context.noBackupFilesDir,
            "kinetix_secure",
        )
        return EncryptedFileTokenStore(
            storageDirectory = storageDirectory,
            cipher = cipher,
            json = json,
            ioDispatcher = ioDispatcher,
        )
    }

    @Provides
    @Singleton
    fun provideKinetixCacheDatabase(
        @ApplicationContext context: Context,
    ): KinetixCacheDatabase {
        return Room.databaseBuilder(
            context,
            KinetixCacheDatabase::class.java,
            "kinetix_cache.db",
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides
    @Singleton
    fun provideKinetixLedgerDatabase(
        @ApplicationContext context: Context,
    ): KinetixLedgerDatabase {
        return Room.databaseBuilder(
            context,
            KinetixLedgerDatabase::class.java,
            "kinetix_ledger.db",
        ).build()
    }

    @Provides
    @Singleton
    fun provideCatalogCacheDao(
        database: KinetixCacheDatabase,
    ): CatalogCacheDao {
        return database.catalogCacheDao()
    }

    @Provides
    @Singleton
    fun provideProfileCacheDao(
        database: KinetixCacheDatabase,
    ): ProfileCacheDao {
        return database.profileCacheDao()
    }

    @Provides
    @Singleton
    fun provideOutboxDao(
        database: KinetixLedgerDatabase,
    ): OutboxDao {
        return database.outboxDao()
    }

    @Provides
    @Singleton
    fun provideCrashReporter(
        @ApplicationContext context: Context,
        firebaseCrashReporter: FirebaseCrashReporter,
        localCrashReporter: LocalCrashReporter,
    ): CrashReporter {
        return if (FirebaseApp.getApps(context).isNotEmpty()) {
            firebaseCrashReporter
        } else {
            localCrashReporter
        }
    }

    @Provides
    @Singleton
    fun provideRequestCorrelationRecorder(
        crashReporter: CrashReporter,
        localCrashReporter: LocalCrashReporter,
    ): RequestCorrelationRecorder {
        return crashReporter as? RequestCorrelationRecorder ?: localCrashReporter
    }

    @Provides
    @Singleton
    fun provideAnalyticsTracker(
        @ApplicationContext context: Context,
        firebaseAnalyticsTracker: FirebaseAnalyticsTracker,
        localAnalyticsTracker: LocalAnalyticsTracker,
    ): AnalyticsTracker {
        return if (FirebaseApp.getApps(context).isNotEmpty()) {
            firebaseAnalyticsTracker
        } else {
            localAnalyticsTracker
        }
    }

    @Provides
    @Singleton
    fun provideFeatureFlagProvider(
        @ApplicationContext context: Context,
        firebaseFeatureFlagProvider: FirebaseFeatureFlagProvider,
        localFeatureFlagProvider: LocalFeatureFlagProvider,
    ): FeatureFlagProvider {
        return if (FirebaseApp.getApps(context).isNotEmpty()) {
            firebaseFeatureFlagProvider
        } else {
            localFeatureFlagProvider
        }
    }
}
