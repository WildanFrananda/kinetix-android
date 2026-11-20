package com.kinetix.shop.di

import com.kinetix.shop.core.network.RequestCorrelationRecorder
import com.kinetix.shop.core.network.RequestIdGenerator
import com.kinetix.shop.core.network.UuidRequestIdGenerator
import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.core.time.SystemClock
import com.kinetix.shop.repositories.auth.AuthRepository
import com.kinetix.shop.repositories.auth.AuthRepositoryImpl
import com.kinetix.shop.repositories.auth.SessionRepository
import com.kinetix.shop.repositories.auth.SessionRepositoryImpl
import com.kinetix.shop.repositories.auth.TwoFactorRepository
import com.kinetix.shop.repositories.auth.TwoFactorRepositoryImpl
import com.kinetix.shop.repositories.catalog.CatalogRepository
import com.kinetix.shop.repositories.catalog.CatalogRepositoryImpl
import com.kinetix.shop.repositories.pricing.PricingRepository
import com.kinetix.shop.repositories.pricing.PricingRepositoryImpl
import com.kinetix.shop.repositories.profile.ProfileRepository
import com.kinetix.shop.repositories.profile.ProfileRepositoryImpl
import com.kinetix.shop.repositories.review.ReviewRepository
import com.kinetix.shop.repositories.review.ReviewRepositoryImpl
import com.kinetix.shop.repositories.search.SearchRepository
import com.kinetix.shop.repositories.search.SearchRepositoryImpl
import com.kinetix.shop.services.config.FeatureFlagProvider
import com.kinetix.shop.services.config.FirebaseFeatureFlagProvider
import com.kinetix.shop.services.local.CatalogCacheStore
import com.kinetix.shop.services.local.OutboxStore
import com.kinetix.shop.services.local.PendingTransactionStore
import com.kinetix.shop.services.local.ProfileCacheStore
import com.kinetix.shop.services.local.RoomCatalogCacheStore
import com.kinetix.shop.services.local.RoomOutboxStore
import com.kinetix.shop.services.local.RoomPendingTransactionStore
import com.kinetix.shop.services.local.RoomProfileCacheStore
import com.kinetix.shop.services.notification.DefaultPushMessageHandler
import com.kinetix.shop.services.notification.FcmTokenStore
import com.kinetix.shop.services.notification.PushMessageHandler
import com.kinetix.shop.services.notification.SharedPreferencesFcmTokenStore
import com.kinetix.shop.services.outbox.OutboxSyncScheduler
import com.kinetix.shop.services.outbox.WorkManagerOutboxSyncScheduler
import com.kinetix.shop.services.session.RetrofitTokenRefreshTransport
import com.kinetix.shop.services.session.SessionRenewer
import com.kinetix.shop.services.session.SingleFlightSessionRenewer
import com.kinetix.shop.services.session.TokenRefreshTransport
import com.kinetix.shop.services.storage.KeyStoreSessionCipher
import com.kinetix.shop.services.storage.SessionCipher
import com.kinetix.shop.services.telemetry.AnalyticsTracker
import com.kinetix.shop.services.telemetry.CrashReporter
import com.kinetix.shop.services.telemetry.FirebaseAnalyticsTracker
import com.kinetix.shop.services.telemetry.FirebaseCrashReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBindingsModule {
    @Binds
    @Singleton
    abstract fun bindClock(
        impl: SystemClock,
    ): Clock

    @Binds
    @Singleton
    abstract fun bindRequestIdGenerator(
        impl: UuidRequestIdGenerator,
    ): RequestIdGenerator

    @Binds
    @Singleton
    abstract fun bindRequestCorrelationRecorder(
        impl: FirebaseCrashReporter,
    ): RequestCorrelationRecorder

    @Binds
    @Singleton
    abstract fun bindCrashReporter(
        impl: FirebaseCrashReporter,
    ): CrashReporter

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(
        impl: FirebaseAnalyticsTracker,
    ): AnalyticsTracker

    @Binds
    @Singleton
    abstract fun bindFeatureFlagProvider(
        impl: FirebaseFeatureFlagProvider,
    ): FeatureFlagProvider

    @Binds
    @Singleton
    abstract fun bindFcmTokenStore(
        impl: SharedPreferencesFcmTokenStore,
    ): FcmTokenStore

    @Binds
    @Singleton
    abstract fun bindPushMessageHandler(
        impl: DefaultPushMessageHandler,
    ): PushMessageHandler

    @Binds
    @Singleton
    abstract fun bindCatalogCacheStore(
        impl: RoomCatalogCacheStore,
    ): CatalogCacheStore

    @Binds
    @Singleton
    abstract fun bindProfileCacheStore(
        impl: RoomProfileCacheStore,
    ): ProfileCacheStore

    @Binds
    @Singleton
    abstract fun bindPendingTransactionStore(
        impl: RoomPendingTransactionStore,
    ): PendingTransactionStore

    @Binds
    @Singleton
    abstract fun bindOutboxStore(
        impl: RoomOutboxStore,
    ): OutboxStore

    @Binds
    @Singleton
    abstract fun bindOutboxSyncScheduler(
        impl: WorkManagerOutboxSyncScheduler,
    ): OutboxSyncScheduler

    @Binds
    @Singleton
    abstract fun bindSessionCipher(
        impl: KeyStoreSessionCipher,
    ): SessionCipher

    @Binds
    @Singleton
    abstract fun bindTokenRefreshTransport(
        impl: RetrofitTokenRefreshTransport,
    ): TokenRefreshTransport

    @Binds
    @Singleton
    abstract fun bindSessionRenewer(
        impl: SingleFlightSessionRenewer,
    ): SessionRenewer

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl,
    ): SessionRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTwoFactorRepository(
        impl: TwoFactorRepositoryImpl,
    ): TwoFactorRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl,
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(
        impl: CatalogRepositoryImpl,
    ): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindPricingRepository(
        impl: PricingRepositoryImpl,
    ): PricingRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        impl: SearchRepositoryImpl,
    ): SearchRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(
        impl: ReviewRepositoryImpl,
    ): ReviewRepository
}
